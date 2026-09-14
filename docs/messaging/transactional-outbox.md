# Transactional Outbox 验证记录

完成日期：2026-09-13（America/Los_Angeles）。本步骤把真实下单事务接到 `ORDER_CREATED` 事件，并通过 MySQL Outbox 将事件可靠投递到 RabbitMQ。

## 解决的问题

直接依次写 MySQL 和 RabbitMQ 会产生 Dual-write Problem（双写问题）：订单可能已经提交，而消息发送失败。普通 `@Transactional` 只能控制 MySQL，不能让两个独立系统共享一次提交。

当前流程改为：

```text
同一个 MySQL Transaction
├── 条件原子扣减库存
├── INSERT orders
└── INSERT outbox_event (PENDING)
             ↓ Transaction 提交后
       Outbox Publisher
             ↓ Publisher Confirm ACK
       RabbitMQ order.created Queue
             ↓
       outbox_event = PUBLISHED
```

订单和 Outbox Event 都写入 MySQL，因此任意一步失败会一起回滚。RabbitMQ 暂时不可用时，订单不依赖它在线；事件留在数据库中等待重试。

## Outbox 表

`outbox_event` 保存事件内容和投递状态：

| 字段 | 用途 |
|---|---|
| `event_id` | 全局唯一消息 ID，供追踪和 Consumer 幂等使用 |
| `aggregate_type` / `aggregate_id` | 事件所属业务对象，本次是 `ORDER` 和订单 ID |
| `event_type` | `ORDER_CREATED` |
| `exchange_name` / `routing_key` | RabbitMQ 投递地址 |
| `payload` | 完整事件 JSON |
| `status` | `PENDING`、`PROCESSING` 或 `PUBLISHED` |
| `retry_count` / `next_retry_at` | 失败次数和下一次允许重试时间 |
| `locked_at` | Publisher 抢占时间，供崩溃恢复使用 |
| `published_at` | RabbitMQ 确认接收的时间 |
| `last_error` | 最近一次失败原因 |

`idx_outbox_ready(status, next_retry_at, created_at, id)` 支持 Publisher 快速查找可发送事件。`event_id` 唯一索引防止同一事件记录被重复保存。

## 原子抢占（Atomic Claim）

多个后端实例可能同时看到一条 `PENDING` 记录。每个实例都必须通过条件更新抢占：

```sql
UPDATE outbox_event
SET status = 'PROCESSING', locked_at = ?
WHERE id = ? AND status = 'PENDING';
```

影响 1 行表示当前实例抢占成功；影响 0 行表示其他实例已经抢走。集成测试验证同一事件第一次抢占返回 1，第二次返回 0。

如果实例在抢占后崩溃，记录会暂时停在 `PROCESSING`。`locked_at` 超过 2 分钟后，Publisher 会将它恢复为 `PENDING`，避免事件永久卡住。

## RabbitMQ 确认与重试

消息设置 `delivery_mode=2`，并启用：

```yaml
spring.rabbitmq.publisher-confirm-type: correlated
spring.rabbitmq.publisher-returns: true
spring.rabbitmq.template.mandatory: true
```

Publisher 使用 `event_id` 作为 Correlation ID，等待最多 5 秒的 Publisher Confirm。只有 RabbitMQ 返回 ACK 且消息没有因为无法路由被退回，数据库状态才更新为 `PUBLISHED`。

失败后记录回到 `PENDING`，`retry_count + 1`，并使用有上限的 Exponential Backoff（指数退避）：2、4、8、16、32、60 秒，之后最多等待 60 秒。

这个设计提供 At-least-once Delivery（至少一次投递）。如果进程在 RabbitMQ ACK 后、MySQL 更新前崩溃，同一事件可能再次发送。因此下一步 Consumer 必须使用 `event_id` 实现 Idempotency（幂等处理）。

## 自动化验证

- `OrderOutboxWriterTest`：验证订单字段、路由元数据和 JSON Payload 被正确写入 Outbox。
- `OutboxPublisherTest`：验证成功确认后标记完成，失败后安排重试。
- `InventoryTransactionIntegrationTest`：验证 Outbox INSERT 失败时，订单 INSERT 和库存扣减一起回滚；验证条件抢占只能成功一次。
- `OrderServiceImplTest`：验证创建成功的订单一定调用 Outbox Writer，库存竞争失败时不会创建 Outbox Event。

## 真实故障恢复实验

初始 `outbox_event` 记录数为 0。保持 Spring Boot 和 MySQL 运行，停止项目 RabbitMQ 容器，然后通过真实 HTTP API 下单：

```text
order_id: 95
user_id: 1
merchant_id: 2
listing_id: 1009988
库存: 100 -> 99
HTTP: 200
```

RabbitMQ 停止期间，数据库状态为：

```text
aggregate_id: 95
event_type: ORDER_CREATED
status: PENDING
published_at: NULL
last_error: AmqpConnectException: Connection refused
```

这证明消息中间件故障没有撤销已经安全记录在 MySQL 中的订单事件，也没有让 HTTP 下单等待 RabbitMQ 恢复。

RabbitMQ 恢复并达到 healthy 后，将测试记录的 `next_retry_at` 提前到当前时间以立即触发验证。Publisher 自动重连并得到 ACK，最终状态：

```text
status: PUBLISHED
retry_count: 6
published_at: 2026-09-13 23:58:44.618275 UTC
last_error: NULL
```

主 Queue 中取出的消息具有：

```text
exchange: lastcall.order.exchange
routing_key: order.created
message_id: 1fc90fc1-ecbb-40c2-a4fc-9a7f46951a51
delivery_mode: 2
orderId: 95
```

消息内容与 Outbox 的 `event_id` 和订单 ID 完全一致。验证后已确认并取出该测试消息，主 Queue 没有残留测试消息。

## 当前边界

Outbox 记录目前作为投递审计保留，不会立即删除。生产环境后续可以增加定期归档或删除旧 `PUBLISHED` 记录的任务。

RabbitMQ 可能收到重复事件，这是 At-least-once Delivery 的正常边界。幂等 Merchant Notification Consumer 已经使用 `processed_event` 完成去重，重复消息不会生成重复通知；实现和真实故障验证见 [幂等商家通知 Consumer](idempotent-merchant-notification-consumer.md)。
