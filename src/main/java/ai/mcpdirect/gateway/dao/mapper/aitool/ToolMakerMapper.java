package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.gateway.dao.mapper.account.TeamMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

//@Mapper
public interface ToolMakerMapper {

    String TABLE_NAME = "aitool.tool_maker";
//    String SELECT_FIELDS = "id, created, status, last_updated lastUpdated, hash, tools, type, name, tags, agent_id agentId";
    String SELECT_FIELDS = "id, created, status, last_updated lastUpdated,type, name, tags, agent_id agentId,user_id userId";

    String TABLE_JOIN_NAME = "aitool.tool_maker tm";
    String SELECT_JOIN_FIELDS = "tm.id, tm.created, tm.status, tm.last_updated lastUpdated,tm.type, tm.name, tm.tags," +
            "tm.agent_id agentId, tm.user_id userId";

//    @Insert("INSERT INTO " + TABLE_NAME + " (id, created, status, last_updated, hash, tools, type, name, tags, agent_id) " +
//            "VALUES (#{id}, #{created}, #{status}, #{lastUpdated}, #{hash}, #{tools}, #{type}, #{name}, #{tags}, #{agentId})")
    @Insert("INSERT INTO " + TABLE_NAME + " (id, created, status, last_updated, type, name, tags, agent_id,user_id) " +
            "VALUES (#{id}, #{created}, #{status}, #{lastUpdated}, #{type}, #{name}, #{tags}, #{agentId},#{userId})")

    void insertToolMaker(AIPortToolMaker toolsMaker);

    @Update("UPDATE " + TABLE_NAME + " SET status = #{status} WHERE id = #{id}")
    int updateToolMakerStatus(@Param("id")long id,@Param("status")int status);

    @Update("UPDATE " + TABLE_NAME + " SET name = #{name} WHERE id = #{id}")
    int updateToolMakerName(@Param("id")long id,@Param("name")String name);

    @Update("UPDATE " + TABLE_NAME + " SET tags = #{tags} WHERE id = #{id}")
    int updateToolMakerTags(@Param("id")long id,@Param("tags")String tags);

    @Update("UPDATE " + TABLE_NAME +
            " SET " +
            "<trim suffixOverrides=\",\">" +
            "<if test='status != null'>status = #{status},</if>" +
            "<if test='name != null'>name = #{name},</if>" +
            "<if test='tags != null'>tags = #{tags},</if>" +
            "</trim> " +
            "WHERE id = #{id}")
    int updateToolMaker(@Param("id")long id,@Param("name")String name,@Param("tags")String tags,@Param("status")Integer status);

    @Delete("DELETE FROM " + TABLE_NAME + " WHERE id = #{id}")
    int deleteToolMaker(@Param("id")long id);


