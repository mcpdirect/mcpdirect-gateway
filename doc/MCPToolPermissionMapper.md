
## selectVirtualPermittedTools
```java
    List<AIPortTool> selectVirtualPermittedTools(
            @Param("accessKeyId") long accessKeyId,
            @Param("makerIds") List<Long> makers
    );
```
```sql
SELECT DISTINCT t.id
FROM aitool.virtual_tool_permission vtp
INNER JOIN aitool.tool t ON vtp.original_tool_id = t.id
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1
  AND t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
  AND EXISTS (
    SELECT 1
    FROM aitool.virtual_tool vt
    WHERE vt.id = vtp.virtual_tool_id
      AND vt.status = 1
      AND vt.maker_status = 1
      AND vt.virtual_maker_id IN (2306963246776320)
)
ORDER BY t.agent_id;
```
针对这个具体查询，我可以提供高度优化的索引方案。查询只返回 `t.id` 并按 `t.agent_id` 排序，这允许更激进的优化。

## 查询分析
```sql
SELECT DISTINCT t.id                -- 只返回id
FROM ... 
WHERE ...
  AND t.status = 1                  -- 等值过滤
  AND t.maker_status = 1            -- 等值过滤
  AND t.agent_status > -1           -- 范围过滤
ORDER BY t.agent_id;                -- 排序
```

## 核心优化策略
**关键洞察**：查询只返回 `t.id` 并按 `agent_id` 排序。可以创建**覆盖索引**让整个查询在索引中完成。

## 推荐的索引方案

### 1. **tool表索引（最重要的优化）**
```sql
-- 方案A：支持排序和过滤的覆盖索引（推荐）
CREATE INDEX idx_tool_covering_for_query 
ON aitool.tool(
    agent_id,        -- 第1位：支持ORDER BY
    status,          -- 第2位：等值过滤
    maker_status,    -- 第3位：等值过滤
    agent_status,    -- 第4位：范围过滤
    id               -- 第5位：SELECT返回值
);

-- 方案B：如果数据分布特殊，可以调整顺序
CREATE INDEX idx_tool_filter_first
ON aitool.tool(
    status,          -- 过滤优先
    maker_status,    -- 过滤优先
    agent_status,    -- 范围过滤
    agent_id,        -- 排序
    id               -- 返回值
);
```

**为什么这个索引如此有效？**
- ✅ 所有WHERE条件都在索引中：`status, maker_status, agent_status`
- ✅ 排序字段在索引中：`agent_id`
- ✅ 返回值在索引中：`id`
- ✅ **完全支持Index-Only Scan**，无需访问表数据

### 2. **virtual_tool_permission表索引**
```sql
-- 支持JOIN和EXISTS子查询
CREATE INDEX idx_vtp_access_original_status 
ON aitool.virtual_tool_permission(
    access_key_id,     -- 等值过滤
    status,            -- 等值过滤
    original_tool_id   -- JOIN字段
)
INCLUDE (virtual_tool_id);  -- EXISTS子查询需要

-- 可选：部分索引减少大小
CREATE INDEX idx_vtp_active_access 
ON aitool.virtual_tool_permission(access_key_id, original_tool_id)
WHERE status = 1
INCLUDE (virtual_tool_id);
```

### 3. **virtual_tool表索引**
```sql
-- 支持EXISTS子查询
CREATE INDEX idx_vt_id_covering 
ON aitool.virtual_tool(
    id,                -- JOIN条件
    status,            -- 过滤
    maker_status,      -- 过滤
    virtual_maker_id   -- IN过滤
);

-- 或者更针对性的索引
CREATE INDEX idx_vt_for_exists 
ON aitool.virtual_tool(id)
WHERE status = 1 AND maker_status = 1 AND virtual_maker_id = 2306963246776320;
```

## 完整的索引创建脚本

