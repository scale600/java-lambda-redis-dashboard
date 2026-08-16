# Implementation Checklist

Progression order for building the Real-Time Traffic Monitoring Dashboard.

> Status: documentation is complete. All code and deployment steps below are
> pending.

## Phase 0 — Prerequisites

- [ ] AWS account with an IAM user (Lambda, API Gateway, S3, EventBridge, CloudWatch)
- [ ] Upstash account + Redis database created (copy REST URL + token)
- [ ] `.env` populated: `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN`
- [ ] Java 17+ and Maven installed
- [ ] Node.js 18+ and npm installed
- [ ] AWS CLI configured

## Phase 1 — Backend (Java + Lambda)

- [ ] `backend/pom.xml` (Maven) with `aws-lambda-java-core` + JSON library + HTTP client
- [ ] Redis client wrapper for the Upstash REST API
- [ ] `RecordVisitHandler` — `POST /visit`, 4-command write path (see [redis-data-model.md](docs/redis-data-model.md))
- [ ] `GetStatsHandler` — `GET /health`, `/stats/overview`, `/stats/timeseries`, `/stats/paths`, `/stats/recent`
- [ ] `AggregateStatsHandler` — daily rollup (`stats:total`, `stats:day:*`, `LTRIM visits:recent`)
- [ ] Unit tests for all handlers
- [ ] `mvn clean package` produces the uber-JAR

## Phase 2 — Backend Deployment (AWS)

- [ ] Create `RecordVisitFunction` (Java 17, arm64, SnapStart)
- [ ] Create `GetStatsFunction` (Java 17, arm64, SnapStart)
- [ ] Create `AggregateStatsFunction` (Java 17, arm64, SnapStart)
- [ ] Set `UPSTASH_REDIS_REST_URL` / `UPSTASH_REDIS_REST_TOKEN` on each function
- [ ] Create API Gateway REST API with 6 routes (see [api.md](docs/api.md))
- [ ] Enable CORS for the dashboard origin
- [ ] Deploy to a stage (`prod`)
- [ ] EventBridge (CloudWatch Events) rule → daily `AggregateStatsFunction`

## Phase 3 — Frontend (React / Vue)

- [ ] Scaffold `frontend/` (React or Vue)
- [ ] Configure `VITE_API_BASE_URL` to the API Gateway base URL
- [ ] Install Chart.js
- [ ] Visitor count card (from `/stats/overview`)
- [ ] Line chart (from `/stats/timeseries`)
- [ ] Path views bar chart / table (from `/stats/paths`)
- [ ] Recent visits list (from `/stats/recent`)
- [ ] Polling loop at ≥ 60s intervals
- [ ] `npm run build`

## Phase 4 — Frontend Deployment (S3)

- [ ] Create an S3 bucket with static website hosting enabled
- [ ] Upload `frontend/dist/` (or `build/`)
- [ ] (Optional) CloudFront distribution in front of the bucket

## Phase 5 — Tracking Integration

- [ ] Add the `sendBeacon` tracking snippet to target pages (see [deployment.md](docs/deployment.md))
- [ ] Verify `POST /visit` returns `204`

## Phase 6 — Verification & Free-Tier Check

- [ ] Dashboard renders all four widgets with live data
- [ ] CloudWatch Logs show no errors across the three functions
- [ ] Daily aggregation produces `stats:day:*` / `stats:week:*` keys
- [ ] Upstash command usage stays under 500K/month (polling ≥ 60s)
- [ ] Lambda stays within the free tier (requests + GB-seconds)

## Cost Guardrails

- Record writes: **4 Redis commands per visit** (maximum)
- Dashboard polling: **≥ 60s**
- Lambdas: **arm64 + SnapStart** enabled
- Reminder: API Gateway and S3 free tiers expire after **12 months**
