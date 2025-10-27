package ai.mcpdirect.gateway.dao;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class MCPDAOHelper implements ApplicationContextAware {
    protected SqlSessionFactory sqlSessionFactory;
    public SqlSessionFactory getSqlSessionFactory(){
        return sqlSessionFactory;
    }

    public String getSqlSessionFactoryBeanName(){
        return "sqlSessionFactory";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        sqlSessionFactory = (SqlSessionFactory) applicationContext.getBean(getSqlSessionFactoryBeanName());
    }

    public <T> T executeSql(SqlBatchExecutor<T> executor) throws Exception {
        SqlSession sqlSession =  sqlSessionFactory.openSession(ExecutorType.BATCH);
        T t;
        try {
            t = executor.executeSql(sqlSession);
            sqlSession.commit();
        }catch (Exception e){
            sqlSession.rollback(true);
            throw e;
        }finally {
            sqlSession.close();
        }
        return t;
    }
}