```sql
-- 1. tool表：最重要的索引
DROP INDEX IF EXISTS idx_tool_covering_for_query;
CREATE INDEX CONCURRENTLY idx_tool_covering_for_query 
ON aitool.tool(agent_id, status, maker_status, agent_status, id);

-- 2. virtual_tool_permission表
DROP INDEX IF EXISTS idx_vtp_access_original_status;
CREATE INDEX CONCURRENTLY idx_vtp_access_original_status 
ON aitool.virtual_tool_permission(access_key_id, status, original_tool_id)
INCLUDE (virtual_tool_id);

-- 3. virtual_tool表
DROP INDEX IF EXISTS idx_vt_id_covering;
CREATE INDEX CONCURRENTLY idx_vt_id_covering 
ON aitool.virtual_tool(id, status, maker_status, virtual_maker_id);

-- 4. 可选：创建统计信息帮助优化器
CREATE STATISTICS IF NOT EXISTS stats_tool_status_group
ON status, maker_status, agent_status FROM aitool.tool;

CREATE STATISTICS IF NOT EXISTS stats_vtp_access_status
ON access_key_id, status FROM aitool.virtual_tool_permission;

-- 5. 更新统计信息
ANALYZE aitool.tool;
ANALYZE aitool.virtual_tool;
ANALYZE aitool.virtual_tool_permission;
```

## 查询重写优化

### 版本1：使用EXISTS优化

假设：t.status=1 AND t.maker_status=1 AND t.agent_status>-1 只筛选出1%的数据
```sql
-- 当前查询已经很好，但可以微调
SELECT DISTINCT t.id
FROM aitool.tool t
WHERE t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
  AND EXISTS (
      SELECT 1
      FROM aitool.virtual_tool_permission vtp
      WHERE vtp.original_tool_id = t.id
        AND vtp.access_key_id = 8571942404025515310
        AND vtp.status = 1
        AND EXISTS (
            SELECT 1
            FROM aitool.virtual_tool vt
            WHERE vt.id = vtp.virtual_tool_id
              AND vt.status = 1
              AND vt.maker_status = 1
              AND vt.virtual_maker_id = 2306963246776320
        )
  )
ORDER BY t.agent_id;
```

### 版本2：使用JOIN重写（可能更快）
```sql
SELECT DISTINCT t.id
FROM (
    -- 先获取符合条件的virtual_tool_id
    SELECT vt.id as virtual_tool_id
    FROM aitool.virtual_tool vt
    WHERE vt.status = 1
      AND vt.maker_status = 1
      AND vt.virtual_maker_id = 2306963246776320
) filtered_vt
INNER JOIN aitool.virtual_tool_permission vtp 
    ON vtp.virtual_tool_id = filtered_vt.virtual_tool_id
INNER JOIN aitool.tool t 
    ON vtp.original_tool_id = t.id
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1
  AND t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

### 版本3：使用CTE（最清晰）
```sql
WITH valid_virtual_tools AS (
    SELECT id
    FROM aitool.virtual_tool
    WHERE status = 1
      AND maker_status = 1
      AND virtual_maker_id = 2306963246776320
),
valid_permissions AS (
    SELECT DISTINCT vtp.original_tool_id
    FROM aitool.virtual_tool_permission vtp
    INNER JOIN valid_virtual_tools vvt ON vtp.virtual_tool_id = vvt.id
    WHERE vtp.access_key_id = 8571942404025515310
      AND vtp.status = 1
)
SELECT t.id
FROM aitool.tool t
INNER JOIN valid_permissions vp ON t.id = vp.original_tool_id
WHERE t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

## PostgreSQL特定优化

### 1. **调整work_mem用于排序**
```sql
-- 如果数据量大，增加work_mem避免磁盘排序
SET LOCAL work_mem = '32MB';
```

### 2. **使用并行查询**
```sql
-- 在postgresql.conf中设置
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
parallel_setup_cost = 100
parallel_tuple_cost = 0.001

-- 或者在查询中提示
SET max_parallel_workers_per_gather = 4;
```

### 3. **启用快速DISTINCT**
```sql
-- PostgreSQL可以使用HashAggregate去重
SET enable_hashagg = on;
SET enable_sort = on;
```

## 执行计划验证

```sql
-- 查看优化后的执行计划
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, WAL)
SELECT DISTINCT t.id
FROM aitool.virtual_tool_permission vtp
INNER JOIN aitool.tool t ON vtp.original_tool_id = t.id
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1
  AND t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
  AND EXISTS (
      SELECT 1
      FROM aitool.virtual_tool vt
      WHERE vt.id = vtp.virtual_tool_id
        AND vt.status = 1
        AND vt.maker_status = 1
        AND vt.virtual_maker_id IN (2306963246776320)
  )
ORDER BY t.agent_id;

-- 关键检查点：
-- 1. tool表是否使用Index Only Scan?
-- 2. 是否避免了排序操作?
-- 3. Buffers: shared hit是否最小?
```

