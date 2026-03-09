package ai.mcpdirect.gateway.mcp;

import ai.mcpdirect.gateway.util.AITool;
import ai.mcpdirect.gateway.util.AIToolDirectory;
import appnet.hstp.ServiceDescription;
import appnet.hstp.ServiceEngine;
import appnet.hstp.USL;
import appnet.hstp.engine.util.JSON;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.*;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ai.mcpdirect.gateway.mcp.MCPdirectGatewayHttpServlet.*;

public class MCPdirectToolProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MCPdirectToolProvider.class);
    private static final McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();
    private static final McpTransportContextExtractor<HttpServletRequest> CONTEXT_EXTRACTOR
            = (r) -> McpTransportContext.create(Map.of("X-MCPdirect", "2.0.0"));
    private static final McpSchema.ServerCapabilities SERVER_CAPABILITIES
            = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .prompts(true)
            .resources(true, true)
            .build();
//    private final ConcurrentHashMap<String, McpServerFeatures.AsyncToolSpecification> toolSpecs =new ConcurrentHashMap<>();
private final ConcurrentHashMap<String, AITool> toolSpecs =new ConcurrentHashMap<>();
//    private final ConcurrentHashMap<String, AITool> tools=new ConcurrentHashMap<>();
    private final long userId;
    private final String secretKey;
    private final String keyName;

//    private volatile McpSyncServer sseServer;
    private volatile McpAsyncServer sseServer;
    private HttpServletSseServerTransportProvider sseTransport;

//    private volatile McpSyncServer streamServer;
    private volatile McpAsyncServer streamServer;
    private HttpServletStreamableServerTransportProvider streamTransport;

    public MCPdirectToolProvider(long userId, String keyName, String secretKey) {

        this.userId = userId;
        this.secretKey = secretKey;
        this.keyName = keyName;
    }
    public long getUserId(){
        return userId;
    }
    public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";
    public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(AITool tool, MimeType mimeType) {
//        McpSchema.Tool.Builder builder = McpSchema.Tool.builder();
//        var tool = builder.name(toolCallback.getToolDefinition().name())
//                .description(toolCallback.getToolDefinition().description())
//                .inputSchema(jsonMapper,toolCallback.getToolDefinition().inputSchema())
//                .build();

        return new McpServerFeatures.SyncToolSpecification(tool.generateMcpSchemaTool(),null, (exchange, request) -> {
            try {
                ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
                String callResult = tool.call(ModelOptionsUtils.toJsonString(request.arguments()), toolContext);
                if (mimeType != null && mimeType.toString().startsWith("image")) {
                    return new McpSchema.CallToolResult(List
                            .of(new McpSchema.ImageContent(null, callResult, mimeType.toString())),
                            false,null,null);
                }
//				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
                return JSON.fromJson(callResult,McpSchema.CallToolResult.class);
            }
            catch (Exception e) {
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true,null,null);
            }
        });
    }
    public static McpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(
            AITool tool, MimeType mimeType, boolean immediate
    ) {
//        McpSchema.Tool.Builder builder = McpSchema.Tool.builder();
//        var tool = builder.name(toolCallback.getToolDefinition().name())
//                .description(toolCallback.getToolDefinition().description())
//                .inputSchema(jsonMapper,toolCallback.getToolDefinition().inputSchema())
//                .build();

        return new McpServerFeatures.AsyncToolSpecification(tool.generateMcpSchemaTool(),null, (exchange, request) -> {
            try {
                ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
                String callResult = tool.call(ModelOptionsUtils.toJsonString(request.arguments()), toolContext);
                if (mimeType != null && mimeType.toString().startsWith("image")) {
                    return Mono.just(new McpSchema.CallToolResult(List
                            .of(new McpSchema.ImageContent(null, callResult, mimeType.toString())),
                            false,null,null));
                }
//				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
                return Mono.just(JSON.fromJson(callResult,McpSchema.CallToolResult.class));
            }
            catch (Exception e) {
                return Mono.just(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true,null,null));
            }
        });
    }
    public void addTools(AIToolDirectory ap,ServiceEngine engine){
//        HashMap<String, AITool> aiTools=new HashMap<>();
        HashSet<String> toolNames = new HashSet<>();
        for (AIToolDirectory.Tools tools : ap.tools.values())
            for (AIToolDirectory.Description d : tools.descriptions) {
                String name = d.name;
                toolNames.add(name);
                AITool aiTool = toolSpecs.get(name);
                if(aiTool==null||aiTool.toolHash()!=d.toolHash) {
                    aiTool = new AITool(userId, ap.keyId, d.toolId, d, engine);
//                    aiTools.put(name, aiTool);
                    toolSpecs.put(name, aiTool);
//                McpServerFeatures.AsyncToolSpecification toolSpec = toAsyncToolSpecification(
//                        aiTool, null,true);
                    try {
                        if (sseServer != null) sseServer.addTool(aiTool.getAsyncToolSpecification()).subscribe();
                        if (streamServer != null) streamServer.addTool(aiTool.getAsyncToolSpecification()).subscribe();
                    } catch (Exception ignore) {
                    }
//                    toolSpecs.put(name, aiTool);
                }
            }
        for (Map.Entry<String, AITool> en : toolSpecs.entrySet()) {
            String name = en.getKey();
            if(!toolNames.contains(name)){
                toolSpecs.remove(name);
                try {
                    if (sseServer != null) sseServer.removeTool(name);
                    if (streamServer != null) streamServer.removeTool(name).subscribe();
                } catch (Exception ignore) {}
            }
        }
//        notifyToolsListChanged();
    }
