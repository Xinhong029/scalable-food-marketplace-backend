# 订单消息可观测性验证记录

完成日期：2026-09-13（America/Los_Angeles）。本步骤为订单 Outbox、RabbitMQ 和商家通知 Consumer 增加 Health Check、Metrics 和事件追踪日志。

## 解决的问题

异步系统中，创建订单的 HTTP 200 只表示订单事务成功，商家通知可能仍在 Outbox、RabbitMQ 或 Consumer 中。如果没有可观测性，排查人员只能逐个系统手工猜测消息停在哪里。

当前可以从三个方向检查：

```text
Health Check：MySQL、Redis、RabbitMQ 是否可连接
Metrics：消息成功、重复、失败、积压和延迟有多少
Logs：某一个 eventId 经过了哪些步骤
```

## Health Check（健康检查）

`lastcall-api` 引入 Spring Boot Actuator。baseline Profile 暴露：

| 端点 | 用途 |
|---|---|
| `/actuator/health` | 应用与依赖健康状态 |
| `/actuator/metrics` | Micrometer 指标名称和逐项查询入口 |
| `/actuator/prometheus` | Prometheus 可定期抓取的文本格式 |

baseline 服务只监听 `127.0.0.1`。本地检查返回：应用 `UP`，MySQL `UP`，Redis 7.4.11 `UP`，RabbitMQ 3.13.7 `UP`。

## Messaging Metrics（消息指标）

指标由 `OrderMessagingMetrics` 集中定义，并统一带有 `application=lastcall-eats` 标签。

| Micrometer 名称 | 类型 | 标签/含义 |
|---|---|---|
| `lastcall.outbox.publish` | Counter | `outcome=success/failure`，发布结果累计次数 |
| `lastcall.order.created.consumer` | Counter | `success/duplicate/failure`，Consumer 每次尝试的结果 |
| `lastcall.outbox.events` | Gauge | `pending/processing/published`，数据库当前各状态记录数 |
| `lastcall.order.created.end.to.end` | Timer | 从事件发生到通知写入完成的时间 |

- **Counter（计数器）**：只能增加，应用重启后从 0 重新开始。适合统计一段运行期间发生了多少次。
- **Gauge（仪表值）**：读取当前值。Outbox Gauge 直接统计数据库，所以重启应用后仍能反映表中现有状态。
- **Timer（计时器）**：同时记录次数与耗时，Prometheus 输出 Count、Sum 和 Histogram Bucket，可进一步计算平均值和 P95。

Consumer 的 `failure` 统计失败尝试，不统计失败消息数。一条消息在三次 Retry 中都失败，会得到 `failure=3`。

## Event Tracing Logs（事件追踪日志）

关键日志都包含 `eventId` 和 `orderId`；涉及通知时也包含 `merchantId`：

```text
ORDER_CREATED staged in outbox eventId=... orderId=... merchantId=...
ORDER_CREATED published eventId=... orderId=... exchange=... routingKey=...
ORDER_CREATED received eventId=... orderId=... merchantId=...
Merchant notification persisted eventId=... orderId=... merchantId=...
```

搜索一个 `eventId` 就能判断消息是否写入 Outbox、是否发布、是否被 Consumer 收到、通知是否落库。重复和失败日志也使用相同字段。

## 真实端到端验证

通过真实 HTTP API 创建订单 96：

```text
orderId: 96
eventId: 75e345dc-abf1-4b9c-94e4-d8263af8fdb3
merchantId: 1
listingId: 1009987
```

MySQL 联表核对结果：Outbox 为 `PUBLISHED`，存在对应 `processed_event` 和 `merchant_notification`。RabbitMQ 主 Queue 最终为 0，说明消息已 ACK。

Prometheus 指标：

```text
outbox publish success: 1
consumer success:       1
consumer duplicate:     1
consumer failure:       3
end-to-end count:       1
end-to-end sum:         0.506739 seconds
outbox pending:         0
outbox processing:      0
outbox published:       2
```

其中端到端样本约 0.507 秒，包含 Outbox 最长约 1 秒的定时扫描等待、RabbitMQ 传输和通知写库。本次只有一个样本，不能把它当作稳定的 P95。

重复验证将订单 96 的原事件再次发布，`duplicate` 从 0 增加到 1，通知仍为 1 条。失败验证使用不存在的订单，让同一消息失败三次，`failure` 从 0 增加到 3，随后消息进入 DLQ；数据库事务没有留下处理记录或通知。验证后已清理该测试死信。

## 本地检查

后端以 baseline Profile 运行时执行：

```bash
python3 scripts/messaging/check_observability.py
```

脚本验证应用、MySQL、Redis、RabbitMQ 都为 `UP`，检查四组自定义指标存在，并打印关键 Prometheus 样本。

## 当前边界

本步骤提供可抓取指标，还没有部署独立 Prometheus Server 或 Grafana Dashboard。生产环境通常还会为 Outbox 积压、发布失败率、Consumer 失败率和 DLQ 消息数配置告警，并限制 Actuator 端点的网络访问范围。
