# 本地基线实验

## 1. 初始化独立数据库

在项目根目录执行：

```bash
python3 scripts/baseline/init_db.py
```

要求本地 MySQL 在 127.0.0.1:3306 运行，默认使用 root 和项目 dev 配置中的数据库密码默认值。
可通过 DB_USERNAME、DB_PASSWORD 环境变量覆盖，不要把真实密码写入提交或聊天。

脚本创建 lastcall_eats_baseline，导入现有测试用户、商家和商品。
数据库已经存在时直接报错，不会自动清空或重新导入。中途失败可能留下部分表，需检查后再处理。
不要直接执行原 sql/seed.sql：它包含 USE lastcall_eats 和 TRUNCATE，会清空原数据库中的表。

## 2. 启动配置

IntelliJ 的 Spring Boot 运行配置中将 Active profiles 设为 baseline。
如果没有该字段，在 Program arguments 中填写 --spring.profiles.active=baseline。
只启用 baseline，不要同时启用 dev。应用将监听 http://127.0.0.1:8081。

baseline 使用 ddl-auto: validate：只检查表与实体是否兼容，不会在启动时自动改表。
第一次验证发现 OrderDO 枚举的默认 MySQL 类型与 schema.sql 的 VARCHAR 不一致，
已为该字段显式指定 JDBC VARCHAR 映射，保持现有 SQL 存储格式。
JWT、MinIO 和 Stripe 值仅供本地实验，Stripe 占位值不能进行支付。
数据库目前只隔离了数据集，仍与开发库共用 MySQL 进程和本地账号；压测会共享 CPU、内存和数据库资源。