//    public void addTool(long userId,long keyId,long toolId,
//                        String name,String description,String inputSchema,
//                        USL usl,ServiceEngine engine){
//        AITool aiTool = tools.get(name);
//        if(aiTool!=null){
//
//        }else aiTool = new AITool(userId,keyId,toolId,
//                secretKey, name, description, inputSchema, usl, engine);
//
//
//        McpServerFeatures.AsyncToolSpecification newTool = toAsyncToolSpecification(
//                aiTool, null,true);
//        if(sseServer!=null) {
//            try {
//                sseServer.addTool(newTool);
//            } catch (Exception ignore) {
////                sseServer.removeTool(name);
////                sseServer.addTool(newTool);
//            }
////            sseServer.notifyToolsListChanged();
//        }
//        if(streamServer!=null) {
//            try {
//                streamServer.addTool(newTool);
//            } catch (Exception ignore) {
////                streamServer.removeTool(name);
////                streamServer.addTool(newTool);
//            }
////            streamServer.notifyToolsListChanged();
//        }
//        toolSpecs.put(name,newTool);
//    }
    public void notifyToolsListChanged(){
        if(sseServer!=null) sseServer.notifyToolsListChanged();
        if(streamServer!=null) streamServer.notifyToolsListChanged();
    }
    public AITool getTool(String name){
        return toolSpecs.get(name);
    }
    public String getApiKey(){
        return secretKey;
    }

    public void closeGracefully(){
        if(sseServer!=null) sseServer.closeGracefully();
        if(streamServer!=null) streamServer.closeGracefully();
    }
    public void sse(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(sseServer==null) synchronized (this){
            if(sseServer==null) {
                String sseEndpoint;
                String messageEndpoint;
                if(req.getHeader("Authorization")!=null||req.getHeader("X-MCPdirect-Key")!=null){
                    sseEndpoint = SSE_ENDPOINT.substring(1);
                    messageEndpoint = SSE_MSG_ENDPOINT.substring(1);
                }else{
                    sseEndpoint = secretKey.substring(4) + SSE_ENDPOINT;
                    messageEndpoint = secretKey.substring(4) + SSE_MSG_ENDPOINT;
                }
                sseTransport = HttpServletSseServerTransportProvider.builder()
                        .contextExtractor(CONTEXT_EXTRACTOR)
                        .sseEndpoint(sseEndpoint)
                        .messageEndpoint(messageEndpoint)
//                        .keepAliveInterval(Duration.ofSeconds(180))
                        .build();

                sseServer = McpServer.async(sseTransport)
                        .serverInfo(keyName, "2.2.0")
                        .capabilities(SERVER_CAPABILITIES)
                        .tools(toolSpecs.values().stream().map(aiTool -> aiTool.getAsyncToolSpecification()).toList())
                        .build();

//                for (McpServerFeatures.AsyncToolSpecification tool : tools.values()) {
//                    sseServer.addTool(tool);
//                }
            }
        }
        sseTransport.service(req,resp);
    }
    public void streamable(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(streamServer==null) synchronized (this){
            if(streamServer==null) {
                String mcpEndpoint;
                if(req.getHeader("Authorization")!=null||req.getHeader("X-MCPdirect-Key")!=null){
                    mcpEndpoint = MCP_ENDPOINT;
                }else{
                    mcpEndpoint = secretKey.substring(4) + MCP_ENDPOINT;
                }
                streamTransport = HttpServletStreamableServerTransportProvider.builder()
                        .contextExtractor(CONTEXT_EXTRACTOR)
                        .mcpEndpoint(mcpEndpoint)
//                        .keepAliveInterval(Duration.ofSeconds(180))
                        .build();
                streamServer = McpServer.async(streamTransport)
                        .serverInfo(keyName, "2.2.0")
                        .capabilities(SERVER_CAPABILITIES)
                        .tools(toolSpecs.values().stream().map(aiTool -> aiTool.getAsyncToolSpecification()).toList())
                        .build();
            }
        }
        streamTransport.service(req,resp);
    }
}
