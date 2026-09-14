# 商品浏览：数据库分页改造

完成日期：2026-09-12（America/Los_Angeles）。本步骤实现数据库过滤和分页，当时保留了关联数据的逐条查询；后续结果见 [消除 N+1 查询](join-projection.md)。

## 改造内容

接口现在接受零起始页码：

```http
GET /api/products/browse?page=0&size=20
```

默认 `page=0`、`size=20`。`page` 不能小于 0，`size` 必须为 1–100；非法参数返回 HTTP 400。
返回 `content`、`page`、`size`、`hasNext`。采用 Spring Data `Slice`，通过多取一条判断下一页，不执行总数 `COUNT(*)`。

Repository 查询现在将可售条件交给 MySQL，并使用稳定排序：

```sql
WHERE is_available = TRUE AND remaining_quantity > 0
ORDER BY created_at DESC, id DESC
LIMIT 21 OFFSET 0
```

请求 `size=20` 时，Spring Data 最多读取 21 条；第 21 条只用于判断 `hasNext`，响应仍返回 20 条。
`id DESC` 负责在多条记录 `created_at` 相同时确定唯一顺序，减少翻页时随机重复或遗漏。

前端商品页改为先请求第 0 页，FlatList 滚动接近底部时读取下一页并追加；下拉刷新回到第 0 页。
搜索框目前只过滤已经加载到手机中的商品，不会搜索尚未加载的后续页面。服务器端搜索属于后续功能。

## 验证结果

真实 MySQL 数据库含 10,002 条 listing，其中 10,000 条可售。

| 检查 | 结果 |
|---|---:|
| 默认响应数量 | 20 |
| 第 0 页 / 第 1 页重复 ID | 0 |
| `hasNext` | true |
| 默认响应大小 | 6,677 bytes |
| `size=101` | HTTP 400 |

使用 MySQL performance_schema 对单次请求前后做语句摘要差分，确认一页当前执行：

- 1 次分页 listing 查询；
- 20 次 template 主键查询；
- 3 次 merchant 主键查询（一级缓存复用）；
- 合计 24 次业务 SELECT。

因此分页把 N+1 的规模从 10,000 个返回项压缩到 20 个返回项，但没有消除 N+1。

30 秒、1 VU、无思考时间的 k6 探索测试：

| 指标 | 全量返回 10,000 条 | 分页返回 20 条 |
|---|---:|---:|
| P50 | 493.66 ms | 4.58 ms |
| P95 | 510.97 ms | 4.94 ms |
| P99 | 521.19 ms | 5.38 ms |
| 请求/秒 | 1.94 | 213.18 |
| HTTP 失败率 | 0% | 0% |
| 响应字符数 | 3,292,094 | 6,677 |

这是“用户获取第一页”的前后体验比较，两边完成的工作量不同，不能描述成同样返回 10,000 条时快了多少。

## 当前仍存在的问题

没有新增索引时，`EXPLAIN ANALYZE` 显示第 0 页仍扫描 10,002 条、过滤出 10,000 条、排序后取 21 条，单次独立 Explain 样本约 9.25 ms。
后续 Join Projection 已消除每页 20 次 template 和 3 次 merchant 往返；其最终 SQL 的执行计划表明下一步需要设计组合索引。

后端完整 Maven clean test：121 个测试通过，0 失败；前端 `npx tsc --noEmit` 通过。
