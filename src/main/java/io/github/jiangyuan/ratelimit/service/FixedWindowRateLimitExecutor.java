package io.github.jiangyuan.ratelimit.service;

import io.github.jiangyuan.ratelimit.annotation.RateLimit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * 固定时间窗口限流执行器
 * <p>
 * 基于 Redis INCR + EXPIRE 实现，通过 Lua 脚本保证原子性。
 * </p>
 */
public class FixedWindowRateLimitExecutor implements RateLimitExecutor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SCRIPT;

    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setResultType(Long.class);
        SCRIPT.setScriptText(
                "local key = KEYS[1] " +
                "local limit = tonumber(ARGV[1]) " +
                "local expire = tonumber(ARGV[2]) " +
                "local current = redis.call('INCR', key) " +
                "if current == 1 then " +
                "   redis.call('EXPIRE', key, expire) " +
                "end " +
                "if current > limit then " +
                "   return 0 " +
                "else " +
                "   return 1 " +
                "end "
        );
    }

    public FixedWindowRateLimitExecutor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean allow(String redisKey, RateLimit rateLimit) {
        Long result = stringRedisTemplate.execute(
                SCRIPT,
                Collections.singletonList(redisKey),
                String.valueOf(rateLimit.permits()),
                String.valueOf(rateLimit.windowSeconds())
        );
        return result != null && result == 1L;
    }
}
