package io.github.jiangyuan.ratelimit.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 工具类：获取请求上下文中的客户端 IP
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 获取客户端 IP 地址，优先从代理头中读取
     */
    public static String getClientIp() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return "";
        }
        HttpServletRequest request = sra.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return isBlank(ip) ? "" : ip;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