## 索引选择策略对比

| 索引方案 | 优点 | 缺点 |
|---------|------|------|
| `(agent_id, status, maker_status, agent_status, id)` | 天然支持ORDER BY，扫描时已排序 | 可能需要扫描更多行 |
| `(status, maker_status, agent_status, agent_id, id)` | 过滤性强，扫描行数少 | 可能需要额外排序 |
| `(agent_status, agent_id, status, maker_status, id)` | 混合方案 | 中庸 |

## 最终建议

**创建这两个索引进行测试：**

```sql
-- 索引A：排序优先
CREATE INDEX idx_tool_sort_first 
ON aitool.tool(agent_id, status, maker_status, agent_status, id);

-- 索引B：过滤优先  
CREATE INDEX idx_tool_filter_first
ON aitool.tool(status, maker_status, agent_status, agent_id, id);
```

让PostgreSQL优化器选择最好的执行计划。通常对于这种查询，**索引A（排序优先）会更好**，因为：

1. 数据已经按 `agent_id` 排序，避免了昂贵的Sort操作
2. PostgreSQL可以在扫描时同时应用过滤条件
3. 即使扫描更多行，顺序I/O也比随机I/O+排序快

## 监控和调整

```sql
-- 查看哪个索引被使用
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename IN ('tool', 'virtual_tool_permission', 'virtual_tool')
ORDER BY idx_scan DESC;

-- 查看索引大小
SELECT
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) as size
FROM pg_indexes
WHERE tablename = 'tool';
```

这个索引方案应该能极大提升查询性能，特别是利用Index-Only Scan避免访问表数据。

## 数据分布改变一切
如果条件 `t.status=1 AND t.maker_status=1 AND t.agent_status>-1` 筛选出90%的数据，那么情况就完全不一样了。
### 原始假设 vs 实际情况
```sql
-- 之前假设：过滤性强（只选出1-10%的数据）
-- 现在情况：过滤性弱（选出90%的数据）

-- 这意味着：
-- tool表：100万行 → 过滤后：90万行
-- 需要检查90万行的权限！
```

## 执行策略需要重新评估

### 原始JOIN版本可能更好：
```sql
-- 当tool表过滤出90%数据时
SELECT DISTINCT t.id
FROM aitool.virtual_tool_permission vtp
INNER JOIN aitool.tool t ON vtp.original_tool_id = t.id
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1
  AND t.status = 1                    -- 90%满足
  AND t.maker_status = 1              -- 90%满足
  AND t.agent_status > -1             -- 90%满足
  AND EXISTS (...)
ORDER BY t.agent_id;
```

### 为什么JOIN版本可能更好？

#### 1. **减少EXISTS调用次数**
```sql
-- EXISTS版本：需要对90万行每行检查权限
-- 90万次 × EXISTS子查询执行

-- JOIN版本：从virtual_tool_permission开始
-- 假设：access_key_id=8571942404025515310 选择性很强
-- 可能只有1000行有权限，然后JOIN到tool表
```

#### 2. **执行路径对比**
```
EXISTS版本执行路径：
1. 扫描tool表90万行（使用覆盖索引）
2. 对每行执行EXISTS子查询（90万次）
3. 结果去重排序

JOIN版本执行路径：
1. 扫描virtual_tool_permission（假设1000行）
2. JOIN tool表（1000次JOIN）
3. 过滤tool条件（1000行中可能900行符合）
4. 结果去重排序
```

## 优化策略调整

### 1. **索引策略需要调整**

**virtual_tool_permission表索引更加关键：**
```sql
-- 优先优化这个索引
CREATE INDEX idx_vtp_access_original_priority 
ON aitool.virtual_tool_permission(access_key_id, status, original_tool_id)
INCLUDE (virtual_tool_id);

-- 因为要从这里开始扫描
```

**tool表索引调整：**
```sql
-- 由于过滤性弱，排序更重要
CREATE INDEX idx_tool_agent_id_priority 
ON aitool.tool(agent_id, id)  -- 支持排序和JOIN
WHERE status = 1 AND maker_status = 1 AND agent_status > -1;  -- 部分索引

-- 或者更简单的：
CREATE INDEX idx_tool_for_join ON aitool.tool(id, agent_id);
```

### 2. **查询重写建议**

