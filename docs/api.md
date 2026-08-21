# API Specification

Base URL: `https://<api-id>.execute-api.<region>.amazonaws.com/<stage>`

All responses are `application/json`. Timestamps are ISO 8601 UTC.

## Auth

- Tracking endpoints (`POST /visit`) are public.
- Stats endpoints may optionally be protected by an API Gateway API key / usage
  plan.

## CORS

The dashboard is served from `https://java-redis.techcloudup.com`. Because the
API Gateway integration is `AWS_PROXY`, each Lambda handler must return the
`Access-Control-Allow-Origin: https://java-redis.techcloudup.com` header and
handle `OPTIONS` preflight requests.

## Error Format

All non-2xx responses follow this shape:

```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "Human-readable description"
  }
}
```

| Status | Meaning |
| --- | --- |
| 400 | Invalid request parameters |
| 404 | Unknown path |
| 500 | Internal error |

## Endpoints

### `POST /visit`

Records a single visit.

**Request body**

```json
{
  "path": "/products/42",
  "site": "example.com",
  "referer": "https://example.com/home",
  "userAgent": "Mozilla/5.0 ..."
}
```

- `path` (string, required) — visited path/URL.
- `site` (string, optional) — hostname of the site the snippet is embedded on
  (e.g. `example.com`). When present, it is prepended to `path` (e.g.
  `example.com/products/42`) for per-path aggregation.
- `referer` (string, optional) — referring page.
- `userAgent` (string, optional) — client User-Agent. The server derives IP.

**Response**

- `204 No Content`

### `GET /health`

Liveness check.

**Response** — `200`

```json
{ "status": "ok" }
```

### `GET /stats/overview`

Current aggregate totals.

**Response** — `200`

```json
{
  "total": 42193,
  "today": 1284,
  "uniqueToday": 731,
  "lastUpdated": "2026-08-16T14:05:00Z"
}
```

### `GET /stats/timeseries`

Visitor trend for the line chart.

**Query parameters**

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| `granularity` | string | `hour` | `hour` or `day` |
| `limit` | integer | `24` | Number of buckets to return |

**Response** — `200`

```json
{
  "granularity": "hour",
  "series": [
    { "timestamp": "2026-08-16T00:00:00Z", "count": 42 },
    { "timestamp": "2026-08-16T01:00:00Z", "count": 37 }
  ]
}
```

### `GET /stats/paths`

Views by access path (site hostname + path) for the bar chart or table.

**Query parameters**

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| `limit` | integer | `20` | Number of top paths to return |

**Response** — `200`

```json
{
  "paths": [
    { "path": "example.com/", "count": 812 },
    { "path": "example.com/products/42", "count": 194 }
  ]
}
```

### `GET /stats/recent`

Most recent visits.

**Query parameters**

| Param | Type | Default | Description |
| --- | --- | --- | --- |
| `limit` | integer | `20` | Number of entries to return |

**Response** — `200`

```json
{
  "visits": [
    {
      "time": "2026-08-16T14:04:12Z",
      "path": "example.com/products/42",
      "ip": "203.0.113.7",
      "userAgent": "Mozilla/5.0 ..."
    }
  ]
}
```


