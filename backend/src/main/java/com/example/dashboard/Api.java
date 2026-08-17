package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/** Shared helpers for building API Gateway responses (including CORS). */
final class Api {

    static final String CORS_ORIGIN = "https://java-redis.techcloudup.com";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Api() {
    }

    static Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", CORS_ORIGIN);
        h.put("Content-Type", "application/json");
        return h;
    }

    static APIGatewayProxyResponseEvent response(int status, String body) {
        APIGatewayProxyResponseEvent r = new APIGatewayProxyResponseEvent();
        r.setStatusCode(status);
        r.setHeaders(headers());
        r.setBody(body);
        return r;
    }

    static APIGatewayProxyResponseEvent noContent() {
        APIGatewayProxyResponseEvent r = new APIGatewayProxyResponseEvent();
        r.setStatusCode(204);
        r.setHeaders(headers());
        return r;
    }

    static APIGatewayProxyResponseEvent ok(String json) {
        return response(200, json);
    }

    static APIGatewayProxyResponseEvent error(int status, String code, String message) {
        ObjectNode node = MAPPER.createObjectNode();
        ObjectNode err = node.putObject("error");
        err.put("code", code);
        err.put("message", message);
        return response(status, node.toString());
    }
}
