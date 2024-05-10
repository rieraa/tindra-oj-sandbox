package com.aneto.tindraojsandbox.util;

import redis.clients.jedis.Jedis;

public class RedisUtils {
    private static final int EXPIRE_TIME_SECONDS = 60;

    private static Jedis jedis = new Jedis("localhost");

    public static boolean checkIfExist(String key) {
        return jedis.exists(key);
    }

    public static void recordData(String key) {
        jedis.setex(key, EXPIRE_TIME_SECONDS, "1");
    }
}