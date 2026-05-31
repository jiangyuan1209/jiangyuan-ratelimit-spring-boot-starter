# 令牌桶限流算法（Token Bucket）

## 1. 核心概念

令牌桶算法的核心思想是：**桶里装的是"通行证"（令牌），每次请求消耗 1 个，桶空了就拒绝，桶会以固定速率自动补充。**

```
    ┌─────────────┐
    │  令牌桶 (T)  │  ◄── 以固定速率 refillRate 补充令牌
    │             │
    │   🪙 🪙 🪙  │  ◄── 桶容量 = permits（最大令牌数）
    └──────┬──────┘
           │ 每次请求取走 1 个
           ▼
    ┌─────────────┐
    │   放行/拒绝   │  有令牌 → 放行；无令牌 → 拒绝
    └─────────────┘
```

## 2. 工作流程

```
时间线 ──────────────────────────────────────────────▶

t=0s     t=0.1s   t=0.2s   t=0.3s   t=1.0s
  |        |        |        |        |
[██][██]  [██]     [ ]      ❌拒绝    [██][██]
  ↓        ↓        ↓                 ↓
 请求1    请求2    请求3    (桶空了)  1秒后桶满了
 消耗1    消耗1    消耗1     拒绝!    补充回来
```

### 请求进来时的步骤

1. **检查**桶里有没有令牌
2. **有令牌** → 拿走 1 个，放行
3. **无令牌** → 拒绝请求
4. **补充令牌**：计算从上次请求到现在该补充多少（惰性计算）

## 3. 惰性补充机制

不是真的有个后台线程在补，而是每次请求时**算一下从上次到现在该补多少**：

```lua
时间差 delta = now - 上次请求时间戳
补充量 refill = delta × refillRate
实际令牌 = min(容量, 当前令牌 + refill)
更新桶状态：tokens = 实际令牌, timestamp = now
```

### Lua 脚本核心逻辑

```lua
-- 读取桶状态
local tokens = tonumber(redis.call('HGET', key, 'tokens'))
local timestamp = tonumber(redis.call('HGET', key, 'timestamp'))

-- 如果是首次，直接初始化满桶
if tokens == nil then
    tokens = capacity
    timestamp = now
end

-- 计算补充量（只有时间差 > 0 且补充速率 > 0 时才补）
local delta = now - timestamp
if delta > 0 and refillRate > 0 then
    local refill = delta * refillRate
    tokens = tokens + refill
    if tokens > capacity then
        tokens = capacity
    end
end

-- 尝试扣减令牌
if tokens >= 1 then
    tokens = tokens - 1
    return 1  -- 放行
else
    return 0  -- 拒绝
end
```

## 4. 与固定窗口算法对比

| 维度 | FixedWindow（固定窗口） | TokenBucket（令牌桶） |
|------|------------------------|----------------------|
| 原理 | 按时间窗口计数，到点重置 | 令牌桶扣减+补充 |
| 突发能力 | 每个窗口初期满额突发 | 桶满时突发 |
| 平滑性 | 窗口边界可能出现 2× 峰值 | 长期速率平滑 |
| 复杂度 | 简单（INCR + EXPIRE） | 稍复杂（Hash + 惰性补充） |
| 适用场景 | 简单限流、防刷 | 需要平滑速率限制的场景 |

### 对比示例

```
实现"1 分钟最多 2 次请求"

FixedWindow (permits=2, window=60s):
00:00 [✅请求1] [✅请求2] [❌] [❌] ... 60s 内全部拒绝
01:00 窗口重置 [✅] [✅] [❌] ...

TokenBucket (permits=2, refillRate=0.033 即每分钟补2个):
t=0    [✅请求1] [✅请求2] [❌]
t=30s  补充 1 个 [✅]
t=60s  再补 1 个 [✅]
更平滑，没有窗口边界的 2× 突增问题

TokenBucket (permits=2, refillRate=0):
t=0    [✅请求1] [✅请求2] [❌] [❌] ...
       不补充，2次用完就一直拒绝直到 key 过期
```

## 5. Redis 存储结构

```
Key: rate-limit:OrderServiceImpl:submitTest:test

Hash 结构:
┌──────────────────────────┐
│ Field    │ Value         │
├──────────────────────────┤
│ tokens   │ 1.5           │  ← 当前令牌数（可以是小数）
│ timestamp│ 1717128000    │  ← 上次更新时间戳（秒）
└──────────────────────────┘
```

## 6. 配置示例

```java
// 场景1：1 分钟内最多 2 次（用固定窗口，最简单）
@RateLimit(key = "#userIdForm.userId",
           permits = 2,
           windowSeconds = 60,
           algorithm = RateLimitAlgorithm.FIXED_WINDOW,
           useGlobalConfig = false,
           message = "提交请求过于频繁")

// 场景2：1 分钟内最多 2 次（用令牌桶，不补充）
@RateLimit(key = "#userIdForm.userId",
           permits = 2,
           refillTokensPerSecond = 0,
           useGlobalConfig = false,
           message = "提交请求过于频繁")

// 场景3：平均 30 秒 1 次，允许突发 2 次
@RateLimit(key = "#userIdForm.userId",
           permits = 2,
           refillTokensPerSecond = 1,  // 每秒补 1 个，即 30 秒补 30 个（但桶容量只有 2）
           useGlobalConfig = false,
           message = "提交请求过于频繁")
```

## 7. 关键参数说明

| 参数 | 含义 | 说明 |
|------|------|------|
| `permits` | 桶容量 | 最大令牌数，也是最大突发请求数 |
| `refillTokensPerSecond` | 补充速率 | 每秒补充多少个令牌 |
| `windowSeconds` | 窗口/过期时间 | TokenBucket 中主要用作 Redis key 的 TTL |
| `limitByIp` | 是否按 IP 维度 | 开启后 key 会追加 IP 地址 |

### refillTokensPerSecond 的影响

```
refillTokensPerSecond = 0   → 不补充，纯消耗型（用完就拒）
refillTokensPerSecond = 1   → 每秒补 1 个，30 秒桶满（容量=2 时）
refillTokensPerSecond = 5   → 每秒补 5 个，0.4 秒桶满（几乎永远放行）⚠️
```

> **注意**：`refillTokensPerSecond` 设置过大会导致限流几乎不生效，因为令牌补充太快。
