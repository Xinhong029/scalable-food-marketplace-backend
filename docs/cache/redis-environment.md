# Redis 本地学习环境

完成日期：2026-09-12（America/Los_Angeles）。

## 当前环境

- Docker 镜像：`redis:7.4-alpine`
- Redis 服务端：7.4.11
- 容器名：`lastcall-redis`
- 宿主机地址：`127.0.0.1:6379`
- 重启策略：`unless-stopped`

端口只绑定到本机回环地址，没有暴露到局域网或公网。这个无密码实例只用于本地开发与性能实验。

## 创建与启动

首次创建：

```bash
docker run -d \
  --name lastcall-redis \
  --restart unless-stopped \
  -p 127.0.0.1:6379:6379 \
  redis:7.4-alpine
```

容器已经存在时：

```bash
docker start lastcall-redis
```

停止：

```bash
docker stop lastcall-redis
```

## 验证

```bash
docker exec lastcall-redis redis-cli PING
```

期望输出：

```text
PONG
```

本次还验证了临时键的写入、读取、TTL 和删除：

```text
SET lastcall:environment-check ok EX 30  -> OK
GET lastcall:environment-check           -> ok
TTL lastcall:environment-check           -> 30
DEL lastcall:environment-check           -> 1
```

临时键已经删除。宿主机通过 `127.0.0.1:6379` 发送 Redis RESP `PING` 也收到 `+PONG`，证明端口映射可用。

## 项目状态

`lastcall-common` 已依赖 `spring-boot-starter-data-redis`，因此应用具备 Lettuce Redis 客户端。项目尚未配置缓存管理器、序列化、TTL、缓存读取或失效逻辑。
