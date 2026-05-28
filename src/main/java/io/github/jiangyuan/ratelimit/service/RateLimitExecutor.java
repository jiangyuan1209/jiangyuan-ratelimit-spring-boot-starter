package io.github.jiangyuan.ratelimit.service;

import io.github.jiangyuan.ratelimit.annotation.RateLimit;

/**
 * 限流执行器接口
 */
public interface RateLimitExecutor {

    /**
     * 判断是否允许访问
     *
     * @param redisKey  Redis key
     * @param rateLimit 注解配置
     * @return true=允许，false=拒绝
     */
    boolean allow(String redisKey, RateLimit rateLimit);
}
