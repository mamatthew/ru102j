package com.redislabs.university.RU102J.dao;

import com.redislabs.university.RU102J.core.KeyHelper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

public class RateLimiterSlidingDaoRedisImpl implements RateLimiter {

    private final JedisPool jedisPool;
    private final long windowSizeMS;
    private final long maxHits;

    public RateLimiterSlidingDaoRedisImpl(JedisPool pool, long windowSizeMS,
                                          long maxHits) {
        this.jedisPool = pool;
        this.windowSizeMS = windowSizeMS;
        this.maxHits = maxHits;
    }

    // Challenge #7
    @Override
    public void hit(String name) throws RateLimitExceededException {
        // START CHALLENGE #7
        try (Jedis jedis = jedisPool.getResource(); Transaction transaction = jedis.multi()) {
            // get key of sorted set for rate limiter
            String key = getKey(name);
            // add new entry to sorted set based on current time
            long currentTime = System.currentTimeMillis();
            transaction.zadd(key, currentTime, currentTime + ":" + Math.random());
            // remove entries that are older than the window size
            double oldestTime = currentTime - windowSizeMS;
            transaction.zremrangeByScore(key, 0.0, oldestTime);
            // check if the number of entries in the sorted set is greater than the max hits
            Response<Long> hits = transaction.zcard(key);
            transaction.exec();
            if (hits.get() > maxHits) {
                throw new RateLimitExceededException();
            }
        }
        // make use of ZADD, ZREMRANGEBYSCORE, and ZCARD
        // get key of sorted set for rate limiter
        // add new entry to sorted set based on current time

        // remove entries that are older than the window size
        // check if the number of entries in the sorted set is greater than the max hits
        // if so, throw a RateLimitExceededException
        // END CHALLENGE #7
    }

    private String getKey(String name) {

        return KeyHelper.getKey("limiter:" + windowSizeMS + ":" + name + ":" + maxHits);
    }
}
