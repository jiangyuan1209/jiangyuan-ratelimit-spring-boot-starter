package io.github.jiangyuan.ratelimit.support;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 表达式解析工具
 */
public final class SpelUtils {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private SpelUtils() {
    }

    /**
     * 解析注解中的 SpEL key 表达式
     *
     * @param spel       表达式，如 "#userId"
     * @param joinPoint  切点上下文
     * @return 解析后的字符串值
     */
    public static String parseKey(String spel, ProceedingJoinPoint joinPoint) {
        if (spel == null || spel.isBlank()) {
            return "";
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        // 同时支持 #p0, #a0 等位置参数
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
        }

        Expression expression = PARSER.parseExpression(spel);
        Object value = expression.getValue(context);
        return value == null ? "" : String.valueOf(value);
    }
}
