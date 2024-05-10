package com.aneto.tindraojsandbox.util;

import java.time.Instant;

public class TimeUtils {
    public static long calculateTimeDifference(long timestamp) {
        long currentTime = Instant.now().getEpochSecond();
        return currentTime - timestamp;
    }

    public static boolean isWithinOneMinute(long timestamp) {
        long difference = calculateTimeDifference(timestamp);
        return difference < 60; // 60秒为1分钟
    }

    public static void main(String[] args) {
        long timestamp = 1620050400; // 假设时间戳为2021-05-03 08:00:00
        boolean withinOneMinute = isWithinOneMinute(timestamp);
        System.out.println("Is within one minute: " + withinOneMinute);
    }
}
