package io.github.jiangyuan.ratelimit.service;

import io.github.jiangyuan.ratelimit.annotation.RateLimit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * 令牌桶限流执行器
 * <p>
 * 基于 Redis Hash + Lua 脚本实现，原子地计算令牌补充与扣减。
 * 桶中保存：tokens（当前令牌数）、timestamp（上次刷新时间戳）。
 * </p>
 */
public class TokenBucketRateLimitExecutor implements RateLimitExecutor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setResultType(Long.class);
        SCRIPT.setScriptText(
                "local key = KEYS[1] " +
                "local capacity = tonumber(ARGV[1]) " +
                "local refillRate = tonumber(ARGV[2]) " +
                "local now = tonumber(ARGV[3]) " +
                "local ttl = tonumber(ARGV[4]) " +
                "local data = redis.call('HMGET', key, 'tokens', 'timestamp') " +
                "local tokens = tonumber(data[1]) " +
                "local timestamp = tonumber(data[2]) " +
                "if tokens == nil then " +
                "   tokens = capacity " +
                "   timestamp = now " +
                "else " +
                "   if timestamp == nil then " +
                "       timestamp = now " +
                "   end " +
                "   local delta = now - timestamp " +
                "   if delta > 0 and refillRate > 0 then " +
                "       local refill = delta * refillRate " +
                "       tokens = tokens + refill " +
                "       if tokens > capacity then " +
                "           tokens = capacity " +
                "       end " +
                "   end " +
                "end " +
                "local allowed = 0 " +
                "if tokens >= 1 then " +
                "   tokens = tokens - 1 " +
                "   allowed = 1 " +
                "end " +
                "redis.call('HMSET', key, 'tokens', tokens, 'timestamp', now) " +
                "redis.call('EXPIRE', key, ttl) " +
                "return allowed "
        );
    }

    public TokenBucketRateLimitExecutor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean allow(String redisKey, RateLimit rateLimit) {
        long now = System.currentTimeMillis() / 1000;
        long ttl = Math.max(rateLimit.windowSeconds(), 60);
        Long result = stringRedisTemplate.execute(
                SCRIPT,
                Collections.singletonList(redisKey),
                String.valueOf(rateLimit.permits()),
                String.valueOf(rateLimit.refillTokensPerSecond()),
                String.valueOf(now),
                String.valueOf(ttl)
        );
        return result != null && result == 1L;
    }
}
