package io.github.jiangyuan.ratelimit.exception;

/**
 * 限流触发的异常
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
