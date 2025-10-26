package ai.mcpdirect.gateway.dao.mapper.account;

import ai.mcpdirect.gateway.dao.entity.account.AIPortTeam;
import ai.mcpdirect.gateway.dao.entity.account.AIPortTeamMember;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface TeamMapper {
    String teamTable = "account.team";
    String teamMemberTable = "account.team_member";

    // Team queries
    String selectTeam = """
            SELECT id,
            name,
            created,
            owner_id ownerId,
            status,
            last_updated lastUpdated FROM
            """ + teamTable + "\n";
    
    String selectTeamJoin = """
            SELECT t.id,
            t.name,
            t.created,
            t.owner_id ownerId,
            t.status,
            t.last_updated lastUpdated
            """;

    // Team Member queries
    String selectTeamMemberJoin = """
            SELECT tm.team_id teamId,
            tm.member_id memberId,
            tm.status,
            tm.created,
            tm.expiration_date expirationDate,
            tm.last_updated lastUpdated
            """;

    // Team operations
    @Select(selectTeam + "WHERE id = #{id}")
    AIPortTeam selectTeamById(@Param("id") long id);

    @Select(selectTeam + "WHERE owner_id = #{ownerId}")
    List<AIPortTeam> selectTeamsByOwnerId(@Param("ownerId") long ownerId);

    @Select(selectTeamJoin +",ua.account ownerAccount,u.name ownerName\n" +
            "FROM " + teamMemberTable + " tm\n" +
            "LEFT JOIN "+teamTable+" t on tm.team_id= t.id\n"+
            "LEFT JOIN "+AccountMapper.userAccountTable+" ua on ua.id=t.owner_id\n"+
            "LEFT JOIN "+AccountMapper.userTable+" u on u.id=t.owner_id\n"+
            "WHERE tm.member_id = #{memberId}")
    List<AIPortTeam>selectTeamsByMemberId(long memberId);
    @Insert("INSERT INTO " + teamTable +
            "(id, name, created, owner_id,status,last_updated) VALUES " +
            "(#{id}, #{name}, #{created}, #{ownerId}, #{status},#{lastUpdated})")
    void insertTeam(AIPortTeam team);

    @Update("<script>UPDATE " + teamTable +
            """
            SET last_updated = #{lastUpdated}
            <if test="name!=null">,name = #{name}</if>
            <if test="status!=null">,status = #{status}</if>
            WHERE owner_id = #{ownerId} AND id = #{id}</script>""")
    int updateTeam(AIPortTeam team);

    @Delete("DELETE FROM " + teamTable + " WHERE id = #{id}")
    int deleteTeam(@Param("id") long id);

    @Delete("DELETE FROM " + teamTable + " WHERE owner_id = #{ownerId}")
    int deleteTeamsByOwnerId(@Param("ownerId") long ownerId);

    // Team Member operations
    @Select(selectTeamMemberJoin + ",ua.account,u.name FROM\n"+
            teamMemberTable + " tm\n"+
            "LEFT JOIN "+AccountMapper.userAccountTable+" ua on ua.id=tm.member_id\n"+
            "LEFT JOIN "+AccountMapper.userTable+" u on u.id=tm.member_id\n"+
            "WHERE tm.team_id = #{teamId}")
    List<AIPortTeamMember> selectTeamMembersByTeamId(@Param("teamId") long teamId);

    @Select(selectTeamMemberJoin + ",ua.account,u.name FROM\n"+
            teamMemberTable + " tm\n"+
            "LEFT JOIN "+AccountMapper.userAccountTable+" ua on ua.id=tm.member_id\n"+
            "LEFT JOIN "+AccountMapper.userTable+" u on u.id=tm.member_id\n" +
            "WHERE tm.team_id = #{teamId} and tm.member_id=#{memberId}")
    AIPortTeamMember selectTeamMemberById(@Param("teamId") long teamId,@Param("memberId") long memberId);

    @Select(selectTeamMemberJoin + ",t.status teamStatus FROM\n"+
            teamMemberTable + " tm\n"+
            "LEFT JOIN "+TeamMapper.teamTable+" t on t.id=tm.team_id\n"+
            "WHERE member_id = #{memberId}")
    List<AIPortTeamMember> selectTeamMembersByMemberId(@Param("memberId") long memberId);

    @Insert("INSERT INTO " + teamMemberTable + 
            "(team_id, member_id, status, created, expiration_date,last_updated) VALUES " +
            "(#{teamId}, #{memberId}, #{status}, #{created}, #{expirationDate},#{lastUpdated})")
    void insertTeamMember(AIPortTeamMember teamMember);

    @Update("<script> UPDATE " + teamMemberTable + " SET " +
            "last_updated=#{lastUpdated}" +
            "<if test='status != null'>,status = #{status}</if>" +
            "<if test='expirationDate != null'>,expiration_date = #{expirationDate}</if>" +
            "WHERE team_id = #{teamId} AND member_id = #{memberId}</script>")
    int updateTeamMember(AIPortTeamMember teamMember);

    @Delete("DELETE FROM " + teamMemberTable + " WHERE team_id = #{teamId} AND member_id = #{memberId}")
    int deleteTeamMember(@Param("teamId") long teamId, @Param("memberId") long memberId);

    @Delete("DELETE FROM " + teamMemberTable + " WHERE team_id = #{teamId}")
    int deleteTeamMembersByTeamId(@Param("teamId") long teamId);

    @Delete("DELETE FROM " + teamMemberTable + " WHERE member_id = #{memberId}")
    int deleteTeamMembersByMemberId(@Param("memberId") long memberId);
}