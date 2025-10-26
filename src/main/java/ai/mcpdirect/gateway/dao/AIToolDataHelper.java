package ai.mcpdirect.gateway.dao;

import ai.mcpdirect.gateway.dao.mapper.aitool.AIToolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIToolDataHelper extends DAOHelper{
    protected static AIToolDataHelper INSTANCE;
    public static AIToolDataHelper getInstance(){
        return INSTANCE;
    }

    public AIToolDataHelper(){
        INSTANCE = this;
    }

    private AIToolMapper toolMapper;

    public AIToolMapper getAIToolMapper() {
        return toolMapper;
    }

    @Autowired
    public void setAIToolMapper(AIToolMapper toolMapper) {
        this.toolMapper = toolMapper;
    }
}