package io.github.jiangyuan.ratelimit.enums;

/**
 * 限流算法枚举
 */
public enum RateLimitAlgorithm {
    /**
     * 令牌桶算法：平滑限流，适合支付、登录、下单等接口
     */
    TOKEN_BUCKET,
    /**
     * 固定时间窗口算法：简单稳定，适合大多数业务场景
     */
    FIXED_WINDOW
}
