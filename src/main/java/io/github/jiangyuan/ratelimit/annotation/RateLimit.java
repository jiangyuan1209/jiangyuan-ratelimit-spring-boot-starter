package io.github.jiangyuan.ratelimit.annotation;

import io.github.jiangyuan.ratelimit.enums.RateLimitAlgorithm;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * <p>
 * 可标注在 Controller 或 Service 方法上，支持 SpEL 表达式解析 key。
 * 限流算法由配置文件全局指定，也可通过 algorithm 字段单独覆盖。
 * </p>
 *
 * <pre>
 * // Controller 示例
 * &#64;GetMapping("/hello")
 * &#64;RateLimit(key = "#userId", permits = 5, windowSeconds = 60, message = "1分钟内请求过多")
 * public String hello(@RequestParam String userId) { ... }
 *
 * // Service 示例
 * &#64;RateLimit(key = "#order.userId", permits = 10, windowSeconds = 60)
 * public void createOrder(Order order) { ... }
 *
 * // 按 IP 限流
 * &#64;RateLimit(key = "#api", permits = 100, windowSeconds = 60, limitByIp = true)
 * public String apiCall(@RequestParam String api) { ... }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 key，支持 SpEL 表达式
     * <p>
     * 例如：{@code #userId}、{@code #order.userId}、{@code #p0}
     * </p>
     */
    String key() default "";

    /**
     * 时间窗口（秒），对 fixed_window 表示窗口长度，对 token_bucket 可理解为恢复周期参考值
     */
    long windowSeconds() default 60;

    /**
     * 允许请求数
     * <p>
     * fixed_window: 窗口内最大请求数
     * token_bucket: 桶容量（最大令牌数）
     * </p>
     */
    long permits() default 10;

    /**
     * 令牌桶每秒补充令牌数，仅 TOKEN_BUCKET 算法生效
     */
    long refillTokensPerSecond() default 1;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 是否自动按请求 IP 追加限流维度
     */
    boolean limitByIp() default false;

    /**
     * 限流算法，默认使用全局配置
     */
    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * 是否使用全局算法配置；设为 false 时以 {@link #algorithm()} 为准
     */
    boolean useGlobalConfig() default true;
}
