package io.github.jiangyuan.ratelimit.config;

import io.github.jiangyuan.ratelimit.aspect.RateLimitAspect;
import io.github.jiangyuan.ratelimit.exception.RateLimitExceptionHandler;
import io.github.jiangyuan.ratelimit.service.FixedWindowRateLimitExecutor;
import io.github.jiangyuan.ratelimit.service.TokenBucketRateLimitExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

/**
 * 限流自动配置类
 * <p>
 * 当 classpath 中存在 Redis 且 {@code rate-limit.enabled=true}（默认 true）时自动生效。
 * Web 相关组件（异常处理器）仅在 Servlet 环境下注册。
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FixedWindowRateLimitExecutor fixedWindowRateLimitExecutor(
            StringRedisTemplate stringRedisTemplate) {
        return new FixedWindowRateLimitExecutor(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenBucketRateLimitExecutor tokenBucketRateLimitExecutor(
            StringRedisTemplate stringRedisTemplate) {
        return new TokenBucketRateLimitExecutor(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(
            RateLimitProperties properties,
            Map<String, io.github.jiangyuan.ratelimit.service.RateLimitExecutor> executorMap) {
        return new RateLimitAspect(properties, executorMap);
    }

    /**
     * Web 环境下的额外配置：注册全局异常处理器
     */
    @Configuration
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    static class RateLimitWebConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RateLimitExceptionHandler rateLimitExceptionHandler() {
            return new RateLimitExceptionHandler();
        }
    }
}