#### 版本A：使用JOIN并优化（推荐）
```sql
SELECT DISTINCT t.id
FROM (
    -- 先获取有权限的original_tool_id
    SELECT DISTINCT vtp.original_tool_id
    FROM aitool.virtual_tool_permission vtp
    INNER JOIN aitool.virtual_tool vt ON vtp.virtual_tool_id = vt.id
    WHERE vtp.access_key_id = 8571942404025515310
      AND vtp.status = 1
      AND vt.status = 1
      AND vt.maker_status = 1
      AND vt.virtual_maker_id = 2306963246776320
) permission_tools
INNER JOIN aitool.tool t ON permission_tools.original_tool_id = t.id
WHERE t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

#### 版本B：使用LATERAL JOIN
```sql
SELECT DISTINCT t.id
FROM (
    SELECT vtp.original_tool_id
    FROM aitool.virtual_tool_permission vtp
    WHERE vtp.access_key_id = 8571942404025515310
      AND vtp.status = 1
      AND EXISTS (
          SELECT 1
          FROM aitool.virtual_tool vt
          WHERE vt.id = vtp.virtual_tool_id
            AND vt.status = 1
            AND vt.maker_status = 1
            AND vt.virtual_maker_id = 2306963246776320
      )
    GROUP BY vtp.original_tool_id  -- 去重
) vtp_filtered
INNER JOIN LATERAL (
    SELECT t.id, t.agent_id
    FROM aitool.tool t
    WHERE t.id = vtp_filtered.original_tool_id
      AND t.status = 1
      AND t.maker_status = 1
      AND t.agent_status > -1
) t ON true
ORDER BY t.agent_id;
```

## 关键性能指标重新评估

### 假设数据分布：
```sql
-- 数据量估算：
tool表：100万行
  符合条件：90万行（90%）

virtual_tool_permission表：50万行
  access_key_id=8571942404025515310：1000行
  符合status=1：800行

virtual_tool表：20万行
  符合条件：500行
```

### 执行成本估算：
```
EXISTS版本：
1. 扫描tool表90万行
2. 执行90万次EXISTS检查
3. 每次EXISTS：检查virtual_tool_permission + virtual_tool
总检查次数：90万 × (权限检查)

JOIN版本：
1. 扫描virtual_tool_permission 800行
2. JOIN virtual_tool 500行
3. JOIN tool表 500行
4. 过滤tool条件：~450行
总检查次数：~500次JOIN操作
```

## 推荐的索引方案（基于新信息）

### 1. **virtual_tool_permission表（最关键）**
```sql
-- 从这张表开始扫描
CREATE INDEX idx_vtp_access_status_virtual 
ON aitool.virtual_tool_permission(access_key_id, status, virtual_tool_id)
INCLUDE (original_tool_id);
```

### 2. **virtual_tool表**
```sql
-- 支持JOIN过滤
CREATE INDEX idx_vt_id_status_maker 
ON aitool.virtual_tool(id, status, maker_status, virtual_maker_id);
```

### 3. **tool表（简化）**
```sql
-- 因为过滤性弱，主要支持JOIN和排序
CREATE INDEX idx_tool_id_agent_status 
ON aitool.tool(id, agent_id, status, maker_status, agent_status);

-- 或者分区索引
CREATE INDEX idx_tool_id_for_join ON aitool.tool(id) INCLUDE (agent_id);
CREATE INDEX idx_tool_filtered ON aitool.tool(agent_id) 
WHERE status = 1 AND maker_status = 1 AND agent_status > -1;
```

## 执行计划建议

```sql
-- 强制从virtual_tool_permission开始
SET enable_nestloop = on;
SET enable_hashjoin = off;
SET enable_mergejoin = off;

EXPLAIN (ANALYZE, BUFFERS)
SELECT DISTINCT t.id
FROM aitool.virtual_tool_permission vtp
INNER JOIN aitool.virtual_tool vt ON vtp.virtual_tool_id = vt.id
INNER JOIN aitool.tool t ON vtp.original_tool_id = t.id
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1
  AND vt.status = 1
  AND vt.maker_status = 1
  AND vt.virtual_maker_id = 2306963246776320
  AND t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

## 最终建议

基于 `t.status=1 AND t.maker_status=1 AND t.agent_status>-1` 筛选出90%的数据：

