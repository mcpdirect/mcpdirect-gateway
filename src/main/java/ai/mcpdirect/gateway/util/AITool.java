package ai.mcpdirect.gateway.util;

import ai.mcpdirect.gateway.service.AIToolHubServiceHandler;
import appnet.hstp.ServiceHeaders;
import appnet.hstp.engine.util.JSON;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

import appnet.hstp.Service;
import appnet.hstp.ServiceEngine;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AITool{
    private static final Logger LOG = LoggerFactory.getLogger(AITool.class);
    private static final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();
    public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";
//    private final ToolDefinition toolDef;
    private final ServiceEngine engine;
//    private final USL usl;
    private final ServiceHeaders headers;
    private final long userId;
    private final long keyId;
    private final long toolId;
//    private final McpSchema.Tool.Builder mcpToolBuilder;
    private final AIToolDirectory.Description description;
    private McpServerFeatures.AsyncToolSpecification asyncToolSpecification;
    public AITool(
            long userId,long keyId,long toolId,AIToolDirectory.Description description,ServiceEngine engine
//            String secretKey,
//            String name,String description,String inputSchema,
//            USL usl,ServiceEngine engine
    ){
        this.userId = userId;
        this.keyId = keyId;
        this.toolId = toolId;
        headers = new ServiceHeaders().addHeader("X-MCPdirect-Key-ID",
                String.valueOf(keyId));
//        this.server = server;
//        toolDef = DefaultToolDefinition.builder().name(name).description(description).inputSchema(inputSchema).build();
//        this.usl = usl;
        this.description = description;
        this.engine = engine;
//        this.mcpToolBuilder = McpSchema.Tool.builder().description(description).inputSchema(jsonMapper,inputSchema);
    }

    public int toolHash(){
        return description.toolHash;
    }

    public McpSchema.Tool generateMcpSchemaTool() {
        return description.mcpToolBuilder.name(description.name).build();
    }

    public McpServerFeatures.AsyncToolSpecification getAsyncToolSpecification() {
        if(asyncToolSpecification==null) asyncToolSpecification =  new McpServerFeatures.AsyncToolSpecification(
                generateMcpSchemaTool(),null, (exchange, request) -> {
            try {
                ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
                String callResult = call(ModelOptionsUtils.toJsonString(request.arguments()), toolContext);
                return Mono.just(JSON.fromJson(callResult,McpSchema.CallToolResult.class));
            }
            catch (Exception e) {
                return Mono.just(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true,null,null));
            }
        });
        return asyncToolSpecification;
    }

//    @Override
    public @NonNull String call(@NonNull String toolInput) {
        return call(toolInput,(McpSchema.Implementation)null);
    }
//    @Override
    public @NonNull String call(@NonNull String toolInput, @Nullable ToolContext toolContext) {
        Map<String, Object> context;
        McpSchema.Implementation clientInfo=null;
        if(toolContext!=null&&(context = toolContext.getContext())!=null) {
            Object obj;
            if((obj=context.get("exchange"))!=null&& obj instanceof McpSyncServerExchange exchange) {
                clientInfo = exchange.getClientInfo();
            }
        }
        return call(toolInput,clientInfo);
    }
    private static final String MCPDirectStudioClient = "mcpdirectstudio";
    public static class ResponseOfAIService {
        public String status = "failed";
        public String message;
        public String data;
    }
    public @NonNull String call(@NonNull String toolInput,McpSchema.Implementation  clientInfo) {
        if(clientInfo!=null){
            String name = clientInfo.name();
//            if(name.toLowerCase(Locale.ROOT).startsWith(MCPDirectStudioClient)&&
//                    usl.getDomainName().equals(name.substring(MCPDirectStudioClient.length()))){
//                return "{\"content\":[{\"type\":\"text\",\"text\":\"Caught Exception, Error: bad request\"}],\"isError\":true}";
//            }
            if(name.toLowerCase(Locale.ROOT).startsWith(MCPDirectStudioClient)){
                return "{\"content\":[{\"type\":\"text\",\"text\":\"Caught Exception, Error: bad request\"}],\"isError\":true}";
            }
            headers.addHeader("X-MCP-Client-Name",clientInfo.name());
        }
        String resp = null;
        int errorCode = Service.SERVICE_FAILED;
        try {
            Service service = description.usl.createServiceClient().
                    headers(headers).
                    content(toolInput).request(engine);
            errorCode = service.getErrorCode();
            if(errorCode==0){
                ResponseOfAIService aiResp = JSON.fromJson(service.getResponseMessageString(), ResponseOfAIService.class);
                resp = aiResp.data;
                AIToolHubServiceHandler.recordToolLog(userId,keyId,toolId);
            }else{
                LOG.error("response({},{}):{},{}",description.usl,toolInput,errorCode,service.getErrorMessage());
            }
        } catch (Exception e) {
            LOG.warn("call({},{})",description.usl,toolInput,e);
        }
        if(resp==null) try{
            McpSchema.CallToolResult error = null;
            switch (errorCode){
                case Service.SERVICE_NOT_FOUND -> {
                    error = McpSchema.CallToolResult.builder().addTextContent(
                            "Caught Exception, Error:  SERVICE_NOT_FOUND. Tool not found. The tool maybe deprecated"
                    ).isError(true).build();
                }
                case Service.SERVICE_UNAUTHORIZED -> {
                    error = McpSchema.CallToolResult.builder().addTextContent(
                            "Caught Exception, Error: SERVICE_UNAUTHORIZED. Unauthorized call. Please  check MCPDirect  Key permissions"
                    ).isError(true).build();
                }
                default -> {
                    error = McpSchema.CallToolResult.builder().addTextContent(
                            "Caught Exception, Error: SERVICE_FAILED, This tool is unavailable. Please check the tool status"
                    ).isError(true).build();
                }
            }
            resp = JSON.toJson(error);
        } catch (Exception ignore) {
        }
        //{"content":[{"type":"text","text":"Caught Exception. Error: Error 255;This tool is unavailable. Please notify user to check the tool status"}],"isError":true}
        return resp;
    }
//    @Override
//    public String toString() {
//        return toolDef.toString();
//    }
}
