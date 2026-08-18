package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles {@code POST /visit}. Writes a 5-command visit record to Upstash Redis:
 *
 * <pre>
 *   HINCRBY counter:hour:{yyyyMMdd} {HH} 1
 *   HINCRBY paths:day:{yyyyMMdd} {path} 1
 *   HINCRBY sites:day:{yyyyMMdd} {site} 1
 *   LPUSH  visits:recent {json}
 *   PFADD  visitors:unique:day:{yyyyMMdd} {ip|ua}
 * </pre>
 */
public class RecordVisitHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisClient redis;

    public RecordVisitHandler() {
        this(RedisClient.fromEnv());
    }

    RecordVisitHandler(RedisClient redis) {
        this.redis = redis;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            JsonNode json = parseBody(input.getBody());

            String path = json.hasNonNull("path") ? json.get("path").asText() : "/";
            String site = json.hasNonNull("site") ? json.get("site").asText() : "unknown";
            String userAgent = json.hasNonNull("userAgent") ? json.get("userAgent").asText() : "";
            String ip = resolveIp(input);

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            String day = now.format(DAY);
            String hour = String.valueOf(now.getHour());

            redis.hincrby("counter:hour:" + day, hour, 1);
            redis.hincrby("paths:day:" + day, path, 1);
            redis.hincrby("sites:day:" + day, site, 1);

            Map<String, Object> visit = new LinkedHashMap<>();
            visit.put("time", now.toInstant().toString());
            visit.put("path", path);
            visit.put("site", site);
            visit.put("ip", ip);
            visit.put("ua", userAgent);
            redis.lpush("visits:recent", MAPPER.writeValueAsString(visit));

            redis.pfadd("visitors:unique:day:" + day, ip + "|" + userAgent);

            return Api.noContent();
        } catch (Exception e) {
            context.getLogger().log("RecordVisitHandler error: " + e.getMessage());
            return Api.error(500, "INTERNAL", e.getMessage());
        }
    }

    private JsonNode parseBody(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return MAPPER.createObjectNode();
        }
        return MAPPER.readTree(body);
    }

    private String resolveIp(APIGatewayProxyRequestEvent input) {
        Map<String, String> headers = input.getHeaders();
        if (headers != null) {
            String forwarded = headers.get("x-forwarded-for");
            if (forwarded == null) {
                forwarded = headers.get("X-Forwarded-For");
            }
            if (forwarded != null) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
        }
        if (input.getRequestContext() != null
                && input.getRequestContext().getIdentity() != null
                && input.getRequestContext().getIdentity().getSourceIp() != null) {
            return input.getRequestContext().getIdentity().getSourceIp();
        }
        return "unknown";
    }
}
