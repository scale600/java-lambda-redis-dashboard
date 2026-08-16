# Deployment Guide

## Prerequisites

- AWS account
- Upstash account
- Java 17+ and Maven
- Node.js 18+ and npm
- AWS CLI configured with credentials

## 1. Upstash Redis

1. Create a database in the [Upstash Console](https://console.upstash.com).
2. Choose a region close to your Lambda region.
3. Note the **REST URL** and **REST token**.
4. Add them to `.env` (and later to Lambda environment variables):

```bash
UPSTASH_REDIS_REST_URL=https://<db>.upstash.io
UPSTASH_REDIS_REST_TOKEN=<token>
```

## 2. Backend (Java + Lambda)

### Build

```bash
cd backend
mvn clean package
```

The build produces an uber-JAR (`target/backend-1.0.jar`) suitable for Lambda.

### Create Lambda functions

For each handler, create a function in the AWS Console (or via SAM/CloudFormation):

| Function | Handler | Environment |
| --- | --- | --- |
| `RecordVisitFunction` | `com.example.dashboard.RecordVisitHandler` | `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` |
| `GetStatsFunction` | `com.example.dashboard.GetStatsHandler` | `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` |
| `AggregateStatsFunction` | `com.example.dashboard.AggregateStatsHandler` | `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` |

- Runtime: **Java 17**.
- Architecture: **arm64** (Graviton2) — ~20% cheaper GB-seconds and faster
  cold starts than x86.
- Enable **SnapStart** (free, Java-only) to cut cold starts from ~2–5s to <100ms.
- Set the environment variables on each function.
- Attach a minimal IAM role (no extra AWS services needed beyond Lambda logging).

### API Gateway

1. Create a REST API.
2. Define routes:

| Method | Path | Lambda |
| --- | --- | --- |
| `POST` | `/visit` | `RecordVisitFunction` |
| `GET` | `/health` | `GetStatsFunction` |
| `GET` | `/stats/overview` | `GetStatsFunction` |
| `GET` | `/stats/timeseries` | `GetStatsFunction` |
| `GET` | `/stats/paths` | `GetStatsFunction` |
| `GET` | `/stats/recent` | `GetStatsFunction` |

3. Enable CORS for the dashboard origin.
4. Deploy to a stage (e.g. `prod`).

### Aggregation schedule

Create an EventBridge (CloudWatch Events) rule that triggers
`AggregateStatsFunction` once a day.

## 3. Frontend (React / Vue)

### Build

```bash
cd frontend
npm install
# set the API base URL in a .env file
npm run build
```

Configure the API Gateway base URL in the frontend build (e.g.
`VITE_API_BASE_URL=https://<api-id>.execute-api.<region>.amazonaws.com/prod`).

### Deploy to S3

1. Create an S3 bucket with static website hosting enabled.
2. Upload the contents of `frontend/dist/` (or `build/`).
3. Optionally add a CloudFront distribution in front of the bucket.

## 4. Embed the Tracking Snippet

Add a small script to each page to monitor:

```html
<script>
  (function () {
    if (!navigator.sendBeacon) return;
    var payload = {
      path: location.pathname,
      referer: document.referrer || null,
      userAgent: navigator.userAgent
    };
    navigator.sendBeacon(
      "https://<api-id>.execute-api.<region>.amazonaws.com/prod/visit",
      JSON.stringify(payload)
    );
  })();
</script>
```

## 5. Verification

1. Hit a tracked page and confirm `POST /visit` returns `204`.
2. Open the dashboard and confirm live counts, trends, paths, and recent visits.
3. Inspect CloudWatch Logs for the Lambda functions for errors.
4. Confirm the daily aggregation job runs and weekly keys appear.

## Cost Optimization

- **arm64 (Graviton2)** — ~20% cheaper GB-seconds and faster cold starts.
- **SnapStart** (Java-only, free) — cuts cold starts from ~2–5s to <100ms.
- **4 commands per visit** — see [redis-data-model.md](redis-data-model.md).
- **Polling interval ≥ 60s** on the dashboard to stay within the Upstash
  command quota.

## Free Tier Reference

| Service | Free Allowance | Note |
| --- | --- | --- |
| AWS Lambda | 1M requests + 400K GB-seconds per month | Always free |
| API Gateway | 1M API calls per month | First 12 months |
| Upstash Redis | 500K commands per month, 256MB storage | Free tier |
| S3 | 5GB storage, 20K GET requests per month | First 12 months |
