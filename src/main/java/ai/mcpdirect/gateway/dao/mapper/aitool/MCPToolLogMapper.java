package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface MCPToolLogMapper {
    String TABLE_NAME = "aitool.tool_log";

    @Insert("INSERT INTO "+TABLE_NAME+" (user_id,key_id,tool_id,created)\n" +
            "VALUES(#{userId},#{keyId},#{toolId},#{created})")
    void insertToolLog(AIPortToolLog log);
}