**1. 使用JOIN版本而不是EXISTS版本**
**2. 优化virtual_tool_permission表的索引**
**3. 让查询从virtual_tool_permission表开始**
**4. 简化tool表索引，主要支持JOIN和排序**

### 具体查询推荐：
```sql
-- 使用这个版本
SELECT DISTINCT t.id
FROM (
    SELECT DISTINCT vtp.original_tool_id
    FROM aitool.virtual_tool_permission vtp
    INNER JOIN aitool.virtual_tool vt ON vtp.virtual_tool_id = vt.id
    WHERE vtp.access_key_id = 8571942404025515310
      AND vtp.status = 1
      AND vt.status = 1
      AND vt.maker_status = 1
      AND vt.virtual_maker_id = 2306963246776320
) filtered
INNER JOIN aitool.tool t ON filtered.original_tool_id = t.id
WHERE t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

数据分布是查询优化的关键！当过滤条件选择性弱时，从最小的、选择性强的表开始通常是更好的选择。


## “具体查询推荐”对比“原始JOIN版本”

### 原始JOIN版本：
```sql
SELECT DISTINCT t.id
FROM aitool.virtual_tool_permission vtp
INNER JOIN aitool.virtual_tool vt ON vtp.virtual_tool_id = vt.id
INNER JOIN aitool.tool t ON vtp.original_tool_id = t.id
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1
  AND vt.status = 1
  AND vt.maker_status = 1
  AND vt.virtual_maker_id = 2306963246776320
  AND t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

### 具体查询推荐（使用子查询）：
```sql
SELECT DISTINCT t.id
FROM (
    SELECT DISTINCT vtp.original_tool_id
    FROM aitool.virtual_tool_permission vtp
    INNER JOIN aitool.virtual_tool vt ON vtp.virtual_tool_id = vt.id
    WHERE vtp.access_key_id = 8571942404025515310
      AND vtp.status = 1
      AND vt.status = 1
      AND vt.maker_status = 1
      AND vt.virtual_maker_id = 2306963246776320
) filtered
INNER JOIN aitool.tool t ON filtered.original_tool_id = t.id
WHERE t.status = 1
  AND t.maker_status = 1
  AND t.agent_status > -1
ORDER BY t.agent_id;
```

## 核心优势分析

### 1. **执行顺序的显式控制**

**原始版本**：PostgreSQL优化器决定执行顺序
```
可能路径1：vtp → vt → t
可能路径2：vt → vtp → t  
可能路径3：t → vtp → vt（最差，因为t表过滤性弱）
```

**推荐版本**：显式分两步执行
```
第1步：先执行子查询（vtp JOIN vt）
第2步：结果再JOIN t表
强制从最小的、选择性强的表开始
```

### 2. **中间结果集最小化**

**原始版本**：
```sql
-- 可能产生大量中间结果
FROM vtp (800行) 
JOIN vt (500行) → 可能产生几千行中间结果
JOIN t (90万行) → 可能产生大量中间结果
最后才应用DISTINCT
```

**推荐版本**：
```sql
-- 分步去重，减少中间数据量
第1步：vtp JOIN vt → 假设产生500行结果
第2步：DISTINCT original_tool_id → 可能只有300个不同的tool_id
第3步：JOIN t表，只JOIN 300次
```

### 3. **DISTINCT操作的优化**

**原始版本**：
```sql
-- 在最后对整个结果集去重
SELECT DISTINCT t.id
FROM ...  -- 可能产生大量中间行
ORDER BY t.agent_id
-- 需要先JOIN所有数据，再去重，再排序
```

**推荐版本**：
```sql
-- 提前去重，减少JOIN操作
SELECT DISTINCT vtp.original_tool_id  -- 提前去重
FROM ...  -- 只有几百行

-- 然后只JOIN去重后的结果
INNER JOIN t ON filtered.original_tool_id = t.id
-- 大幅减少JOIN次数
```

## 数据流对比

### 假设数据量：
```
vtp表：access_key_id过滤后800行
vt表：过滤后500行
t表：过滤条件后90万行
```

### 原始版本可能的数据流：
```
1. vtp JOIN vt：800×500 = 最多40万中间行
2. 中间结果JOIN t：40万×？= 巨大！
3. 最后DISTINCT和排序：操作大数据集
```

