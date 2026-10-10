# Backend Latency Observability（后端延迟可观测性）

本文档说明 LastCall Eats 如何使用 Micrometer（指标门面）、Prometheus（时序指标系统）和
Grafana Dashboard（Grafana 仪表盘），让 React Native Team（移动客户端团队）直接查看后端
Latency Trend（延迟趋势）、Throughput（吞吐量）、Error Rate（错误率）和 Redis Cache
Effectiveness（Redis 缓存效果）。

## 数据链路

```text
React Native request
        ↓
Spring Boot Browse API
        ↓ Micrometer Timer / Counter
/actuator/prometheus
        ↓ scrape every 5 seconds
Prometheus
        ↓ PromQL
Shared Grafana Dashboard
```

React Native App（移动客户端应用）不会直接访问 `/actuator/prometheus`。这个 Endpoint（端点）
是面向 Prometheus Scraper（Prometheus 抓取器）的管理接口。客户端开发者通过共享 Grafana
Dashboard 查看趋势，避免把管理接口和原始指标暴露到移动应用中。

## Browse Metrics（浏览指标）

`ProductBrowseMetrics` 定义三个专用指标：

| Micrometer Name（名称） | Type（类型） | Labels（标签） | Meaning（含义） |
|---|---|---|---|
| `lastcall.product.browse.duration` | Timer（计时器） | `cache`, `outcome` | Service Path Latency（服务路径延迟） |
| `lastcall.product.browse.requests` | Counter（计数器） | `cache`, `outcome` | 完成的浏览请求数 |
| `lastcall.product.browse.cache` | Counter（计数器） | `result` | Cache Hit / Miss（缓存命中/未命中） |

允许的 Label Values（标签值）只有：

- `cache=hit|miss|unknown`
- `outcome=success|failure`
- `result=hit|miss`

指标不包含 `userId`、`listingId`、`page` 或任意 URL 参数，因此不会随业务数据增长产生
High Cardinality（高基数）。

`lastcall.product.browse.duration` 启用 Percentile Histogram（百分位直方图），并设置从 1 ms 到
1 s 的 Service-Level Objective Buckets（服务级目标桶）。Prometheus 可以使用
`histogram_quantile()` 计算任意时间窗口内的 P50、P95 和 P99。

Spring Boot Actuator（执行器）仍然自动提供 `http.server.requests`。专用 Timer 负责区分 Redis
Cache Hit（缓存命中）和 Cache Miss（缓存未命中），自动 HTTP Timer 则反映 Controller、Service
和 Response Serialization（响应序列化）组成的服务器端请求耗时。

## Dashboard Panels（仪表盘面板）

自动 Provision（预配置）的 `LastCall Eats Backend Latency` Dashboard 包含：

- Backend Scrape Health（后端抓取健康状态）
- Browse P50 / P95 / P99 Latency（浏览延迟百分位）
- Browse Requests Per Second / RPS（每秒浏览请求数）
- Redis Browse Cache Hit Ratio（浏览缓存命中率）
- Cache Hit vs. Cache Miss P95（缓存命中与未命中的 P95 对比）
- Browse Error Rate（浏览错误率）
- Outbox Backlog（Outbox 积压）
- Publisher and Consumer Outcomes（发布者和消费者结果）

例如，Browse P95 使用：

```promql
1000 * histogram_quantile(
  0.95,
  sum(rate(lastcall_product_browse_duration_seconds_bucket{outcome="success"}[5m])) by (le)
)
```

单位乘以 1000 后从 Seconds（秒）转换为 Milliseconds（毫秒）。

Cache Hit Ratio（缓存命中率）使用：

```promql
100 *
sum(rate(lastcall_product_browse_cache_total{result="hit"}[5m]))
/
clamp_min(sum(rate(lastcall_product_browse_cache_total[5m])), 0.000001)
```

## 本地运行

先用 `dev` Profile（开发配置）启动 Spring Boot，默认监听 8080：

```bash
./mvnw spring-boot:run -pl lastcall-api
```

再启动 Observability Stack（可观测性环境）：

```bash
docker compose -f observability/docker-compose.yml up -d
```

访问：

| URL | Purpose（用途） |
|---|---|
| `http://localhost:3000/d/lastcall-backend-latency` | Grafana Dashboard |
| `http://localhost:9090/targets` | Prometheus Scrape Target（抓取目标） |
| `http://localhost:8080/actuator/prometheus` | Spring Boot 原始 Prometheus Metrics |

Grafana 的 Anonymous Viewer（匿名只读访问）已启用，且端口只绑定在 `127.0.0.1`。如果团队需要
跨机器共享，应部署到受保护的内部环境，并配置 Authentication（身份验证）和 Network Access
Control（网络访问控制）。

## Baseline Profile（基准测试配置）

Baseline App（基准测试应用）监听 8081。若要观察它，将
`observability/prometheus/prometheus.yml` 中的 Target（目标）从
`host.docker.internal:8080` 改成 `host.docker.internal:8081`，再重启 Prometheus。

## 如何判断问题位于哪里

- HTTP P95 和 Browse Service P95 同时升高：优先检查 Redis Hit Ratio、MySQL 查询和连接池。
- HTTP P95 升高但 Browse Service P95 正常：检查 Security Filter（安全过滤器）、Response
  Serialization（响应序列化）、Servlet Thread Pool（Servlet 线程池）或其他请求层开销。
- Cache Miss P95 正常但 Hit Ratio 降低：性能下降主要来自更多请求回源 MySQL，应检查 TTL、
  Invalidation（失效）频率和 Redis 可用性。
- Cache Hit P95 也升高：检查 Redis 网络、连接池、CPU 和 Value Size（值大小）。
- Backend Scrape Health 为 0：Prometheus 无法访问 Spring Boot；检查应用端口、Profile 和
  `/actuator/prometheus` 暴露配置。

## 简历表述

代码和 Dashboard 可以直接支持以下描述：

> Instrumented the Spring Boot service with Micrometer and Prometheus, and delivered a shared
> Grafana dashboard for the React Native team to monitor backend P95 latency, throughput, error
> rates, and Redis cache performance.

这里的 `direct visibility` 指 React Native 开发者可以直接打开共享 Dashboard 查看后端趋势，
并不表示 Prometheus Metrics 被嵌入最终用户使用的移动 App。
