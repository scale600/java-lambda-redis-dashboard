package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles {@code GET /health} and {@code GET /stats/{proxy+}} where
 * {@code proxy} is one of {@code overview}, {@code timeseries}, {@code paths},
 * {@code sites}, or {@code recent}.
 */
public class GetStatsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisClient redis;

    public GetStatsHandler() {
        this(RedisClient.fromEnv());
    }

    GetStatsHandler(RedisClient redis) {
        this.redis = redis;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String path = input.getPath();
            if (path != null && path.endsWith("/health")) {
                return Api.ok("{\"status\":\"ok\"}");
            }

            Map<String, String> pathParams = input.getPathParameters();
            String proxy = pathParams == null ? null : pathParams.get("proxy");
            if (proxy == null) {
                return Api.error(404, "NOT_FOUND", "Unknown path");
            }

            switch (proxy) {
                case "overview":
                    return overview();
                case "timeseries":
                    return timeseries(input.getQueryStringParameters());
                case "paths":
                    return paths(input.getQueryStringParameters());
                case "sites":
                    return sites(input.getQueryStringParameters());
                case "recent":
                    return recent(input.getQueryStringParameters());
                default:
                    return Api.error(404, "NOT_FOUND", "Unknown stats endpoint: " + proxy);
            }
        } catch (Exception e) {
            context.getLogger().log("GetStatsHandler error: " + e.getMessage());
            return Api.error(500, "INTERNAL", e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent overview() {
        String today = today();
        long total = longOrZero(redis.get("stats:total"));
        long todayCount = sumHourly(today);
        long unique = redis.pfcount("visitors:unique:day:" + today);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("total", total);
        root.put("today", todayCount);
        root.put("uniqueToday", unique);
        root.put("lastUpdated", ZonedDateTime.now(ZoneOffset.UTC).toInstant().toString());
        return Api.ok(root.toString());
    }

    private APIGatewayProxyResponseEvent timeseries(Map<String, String> query) {
        String granularity = "hour";
        int limit = 24;
        if (query != null) {
            granularity = query.getOrDefault("granularity", "hour");
            limit = parseInt(query.get("limit"), 24);
        }

        ObjectNode root = MAPPER.createObjectNode();
        root.put("granularity", granularity);
        ArrayNode series = root.putArray("series");

        if ("day".equals(granularity)) {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate start = today.minusDays(limit - 1L);
            for (int i = 0; i < limit; i++) {
                LocalDate d = start.plusDays(i);
                String day = d.format(DAY);
                long count = d.isEqual(today) ? sumHourly(day) : longOrZero(redis.get("stats:day:" + day));
                ObjectNode point = series.addObject();
                point.put("timestamp", d.atStartOfDay(ZoneOffset.UTC).toInstant().toString());
                point.put("count", count);
            }
        } else {
            Map<String, String> hourly = redis.hgetall("counter:hour:" + today());
            for (int h = 0; h < 24; h++) {
                long count = longOrZero(hourly.get(String.valueOf(h)));
                ObjectNode point = series.addObject();
                point.put("timestamp", LocalDate.now(ZoneOffset.UTC).atTime(h, 0).atZone(ZoneOffset.UTC).toInstant().toString());
                point.put("count", count);
            }
        }
        return Api.ok(root.toString());
    }

    private APIGatewayProxyResponseEvent paths(Map<String, String> query) {
        int limit = query != null ? parseInt(query.get("limit"), 20) : 20;
        Map<String, String> raw = redis.hgetall("paths:day:" + today());

        List<Map.Entry<String, Long>> entries = new ArrayList<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(e.getKey(), longOrZero(e.getValue())));
        }
        entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode arr = root.putArray("paths");
        int n = 0;
        for (Map.Entry<String, Long> e : entries) {
            if (n++ >= limit) {
                break;
            }
            ObjectNode item = arr.addObject();
            item.put("path", e.getKey());
            item.put("count", e.getValue());
        }
        return Api.ok(root.toString());
    }

    private APIGatewayProxyResponseEvent sites(Map<String, String> query) {
        int limit = query != null ? parseInt(query.get("limit"), 20) : 20;
        Map<String, String> raw = redis.hgetall("sites:day:" + today());

        List<Map.Entry<String, Long>> entries = new ArrayList<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(e.getKey(), longOrZero(e.getValue())));
        }
        entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode arr = root.putArray("sites");
        int n = 0;
        for (Map.Entry<String, Long> e : entries) {
            if (n++ >= limit) {
                break;
            }
            ObjectNode item = arr.addObject();
            item.put("site", e.getKey());
            item.put("count", e.getValue());
        }
        return Api.ok(root.toString());
    }

    private APIGatewayProxyResponseEvent recent(Map<String, String> query) {
        int limit = query != null ? parseInt(query.get("limit"), 20) : 20;
        List<String> raw = redis.lrange("visits:recent", 0, limit - 1);

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode arr = root.putArray("visits");
        for (String s : raw) {
            try {
                arr.add(MAPPER.readTree(s));
            } catch (Exception ignored) {
                // skip malformed entries
            }
        }
        return Api.ok(root.toString());
    }

    private long sumHourly(String day) {
        long sum = 0;
        for (String v : redis.hgetall("counter:hour:" + day).values()) {
            sum += longOrZero(v);
        }
        return sum;
    }

    private static long longOrZero(String s) {
        if (s == null) {
            return 0L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static int parseInt(String s, int def) {
        if (s == null) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String today() {
        return LocalDate.now(ZoneOffset.UTC).format(DAY);
    }
}
