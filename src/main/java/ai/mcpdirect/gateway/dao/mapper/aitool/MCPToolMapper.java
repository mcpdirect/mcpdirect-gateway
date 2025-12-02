package ai.mcpdirect.gateway.dao.mapper.aitool;

import org.apache.ibatis.annotations.*;

@Mapper
public interface MCPToolMapper extends MCPToolAgentMapper, MCPToolPermissionMapper,
        MCPToolMakerMapper, MCPTeamToolMakerMapper, MCPToolLogMapper {
    String TABLE_NAME_STUB = "aitool.tool_stub";
    String TABLE_NAME = "aitool.tool";
    String TABLE_NAME_V = "aitool.virtual_tool";

    @Update("UPDATE "+TABLE_NAME_STUB +" SET usage=usage+#{usage} WHERE id=#{id}")
    void updateToolUsage(@Param("id")long id,@Param("usage")int usage);
}