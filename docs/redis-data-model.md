# Redis Data Model

Upstash Redis is a serverless Redis accessed over REST (and TCP). All data is
stored in a single logical database.

## Key Naming Convention

```
<domain>:<metric>:<granularity>:<bucket>
```

| Part | Example | Description |
| --- | --- | --- |
| `domain` | `counter`, `paths`, `visits`, `visitors`, `stats` | What the key tracks |
| `metric` | `hour`, `recent`, `unique`, `total`, `day`, `week` | Metric / aggregation level |
| `granularity` | `day`, `hour`, `week` | Time bucket granularity (if any) |
| `bucket` | `20260816`, `2026081614`, `2026W33` | Time bucket identifier |

## Write Path (4 commands per visit)

A visit performs exactly 4 commands:

```redis
HINCRBY counter:hour:20260816 14 1
HINCRBY paths:day:20260816 "/products/42" 1
LPUSH visits:recent '{"time":"...","path":"/products/42","ip":"...","ua":"..."}'
PFADD visitors:unique:day:20260816 "203.0.113.7|Mozilla/5.0"
```

- Daily totals are derived by summing the 24 hourly fields (one `HGETALL`).
- `visits:recent` is trimmed (`LTRIM 0 99`) periodically by the aggregation
  Lambda, not on every visit.

## Data Structures

| Key | Type | Purpose | Written | TTL |
| --- | --- | --- | --- | --- |
| `counter:hour:{yyyyMMdd}` | hash | Hourly buckets (field = `HH`); daily = sum | per visit | 7 days |
| `paths:day:{yyyyMMdd}` | hash | Views per path (field = path) | per visit | 7 days |
| `visits:recent` | list | Recent visits (JSON payloads) | per visit | — (capped 100) |
| `visitors:unique:day:{yyyyMMdd}` | HyperLogLog | Unique visitors (`ip\|ua`) | per visit | 7 days |
| `stats:total` | string | Lifetime total visits | daily (aggregation) | — |
| `stats:day:{yyyyMMdd}` | string | Pre-aggregated daily total | daily (aggregation) | 30 days |
| `stats:week:{yyyy'W'ww}` | hash | Weekly totals | daily (aggregation) | 30 days |

## Example Commands

```redis
# Per visit (4 commands)
HINCRBY counter:hour:20260816 14 1
HINCRBY paths:day:20260816 "/products/42" 1
LPUSH visits:recent '{"time":"...","path":"/products/42","ip":"...","ua":"..."}'
PFADD visitors:unique:day:20260816 "203.0.113.7|Mozilla/5.0"

# Read
HGETALL counter:hour:20260816          # hourly series / daily total (sum fields)
HGETALL paths:day:20260816             # views per path
LRANGE visits:recent 0 19              # recent visits
PFCOUNT visitors:unique:day:20260816   # unique visitors
GET stats:total                        # lifetime total

# Aggregation (scheduled, daily)
INCRBY stats:total <today-total>
SET stats:day:20260816 <today-total>
LTRIM visits:recent 0 99
```

## Retention & Expiry

- Hourly and daily buckets expire after 7 days.
- `stats:total` and `visits:recent` are unbounded but the list is capped.
- `AggregateStatsFunction` compacts completed days into `stats:day:*` and
  `stats:week:*` (30 days) before the raw keys expire.

## Aggregation Approach

The dashboard renders hour-level and day-level charts. Hourly buckets are stored
directly as hash fields; daily totals are pre-computed by the aggregation Lambda
into `stats:day:*`, so read-time computation is minimal.

## Sizing Notes

- `visits:recent` is capped at 100 entries to bound memory.
- HyperLogLog keeps unique-visitor counting to ~12 KB per day with ~0.81% error.
- Path hashes can grow large; `GetStatsFunction` limits reads to the top N via
  `HSCAN` + ranking.
