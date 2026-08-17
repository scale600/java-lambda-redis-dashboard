# <img src="assets/java-logo.svg" alt="Java" width="40" height="40"> Real-Time Traffic Monitoring Dashboard

A **Java 17 + AWS Lambda** serverless dashboard that records visitor traffic into
Upstash Redis and visualizes real-time analytics. The backend is written entirely
in Java; a React dashboard renders the data from CloudFront + S3.

## Java Backend

The core of this project is a **Java 17** backend deployed as three AWS Lambda
functions, packaged into a single Maven uber-JAR.

| Class | Responsibility |
| --- | --- |
| `RecordVisitHandler` | `POST /visit` — records a visit with a 4-command Redis write |
| `GetStatsHandler` | `GET /health` + `GET /stats/{proxy+}` — reads aggregated stats |
| `AggregateStatsHandler` | Daily rollup triggered by EventBridge (00:00 UTC) |
| `RedisClient` | Minimal REST wrapper for Upstash Redis (no Redis TCP client) |
| `Api` | Shared CORS / error-response helpers |

```java
// RecordVisitHandler — one visit = four Redis commands
redis.hincrby("counter:hour:" + day, hour, 1);
redis.hincrby("paths:day:" + day, path, 1);
redis.lpush("visits:recent", MAPPER.writeValueAsString(visit));
redis.pfadd("visitors:unique:day:" + day, ip + "|" + userAgent);
```

**Java techniques used:**

- `java.net.http.HttpClient` (Java 11+) — HTTP client for the Upstash REST API
- Jackson `ObjectMapper` — JSON parse/serialize
- `java.time` (`LocalDate`, `ZonedDateTime`, `ZoneOffset`) — UTC time bucketing
- `WeekFields.ISO` — ISO week numbering for weekly aggregation
- `RequestHandler` functional interface — AWS Lambda entry point
- Package-private constructor + Mockito — dependency injection for unit tests

See [docs/backend.md](docs/backend.md) for the full implementation.

## Features

- Live visitor count (large number card)
- Hourly / daily visitor trends (line chart)
- Views by access path / URL (bar chart or table)
- Recent visit log (list)
- Fully serverless
- Served at https://java-redis.techcloudup.com (Cloudflare DNS)

## Tech Stack

| Area | Technology |
| --- | --- |
| **Backend** | **Java 17 + AWS Lambda** |
| API | Amazon API Gateway (REST) |
| Data Store | Upstash Redis (serverless) |
| Build | Maven (shade uber-JAR) |
| Frontend | React |
| Visualization | Chart.js |
| Hosting | CloudFront + S3 (custom domain via Cloudflare) |
| IaC | Terraform |

## Architecture

```
Visitor ──▶ API Gateway ──▶ Lambda (record) ──▶ Upstash Redis
                                              ▲
CloudWatch Events ──▶ Lambda (aggregate) ─────┘

Browser ──▶ Cloudflare (DNS) ──▶ CloudFront ──▶ S3 (dashboard)
Browser ──▶ API Gateway ──▶ Lambda (stats) ──▶ Upstash Redis
```

See [docs/architecture.md](docs/architecture.md) for the full system design and
data flow.

## Project Structure

```
.
├── README.md
├── docs/
│   ├── architecture.md            # System design & data flow
│   ├── api.md                     # REST API specification
│   ├── redis-data-model.md        # Redis keys & data structures
│   ├── backend.md                 # Java Lambda backend implementation
│   └── deployment.md              # Deployment guide (Terraform)
├── infra/                         # Terraform (IaC) — Lambda, API GW, S3, EventBridge
│   ├── providers.tf
│   ├── lambda.tf
│   ├── api_gateway.tf
│   └── ...
├── backend/                       # Java Lambda (Maven) — core
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/dashboard/
│       │   ├── RecordVisitHandler.java
│       │   ├── GetStatsHandler.java
│       │   ├── AggregateStatsHandler.java
│       │   ├── RedisClient.java
│       │   └── Api.java
│       └── test/java/com/example/dashboard/
│           ├── RecordVisitHandlerTest.java
│           ├── GetStatsHandlerTest.java
│           └── AggregateStatsHandlerTest.java
├── frontend/                      # React dashboard
│   ├── package.json
│   └── src/...
└── .env                           # Local environment variables (git-ignored)
```

## Documentation

- [Backend](docs/backend.md)
- [Architecture](docs/architecture.md)
- [API Specification](docs/api.md)
- [Redis Data Model](docs/redis-data-model.md)
- [Deployment Guide](docs/deployment.md)

## Prerequisites

- AWS account
- Upstash account (free Redis database)
- Java 17+ and Maven
- Node.js 18+ (for the frontend)

## Environment Variables

| Variable | Description |
| --- | --- |
| `UPSTASH_REDIS_REST_URL` | Upstash Redis REST endpoint URL |
| `UPSTASH_REDIS_REST_TOKEN` | Upstash Redis REST auth token |

## Getting Started

1. Create an Upstash Redis database and copy the REST URL + token into `.env`.
2. Build and deploy the backend (see [deployment.md](docs/deployment.md)).
3. Build and deploy the frontend to S3.
4. Embed the tracking snippet on the pages you want to monitor.

## Learning Points

- **Java 17 + AWS Lambda** — `RequestHandler` interface, `APIGatewayProxyRequestEvent`
- **`java.net.http.HttpClient`** — modern HTTP client for REST integration
- **Jackson** — JSON parse/serialize with `ObjectMapper`
- **`java.time` API** — UTC time bucketing, ISO week numbering (`WeekFields`)
- **Maven** — shade plugin producing a deployable uber-JAR
- **Testing** — JUnit 5 + Mockito with constructor injection
- State management with Redis
- Time-series data aggregation
- Frontend-backend integration
