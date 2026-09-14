# 商品浏览：组合索引与先分页再连接

完成日期：2026-09-12（America/Los_Angeles）。本步骤让商品浏览查询保留一次 SELECT，同时把 JOIN 前处理的 listing 从 10,000 条降到第一页所需的 21 条。

## 组合索引

`product_listing` 新增：

```sql
KEY idx_listing_browse (is_available, created_at DESC, id DESC)
```

字段顺序对应浏览查询：

1. `is_available` 是等值过滤，先进入可售记录区域；
2. `created_at DESC` 让该区域按最新上架时间排列；
3. `id DESC` 在创建时间相同时提供唯一、稳定的顺序。

`remaining_quantity > 0` 是范围条件。把它放在时间字段之前会让索引难以继续满足 `created_at, id` 的排序，因此本索引让 MySQL 沿时间顺序读取，同时检查库存，找到 21 条符合条件的记录后停止。

索引已写入 `sql/schema.sql`，新建数据库时会自动创建。本次也通过 `ALTER TABLE` 将它加入隔离的 `lastcall_eats_baseline` 数据库。项目目前没有 Flyway/Liquibase，已有环境仍需执行相应 DDL；自动数据库迁移属于后续工程化工作。

## 为什么还调整了查询

只添加索引后的第一次 `EXPLAIN ANALYZE` 仍处理 10,000 条：MySQL 为连接只有 3 行的 merchant 选择了哈希连接，打乱索引顺序，最后仍需排序。

最终查询采用“先分页，再连接”：

```sql
FROM (
  SELECT ...
  FROM product_listing
  WHERE is_available = TRUE
    AND remaining_quantity > 0
  ORDER BY created_at DESC, id DESC
  LIMIT :limit OFFSET :offset
) listing_page
JOIN product_template template ON template.id = listing_page.template_id
JOIN merchant merchant ON merchant.id = listing_page.merchant_id
```

内部派生表（Derived Table）先利用索引取出一页；外部查询只连接这一小页。请求 `size=20` 时，服务传入 `limit=21`，第 21 条只用于计算 `hasNext`。

原生查询返回 `UserBrowseProjection` 接口投影，再转换成公开的 `UserBrowseResponse`。响应结构保持 `content/page/size/hasNext`。

## 执行计划变化

| 阶段 | listing 处理量 | template JOIN | 主要数据库工作 |
|---|---:|---:|---|
| JOIN、无浏览索引 | 10,000 | 10,000 | 全表扫描、连接、排序 |
| 只添加索引、原 JOIN | 10,000 | 10,000 | 索引定位后仍因连接计划排序 |
| 组合索引 + 先分页再连接 | 21 | 21 | 索引顺序读取一页后连接 |

最终 `EXPLAIN ANALYZE` 独立样本约 1.06 ms；MySQL 语句摘要在 15,320 次执行后的平均数据库时间约 0.89 ms。

## HTTP 性能对照

30 秒、1 VU、无思考时间的本地 k6 探索测试：

| 指标 | 分页 + N+1 | JOIN、无最终索引方案 | 组合索引 + 先分页再 JOIN |
|---|---:|---:|---:|
| 业务 SELECT/请求 | 24 | 1 | 1 |
| P50 | 4.58 ms | 25.53 ms | 1.87 ms |
| P95 | 4.94 ms | 28.27 ms | 2.52 ms |
| P99 | 5.38 ms | 31.23 ms | 3.00 ms |
| 请求/秒 | 213.18 | 38.28 | 510.60 |
| HTTP 失败率 | 0% | 0% | 0% |
| 响应大小 | 6,677 bytes | 6,676 bytes | 6,676 bytes |

最终测试共完成 15,318 个 HTTP 请求，内容检查全部通过。单独一次 HTTP 请求使目标 SQL 的 `COUNT_STAR` 从 15,320 增加到 15,321，确认每个浏览请求只执行一条业务 SELECT。

## 验证

- 前两页各返回 20 条，重复 listing ID 为 0，均正确报告 `hasNext=true`；
- 返回字段包含 listing、template、merchant 信息；
- 后端 119 个测试通过，0 失败、0 错误；
- 前端 `npx tsc --noEmit` 通过；
- `git diff --check` 通过。
