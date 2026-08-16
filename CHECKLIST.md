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
- [ ] Terraform 1.5+ installed
- [ ] AWS CLI configured

## Phase 1 — Backend (Java + Lambda)

- [ ] `backend/pom.xml` (Maven) with `aws-lambda-java-core`, `aws-lambda-java-events`, JSON library, HTTP client
- [ ] Handlers in package `com.example.dashboard` (matches Terraform `handler` values)
- [ ] Redis client wrapper for the Upstash REST API
- [ ] `RecordVisitHandler` — `POST /visit`, 4-command write path; parse the body as JSON regardless of Content-Type (`sendBeacon` sends `text/plain`)
- [ ] `GetStatsHandler` — `GET /health` + `/stats/{proxy+}` (route `overview`/`timeseries`/`paths`/`recent` via the `proxy` path param); return `Access-Control-Allow-Origin`
- [ ] `AggregateStatsHandler` — daily rollup of the previous day (`stats:total`, `stats:day:*`, `LTRIM visits:recent`)
- [ ] Unit tests for all handlers
- [ ] `mvn clean package` produces the uber-JAR

## Phase 2 — Backend Deployment (Terraform)

- [ ] `cp infra/terraform.tfvars.example infra/terraform.tfvars` and set Upstash URL + token
- [ ] `terraform init`
- [ ] `terraform plan` (review changes)
- [ ] `terraform apply -target=aws_acm_certificate.frontend` (create cert only)
- [ ] `terraform output acm_validation_records` → add CNAME record(s) to Cloudflare (grey cloud)
- [ ] `terraform apply` — validates cert + provisions 3 Lambdas (arm64 + SnapStart), API Gateway, EventBridge, S3 bucket, CloudFront
- [ ] Note `terraform output api_base_url` for the frontend

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

## Phase 4 — Frontend Deployment (CloudFront + S3)

- [ ] Get bucket name: `terraform output frontend_bucket_name`
- [ ] Upload `frontend/dist/` (or `build/`) via `aws s3 sync`
- [ ] Add CNAME `java-redis` → `terraform output cloudfront_domain` (proxied) in Cloudflare

## Phase 5 — Tracking Integration

- [ ] Add the `sendBeacon` tracking snippet to target pages (see [deployment.md](docs/deployment.md))
- [ ] Verify `POST /visit` returns `204`

## Phase 6 — Verification

- [ ] Dashboard renders all four widgets with live data
- [ ] CloudWatch Logs show no errors across the three functions
- [ ] Daily aggregation produces `stats:day:*` / `stats:week:*` keys


