package ai.mcpdirect.gateway.dao;

import ai.mcpdirect.gateway.dao.mapper.account.MCPAccessKeyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MCPAccessKeyDataHelper extends MCPDAOHelper {
    protected static MCPAccessKeyDataHelper INSTANCE;
    public static MCPAccessKeyDataHelper getInstance(){
        return INSTANCE;
    }

    public MCPAccessKeyDataHelper(){
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
    private MCPAccessKeyMapper mapper;

    public MCPAccessKeyMapper getMCPAccessKeyMapper() {
        return mapper;
    }

    @Autowired
    public void setMCPAccessKeyMappers(MCPAccessKeyMapper mapper) {
        this.mapper = mapper;
    }
}
