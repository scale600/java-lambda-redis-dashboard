package com.example.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal REST client for Upstash Redis.
 *
 * Sends each command as a JSON array to the REST endpoint
 * (POST https://&lt;endpoint&gt;.upstash.io with {@code Authorization: Bearer <token>})
 * and returns the parsed {@code result} field.
 */
public class RedisClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String token;

    public RedisClient(String baseUrl, String token) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.token = token;
    }

    public static RedisClient fromEnv() {
        return new RedisClient(
                System.getenv("UPSTASH_REDIS_REST_URL"),
                System.getenv("UPSTASH_REDIS_REST_TOKEN"));
    }

    /** Executes a single command and returns the raw {@code result} node. */
    public JsonNode command(List<String> args) {
        try {
            String body = MAPPER.writeValueAsString(args);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = MAPPER.readTree(response.body());
            if (json.has("error")) {
                throw new RuntimeException("Upstash error: " + json.get("error"));
            }
            return json.get("result");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while calling Upstash", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Upstash: " + e.getMessage(), e);
        }
    }

    // --- String ---

    public String get(String key) {
        JsonNode r = command(List.of("GET", key));
        return (r == null || r.isNull()) ? null : r.asText();
    }

    public void set(String key, String value) {
        command(List.of("SET", key, value));
    }

    public long incrby(String key, long by) {
        return command(List.of("INCRBY", key, String.valueOf(by))).asLong();
    }

    public void expire(String key, long seconds) {
        command(List.of("EXPIRE", key, String.valueOf(seconds)));
    }

    // --- Hash ---

    public long hincrby(String key, String field, long by) {
        return command(List.of("HINCRBY", key, field, String.valueOf(by))).asLong();
    }

    public Map<String, String> hgetall(String key) {
        JsonNode r = command(List.of("HGETALL", key));
        Map<String, String> out = new LinkedHashMap<>();
        if (r != null && r.isArray()) {
            for (int i = 0; i + 1 < r.size(); i += 2) {
                out.put(r.get(i).asText(), r.get(i + 1).asText());
            }
        }
        return out;
    }

    // --- List ---

    public long lpush(String key, String value) {
        return command(List.of("LPUSH", key, value)).asLong();
    }

    public void ltrim(String key, long start, long stop) {
        command(List.of("LTRIM", key, String.valueOf(start), String.valueOf(stop)));
    }

    public List<String> lrange(String key, long start, long stop) {
        JsonNode r = command(List.of("LRANGE", key, String.valueOf(start), String.valueOf(stop)));
        List<String> out = new ArrayList<>();
        if (r != null && r.isArray()) {
            for (JsonNode n : r) {
                out.add(n.asText());
            }
        }
        return out;
    }

    // --- HyperLogLog ---

    public long pfadd(String key, String value) {
        return command(List.of("PFADD", key, value)).asLong();
    }

    public long pfcount(String key) {
        return command(List.of("PFCOUNT", key)).asLong();
    }
}
