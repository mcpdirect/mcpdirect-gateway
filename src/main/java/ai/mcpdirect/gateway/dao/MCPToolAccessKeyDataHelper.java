package ai.mcpdirect.gateway.dao;

import ai.mcpdirect.gateway.dao.mapper.aitool.MCPToolAccessKeyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MCPToolAccessKeyDataHelper extends MCPDAOHelper {
    protected static MCPToolAccessKeyDataHelper INSTANCE;
    public static MCPToolAccessKeyDataHelper getInstance(){
        return INSTANCE;
    }

    public MCPToolAccessKeyDataHelper(){
        INSTANCE = this;
    }

    // private AccountSystemPropertyMapper propertyMapper;

    // public AccountSystemPropertyMapper getAccountSystemPropertyMapper() {
    //     return propertyMapper;
    // }

    // @Autowired
    // public void setAccountPropertyMapper(AccountSystemPropertyMapper mapper) {
    //     this.propertyMapper = mapper;
    // }

    // @Override
    // public void loadSystemProperties(){
    //     for (AIPortSystemProperty property : propertyMapper.getSystemProperties()) {
    //         praProperties.put(property.key,property);
    //     }
    // }
    private MCPToolAccessKeyMapper mapper;

    public MCPToolAccessKeyMapper getMCPAccessKeyMapper() {
        return mapper;
    }

    @Autowired
    public void setMCPAccessKeyMappers(MCPToolAccessKeyMapper mapper) {
        this.mapper = mapper;
    }
}
