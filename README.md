# Real-Time Traffic Monitoring Dashboard

A serverless dashboard that extends a Redis visitor counter to record visitor
information on every API call and visualize real-time traffic.

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
| Backend | Java + AWS Lambda |
| API | Amazon API Gateway (REST) |
| Data Store | Upstash Redis (serverless) |
| Frontend | React or Vue.js |
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
├── CHECKLIST.md                   # Implementation progression checklist
├── note.md                        # Project idea / plan
├── docs/
│   ├── architecture.md            # System design & data flow
│   ├── api.md                     # REST API specification
│   ├── redis-data-model.md        # Redis keys & data structures
│   └── deployment.md              # Deployment guide (Terraform)
├── infra/                         # Terraform (IaC) — Lambda, API GW, S3, EventBridge
│   ├── providers.tf
│   ├── lambda.tf
│   ├── api_gateway.tf
│   └── ...
├── backend/                       # Java Lambda (Maven)
│   ├── pom.xml
│   └── src/main/java/...
├── frontend/                      # React / Vue dashboard
│   ├── package.json
│   └── src/...
└── .env                           # Local environment variables (git-ignored)
```

## Documentation

- [Implementation Checklist](CHECKLIST.md)
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

## Difficulty

⭐⭐⭐ (Intermediate)

## Learning Points

- State management with Redis
- Time-series data aggregation
- Frontend-backend integration