也可以在项目根目录用终端构建和启动：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw package
"$(/usr/libexec/java_home -v 21)/bin/java" -jar lastcall-api/target/lastcall-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=baseline
```

启动后在另一个终端检查：

```bash
curl -i http://127.0.0.1:8081/api/products/browse
```

如果 8081 已有本次实验启动的服务，不要重复启动。在自己启动服务的终端按 Ctrl+C 可停止。

## 3. 登录、下单、查询的顺序冒烟测试

确认 8081 上运行的是上述 baseline 配置，然后在项目根目录执行：

```bash
python3 scripts/baseline/smoke_order.py
```

脚本使用 Alice 示例账号登录，将 token 保存在内存里，带 Authorization: Bearer 请求头下单和查询。
每次执行会选择 Alice 尚未购买且至少剩余两份的 listing，实际创建一个待支付订单并扣减一份库存。
随后重复购买同一个 listing，验证当天重复购买被拒绝、订单数和库存没有再次变化。
没有可选商品时脚本停止，不自动重置数据；不要把它当作无限重复的压测脚本。
它只验证顺序业务流程，不证明并发安全或请求幂等性，不调用支付。

本次执行结果：Alice 购买 listing 2（Croissant Box），订单 7；库存 3 → 2；
状态 PENDING_PAYMENT；顺序重复购买返回 HTTP 400，库存仍为 2。
Alice 的订单数从 2 增至 3。运行报告保存在 target/baseline/smoke-order.json，不包含 token。

## 4. 商品浏览性能实验

使用 k6 本地执行；安装方法见 https://grafana.com/docs/k6/latest/set-up/install-k6/ 。
本次使用 k6 2.2.0，所有请求发往 127.0.0.1:8081，不发送到云压测服务。

少量数据测试（仅适用于浏览接口仍返回 9 条时）：

```bash
k6 run --quiet --no-usage-report -e DURATION=5s -e REPORT=target/baseline/warmup-9.json scripts/baseline/browse.js
python3 scripts/baseline/observe.py --count 9 --vus 1
```

扩充独立测试库到总计 10,000 条 listing；新增 listing 各有一个不同模板，轮流关联原有商家。
该操作保留原有商品和订单；重复运行不会继续扩充。仅在没有其他测试写入时运行。

```bash
python3 scripts/baseline/seed_load.py
k6 run --quiet --no-usage-report -e EXPECTED_COUNT=10000 -e DURATION=5s -e REPORT=target/baseline/warmup-10000.json scripts/baseline/browse.js
python3 scripts/baseline/observe.py --count 10000 --vus 1
python3 scripts/baseline/observe.py --count 10000 --vus 5
python3 scripts/baseline/observe.py --count 10000 --vus 10
```

每档默认 30 秒，固定数量 VU，无思考时间，每个 VU 等上次请求完成后才发下次请求。
这是闭环负载模型；响应变慢会自动降低请求到达率，不能用来宣称系统的最大承载能力。
每次请求都检查 HTTP 200、业务码、返回条数和基本字段。15 秒请求超时，失败率与内容校验分别记录。
响应字符数不是字节数；data_received 是包含协议开销的接收字节量。
单次短测试中的 P99 只是探索性指标，样本不足时尤其不稳定。

原始结果：target/baseline/<数据条数>-vu<并发数>.json。
资源采样：同目录下对应的 -resources.json；约每 2 秒记录进程 CPU、RSS 和 MySQL 全局状态。
MySQL 计数包括同一服务上的其他客户端，采样自身也会短暂打开连接；不能直接当作应用连接池指标。
压测客户端、后端、数据库共用本机资源。本轮先保存实际配置，后续相同配置下比较优化收益。

## 环境检查记录

- Java：21.0.10（终端原默认版本为 17）。
- Maven Wrapper：3.9.12。
- MySQL 服务端：9.6.0；README 声明 MySQL 8，后续报告需注明实际版本。
- 现有自动化测试：120 个，0 失败，0 错误；这些不是性能或真实 MySQL 并发测试。
- 已完成 9 条数据 / 1 VU，以及 10,000 条数据 / 1、5、10 VU 的初始基线，详见 [结果报告](baseline-results/2026-09-11/report.md)。

## 5. 并发库存实验

执行方式与注意事项见 `scripts/baseline/concurrent_order.py` 文件说明。每轮新增 100 个用户和一个库存 10 的 listing。

- 修改前：[17 个订单抢 10 份库存，并出现 58 个死锁 HTTP 500](concurrency-results/5061094f8f8e/report.md)。
- 原子扣减修复后：[10 个订单、库存 0、90 个业务拒绝、0 个 HTTP 500](concurrency-results/304ca71f14c5/report.md)。

## 6. 商品浏览当前查询分析

当前代码路径、MySQL `EXPLAIN ANALYZE`、单次 HTTP/SELECT 对照以及候选 JOIN 的初始执行计划，见
[current-query-analysis.md](query-optimization/current-query-analysis.md)。本步骤只做诊断，没有修改商品浏览查询。

## 7. 商品浏览数据库分页

数据库过滤、Slice 分页、稳定排序、参数限制、前端滚动加载和阶段性性能结果，见
[database-pagination.md](query-optimization/database-pagination.md)。

## 8. 消除商品浏览 N+1 查询

三表 Join、DTO Projection、每页业务 SELECT 从 24 次降为 1 次的验证，以及当前缺少组合索引造成的
执行计划瓶颈，见 [join-projection.md](query-optimization/join-projection.md)。

## 9. 商品浏览索引优化

浏览组合索引、先分页再 JOIN、最终执行计划与三阶段性能对照，见
[index-optimization.md](query-optimization/index-optimization.md)。

## 10. Redis 本地环境

项目专用 Redis 容器、版本、端口绑定和验证命令，见
[redis-environment.md](cache/redis-environment.md)。

Redis 商品浏览缓存上线前的固定性能对照见
[pre-cache-baseline.md](cache/pre-cache-baseline.md)。

商品浏览 Cache-Aside 的实现、命中验证、TTL、故障回退和性能对照见
[cache-aside.md](cache/cache-aside.md)。

商品创建、下架和下单后的事务提交缓存失效设计与验证见
[cache-invalidation.md](cache/cache-invalidation.md)。

## 11. RabbitMQ 本地环境

项目专用 RabbitMQ 容器、AMQP 和管理端口、持久化数据卷以及测试消息验证见
[rabbitmq-environment.md](messaging/rabbitmq-environment.md)。

## 12. 订单消息契约与 RabbitMQ 拓扑

`ORDER_CREATED` 事件字段、业务 Exchange、Routing Key、持久化主 Queue、DLX、DLQ，以及正常和失败路由验证见
[order-messaging-topology.md](messaging/order-messaging-topology.md)。

## 13. Transactional Outbox

订单与事件的同事务写入、Publisher Confirm、原子抢占、指数退避、RabbitMQ 停机与恢复实验见
[transactional-outbox.md](messaging/transactional-outbox.md)。

## 14. 幂等商家通知 Consumer

订单创建事件的异步消费、`processed_event` 原子去重、商家站内通知 API、失败重试与 DLQ 真实验证见
[idempotent-merchant-notification-consumer.md](messaging/idempotent-merchant-notification-consumer.md)。

## 15. 订单消息可观测性

Actuator 依赖健康检查、Micrometer/Prometheus 消息指标、`eventId` 追踪日志和真实订单端到端验证见
[observability.md](messaging/observability.md)。
