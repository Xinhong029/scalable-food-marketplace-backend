# 幂等商家通知 Consumer 验证记录

完成日期：2026-09-13（America/Los_Angeles）。本步骤让 `ORDER_CREATED` 消息产生可查询的商家站内通知，并补上重复消息去重、失败重试和 DLQ。

## 解决的问题

Transactional Outbox 提供 At-least-once Delivery（至少一次投递）。它保证事件最终能送达，但在 Publisher 收到 RabbitMQ ACK 后、更新 MySQL 前崩溃时，同一事件可能再次发送。如果 Consumer 每收到一次就创建一次通知，商家会看到重复通知。

当前消费路径是：

```text
lastcall.order.created.queue
        ↓ MerchantNotificationConsumer
同一个 MySQL Transaction
├── INSERT IGNORE processed_event
└── INSERT merchant_notification
        ↓ Transaction 提交成功
RabbitMQ ACK
```

## 幂等处理（Idempotent Consumption）

`processed_event` 使用 `(event_id, consumer_name)` 唯一约束。Consumer 先执行：

```sql
INSERT IGNORE INTO processed_event (event_id, consumer_name, processed_at)
VALUES (?, 'merchant-notification-consumer', NOW(6));
```

- 返回 1：这是该 Consumer 第一次看到这个事件，继续创建通知。
- 返回 0：唯一约束说明事件已经成功处理，直接正常返回，让 RabbitMQ ACK 重复消息。

这里使用一条数据库语句完成判断与写入，避免两个线程先后执行“查询是否存在”和“插入”时产生竞争。`INSERT IGNORE` 是 MySQL 写法；如果迁移到 PostgreSQL，可换成 `INSERT ... ON CONFLICT DO NOTHING`。

去重记录和通知写入受同一个 `@Transactional` 控制。如果创建通知失败，去重记录也会回滚。下次重试仍能继续处理，不会因为一条提前提交的去重记录而错误跳过消息。

`merchant_notification.event_id` 还有一层唯一约束，防止程序缺陷绕过去重表后创建重复通知。

## ACK、重试和 DLQ

Listener 使用自动确认（Automatic Acknowledgement）：

- 方法正常返回：Spring 向 RabbitMQ 发送 ACK，消息从主 Queue 删除。
- 方法抛出异常：当前数据库事务回滚，Spring Retry 在 Consumer 内重新调用方法。
- 共失败 3 次：消息被 Reject，并且 `default-requeue-rejected=false`，所以不会无限回到主 Queue。
- 主 Queue 的死信配置将被拒绝的消息发送到 DLX，再路由到 `lastcall.order.created.dlq`。

重试间隔为 1 秒、2 秒，最大间隔限制为 5 秒。DLQ 保存需要人工排查或修复后重新投递的消息。

## 商家通知 API

两个接口都位于 `/api/merchant/**`，需要 `ROLE_MERCHANT` JWT：

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/merchant/notifications` | 按创建时间倒序返回当前登录商家的通知 |
| PATCH | `/api/merchant/notifications/{id}/read` | 把属于当前商家的通知标为已读 |

标记已读时用 `id + merchant_id` 一起查找，因此商家不能通过猜测通知 ID 修改其他商家的通知。

## 自动化测试

- `MerchantNotificationConsumerTest`：第一次事件创建通知，重复事件跳过。
- `MerchantNotificationServiceImplTest`：通知倒序查询、标记已读，以及商家不能读取不属于自己的通知。
- `verify_notification_api.py`：使用示例商家登录，查询真实通知并通过 HTTP 标记已读；JWT 只保存在脚本内存中，不输出。

## 真实链路验证

向业务 Exchange 连续发布两份相同事件：

```text
event_id: 1fc90fc1-ecbb-40c2-a4fc-9a7f46951a51
order_id: 95
merchant_id: 2
RabbitMQ publish: routed=true（两次）
```

结果：

```text
merchant_notification: 1 条
processed_event: 1 条
应用日志: Skipping duplicate ORDER_CREATED event ...
```

商家 API 验证结果：登录 200、列表查询 200、标记已读 200，订单 95 的通知从 `read=false` 变为 `read=true`，并写入 `readAt`。

失败路径使用不存在的订单 `999999999` 触发外键失败。日志记录了 3 次数据库尝试，然后显示 `Retries exhausted`。最终结果：

```text
merchant_notification: 0 条
processed_event: 0 条（事务已回滚）
DLQ: 1 条
x-first-death-reason: rejected
routing_key: order.created.dead
```

读取证据后已确认并清理该测试死信，主 Queue 和 DLQ 均无残留测试消息。

## 当前边界

本步骤实现的是持久化站内通知，尚未接入邮件、短信或移动端 Push。DLQ 目前供开发人员在 RabbitMQ 管理界面排查；生产系统还可以增加 DLQ 告警和经过修复后的人工重放工具。
