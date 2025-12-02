package ai.mcpdirect.gateway.dao.mapper.aitool;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolAccessKeyCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MCPToolAccessKeyMapper {
    String accessKeyTable="aitool.tool_access_key";
    String selectAccessKeyCredential = """
            SELECT id,
            secret_key secretKey,
            status,"name",
            effective_date effectiveDate,
            expiration_date expirationDate,
            user_id userId,
            created FROM
            """+accessKeyTable+"\n";

    @Select(selectAccessKeyCredential+"where id=#{accessKeyId}")
    AIPortToolAccessKeyCredential selectAccessKeyCredentialById(@Param("accessKeyId") long accessKeyId);

    @Update("UPDATE "+accessKeyTable+" SET usage=usage+#{usage} WHERE id=#{id}")
    void updateAccessKeyUsage(@Param("id")long id,@Param("usage")int usage);
}
