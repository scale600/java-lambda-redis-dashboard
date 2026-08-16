# Real-Time Traffic Monitoring Dashboard

> 📊 A real-time traffic analytics screen extending a Redis visitor counter

## Overview

A serverless dashboard project that extends a visitor counter to store visitor
information in Upstash Redis on every API call and visualize real-time traffic.

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend | Java + AWS Lambda |
| API | Amazon API Gateway (REST) |
| Data Store | Upstash Redis (serverless) |
| Frontend | React or Vue.js |
| Visualization | Chart.js |
| Hosting | Amazon S3 (static files) |

## Dashboard Components

- Live visitor count (large number card)
- Hourly/daily visitor trends (line chart)
- Views by access path (URL) (table or bar chart)
- Recent visit log (list)

## Backend (Java + Lambda)

- Store visitor info (IP, User-Agent, access path, time) in Upstash Redis on each API call
- Lambda function to aggregate daily/weekly statistics
- Serve as a RESTful API via API Gateway

## Frontend (Web Dashboard)

- Built with React or Vue.js
- Visualized with Chart.js or similar charting libraries
- Hosted as static files in an S3 bucket

## Free Tier Usage

| Service | Free Allowance | Note |
| --- | --- | --- |
| AWS Lambda | 1M requests + 400K GB-seconds per month | Always free |
| API Gateway | 1M API calls per month | First 12 months |
| Upstash Redis | 500K commands per month, 256MB storage | Free tier |
| S3 | 5GB storage, 20K GET requests per month | First 12 months |

## Difficulty

⭐⭐⭐ (Intermediate)

## Learning Points

- State management with Redis
- Time-series data aggregation
- Frontend-backend integration
