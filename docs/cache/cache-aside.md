# 商品浏览 Cache-Aside

完成日期：2026-09-12（America/Los_Angeles）。本步骤给分页商品浏览接口加入 Redis 旁路缓存，并验证命中、未命中、TTL 和 Redis 故障回退。

## 请求流程

```text
GET /api/products/browse?page=0&size=20
        ↓
用 page + size 组成 Redis key
        ↓
Redis 中有数据？
  ├─ 有：反序列化 JSON 并直接返回（Cache Hit）
  └─ 无：执行分页 JOIN 查询 MySQL（Cache Miss）
              ↓
          把结果序列化成 JSON，写入 Redis，TTL 60 秒
              ↓
          返回结果
```

Redis 暂时不可用或缓存 JSON 无法解析时，缓存组件返回 Miss，服务继续查询 MySQL。缓存写入失败也不会覆盖已经得到的数据库结果。这种策略称为 Fail-open。

## 缓存结构

键的格式：

```text
lastcall-eats:product-browse:page:{page}:size:{size}
```

第一页、每页 20 条的实际键是：

```text
lastcall-eats:product-browse:page:0:size:20
```

值是 JSON，包含 `content`、`page`、`size` 和 `hasNext`。缓存没有直接保存 Spring 的 `SliceImpl`，避免让缓存格式依赖框架内部实现。

TTL 默认是 60 秒，可通过配置项 `app.cache.product-browse-ttl` 修改。Redis 键到期后自动删除，下一次请求重新访问 MySQL 并建立新缓存。

## 修改文件

- `lastcall-product/.../cache/ProductBrowseCache.java`：读取和写入 Redis，生成分页键，设置 TTL；Redis 出错时回退 MySQL。
- `lastcall-product/.../cache/ProductBrowseCacheEntry.java`：定义稳定、可序列化的分页缓存结构，并负责还原成 Spring `Slice`。
- `lastcall-product/.../service/impl/ProductListingServiceImpl.java`：在数据库查询前读缓存，在 Cache Miss 查询后写缓存。
- `lastcall-product/.../dto/UserBrowseResponse.java`：加入 Jackson JSON 构造参数，使商品对象可以从 Redis JSON 恢复。
- `lastcall-api/src/main/resources/application-baseline.yml`：配置本地 Redis 地址、连接超时和 60 秒 TTL。
- `lastcall-api/src/main/resources/application.yml`：关闭未使用的 Redis Repository 扫描；项目通过 `StringRedisTemplate` 使用 Redis。
- `lastcall-product/.../ProductListingServiceImplTest.java`：验证 Cache Hit 不执行数据库浏览查询，Cache Miss 写入缓存。

## 功能验证

测试 URL 为 `GET /api/products/browse?page=0&size=20`。

| 检查 | 结果 |
|---|---|
| 初始缓存键 | 不存在 |
| 第一次请求 | HTTP 200；MySQL 查询计数 29,328 → 29,329；创建 Redis 键 |
| Redis value 大小 | 6,636 bytes |
| TTL | 写入时 60 秒；首次人工检查时剩余 51 秒 |
| 第二次相同请求 | HTTP 200；MySQL 查询计数保持 29,329 |
| 响应一致性 | 两个响应各 20 条；SHA-256 相同；完整 JSON 相等 |
| TTL 到期 | 键自动消失；下一次请求使 MySQL 计数 29,329 → 29,330；TTL 恢复为 60 秒 |
| Redis 停止 | 接口仍返回 HTTP 200；MySQL 计数 29,331 → 29,332 |
| Redis 恢复 | `PING` 返回 `PONG` |

## 性能对照

测试条件均为约 10,000 条可售 listing、第一页 20 条、1 VU、30 秒、无思考时间。

| 指标 | 缓存前 | Cache-Aside 后 | 变化 |
|---|---:|---:|---:|
| 完成请求 | 14,007 | 23,724 | +69.38% |
| P50 | 1.928 ms | 1.154 ms | -40.15% |
| P95 | 2.622 ms | 1.581 ms | -39.70% |
| P99 | 3.027 ms | 2.391 ms | -21.01% |
| 请求/秒 | 466.90 | 790.77 | +69.37% |
| HTTP 失败率 | 0% | 0% | 不变 |
| MySQL SELECT | 14,007 | 1 | -99.99% 以上 |

缓存后测试从空键开始。23,724 次 HTTP 请求只执行第一次 MySQL 查询，其余 23,723 次由 Redis 提供数据。可长期保留的测量摘要位于 `docs/cache/results/2026-09-12-post-cache.json`。

## 数据一致性

Cache-Aside 的读取、TTL 和故障回退已经完成。商品创建、下架和下单扣库存后的事务提交缓存失效见 `docs/cache/cache-invalidation.md`。
