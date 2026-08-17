package com.example.dashboard;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;

/**
 * Daily rollup. Triggered at 00:00 UTC by EventBridge. Aggregates the previous
 * (completed) day's hourly counters into {@code stats:day} and
 * {@code stats:total}, trims the recent-visits list, and sets TTLs on the
 * completed day's raw keys.
 */
public class AggregateStatsHandler implements RequestHandler<Map<String, Object>, String> {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long DAY_SECONDS = 86400L;

    private final RedisClient redis;

    public AggregateStatsHandler() {
        this(RedisClient.fromEnv());
    }

    AggregateStatsHandler(RedisClient redis) {
        this.redis = redis;
    }

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        try {
            LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
            String day = yesterday.format(DAY);

            long total = 0;
            for (String v : redis.hgetall("counter:hour:" + day).values()) {
                total += parseLong(v);
            }

            // Pre-aggregated daily total (30-day retention)
            redis.set("stats:day:" + day, String.valueOf(total));
            redis.expire("stats:day:" + day, 30 * DAY_SECONDS);

            // Lifetime total
            redis.incrby("stats:total", total);

            // Weekly bucket (30-day retention)
            String week = weekKey(yesterday);
            redis.hincrby("stats:week:" + week, day, total);
            redis.expire("stats:week:" + week, 30 * DAY_SECONDS);

            // Trim recent-visits list
            redis.ltrim("visits:recent", 0, 99);

            // Expire raw daily keys (7-day retention)
            redis.expire("counter:hour:" + day, 7 * DAY_SECONDS);
            redis.expire("paths:day:" + day, 7 * DAY_SECONDS);
            redis.expire("visitors:unique:day:" + day, 7 * DAY_SECONDS);

            context.getLogger().log("Aggregated " + day + ": " + total + " visits");
            return "OK";
        } catch (Exception e) {
            context.getLogger().log("AggregateStatsHandler error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String weekKey(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        int year = date.get(wf.weekBasedYear());
        int week = date.get(wf.weekOfWeekBasedYear());
        return String.format(Locale.ROOT, "%04dW%02d", year, week);
    }

    private static long parseLong(String s) {
        if (s == null) {
            return 0L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