    @Update("UPDATE " + TABLE_NAME + " SET agent_id = #{to},name=CONCAT(name,'-','#{datetime}') WHERE agent_id = #{from}")
    int transferToolMakers(@Param("from")long from,@Param("to")long to,@Param("datetime")String datetime);

//    @Update("UPDATE " + TABLE_NAME + " SET last_updated = #{lastUpdated}, hash = #{hash}, tools = #{tools} WHERE id = #{id}")
//    @Update("UPDATE " + TABLE_NAME + " SET last_updated = #{lastUpdated}, hash = #{hash}, tools = #{tools} WHERE id = #{id}")
//    int updateToolsMakerTools(@Param("id")long id,@Param("lastUpdated")long lastUpdate,
//                              @Param("hash")long hash,@Param("tools")String tools);
    @Select("SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME + " WHERE id = #{id}")
    AIPortToolMaker selectToolMakerById(@Param("id") long id);

//    @Select("SELECT " + SELECT_JOIN_FIELDS+" , ta.status agentStatus" + " FROM " + TABLE_NAME + " tm\n" +
//            "LEFT JOIN "+ToolsAgentMapper.TABLE_NAME +" ta on tm.agent_id = ta.id\n"+
//            "WHERE tm.id = #{id}")
//    AIPortToolsMakerWithAgent selectToolsMakerWithAgentById(@Param("id") long id);

//    @Select("SELECT " + SELECT_JOIN_FIELDS+" , ta.status agentStatus" + " FROM " + TABLE_NAME + " tm\n" +
//            "LEFT JOIN "+ToolsAgentMapper.TABLE_NAME +" ta on tm.agent_id = ta.id\n"+
//            " WHERE tm.agent_id = #{agentId} and tm.name=#{name}")
//    AIPortToolsMakerWithAgent selectToolsMakerWithAgentByName(@Param("agentId") long agentId,@Param("name")String name);

    @Select("SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME + " WHERE agent_id = #{agentId} and name=#{name}")
    AIPortToolMaker selectToolMakerByName(@Param("agentId") long agentId, @Param("name")String name);

    @Select("SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME + " WHERE agent_id = #{agentId}")
    List<AIPortToolMaker> selectToolMakerByAgentId(@Param("agentId") long agentId);

    @Select("<script>SELECT " + SELECT_JOIN_FIELDS +
            ",ta.status agentStatus,ta.name agentName FROM " + TABLE_JOIN_NAME + "\n" +
            "LEFT JOIN "+ToolAgentMapper.TABLE_JOIN_NAME +" on tm.agent_id = ta.id\n"+
            "WHERE ta.user_id = #{userId} and tm.last_updated>#{lastUpdated}\n" +
            "<if test=\"agentId!=null\">and tm.agent_id=#{agentId}</if>" +
            "<if test=\"type!=null\">and tm.type=#{type}</if>" +
            "<if test=\"name!=null\">and LOWER(tm.name) LIKE CONCAT('%', #{name}, '%')</if>" +
            "</script>")
    List<AIPortToolMaker> selectToolMakersByUserId(@Param("userId") long userId, @Param("name")String name,
                                                   @Param("type") Integer type, @Param("agentId")Long agentId,
                                                   @Param("lastUpdated")long lastUpdated);



    @Select("<script> SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME +
            " WHERE agent_id = #{userId} and type=0 and last_updated>#{lastUpdated}\n" +
            "<if test=\"name!=null\">and LOWER(name) LIKE CONCAT('%', #{name}, '%')</if></script>")
    List<AIPortToolMaker> selectVirtualToolMakerByUserId(
            @Param("userId") long userId,@Param("name")String name,@Param("lastUpdated")long lastUpdated
    );

    @Select("<script>SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME +"\n"+ """
                                WHERE agent_id IN
                                <foreach item='item' index='index' collection='agentIds' open='(' separator=', ' close=')'>
                                #{item}
                                </foreach></script>""")
    List<AIPortToolMaker> selectToolMakerByAgentIds(@Param("agentIds") List<Long> agentIds);

    @Select("<script>SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME +"\n"+ """
                                WHERE id IN
                                <foreach item='item' index='index' collection='makerIds' open='(' separator=', ' close=')'>
                                #{item}
                                </foreach></script>""")
    List<AIPortToolMaker> selectToolMakerByIds(@Param("makerIds") List<Long> makerIds);
    @Select("SELECT " + SELECT_JOIN_FIELDS +",ttm.team_id teamId\n" +
            "FROM "+TeamToolMakerMapper.TEAM_TOOL_MAKER_TABLE+" ttm\n" +
            "LEFT JOIN "+TABLE_JOIN_NAME+" on tm.id=ttm.tool_maker_id and tm.last_updated>#{lastUpdated}\n" +
            "WHERE ttm.team_id=#{teamId}")
    List<AIPortToolMaker> selectToolMakersByTeamId(@Param("teamId") long teamId,
                                                   @Param("lastUpdated")long lastUpdated);

    @Select("SELECT " + SELECT_JOIN_FIELDS +",atm.team_id teamId\n" +
            "FROM "+ TeamMapper.teamMemberTable+" atm\n" +
            "JOIN "+TeamToolMakerMapper.TEAM_TOOL_MAKER_TABLE+" ttm on atm.team_id = ttm.team_id\n" +
            "JOIN "+TABLE_JOIN_NAME+" on ttm.tool_maker_id = tm.id and tm.last_updated>#{lastUpdated}\n" +
            "WHERE atm.member_id=#{userId}")
    List<AIPortToolMaker> selectToolMakersByTeamMemberId(@Param("userId") long userId,
                                                         @Param("lastUpdated")long lastUpdated);
}
