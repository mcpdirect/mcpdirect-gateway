package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolMaker;
import org.apache.ibatis.annotations.*;

import java.util.List;

//@Mapper
public interface MCPToolMakerMapper {

    String TABLE_NAME = "aitool.tool_maker";
//    String SELECT_FIELDS = "id, created, status, last_updated lastUpdated, hash, tools, type, name, tags, agent_id agentId";
    String SELECT_FIELDS = "id, created, status, last_updated lastUpdated,type, name, tags, agent_id agentId,user_id userId";

    @Select("<script>SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME +"\n"+ """
                                WHERE id IN
                                <foreach item='item' index='index' collection='makerIds' open='(' separator=', ' close=')'>
                                #{item}
                                </foreach></script>""")
    List<AIPortToolMaker> selectToolMakerByIds(@Param("makerIds") List<Long> makerIds);
}
