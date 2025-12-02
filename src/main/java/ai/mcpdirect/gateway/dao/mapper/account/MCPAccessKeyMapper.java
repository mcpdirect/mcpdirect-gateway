//package ai.mcpdirect.gateway.dao.mapper.account;
//
//import ai.mcpdirect.gateway.dao.entity.account.AIPortAccessKeyCredential;
//import org.apache.ibatis.annotations.*;
//
//@Mapper
//public interface MCPAccessKeyMapper {
//    String accessKeyTable="account.access_key";
//    String selectAccessKeyCredential = """
//            SELECT id,
//            secret_key secretKey,
//            status,"name",
//            effective_date effectiveDate,
//            expiration_date expirationDate,
//            user_id userId,
//            user_roles userRoles,
//            created FROM
//            """+accessKeyTable+"\n";
//
//    @Select(selectAccessKeyCredential+"where id=#{accessKeyId}")
//    AIPortAccessKeyCredential selectAccessKeyCredentialById(@Param("accessKeyId") long accessKeyId);
//
//    @Update("UPDATE "+accessKeyTable+" SET usage=usage+#{usage} WHERE id=#{id}")
//    void updateAccessKeyUsage(@Param("id")long id,@Param("usage")int usage);
//}
