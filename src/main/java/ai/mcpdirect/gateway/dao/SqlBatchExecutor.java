package ai.mcpdirect.gateway.dao;

import org.apache.ibatis.session.SqlSession;

public interface SqlBatchExecutor<T>{
    T executeSql(SqlSession sqlSession) throws Exception;
}