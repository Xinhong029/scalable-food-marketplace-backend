# 商品浏览缓存失效

完成日期：2026-09-12（America/Los_Angeles）。本步骤在商品创建、下架和下单扣库存后删除商品浏览分页缓存，避免在 TTL 内继续返回旧商品或旧库存。

## 为什么在事务提交后删除

数据库写操作和 Redis 删除不属于同一个数据库事务。如果先删缓存、随后数据库事务回滚，其他请求可能白白重建缓存。实现使用 Spring `TransactionSynchronization` 注册 `afterCommit()` 回调：

```text
修改数据库
    ↓
注册缓存失效回调
    ↓
数据库事务提交成功？
  ├─ 是：执行 Redis 缓存失效
  └─ 否：事务回滚，不执行回调
```

触发点包括：

1. 商家创建 listing；
2. 商家下架 listing；
3. 用户下单成功，库存原子扣减并创建订单。

## 删除哪些缓存

一个 listing 的创建、下架或库存变化可能影响第一页，也可能因为排序和分页边界影响后面的页面。因此当前实现删除所有以 `lastcall-eats:product-browse:` 开头的缓存。

代码使用 Redis `SCAN` 分批查找键，没有使用会一次遍历并阻塞 Redis 的 `KEYS` 命令。找到的键使用 `UNLINK` 删除，让 Redis 在后台回收 value 内存。

当前选择是正确性优先的粗粒度失效。浏览写入频率远小于读取频率，删除全部分页缓存成本可控。数据量和写入频率继续增长后，可升级为缓存版本号或事件驱动的精细失效。

## 故障行为

Redis 删除失败不会回滚已经成功提交的订单或商品修改。旧缓存最多在 60 秒 TTL 后自动消失，日志会记录 warning。这保持 MySQL 作为 Source of Truth，同时避免缓存故障破坏核心写入操作。

## 自动化和真实验证

- `ProductBrowseCacheTest` 验证事务提交前不访问 Redis，触发 `afterCommit()` 后才执行 `SCAN` 和 `UNLINK`。
- 商品服务测试验证创建和下架成功时注册失效；失败路径不修改数据库。
- 订单服务测试验证成功订单注册失效，库存竞争失败时不注册失效。
- `scripts/baseline/verify_cache_invalidation.py` 在隔离 baseline 数据库执行真实 HTTP 和 Redis 验证。

真实验证结果：

| 检查 | 结果 |
|---|---|
| 下单前缓存 | `size=20` 和 `size=100` 两个键都存在 |
| 成功下单 | HTTP 200；库存 100 → 99 |
| 提交后缓存 | 两个键都被删除 |
| 下一次浏览 | 从 MySQL 读取库存 99，并重建缓存 |
| 重复下单 | HTTP 400 |
| 失败后的缓存 | 仍然存在，TTL 保持 60 秒 |

机器可读结果保存在 `docs/cache/results/2026-09-12-invalidation.json`。
