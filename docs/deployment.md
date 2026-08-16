# Deployment Guide

Infrastructure is managed with **Terraform** (`infra/`). The backend (Java
Lambda), API Gateway, EventBridge schedule, and frontend S3 bucket are all
provisioned with `terraform apply`.

## Prerequisites

- AWS account
- Upstash account
- Cloudflare account (manages `techcloudup.com` DNS)
- Java 17+ and Maven
- Node.js 18+ and npm
- Terraform 1.5+
- AWS CLI configured with credentials

## 1. Upstash Redis

1. Create a database in the [Upstash Console](https://console.upstash.com).
2. Choose a region close to your Lambda region.
3. Note the **REST URL** and **REST token**.

## 2. Backend (Java + Lambda)

### Build the JAR

```bash
cd backend
mvn clean package
```

This produces `target/backend-1.0.jar`, which Terraform references.

### Configure Terraform

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars: set upstash_redis_rest_url / upstash_redis_rest_token
```

### Deploy

```bash
cd infra
terraform init
terraform plan
```

The ACM certificate is DNS-validated, and `techcloudup.com` DNS lives on
Cloudflare (outside Terraform), so the first deploy is done in two steps:

```bash
# 1) Create the certificate only (skips validation + CloudFront)
terraform apply -target=aws_acm_certificate.frontend

# 2) Add the validation record(s) to Cloudflare (grey cloud)
terraform output acm_validation_records

# 3) Full apply — validates the cert, then creates everything else
terraform apply
```

Terraform provisions:

- 3 Lambda functions (Java 17, **arm64**, **SnapStart**) with `live` aliases
- API Gateway REST API: `POST /visit`, `GET /health`, `/stats/{proxy+}`
- EventBridge daily schedule → `AggregateStatsFunction`
- S3 bucket (private) + CloudFront distribution + ACM certificate for
  `java-redis.techcloudup.com`

Key outputs:

```bash
terraform output api_base_url           # API Gateway base URL
terraform output cloudfront_domain      # CNAME target for Cloudflare
```

## 3. Frontend (React / Vue)

### Build

```bash
cd frontend
npm install
npm run build
```

Configure the API base URL (from `terraform output api_base_url`) in the
frontend build, e.g.
`VITE_API_BASE_URL=https://<api-id>.execute-api.<region>.amazonaws.com/prod`.

### Deploy to S3

```bash
cd infra
terraform output frontend_bucket_name
cd ../frontend
aws s3 sync dist/ s3://<bucket>/
```

The bucket is private and served through CloudFront. After a redeploy,
CloudFront may serve cached files for up to 5 minutes (default TTL); run a
CloudFront invalidation to see changes immediately.

### Custom Domain (Cloudflare)

The dashboard is served at `java-redis.techcloudup.com`; DNS for
`techcloudup.com` is on Cloudflare.

After the full `terraform apply` completes, add a CNAME record in Cloudflare:

- `java-redis` → the value of `terraform output cloudfront_domain`, with proxy
  enabled (orange cloud).

(Validation records are handled during the first deploy — see the "Deploy"
section above.)

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


