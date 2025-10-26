package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortTeamToolMaker;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MCPTeamToolMakerMapper {
    String TEAM_TOOL_MAKER_TABLE = "aitool.team_tool_maker";
    String SELECT_FROM_TEAM_TOOL_MAKER_TABLE =
            "SELECT tool_maker_id toolMakerId, team_id teamId, status, created, last_updated lastUpdated FROM "+ TEAM_TOOL_MAKER_TABLE +"\n";

    @Select(SELECT_FROM_TEAM_TOOL_MAKER_TABLE +"WHERE team_id=#{team_Id}")
    List<AIPortTeamToolMaker> selectTeamToolMakerByTeamId(long teamId);

    @Select("""
            SELECT tm.team_id teamId, tm.status memberStatus,t.status teamStatus,
            ttm.tool_maker_id toolMakerId, ttm.status, ttm.created, ttm.last_updated lastUpdated
            FROM account.team_member tm
            JOIN aitool.team_tool_maker ttm ON tm.team_id = ttm.team_id
            JOIN account.team t ON t.id = tm.team_id
            WHERE tm.member_id = #{memberId}""")
    List<AIPortTeamToolMaker> selectTeamToolMakerByMemberId(long memberId);

}
