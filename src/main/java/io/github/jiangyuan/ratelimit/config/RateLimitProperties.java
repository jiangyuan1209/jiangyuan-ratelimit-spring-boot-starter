package io.github.jiangyuan.ratelimit.config;

import io.github.jiangyuan.ratelimit.enums.RateLimitAlgorithm;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流全局配置属性
 * <p>
 * 配置文件示例：
 * <pre>
 * rate-limit:
 *   enabled: true
 *   algorithm: TOKEN_BUCKET
 *   key-prefix: "rl:"
 * </pre>
 * </p>
 */
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /** 是否启用限流 */
    private boolean enabled = true;

    /** 全局算法：TOKEN_BUCKET / FIXED_WINDOW */
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    /** Redis key 前缀 */
    private String keyPrefix = "rl:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
