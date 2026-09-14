# 商品浏览：当前查询执行分析

分析日期：2026-09-12（America/Los_Angeles）。本步骤只诊断，没有修改商品浏览查询。
环境为本地 baseline 应用、MySQL 9.6.0；数据库含 10,002 条 listing，其中 10,000 条满足当前 Java 过滤条件，关联 10,002 个不同 template 和 3 个 merchant。

## 当前调用路径

`GET /api/products/browse` 调用 `browseAvailableListings()`：

1. `findAllByOrderByCreatedAtDesc()` 从 product_listing 读取全部记录，并按 created_at 倒序排序。
2. Java Stream 再检查 `is_available == true` 和 `remaining_quantity > 0`。因此两个不符合条件的 listing 也先从数据库传入 Java。
3. 对剩余的每条 listing，`toBrowseResponse()` 分别调用 `productTemplateRepo.findById()` 和 `merchantRepo.findById()`。
4. 本次 10,000 个返回项各有不同 template，因此触发约 10,000 次 template 主键查询。merchant 只有 3 个，并在同一事务的 JPA 一级缓存中复用，因此约查询 3 次。

理论查询结构约为：1 次 listing 查询 + 10,000 次 template 查询 + 3 次 merchant 查询 = 10,004 次 SELECT。
单次实测 MySQL 全局 Com_select 增量为 10,005；这是全局近似计数，可能包含同一 MySQL 实例的其他活动。

## 当前列表 SQL 的执行计划

等价 SQL：

```sql
SELECT ...
FROM product_listing
ORDER BY created_at DESC;
```

`EXPLAIN ANALYZE`：

```text
Sort: product_listing.created_at DESC
  actual time=3.83..4.32 ms, rows=10002
  -> Table scan on product_listing
     actual time=0.241..2.04 ms, rows=10002
```

含义：

- Table scan：扫描整张 product_listing 表；当前 SQL 没有 WHERE，数据库必须读取全部 10,002 行。
- Sort：现有索引没有 created_at，MySQL 读取后另外排序。
- rows=10002：本次真正流过该步骤的记录数。
- actual time：MySQL 内部执行该步骤的实测时间，不包括后续一万次查询、Java DTO 转换、JSON 序列化和 HTTP 传输。

template 和 merchant 的 `WHERE id = ?` 查询通过主键定位单行，本身很快。问题在于 template 查询被重复执行约一万次，造成大量数据库往返（Database Round Trips）。

## 一次 HTTP 请求实测

| 指标 | 结果 |
|---|---:|
| HTTP 状态 | 200 |
| 返回 listing | 10,000 |
| 响应大小 | 3,292,094 bytes |
| 本地端到端耗时 | 662.59 ms |
| MySQL 全局 SELECT 增量 | 10,005 |

这是单次诊断样本，不代替已有 30 秒 k6 基线。它用来把 HTTP 请求与数据库查询次数对应起来。

## 预览候选 Join 查询

把 listing、template、merchant 合为一条 JOIN，并加 `LIMIT 20` 后，在当前索引下仍扫描 10,002 条 listing、连接 10,000 条可售记录、排序后才取 20 条，实测约 25.7 ms。
这说明 JOIN 可以消除应用与数据库之间约一万次往返，但只有 LIMIT 还不够；后续还需要让过滤、排序与分页共同工作，并根据执行计划设计索引。

当前 product_listing 只有主键以及 merchant_id、template_id、date 三个单列索引，没有覆盖可售过滤和 `created_at, id` 排序的索引。
