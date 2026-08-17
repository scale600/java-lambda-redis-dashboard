package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AggregateStatsHandlerTest {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisClient redis = mock(RedisClient.class);
    private final AggregateStatsHandler handler = new AggregateStatsHandler(redis);
    private final Context context = mock(Context.class);

    {
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    @Test
    void rollsUpPreviousDay() {
        String yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1).format(DAY);

        when(redis.hgetall("counter:hour:" + yesterday)).thenReturn(Map.of("0", "10", "1", "5"));

        String result = handler.handleRequest(Map.of(), context);

        assertEquals("OK", result);
        verify(redis).set("stats:day:" + yesterday, "15");
        verify(redis).incrby("stats:total", 15L);
        verify(redis).ltrim("visits:recent", 0L, 99L);
        verify(redis).expire("counter:hour:" + yesterday, 7 * 86400L);
    }
}
