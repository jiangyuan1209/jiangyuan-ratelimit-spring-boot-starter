package io.github.jiangyuan.ratelimit.aspect;

import io.github.jiangyuan.ratelimit.annotation.RateLimit;
import io.github.jiangyuan.ratelimit.config.RateLimitProperties;
import io.github.jiangyuan.ratelimit.enums.RateLimitAlgorithm;
import io.github.jiangyuan.ratelimit.exception.RateLimitException;
import io.github.jiangyuan.ratelimit.service.RateLimitExecutor;
import io.github.jiangyuan.ratelimit.support.IpUtils;
import io.github.jiangyuan.ratelimit.support.SpelUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 限流 AOP 切面
 * <p>
 * 拦截带有 {@link RateLimit} 注解的方法，根据配置选择限流算法执行限流。
 * 支持注解级别的算法覆盖（useGlobalConfig = false 时）。
 * </p>
 */
@Aspect
public class RateLimitAspect {

    private final RateLimitProperties properties;
    private final Map<String, RateLimitExecutor> executorMap;

    public RateLimitAspect(RateLimitProperties properties, Map<String, RateLimitExecutor> executorMap) {
        this.properties = properties;
        this.executorMap = executorMap;
    }

    @Around("@annotation(io.github.jiangyuan.ratelimit.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 全局开关
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 拼装 Redis key
        String redisKey = buildRedisKey(joinPoint, rateLimit);

        // 选择执行器（注解可覆盖全局配置）
        RateLimitAlgorithm algorithm = rateLimit.useGlobalConfig()
                ? properties.getAlgorithm()
                : rateLimit.algorithm();
        RateLimitExecutor executor = chooseExecutor(algorithm);

        boolean allowed = executor.allow(redisKey, rateLimit);
        if (!allowed) {
            throw new RateLimitException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 构建完整的 Redis key
     * 格式：{prefix}{className}:{methodName}:{businessKey}[:ip]
     */
    private String buildRedisKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String businessKey = SpelUtils.parseKey(rateLimit.key(), joinPoint);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();

        StringBuilder sb = new StringBuilder(properties.getKeyPrefix())
                .append(className)
                .append(":")
                .append(methodName)
                .append(":");

        if (businessKey != null && !businessKey.isEmpty()) {
            sb.append(businessKey);
        } else {
            sb.append("default");
        }

        // 如果开启了按 IP 限流，追加 IP 维度
        if (rateLimit.limitByIp()) {
            String ip = IpUtils.getClientIp();
            if (!ip.isEmpty()) {
                sb.append(":").append(ip);
            }
        }

        return sb.toString();
    }

    private RateLimitExecutor chooseExecutor(RateLimitAlgorithm algorithm) {
        if (algorithm == RateLimitAlgorithm.FIXED_WINDOW) {
            return executorMap.get("fixedWindowRateLimitExecutor");
        }
        return executorMap.get("tokenBucketRateLimitExecutor");
    }
}
