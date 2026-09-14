# 订单消息拓扑

完成日期：2026-09-13（America/Los_Angeles）。本步骤建立 `ORDER_CREATED` 事件契约和 RabbitMQ 业务拓扑，还没有把订单事务接到消息发布器，也没有实现商家通知 Consumer。

## 要解决的问题

订单创建完成后，通知商家这类后续工作不应该延长用户的下单请求。后续会由订单服务发送事件，Consumer 异步处理通知：

```text
创建订单
  -> Producer 发布 ORDER_CREATED
  -> Exchange 根据 Routing Key 路由
  -> 主 Queue 保存消息
  -> Consumer 异步通知商家
```

本步骤先固定事件格式和传输通道，让后续 Producer、Transactional Outbox 和 Consumer 使用同一份契约。

## 事件契约（Event Contract）

`OrderCreatedEvent` 包含：

| 字段 | 含义 |
|---|---|
| `eventId` | 每个事件唯一的 UUID，后续用于幂等去重 |
| `eventType` | 固定为 `ORDER_CREATED` |
| `orderId` | 新订单 ID |
| `userId` | 下单用户 ID |
| `merchantId` | 接收通知的商家 ID |
| `listingId` | 被购买的上架商品 ID |
| `price` | 订单价格 |
| `occurredAt` | 事件发生的 UTC 时间 |

事件是过去已经发生的事实，所以名称使用 `OrderCreated`，而不是命令式的 `CreateOrder`。

## RabbitMQ 拓扑（Topology）

正常路径：

```text
lastcall.order.exchange (Topic Exchange, durable)
  -- routing key: order.created -->
lastcall.order.created.queue (durable)
```

失败路径：

```text
主 Queue 中的消息被拒绝并且不重新入队
  -> lastcall.order.dlx (Direct Exchange, durable)
  -- routing key: order.created.dead -->
lastcall.order.created.dlq (durable)
```

- **Exchange（交换机）**：像分拣中心，根据 Routing Key 决定消息去哪个 Queue。
- **Routing Key（路由键）**：消息上的分类标签。本事件使用 `order.created`。
- **Queue（队列）**：保存等待 Consumer 处理的消息。
- **DLX（Dead-letter Exchange，死信交换机）**：接收处理失败或被拒绝的消息。
- **DLQ（Dead-letter Queue，死信队列）**：保存失败消息，避免消息无声丢失，便于排查和重新处理。

业务 Exchange 使用 Topic 类型，后续可以增加 `order.paid`、`order.cancelled` 等 Routing Key。DLX 使用 Direct 类型，因为这里只需要精确匹配 `order.created.dead`。

所有 Exchange 和 Queue 都设置 `durable=true`；测试消息使用 `delivery_mode=2`。两者一起使用时，RabbitMQ 重启后仍能恢复持久化消息。

## Spring 实现

`OrderMessagingConfig` 把 Exchange、Queue、Binding、DLX、DLQ 声明为 Spring Bean。`RabbitAdmin` 负责把这些 Java 配置真正创建到 RabbitMQ；启动初始化器调用 `initialize()`，确保当前没有 Producer 或 Consumer 时也会建立连接并声明拓扑。

`Jackson2JsonMessageConverter` 负责在 Java Event 和 JSON Message 之间转换。这样发送方处理 Java 对象，RabbitMQ 中保存可读的 JSON。

## 验证结果

应用使用 Java 21 和 `baseline` Profile 启动后，RabbitMQ 管理接口返回：

| 检查 | 结果 |
|---|---|
| 业务 Exchange | `lastcall.order.exchange`，Topic，durable |
| 主 Queue | `lastcall.order.created.queue`，durable |
| 正常 Binding | `order.created` |
| DLX | `lastcall.order.dlx`，Direct，durable |
| DLQ | `lastcall.order.created.dlq`，durable |
| 死信 Binding | `order.created.dead` |
| 正常事件发布 | `routed=true` |
| 正常事件取出 | Exchange、Routing Key 和 JSON Payload 正确，`delivery_mode=2` |
| Consumer 拒绝模拟 | 主 Queue 使用 `reject_requeue_false` 拒绝 |
| 死信结果 | 同一消息进入 DLQ，`x-first-death-reason=rejected` |
| 测试后队列 | 主 Queue 和 DLQ 都已取空 |

单元测试验证 Event 的 JSON 序列化/反序列化，以及主 Queue 的持久化和死信参数。

## 当前实现

消息基础设施、契约和 Transactional Outbox 已经完成。真实下单会在同一 MySQL 事务中保存 Outbox Event，并由 Publisher 异步投递到 RabbitMQ；详见 [Transactional Outbox 验证记录](transactional-outbox.md)。

商家通知 Consumer 已经异步生成站内通知，并使用 `event_id` 防止重复处理；重试、DLQ 和真实重复消息验证见 [幂等商家通知 Consumer](idempotent-merchant-notification-consumer.md)。
