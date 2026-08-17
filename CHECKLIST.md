# Implementation Checklist

Progression order for building the Real-Time Traffic Monitoring Dashboard.

> Status: All phases complete. The dashboard is live and verified end-to-end at
> https://java-redis.techcloudup.com.

## Phase 0 — Prerequisites

- [x] AWS account with an IAM user (Lambda, API Gateway, S3, EventBridge, CloudWatch)
- [x] Upstash account + Redis database created (copy REST URL + token)
- [x] `.env` populated: `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN`
- [x] Java 17+ and Maven installed
- [x] Node.js 18+ and npm installed
- [x] Terraform 1.5+ installed
- [x] AWS CLI configured

## Phase 1 — Backend (Java + Lambda)

- [x] `backend/pom.xml` (Maven) with `aws-lambda-java-core`, `aws-lambda-java-events`, JSON library, HTTP client
- [x] Handlers in package `com.example.dashboard` (matches Terraform `handler` values)
- [x] Redis client wrapper for the Upstash REST API
- [x] `RecordVisitHandler` — `POST /visit`, 4-command write path; parse the body as JSON regardless of Content-Type (`sendBeacon` sends `text/plain`)
- [x] `GetStatsHandler` — `GET /health` + `/stats/{proxy+}` (route `overview`/`timeseries`/`paths`/`recent` via the `proxy` path param); return `Access-Control-Allow-Origin`
- [x] `AggregateStatsHandler` — daily rollup of the previous day (`stats:total`, `stats:day:*`, `LTRIM visits:recent`)
- [x] Unit tests for all handlers
- [x] `mvn clean package` produces the uber-JAR

## Phase 2 — Backend Deployment (Terraform)

- [x] `cp infra/terraform.tfvars.example infra/terraform.tfvars` and set Upstash URL + token
- [x] `terraform init`
- [x] `terraform plan` (review changes)
- [x] `terraform apply -target=aws_acm_certificate.frontend` (create cert only)
- [x] `terraform output acm_validation_records` → add CNAME record(s) to Cloudflare (grey cloud)
- [x] `terraform apply` — validates cert + provisions 3 Lambdas (arm64 + SnapStart), API Gateway, EventBridge, S3 bucket, CloudFront
- [x] Note `terraform output api_base_url` for the frontend

## Phase 3 — Frontend (React / Vue)

- [x] Scaffold `frontend/` (React or Vue)
- [x] Configure `VITE_API_BASE_URL` to the API Gateway base URL
- [x] Install Chart.js
- [x] Visitor count card (from `/stats/overview`)
- [x] Line chart (from `/stats/timeseries`)
- [x] Path views bar chart / table (from `/stats/paths`)
- [x] Recent visits list (from `/stats/recent`)
- [x] Polling loop at ≥ 60s intervals
- [x] `npm run build`

## Phase 4 — Frontend Deployment (CloudFront + S3)

- [x] Get bucket name: `terraform output frontend_bucket_name`
- [x] Upload `frontend/dist/` (or `build/`) via `aws s3 sync`
- [x] Add CNAME `java-redis` → `terraform output cloudfront_domain` (proxied) in Cloudflare

## Phase 5 — Tracking Integration

- [x] Add the `sendBeacon` tracking snippet to target pages (see [deployment.md](docs/deployment.md))
- [x] Verify `POST /visit` returns `204`

## Phase 6 — Verification

- [x] Dashboard renders all four widgets with live data
- [x] CloudWatch Logs show no errors across the three functions
- [x] Daily aggregation produces `stats:day:*` / `stats:week:*` keys


