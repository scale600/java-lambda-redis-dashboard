package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetStatsHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedisClient redis = mock(RedisClient.class);
    private final GetStatsHandler handler = new GetStatsHandler(redis);
    private final Context context = mock(Context.class);

    {
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    @Test
    void healthReturnsOk() throws Exception {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setPath("/health");

        APIGatewayProxyResponseEvent response = handler.handleRequest(input, context);

        assertEquals(200, response.getStatusCode());
        JsonNode json = MAPPER.readTree(response.getBody());
        assertEquals("ok", json.get("status").asText());
    }

    @Test
    void overviewAggregatesTotals() throws Exception {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setPath("/stats/overview");
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("proxy", "overview");
        input.setPathParameters(pathParams);

        when(redis.get("stats:total")).thenReturn("100");
        when(redis.hgetall(startsWith("counter:hour:"))).thenReturn(Map.of("0", "5", "1", "3"));
        when(redis.pfcount(startsWith("visitors:unique:day:"))).thenReturn(7L);

        APIGatewayProxyResponseEvent response = handler.handleRequest(input, context);

        assertEquals(200, response.getStatusCode());
        JsonNode json = MAPPER.readTree(response.getBody());
        assertEquals(100, json.get("total").asLong());
        assertEquals(8, json.get("today").asLong());
        assertEquals(7, json.get("uniqueToday").asLong());
    }

    @Test
    void unknownEndpointReturns404() {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setPath("/stats/nope");
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("proxy", "nope");
        input.setPathParameters(pathParams);

        APIGatewayProxyResponseEvent response = handler.handleRequest(input, context);

        assertEquals(404, response.getStatusCode());
    }
}
