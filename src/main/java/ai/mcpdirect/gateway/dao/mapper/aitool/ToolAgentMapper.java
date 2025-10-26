package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolAgent;
import org.apache.ibatis.annotations.*;

import java.util.List;

//@Mapper
public interface ToolAgentMapper {

    String TABLE_NAME = "aitool.tool_agent";
    String SELECT_FIELDS = "id, user_id userId, engine_id engineId, created, device, name, tags, status,device_id";

    @Select("<script>SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME +"\n"+ """
                                WHERE id IN
                                <foreach item='item' index='index' collection='agentIds' open='(' separator=', ' close=')'>
                                #{item}
                                </foreach></script>""")
    List<AIPortToolAgent> selectToolAgentByIds(@Param("agentIds") List<Long> agentIds);

}