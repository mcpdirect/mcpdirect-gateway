package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortTool;
import org.apache.ibatis.annotations.*;

import java.util.List;

//@Mapper
public interface MCPToolPermissionMapper {

    String TABLE_NAME = "aitool.tool_permission";

    String TABLE_NAME_V = "aitool.virtual_tool_permission";
    String SELECT_TOOL_FIELDS = "t.id,t.name,t.agent_id agentId,t.meta_data metaData";

    @Select("SELECT "+SELECT_TOOL_FIELDS+",t.maker_id makerId FROM "+TABLE_NAME+" tp\n" +
            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME+" t ON tp.tool_id = t.id\n" +
            "AND t.status=1 AND t.agent_status>-1 AND t.maker_status=1\n" +
            "WHERE tp.access_key_id=#{accessKeyId} AND tp.status=1\n" +
            "ORDER BY t.agent_id")
    List<AIPortTool> selectPermittedTools(@Param("accessKeyId") long accessKeyId);

    //SELECT t.*
    //FROM aitool.tool_permission tp
    //INNER JOIN aitool.tool t ON tp.tool_id = t.id
    //WHERE tp.access_key_id = 10000
    //  AND tp.status = 1
    //  AND t.maker_id IN (5, 6, 7, 8)
    //  AND t.status = 1
    //ORDER BY t.id;
    //
    // 方案1：最优化索引（推荐）
    //
    //-- 为 tool_permission 表：覆盖所有查询条件
    //CREATE INDEX idx_tp_access_status_tool ON aitool.tool_permission(access_key_id, status, tool_id);
    //
    //-- 为 tool 表：覆盖所有查询条件和排序
    //CREATE INDEX idx_tool_id_maker_status ON aitool.tool(id, maker_id, status);
    //-- 或者，如果数据倾斜严重，使用这个：
    //CREATE INDEX idx_tool_maker_status_id ON aitool.tool(maker_id, status, id);
    //
    // 方案2：分区索引（如果数据量大）
    //-- 如果 status=1 是活跃状态，只索引活跃记录
    //CREATE INDEX idx_tp_active ON aitool.tool_permission(access_key_id, tool_id)
    //WHERE status = 1;
    //
    //CREATE INDEX idx_tool_active ON aitool.tool(maker_id, id)
    //WHERE status = 1;
    //
    //-- 如果 status=1 的数据占比较小
    //CREATE INDEX idx_tp_access_tool_filtered ON aitool.tool_permission(access_key_id, tool_id)
    //WHERE status = 1;
    //
    //-- 对于 tool 表
    //CREATE INDEX idx_tool_maker_id_filtered ON aitool.tool(maker_id, id)
    //WHERE status = 1;
//    @Select("SELECT "+SELECT_TOOL_FIELDS+",t.maker_id makerId FROM "+TABLE_NAME+" tp\n" +
//            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME+" t ON tp.tool_id = t.id\n" +
//            "AND t.status=1 AND t.agent_status>-1 AND t.maker_status=1\n" +
//            "WHERE tp.access_key_id=#{accessKeyId} AND tp.status=1\n" +
//            "ORDER BY t.agent_id")
//    List<AIPortTool> selectPermittedTools(
//            @Param("accessKeyId") long accessKeyId,
//            @Param("toolMakers") List<Long> makers
//    );

    @Select("SELECT "+SELECT_TOOL_FIELDS+",t.maker_id makerId FROM "+TABLE_NAME_V+" tp\n" +
//            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME_V+" vt ON tp.tool_id = vt.id AND vt.maker_status = 1\n" +
            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME+" t ON tp.original_tool_id = t.id\n" +
            "AND t.status=1 AND t.agent_status>-1 AND t.maker_status=1\n" +
            "WHERE tp.access_key_id=#{accessKeyId} AND tp.status=1\n" +
            "ORDER BY t.agent_id")
    List<AIPortTool> selectVirtualPermittedTools(@Param("accessKeyId") long accessKeyId);

//    SELECT DISTINCT t.*
//    FROM aitool.virtual_tool_permission vtp
//    INNER JOIN aitool.tool t ON vtp.original_tool_id = t.id
//    WHERE vtp.access_key_id = 8571942404025515310
//      AND vtp.status = 1
//      AND t.status = 1
//      AND t.maker_status=1
//      AND t.agent_status>-1
//      AND EXISTS (
//          SELECT 1
//          FROM aitool.virtual_tool vt
//          WHERE vt.id = vtp.tool_id
//            AND vt.maker_id IN (2306963246776320)
//            AND vt.status = 1
//            AND vt.maker_status = 1
//      )
//    ORDER BY t.agent_id
//
// 1. virtual_tool_permission 表索引
//-- 主索引：覆盖查询条件
//CREATE INDEX idx_vtp_access_status_original
//ON aitool.virtual_tool_permission(access_key_id, status, original_tool_id)
//INCLUDE (virtual_tool_id);
//
//-- 可选：部分索引（如果status=1占比较小）
//CREATE INDEX idx_vtp_access_active
//ON aitool.virtual_tool_permission(access_key_id, original_tool_id)
//WHERE status = 1
//INCLUDE (virtual_tool_id);
//
//-- 支持EXISTS子查询的索引
//CREATE INDEX idx_vtp_virtual_tool_id
//ON aitool.virtual_tool_permission(virtual_tool_id);
//
// 2. virtual_tool 表索引
//
//-- 主索引：支持EXISTS子查询
//CREATE INDEX idx_vt_id_maker_status
//ON aitool.virtual_tool(id, virtual_maker_id, status, maker_status);
//
//-- 可选：针对特定virtual_maker_id的索引
//CREATE INDEX idx_vt_virtual_maker_active
//ON aitool.virtual_tool(virtual_maker_id, id)
//WHERE status = 1 AND maker_status = 1;
//
// 3. tool 表索引（最复杂）
//
//-- 方案A：覆盖所有过滤条件和排序（推荐）
//CREATE INDEX idx_tool_optimized_a
//ON aitool.tool(
//    status,          -- 1. 等值过滤，可能高选择性
//    maker_status,    -- 2. 等值过滤，进一步过滤
//    agent_status,    -- 3. 范围过滤
//    agent_id,        -- 4. 排序字段
//    id               -- 5. JOIN字段
//)
//INCLUDE (name, meta_data);
//    @Select("SELECT "+SELECT_TOOL_FIELDS+",vt.maker_id makerId FROM "+TABLE_NAME_V+" tp\n" +
//            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME_V+" vt ON tp.tool_id = vt.id AND vt.maker_status = 1\n" +
//            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME+" t ON tp.original_tool_id = t.id\n" +
//            "AND t.status=1 AND t.agent_status>-1 AND t.maker_status=1\n" +
//            "WHERE tp.access_key_id=#{accessKeyId} AND tp.status=1\n" +
//            "ORDER BY t.agent_id")
//    List<AIPortTool> selectVirtualPermittedTools(
//            @Param("accessKeyId") long accessKeyId,
//            @Param("makerIds") List<Long> makers
//    );
}
