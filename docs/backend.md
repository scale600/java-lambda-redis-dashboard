# Backend (Java + AWS Lambda)

The backend is a set of three AWS Lambda functions written in Java 17 that
record visitor traffic into Upstash Redis and serve aggregated statistics to the
dashboard. All three functions are packaged into a single shaded uber-JAR and
deployed by Terraform.

## Package Structure

```
backend/src/main/java/com/example/dashboard/
├── RecordVisitHandler.java    # POST /visit — records a visit (4-command write)
├── GetStatsHandler.java       # GET /health + /stats/{proxy+} — reads stats
├── AggregateStatsHandler.java # scheduled daily rollup (EventBridge)
├── RedisClient.java           # minimal REST wrapper for Upstash Redis
└── Api.java                   # shared response helpers (CORS, error shape)

backend/src/test/java/com/example/dashboard/
├── RecordVisitHandlerTest.java
├── GetStatsHandlerTest.java
└── AggregateStatsHandlerTest.java
```

Each handler implements
`com.amazonaws.services.lambda.runtime.RequestHandler` and is referenced from
Terraform by its fully-qualified class name (e.g.
`com.example.dashboard.RecordVisitHandler::handleRequest`).

## Build

```bash
cd backend
mvn clean package
```

This runs the unit tests and produces the shaded uber-JAR at
`backend/target/backend-1.0.jar`, which Terraform uploads via
`var.lambda_jar_path`. The shade plugin bundles Jackson (and its transitive
dependencies) and strips `META-INF/*.SF|DSA|RSA` signature files, which otherwise
cause a "SecurityException: Invalid signature file" at runtime.

| Dependency | Version | Purpose |
| --- | --- | --- |
| `aws-lambda-java-core` | 1.2.3 | `RequestHandler` interface, `Context` |
| `aws-lambda-java-events` | 3.11.4 | `APIGatewayProxyRequestEvent` / `ResponseEvent` |
| `jackson-databind` | 2.17.2 | JSON parse / serialize |
| `junit-jupiter` | 5.10.2 | unit tests (test scope) |
| `mockito-core` | 5.11.0 | mock `RedisClient` (test scope) |

## RedisClient

`RedisClient` is a minimal REST client for Upstash Redis. It does **not** use a
Redis TCP client — instead it POSTs each command to the Upstash REST endpoint.

- Constructor `RedisClient(baseUrl, token)` validates both arguments;
  `RedisClient.fromEnv()` reads `UPSTASH_REDIS_REST_URL` and
  `UPSTASH_REDIS_REST_TOKEN`.
- `command(List<String> args)` serializes the args as a JSON array, sends
  `POST https://<endpoint>.upstash.io` with `Authorization: Bearer <token>`,
  and returns the parsed `result` field. An `{"error": ...}` response is raised
  as a `RuntimeException`.
- Uses a shared `java.net.http.HttpClient` with a 5s connect timeout and a 10s
  per-request timeout.

Wrapped command groups:

| Group | Methods |
| --- | --- |
| String | `get`, `set`, `incrby`, `expire` |
| Hash | `hincrby`, `hgetall` |
| List | `lpush`, `ltrim`, `lrange` |
| HyperLogLog | `pfadd`, `pfcount` |

`hgetall` returns a `LinkedHashMap<String,String>` (insertion-ordered); `lrange`
returns a `List<String>`.

## Api (shared response helpers)

`Api` is a package-private helper that builds `APIGatewayProxyResponseEvent`s
with consistent CORS headers and a uniform error shape.

- `CORS_ORIGIN = "https://java-redis.techcloudup.com"` is added to every
  response as `Access-Control-Allow-Origin` (plus `Content-Type:
  application/json`).
- Helpers: `ok(json)` → 200, `noContent()` → 204, `error(status, code, message)`
  → JSON error body.

Error responses use this shape:

```json
{ "error": { "code": "INTERNAL", "message": "..." } }
```

## RecordVisitHandler — `POST /visit`

Handles the tracking beacon. The frontend sends the payload via
`navigator.sendBeacon`, which posts with `Content-Type: text/plain`, so the body
is parsed as JSON **regardless of Content-Type**.

**Input extraction**

- `path` — from the JSON body (defaults to `/`).
- `userAgent` — from the JSON body (defaults to `""`).
- `ip` — resolved server-side, never trusted from the client:
  1. `X-Forwarded-For` header (first entry),
  2. `requestContext.identity.sourceIp`,
  3. `"unknown"` as a fallback.

**Write path (4 commands per visit)**

```redis
HINCRBY counter:hour:{yyyyMMdd} {HH} 1
HINCRBY paths:day:{yyyyMMdd} {path} 1
LPUSH  visits:recent {json}
PFADD  visitors:unique:day:{yyyyMMdd} {ip|ua}
```

