package ai.mcpdirect.gateway.util;

import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolLog;
import ai.mcpdirect.gateway.service.AIToolHubServiceHandler;
import appnet.hstp.ServiceHeaders;
import ai.mcpdirect.util.MCPdirectAccessKeyValidator;
import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import appnet.hstp.Service;
import appnet.hstp.ServiceEngine;
import appnet.hstp.USL;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Map;

public class AITool implements ToolCallback{
    private static final Logger LOG = LoggerFactory.getLogger(AITool.class);
    private final ToolDefinition toolDef;
    private final ServiceEngine engine;
    private final USL usl;
//    private final McpSyncServer server;
    private final ServiceHeaders headers;
    private final long userId;
    private final long keyId;
    private final long toolId;
    public AITool(long userId,long keyId,long toolId,
            String secretKey,
                  String name,String description,String inputSchema,
                  USL usl,ServiceEngine engine){
        this.userId = userId;
        this.keyId = keyId;
        this.toolId = toolId;
        headers = new ServiceHeaders().addHeader("X-MCPdirect-Key-ID",
                String.valueOf(MCPdirectAccessKeyValidator.hashCode(secretKey)));
//        this.server = server;
        toolDef = DefaultToolDefinition.builder().name(name).description(description).inputSchema(inputSchema).build();
        this.usl = usl;
        this.engine = engine;
    }

//    public McpSyncServer getMcpSyncServer() {
//        return server;
//    }

    @Override
    public @NonNull ToolDefinition getToolDefinition() {
        return toolDef;
    }

    @Override
    public @NonNull String call(@NonNull String toolInput) {
        return call(toolInput,(McpSchema.Implementation)null);
    }
    @Override
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
    private static final String MCPDirectStudioClient = "mcpdirectstudio#";
    public static class ResponseOfAIService {
        public String status = "failed";
        public String message;
        public String data;
    }
    public @NonNull String call(@NonNull String toolInput,McpSchema.Implementation  clientInfo) {
        if(clientInfo!=null){
            String name = clientInfo.name();
            if(name.toLowerCase(Locale.ROOT).startsWith(MCPDirectStudioClient)&&
                    usl.getDomainName().equals(name.substring(MCPDirectStudioClient.length()))){
                return "Bad Request";
            }
            headers.addHeader("X-MCP-Client-Name",clientInfo.name());
        }
        String resp = null;
        int errorCode = Service.SERVICE_FAILED;
        try {
            Service service = usl.createServiceClient().
                    headers(headers).
                    content(toolInput).request(engine);
            errorCode = service.getErrorCode();
            if(errorCode==0){
                ResponseOfAIService aiResp = JSON.fromJson(service.getResponseMessageString(), ResponseOfAIService.class);
                resp = aiResp.data;
                AIToolHubServiceHandler.recordToolLog(userId,keyId,toolId);
            }else{
                LOG.error("response({},{}):{},{}",usl,toolInput,errorCode,service.getErrorMessage());
            }
        } catch (Exception e) {
            LOG.warn("call({},{})",usl,toolInput,e);
        }
        if(resp==null) try{
            McpSchema.CallToolResult error = null;
            switch (errorCode){
                case Service.SERVICE_NOT_FOUND -> {
                    error = new McpSchema.CallToolResult(
                            "Caught Exception. Error: Error "+Service.SERVICE_NOT_FOUND+";"+
                            "Tool not found. Please notify user that the tool maybe deprecated",true);
//                    resp = """
//                           {
//                           "content":[{
//                           "type":"text",
//                           "text":"Tool not found. Please tell user the tool maybe deprecated"
//                           }],
//                           "isError":true
//                           }
//                           """;
                }
                case Service.SERVICE_UNAUTHORIZED -> {
                    error = new McpSchema.CallToolResult(
                            "Caught Exception. Error: Error "+Service.SERVICE_UNAUTHORIZED+";"+
                            "Unauthorized call. Please notify user to check MCPDirect Agent Key permissions",true);
//                    resp = """
//                           {
//                           "content":[{
//                           "type":"text",
//                           "text":"Unauthorized call. Please tell user to check MCPDirect Agent Key permissions"
//                           }],
//                           "isError":true
//                           }
//                           """;
//                    resp = "Unauthorized call. Please tell user to check MCPDirect Agent Key permissions";
                }
                default -> {
                    error = new McpSchema.CallToolResult(
                            "Caught Exception. Error: Error "+Service.SERVICE_FAILED+";"+
                            "This tool is unavailable. Please notify user to check the tool status",true);
//                    resp = """
//                           {
//                           "content":[{
//                           "type":"text",
//                           "text":"Tool not ready. Please tell user to check the tool status and try again"
//                           }],
//                           "isError":true
//                           }
//                           """;
//                    resp = "Tool not ready. Please tell user to check the tool status and try again";
                }
            }
            resp = JSON.toJson(error);
        } catch (Exception ignore) {
        }
        return resp;
    }
    @Override
    public String toString() {
        return toolDef.toString();
    }
}
