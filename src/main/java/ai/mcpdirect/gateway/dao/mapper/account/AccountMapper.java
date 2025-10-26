package ai.mcpdirect.gateway.dao.mapper.account;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper extends AccessKeyMapper,TeamMapper{
    String userTable = "account.user";
    String userAccountTable = "account.user_account";
}
