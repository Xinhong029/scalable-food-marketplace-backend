# LastCall Eats：简历与面试讲解

这份文档用于把项目中的技术工作讲清楚。简历只放可以由仓库代码和实验报告支持的数据；面试时再展开设计选择与边界。

## 英文简历描述

建议根据版面选择其中 3 条：

- Engineered concurrency-safe inventory reservation with a conditional atomic MySQL update and transactional order creation; a 100-client contention test for 10 units produced exactly 10 orders, zero overselling, and zero server errors.
- Redesigned a 10,000-item browse endpoint using database pagination, DTO projection, Join queries, and a composite index, reducing business SELECTs from about 10,004 to 1 per request and local P95 latency from 511 ms to 2.52 ms.
- Implemented Redis Cache-Aside with TTL, post-commit invalidation, and database fallback, reaching 790.77 requests/s and serving 23,724 requests with one MySQL SELECT in a controlled 30-second benchmark.
- Built at-least-once order event delivery with a Transactional Outbox, atomic publisher claims, RabbitMQ Publisher Confirms, exponential backoff, an idempotent Consumer, retry, and a dead-letter queue.
- Added Actuator and Micrometer observability for MySQL, Redis, RabbitMQ, Outbox backlog, publication/consumption outcomes, end-to-end event latency, and event-ID-correlated logs.

## 中文简历描述

- 使用 MySQL 条件原子更新和事务化订单创建解决并发超卖；100 个客户端竞争 10 份库存时恰好生成 10 个订单，库存归零且无 HTTP 500。
- 通过数据库分页、DTO Projection、三表 Join 和组合索引优化 10,000 条商品浏览，将单次请求业务 SELECT 从约 10,004 次降至 1 次，本地 P95 从约 511 ms 降至 2.52 ms。
- 实现带 TTL、事务提交后失效和数据库降级的 Redis Cache-Aside；固定实验达到约 790.77 请求/秒，23,724 次请求仅产生 1 次 MySQL SELECT。
- 使用 Transactional Outbox、Publisher Confirm、原子抢占和指数退避实现订单事件至少一次投递，并通过幂等 Consumer、Retry、DLQ 可靠生成商家通知。
- 使用 Actuator、Micrometer 和 Prometheus 指标监控依赖健康、Outbox 积压、消息处理结果和端到端延迟，并通过 `eventId` 关联异步链路日志。

## 一分钟项目介绍

LastCall Eats 是一个帮助商家折价销售当日剩余食品的 React Native 和 Spring Boot 应用。我重点升级了后端在数据量和故障场景下的可靠性。

商品浏览原来一次返回一万条数据，并产生约一万次关联查询。我把它改为数据库分页，通过一次 Join Projection 取回页面需要的商品、模板和商家字段，再用组合索引和先分页后连接让 MySQL 只处理当前页。之后加入 Redis Cache-Aside、TTL 和事务提交后的缓存失效。

订单侧使用数据库条件原子扣减避免超卖。为了避免订单已经提交但 RabbitMQ 消息发送失败，我使用 Transactional Outbox，把订单和事件写进同一个 MySQL 事务。Publisher 异步发送并等待 RabbitMQ Confirm；Consumer 使用 `eventId` 去重，失败三次后进入 DLQ。最后用 Actuator、Micrometer 和关联日志观察依赖状态、消息积压、处理结果和延迟。

## 重点面试问题

### 为什么不用 Java `synchronized` 解决库存竞争？

`synchronized` 只约束一个 JVM。应用启动多个实例后，不同机器上的请求仍然会同时修改库存。数据库条件更新把“检查库存”和“扣减”合成一条原子 SQL，所有实例最终都经过同一个数据库约束。

### 为什么订单成功后不直接发送 RabbitMQ 消息？

MySQL 提交和 RabbitMQ 发送是两个独立操作。先提交订单再发消息时，RabbitMQ 故障会导致有订单、无消息；先发消息再提交订单时，数据库失败可能导致有消息、无订单。Outbox 把订单和事件放在同一个 MySQL 事务里，Publisher 稍后负责投递。

### Outbox 是否保证消息只发送一次？

不保证。当前提供 At-least-once Delivery（至少一次投递）。Publisher 可能已经发送成功，却在更新 Outbox 状态前崩溃，因此会再次发送。Consumer 使用 `(event_id, consumer_name)` 唯一约束实现幂等处理。

### Retry 和 DLQ 分别解决什么？

Retry 处理短暂故障，例如数据库瞬时不可用。超过三次仍失败，继续重试会阻塞或浪费资源，所以消息被拒绝并通过 DLX 保存到 DLQ，等待排查和重放。

### 为什么分页和 Join 后还需要组合索引？

分页限制了返回量，Join 消除了 N+1，但数据库仍可能扫描和排序一万条记录后才找到第一页。组合索引按照过滤与排序顺序组织数据，让 MySQL 沿索引读取第一页所需的 21 条记录后停止。

### Cache-Aside 如何保证数据一致？

读取时先查 Redis，Miss 再查 MySQL 并回填。写操作先提交数据库，事务成功后删除浏览缓存；下次读取再建立新缓存。TTL 是额外的最终恢复机制。这个方案提供实用的最终一致性，不承诺 Redis 与 MySQL 在每个瞬间完全一致。

### 如何排查“订单成功但商家没有通知”？

先用 `eventId` 查 Outbox 是否存在、状态是否为 `PUBLISHED`，再看 Publisher 和 Consumer 日志。Metrics 可以判断发布失败或 Consumer 失败是否在增长，RabbitMQ 管理页检查主 Queue 和 DLQ，最后查 `processed_event` 与 `merchant_notification`。

## 数据使用边界

性能数字来自同一台本地机器上的固定 k6 探索实验，不代表生产环境容量。P95、请求/秒和 SELECT 次数只在相同数据量、页面大小、并发和持续时间下比较。面试中应称为 controlled local benchmark（受控本地基准），不要称为系统最大吞吐量。
