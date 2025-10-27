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

    @Select("SELECT "+SELECT_TOOL_FIELDS+",vt.maker_id makerId  FROM "+TABLE_NAME_V+" tp\n" +
            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME_V+" vt ON tp.tool_id = vt.id AND vt.maker_status = 1\n" +
            "LEFT JOIN "+ MCPToolMapper.TABLE_NAME+" t ON vt.tool_id = t.id\n" +
            "AND t.status=1 AND t.agent_status>-1 AND t.maker_status=1\n" +
            "WHERE tp.access_key_id=#{accessKeyId} AND tp.status=1\n" +
            "ORDER BY t.agent_id")
    List<AIPortTool> selectVirtualPermittedTools(@Param("accessKeyId") long accessKeyId);
}
