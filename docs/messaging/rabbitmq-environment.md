# RabbitMQ 本地环境

完成日期：2026-09-13（America/Los_Angeles）。本环境用于订单异步消息、故障恢复和死信队列实验。

## 运行方式

RabbitMQ 使用项目专用 Docker 容器运行：

```text
容器：lastcall-rabbitmq
镜像：rabbitmq:3.13-management-alpine
RabbitMQ：3.13.7
重启策略：unless-stopped
数据卷：lastcall-rabbitmq-data
```

端口只绑定本机：

| 地址 | 用途 |
|---|---|
| `127.0.0.1:5672` | AMQP，Spring Boot 发送和接收消息 |
| `127.0.0.1:15672` | RabbitMQ Management 管理页面和 HTTP API |

本地学习环境凭据：

```text
username: lastcall
password: lastcall-local
virtual host: /
```

管理页面为 `http://127.0.0.1:15672`。这些是 baseline 本地凭据，部署环境通过环境变量覆盖。

## 创建命令

```bash
docker run -d \
  --name lastcall-rabbitmq \
  --restart unless-stopped \
  -p 127.0.0.1:5672:5672 \
  -p 127.0.0.1:15672:15672 \
  -e RABBITMQ_DEFAULT_USER=lastcall \
  -e RABBITMQ_DEFAULT_PASS=lastcall-local \
  -v lastcall-rabbitmq-data:/var/lib/rabbitmq \
  --health-cmd 'rabbitmq-diagnostics -q ping' \
  --health-interval 5s \
  --health-timeout 5s \
  --health-retries 12 \
  rabbitmq:3.13-management-alpine
```

日常操作：

```bash
docker start lastcall-rabbitmq
docker stop lastcall-rabbitmq
docker logs lastcall-rabbitmq
docker exec lastcall-rabbitmq rabbitmq-diagnostics -q ping
```

Docker Volume 保存 RabbitMQ 数据目录。停止或重新启动容器时，持久化 Queue 和持久化 Message 可以保留；删除 Volume 才会删除其中的数据。

## Spring baseline 配置

`application-baseline.yml` 已加入 `spring.rabbitmq`：

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:127.0.0.1}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:lastcall}
    password: ${RABBITMQ_PASSWORD:lastcall-local}
    virtual-host: ${RABBITMQ_VHOST:/}
    connection-timeout: 2s
```

冒号后面的值是本地默认值。部署时可以通过 `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` 和 `RABBITMQ_VHOST` 覆盖。

## 验证结果

| 检查 | 结果 |
|---|---|
| Docker Health | `healthy` |
| RabbitMQ Diagnostics | `Ping succeeded` |
| AMQP 端口 5672 | TCP 连接成功 |
| Management 端口 15672 | TCP 连接成功 |
| Management API | 认证成功，报告 RabbitMQ 3.13.7 |
| 临时 Queue 创建 | HTTP 201 |
| 发布测试消息 | `routed=true` |
| 发布后 Ready 消息 | 1 |
| 取出消息 | payload 完整，`redelivered=false` |
| 取出后剩余消息 | 0 |
| 临时 Queue 清理 | HTTP 204 |
| 持久化验证 | Durable Queue 和 delivery mode 2 消息在容器重启后仍存在 |
| 重启后消息状态 | `messages=1`、`messages_persistent=1`，取出内容完整 |

测试 Payload：

```json
{
  "type": "environment-check",
  "message": "RabbitMQ is ready"
}
```

这次测试使用 RabbitMQ 默认 Exchange 和临时 Queue，只验证基础设施。业务 Exchange、Routing Key、持久化 Queue 和 DLQ 的 Spring 声明与验证见
[订单消息拓扑](order-messaging-topology.md)。
