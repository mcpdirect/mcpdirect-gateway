package ai.mcpdirect.gateway.dao;

import ai.mcpdirect.gateway.dao.mapper.aitool.MCPToolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MCPToolDataHelper extends MCPDAOHelper {
    protected static MCPToolDataHelper INSTANCE;
    public static MCPToolDataHelper getInstance(){
        return INSTANCE;
    }

    public MCPToolDataHelper(){
        INSTANCE = this;
    }

    private MCPToolMapper toolMapper;

    public MCPToolMapper getMCPToolMapper() {
        return toolMapper;
    }

    @Autowired
    public void setMCPAIToolMapper(MCPToolMapper toolMapper) {
        this.toolMapper = toolMapper;
    }
}