The recent-visit JSON payload is `{"time","path","ip","ua"}`. The handler
returns `204 No Content` on success.

## GetStatsHandler — `GET /health` + `/stats/{proxy+}`

One handler serves both the health check and the four stats routes by switching
on the `proxy` path parameter.

| Route | Proxy | Redis reads | Response |
| --- | --- | --- | --- |
| `GET /health` | — | — | `{"status":"ok"}` |
| `GET /stats/overview` | `overview` | `GET stats:total`, `HGETALL counter:hour:{today}`, `PFCOUNT visitors:unique:day:{today}` | `total`, `today`, `uniqueToday`, `lastUpdated` |
| `GET /stats/timeseries` | `timeseries` | `HGETALL counter:hour:{today}` (hour), or `GET stats:day:*` (day) | `granularity`, `series[{timestamp,count}]` |
| `GET /stats/paths` | `paths` | `HGETALL paths:day:{today}` | `paths[{path,count}]` (sorted desc, top N) |
| `GET /stats/recent` | `recent` | `LRANGE visits:recent 0 {limit-1}` | `visits[{time,path,ip,ua}]` |

Details:

- `overview.today` is the sum of the current day's 24 hourly hash fields; it is
  computed live (not from `stats:total`, which only updates daily).
- `timeseries` with `granularity=hour` always returns 24 buckets (00–23);
  `granularity=day` returns `limit` daily buckets. `limit` defaults to 24.
- `paths` sorts by count descending and truncates to `limit` (default 20).
- `recent` re-serializes each stored JSON string; malformed entries are skipped.

## AggregateStatsHandler — daily rollup

Triggered once a day (00:00 UTC) by EventBridge. It compacts the **previous**
(completed) day's hourly counters so raw keys can expire.

```redis
INCRBY stats:total {yesterday-total}
SET    stats:day:{yyyyMMdd} {yesterday-total}   EX 30d
HINCRBY stats:week:{yyyy'W'ww} {yyyyMMdd} {yesterday-total}  EX 30d
LTRIM  visits:recent 0 99
EXPIRE counter:hour:{yyyyMMdd} 7d
EXPIRE paths:day:{yyyyMMdd} 7d
EXPIRE visitors:unique:day:{yyyyMMdd} 7d
```

The weekly key uses ISO week numbering (`WeekFields.ISO`), e.g. `stats:week:2026W33`.
It returns `"OK"` and logs `Aggregated {day}: {n} visits`.

> `stats:total` only reflects *completed* days. Visits from the current day are
> visible immediately via the live hourly counters (`/stats/overview` → `today`)
> but do not land in `stats:total` until the next daily run.

## Handler → Route Mapping

| Handler | Trigger | Lambda handler string |
| --- | --- | --- |
| `RecordVisitHandler` | API Gateway `POST /visit` | `com.example.dashboard.RecordVisitHandler::handleRequest` |
| `GetStatsHandler` | API Gateway `GET /health`, `GET /stats/{proxy+}` | `com.example.dashboard.GetStatsHandler::handleRequest` |
| `AggregateStatsHandler` | EventBridge daily schedule | `com.example.dashboard.AggregateStatsHandler::handleRequest` |

## Lambda Configuration

All three functions share the same runtime profile (from `infra/lambda.tf`):

| Setting | Value |
| --- | --- |
| Runtime | `java17` |
| Architecture | `arm64` |
| Memory | 512 MB |
| Timeout | 30s (record/get), 60s (aggregate) |
| SnapStart | `PublishedVersions` |
| Alias | `live` (required — SnapStart only applies to published versions) |
| Environment | `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` |

API Gateway and EventBridge invoke the functions through their `live` aliases,
so a redeploy publishes a new version and atomically repoints the alias.

## Error Handling

- Every handler wraps its logic in a single `try/catch` and returns
  `500 INTERNAL` (with a logged message) on failure rather than throwing.
- `RedisClient` raises `RuntimeException` on REST/transport errors; handlers
  surface it as a 500.
- `GetStatsHandler` returns `404 NOT_FOUND` for an unknown `proxy` value.

## Testing

Unit tests use JUnit 5 + Mockito and mock `RedisClient` (each handler exposes a
package-private constructor taking a `RedisClient` for injection).

- `RecordVisitHandlerTest` — body parsing (JSON regardless of Content-Type),
  IP resolution fallbacks, and the four write commands.
- `GetStatsHandlerTest` — route switching, hourly/day series assembly, path
  ranking, recent-visit parsing, and error codes.
- `AggregateStatsHandlerTest` — yesterday rollup, weekly bucketing, `stats:total`
  increment, and `LTRIM`/`EXPIRE` calls.

Run them with:

```bash
cd backend
mvn test
```
