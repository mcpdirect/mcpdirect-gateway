package ai.mcpdirect.gateway.mcp;

public interface MCPdirectToolProviderFactory {
    default MCPdirectToolProvider getMCPdirectToolProvider(String apiKey){
        return  null;
    }
//    default MCPdirectToolProvider getMCPdirectStreamableToolProvider(String apiKey){
//        return null;
//    }
}
