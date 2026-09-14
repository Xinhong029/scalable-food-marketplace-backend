# 商品浏览：消除 N+1 查询

完成日期：2026-09-12（America/Los_Angeles）。本步骤使用连接查询和 DTO 投影，把分页浏览的业务查询从 24 次降为 1 次。

## 改造前

分页先查询 20 条 `product_listing`，服务层随后遍历结果：

```text
1 次 listing 分页查询
+ 20 次 template 主键查询
+ 3 次 merchant 主键查询（同一事务内由 JPA 一级缓存复用）
= 24 次业务 SELECT
```

这种“先查一批主体，再为每一项补查关联数据”的模式称为 N+1 查询问题（N+1 Query Problem）。

## 改造后

`ProductListingRepo.findAvailableForBrowse(Pageable)` 使用 JPQL 连接三种实体：

```sql
FROM ProductListingDO listing
JOIN ProductTemplateDO template ON template.id = listing.templateId
JOIN MerchantDO merchant ON merchant.id = listing.merchantId
WHERE listing.isAvailable = true
  AND listing.remainingQuantity > 0
ORDER BY listing.createdAt DESC, listing.id DESC
```

该阶段查询通过构造器表达式直接创建 `UserBrowseResponse`。这称为 DTO 投影（DTO Projection）：数据库只返回页面所需的 12 个字段，不先创建三个完整实体，也不再由 Java 逐项查询 template 和 merchant。后续索引优化将其调整为原生接口投影，见 [组合索引与先分页再连接](index-optimization.md)。

服务层因此只需转交结果：

```java
return productListingRepo.findAvailableForBrowse(pageable);
```

旧的 `toBrowseResponse()` 及其循环内的两个 `findById()` 已删除，`MerchantRepo` 也不再是该服务的依赖。

## 正确性与查询次数验证

- 真实接口返回 HTTP 200、20 条记录、`page=0`、`size=20`、`hasNext=true`；响应为 6,676 bytes。
- 返回项仍含 listing、template 和 merchant 的全部页面字段。
- MySQL `performance_schema` 中目标 JOIN 语句的 `COUNT_STAR` 在一次请求后恰好增加 1。
- Repository 集成测试真实插入三张表的数据，验证 JOIN 字段、可售过滤、顺序、分页和 `hasNext`。
- 后端 119 个测试通过，0 失败；前端 `npx tsc --noEmit` 通过。

## 性能结果与下一瓶颈

30 秒、1 VU、无思考时间的 k6 探索测试：

| 指标 | 分页但仍有 N+1 | JOIN DTO 投影 |
|---|---:|---:|
| 业务 SELECT/请求 | 24 | 1 |
| P50 | 4.58 ms | 25.53 ms |
| P95 | 4.94 ms | 28.27 ms |
| P99 | 5.38 ms | 31.23 ms |
| 请求/秒 | 213.18 | 38.28 |
| HTTP 失败率 | 0% | 0% |
| 响应大小 | 6,677 bytes | 6,676 bytes |

查询次数减少并没有立即降低延迟，因为当前 JOIN 没有合适的 listing 组合索引。`EXPLAIN ANALYZE` 显示：

```text
Table scan product_listing: 10,002 行
过滤出可售 listing: 10,000 行
template 主键连接: 10,000 次
排序 10,000 行
最后取 21 行（20 条响应 + 1 条判断 hasNext）
```

MySQL 语句摘要显示 JOIN 查询平均数据库时间约 22.83 ms；一次独立 `EXPLAIN ANALYZE` 样本为 51.1 ms。二者都是本地样本，执行计划比单个耗时数字更重要。

后续已添加支持过滤和排序的组合索引，并将查询调整为先分页再 JOIN，使 MySQL 从索引读取 21 条后只连接这 21 条。结果见 [index-optimization.md](index-optimization.md)。
