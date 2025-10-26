package ai.mcpdirect.gateway.dao.mapper.aitool;

import org.apache.ibatis.annotations.*;

@Mapper
public interface MCPToolMapper extends MCPToolAgentMapper, MCPToolPermissionMapper, MCPToolMakerMapper, MCPTeamToolMakerMapper {
    String TABLE_NAME = "aitool.tool";
    String TABLE_NAME_V = "aitool.virtual_tool";
}