### 推荐版本的数据流：
```
1. 子查询：vtp JOIN vt → 最多40万行
2. 子查询：DISTINCT original_tool_id → 假设300个唯一值
3. 主查询：300个id JOIN t表 → 300次JOIN
4. 排序：最多300行排序
```

## PostgreSQL执行计划差异

### 原始版本可能产生的计划：
```sql
Unique (cost=很贵)
  -> Sort (cost=很贵)
     -> Hash Join (cost=贵)
        -> Nested Loop (vtp JOIN vt)
        -> Seq Scan on tool t  -- 可能全表扫描！
```

### 推荐版本可能产生的计划：
```sql
Unique (cost=较小)
  -> Sort (cost=较小)
     -> Nested Loop
        -> HashAggregate  -- 提前聚合去重
           -> Nested Loop (vtp JOIN vt)  -- 小表JOIN
        -> Index Scan using idx_tool_id on tool t  -- 按ID查找
```

## 关键优化点

### 1. **避免tool表全表扫描**
```sql
-- 原始版本可能：
Hash Join
  -> vtp和vt的JOIN结果
  -> Seq Scan on tool t  -- 扫描90万行！

-- 推荐版本：
Nested Loop
  -> 子查询结果（几百行）
  -> Index Scan on tool t using idx_tool_id  -- 只查几百次！
```

### 2. **利用索引效果更好**
推荐版本可以更好利用这些索引：
```sql
-- tool表只需要简单的id索引
CREATE INDEX idx_tool_id_simple ON tool(id);

-- 不需要复杂的覆盖索引
-- 因为只通过id JOIN，然后过滤
```

### 3. **内存使用更优**
```sql
-- 原始版本：可能需要大量work_mem进行Hash Join
-- 推荐版本：小结果集，内存需求小
```

## 具体场景验证

### 场景：假设最终符合条件的tool只有200个

**原始版本**：
```sql
-- 即使最终只有200个结果，中间可能处理：
1. vtp JOIN vt：处理几千行
2. 与t表JOIN：可能扫描t表大部分数据
3. 对大量中间结果去重排序
```

**推荐版本**：
```sql
1. 子查询：处理几千行，去重得到200个id
2. 用200个id去t表查找：200次索引查找
3. 对200行结果排序
```

## 潜在劣势和注意事项

### 推荐版本的潜在问题：
```sql
-- 如果子查询不去重（original_tool_id重复少）
-- 那么优势不明显

-- 如果PostgreSQL优化器足够智能
-- 可能自动优化原始版本
```

### 如何选择：
```sql
-- 检查数据特征：
SELECT 
    COUNT(DISTINCT vtp.original_tool_id) as distinct_tools,
    COUNT(*) as total_permissions
FROM virtual_tool_permission vtp
WHERE vtp.access_key_id = 8571942404025515310
  AND vtp.status = 1;

-- 如果 distinct_tools << total_permissions
-- 推荐版本优势明显
```

## 性能测试对比

```sql
-- 测试原始版本
EXPLAIN (ANALYZE, BUFFERS, TIMING)
SELECT DISTINCT t.id
FROM vtp INNER JOIN vt ... INNER JOIN t ...
WHERE ... ORDER BY t.agent_id;

-- 测试推荐版本
EXPLAIN (ANALYZE, BUFFERS, TIMING)
SELECT DISTINCT t.id
FROM (
    SELECT DISTINCT vtp.original_tool_id
    FROM vtp INNER JOIN vt ...
    WHERE ...
) filtered
INNER JOIN t ON ... WHERE ... ORDER BY t.agent_id;

-- 对比指标：
-- 1. 执行时间
-- 2. Buffers hit/read
-- 3. Sort Method (quicksort vs top-N heapsort)
-- 4. Join类型 (Nested Loop vs Hash Join)
```

## 总结优势

**推荐版本在以下情况特别有效：**

1. ✅ **virtual_tool_permission表original_tool_id重复度高**  
   （一个tool对应多个permission）

2. ✅ **最终结果集远小于中间JOIN结果**

3. ✅ **希望强制特定的执行顺序**

4. ✅ **tool表很大但通过id查找很快**

5. ✅ **需要避免tool表全表扫描**

**在您的情况下（t表过滤出90%数据）**，推荐版本的优势更明显，因为它：
1. 避免了对90万行tool表的大规模JOIN
2. 通过提前去重大幅减少JOIN次数
3. 强制从选择性强的条件开始查询
4. 减少内存和排序开销