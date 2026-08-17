package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordVisitHandlerTest {

    private final RedisClient redis = mock(RedisClient.class);
    private final RecordVisitHandler handler = new RecordVisitHandler(redis);
    private final Context context = mock(Context.class);

    {
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    @Test
    void recordsVisitWithFourCommandsAndReturns204() {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody("{\"path\":\"/products/42\",\"userAgent\":\"Mozilla/5.0\"}");
        Map<String, String> headers = new HashMap<>();
        headers.put("x-forwarded-for", "203.0.113.7");
        input.setHeaders(headers);

        APIGatewayProxyResponseEvent response = handler.handleRequest(input, context);

        assertEquals(204, response.getStatusCode());
        verify(redis).hincrby(startsWith("counter:hour:"), anyString(), eq(1L));
        verify(redis).hincrby(startsWith("paths:day:"), eq("/products/42"), eq(1L));
        verify(redis).lpush(eq("visits:recent"), contains("203.0.113.7"));
        verify(redis).pfadd(startsWith("visitors:unique:day:"), contains("203.0.113.7"));
    }

    @Test
    void defaultsMissingPathToRoot() {
        APIGatewayProxyRequestEvent input = new APIGatewayProxyRequestEvent();
        input.setBody("{}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(input, context);

        assertEquals(204, response.getStatusCode());
        verify(redis).hincrby(startsWith("paths:day:"), eq("/"), eq(1L));
    }
}
