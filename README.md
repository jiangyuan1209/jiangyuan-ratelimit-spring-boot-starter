# JiangYuan RateLimit Spring Boot Starter

基于 Redis + Lua 的 Spring Boot 接口限流组件，支持令牌桶和固定时间窗口两种算法。

## Maven 依赖

```xml
<dependency>
    <groupId>io.github.jiangyuan1209</groupId>
    <artifactId>jiangyuan-ratelimit-spring-boot-starter</artifactId>
    <version>0.0.2</version>
</dependency>
```

只需添加依赖即可使用，无需额外配置。Spring Boot 会自动加载限流功能。

## 特性

- 声明式 `@RateLimit` 注解，标注在 Controller 或 Service 方法上
- 支持 SpEL 表达式动态解析限流 key（如 `#userId`、`#order.id`）
- 两种限流算法：**令牌桶**（TOKEN_BUCKET）和 **固定时间窗口**（FIXED_WINDOW）
- 配置文件全局指定算法，注解可单独覆盖
- 支持按客户端 IP 自动追加限流维度
- Lua 脚本保证 Redis 操作原子性
- 限流异常返回 HTTP 429 状态码 + 自定义提示

## 快速开始

### 1. 配置文件

```yaml
rate-limit:
  enabled: true
  algorithm: TOKEN_BUCKET    # TOKEN_BUCKET / FIXED_WINDOW
  key-prefix: "rl:"
```

### 2. 在方法上加注解

```java
import io.github.jiangyuan.ratelimit.annotation.RateLimit;

@GetMapping("/hello")
@RateLimit(key = "#userId", permits = 5, windowSeconds = 60, message = "请求过于频繁")
public String hello(@RequestParam String userId) {
    return "hello " + userId;
}
```

### 3. 参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| key | String | `""` | 限流维度 key，支持 SpEL（如 `#userId`、`#p0`） |
| permits | long | `10` | 固定窗口：窗口内最大请求数；令牌桶：桶容量 |
| windowSeconds | long | `60` | 固定窗口：窗口长度；令牌桶：Redis key 过期时间 |
| refillTokensPerSecond | long | `1` | 令牌桶每秒补充令牌数，设为 `0` 表示不补充（仅 TOKEN_BUCKET 生效） |
| message | String | `请求过于频繁，请稍后再试` | 限流触发时的提示信息 |
| limitByIp | boolean | `false` | 是否自动按客户端 IP 追加限流维度 |
| algorithm | RateLimitAlgorithm | `TOKEN_BUCKET` | 限流算法，配合 useGlobalConfig 使用 |
| useGlobalConfig | boolean | `true` | 是否使用配置文件中的全局算法，设为 false 时以 algorithm 为准 |

### 4. 使用示例

**按用户限流**

```java
@RateLimit(key = "#userId", permits = 5, windowSeconds = 60)
public String getUserInfo(@RequestParam String userId) { ... }
```

**按 IP 限流**

```java
@RateLimit(key = "/api/search", permits = 100, windowSeconds = 60, limitByIp = true)
public String search(@RequestParam String keyword) { ... }
```

**Service 方法限流**

```java
@RateLimit(key = "#userId", permits = 3, windowSeconds = 30, message = "下单太频繁")
public void createOrder(String userId) { ... }
```

**覆盖全局算法**

```java
@RateLimit(key = "#id", permits = 50, windowSeconds = 60, useGlobalConfig = false, algorithm = RateLimitAlgorithm.FIXED_WINDOW)
public String batchQuery(Long id) { ... }
```

### 5. 限流异常

触发限流时返回 HTTP 429：

```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试"
}
```

如需自定义异常处理，可排除自动配置中的 `RateLimitWebConfiguration`，自行注册异常处理器。

## 算法对比

| | 令牌桶 (TOKEN_BUCKET) | 固定窗口 (FIXED_WINDOW) |
|---|---|---|
| 平滑性 | 平滑，突发可控 | 窗口边界处可能突刺 |
| 适用场景 | 支付、登录、下单 | 大多数普通查询 |
| 资源消耗 | 略高（Hash 结构） | 低（INCR 计数器） |

## 项目结构

```
io.github.jiangyuan.ratelimit/
├── annotation/     # @RateLimit 注解定义
├── aspect/         # AOP 切面拦截与限流逻辑
├── config/         # 自动配置类 + 配置属性
├── enums/          # 限流算法枚举
├── exception/      # 限流异常 + 全局异常处理器
├── service/        # 执行器接口 + 两种算法实现
└── support/        # SpEL 解析、IP 获取工具
```

## 注意事项

- **Service 自调用问题**：同一 Bean 内部 `this.xxx()` 调用 `@RateLimit` 方法时 AOP 不生效（Spring AOP 代理限制），需通过代理对象调用或拆分到不同 Bean
- **Redis 必须可用**：限流依赖 Redis，如果 Redis 不可用会导致请求失败
- **key 设计**：建议限流 key 能唯一标识限流维度（用户/设备/IP 等），避免不同接口串限流
