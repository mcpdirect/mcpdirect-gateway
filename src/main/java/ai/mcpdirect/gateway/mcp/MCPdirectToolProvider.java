package ai.mcpdirect.gateway.mcp;

import ai.mcpdirect.gateway.util.AITool;
import ai.mcpdirect.gateway.util.AIToolDirectory;
import appnet.hstp.ServiceDescription;
import appnet.hstp.ServiceEngine;
import appnet.hstp.USL;
import appnet.hstp.engine.util.JSON;
import io.modelcontextprotocol.common.McpTransportContext;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ai.mcpdirect.gateway.mcp.MCPdirectGatewayHttpServlet.*;

public class MCPdirectToolProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MCPdirectToolProvider.class);
    private static final McpJsonMapper jsonMapper = McpJsonMapper.getDefault();
    private static final McpTransportContextExtractor<HttpServletRequest> CONTEXT_EXTRACTOR
            = (r) -> McpTransportContext.create(Map.of("X-MCPdirect", "2.0.0"));
    private static final McpSchema.ServerCapabilities SERVER_CAPABILITIES
            = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .prompts(true)
            .resources(true, true)
            .build();
    private final ConcurrentHashMap<String, McpServerFeatures.AsyncToolSpecification> toolSpecs =new ConcurrentHashMap<>();
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
    public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback, MimeType mimeType) {
        McpSchema.Tool.Builder builder = McpSchema.Tool.builder();
        var tool = builder.name(toolCallback.getToolDefinition().name())
                .description(toolCallback.getToolDefinition().description())
                .inputSchema(jsonMapper,toolCallback.getToolDefinition().inputSchema())
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, null,(exchange, request) -> {
            try {
                ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
                String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request.arguments()), toolContext);
                if (mimeType != null && mimeType.toString().startsWith("image")) {
                    return new McpSchema.CallToolResult(List
                            .of(new McpSchema.ImageContent(List.of(McpSchema.Role.ASSISTANT), null, callResult, mimeType.toString())),
                            false);
                }
//				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
                return JSON.fromJson(callResult,McpSchema.CallToolResult.class);
            }
            catch (Exception e) {
                return new McpSchema.CallToolResult(e.getMessage(), true);
            }
        });
    }
    public static McpServerFeatures.AsyncToolSpecification toAsyncToolSpecification(
            ToolCallback toolCallback, MimeType mimeType, boolean immediate
    ) {
        McpSchema.Tool.Builder builder = McpSchema.Tool.builder();
        var tool = builder.name(toolCallback.getToolDefinition().name())
                .description(toolCallback.getToolDefinition().description())
                .inputSchema(jsonMapper,toolCallback.getToolDefinition().inputSchema())
                .build();

        return new McpServerFeatures.AsyncToolSpecification(tool, null,(exchange, request) -> {
            try {
                ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
                String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request.arguments()), toolContext);
                if (mimeType != null && mimeType.toString().startsWith("image")) {
                    return Mono.just(new McpSchema.CallToolResult(List
                            .of(new McpSchema.ImageContent(List.of(McpSchema.Role.ASSISTANT), null, callResult, mimeType.toString())),
                            false));
                }
//				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
                return Mono.just(JSON.fromJson(callResult,McpSchema.CallToolResult.class));
            }
            catch (Exception e) {
                return Mono.just(new McpSchema.CallToolResult(e.getMessage(), true));
            }
        });
    }
    public void addTools(AIToolDirectory ap,ServiceEngine engine){
        HashMap<String, AITool> aiTools=new HashMap<>();
        for (AIToolDirectory.Tools tools : ap.tools.values())
            for (AIToolDirectory.Description d : tools.descriptions) {
                String name = d.name;
                ServiceDescription s = d.metaData;
                String description = s.description;
                if(d.tags!=null&&!(d.tags=d.tags.trim()).isEmpty()){
                    description+="\n\n**This tool is associated with "+d.tags+"**";
                }
                USL usl = new USL(s.serviceName,tools.engineId,s.servicePath);
                if(name==null||(name=name.trim()).isEmpty()){
                    String path = s.servicePath;;
                    name = path.substring(path.lastIndexOf("/")+1);
                }
                AITool aiTool = aiTools.get(name);
                if(aiTool!=null){
                    name += ("_"+Integer.toString((Long.toString(d.toolId).hashCode()&0x3FF),32));
                }
                aiTool = new AITool(userId,ap.keyId,d.toolId, secretKey, name, description, s.requestSchema, usl, engine);
                aiTools.put(name,aiTool);
                McpServerFeatures.AsyncToolSpecification toolSpec = toAsyncToolSpecification(
                        aiTool, null,true);
                try {
                    if (sseServer != null) sseServer.addTool(toolSpec);
                    if (streamServer != null) streamServer.addTool(toolSpec);
                } catch (Exception ignore) {}
                toolSpecs.put(name,toolSpec);
            }
        for (Map.Entry<String, McpServerFeatures.AsyncToolSpecification> en : toolSpecs.entrySet()) {
            String name = en.getKey();
            if(!aiTools.containsKey(name)){
                toolSpecs.remove(name);
                try {
                    if (sseServer != null) sseServer.removeTool(name);
                    if (streamServer != null) streamServer.removeTool(name);
                } catch (Exception ignore) {}
            }
        }
        notifyToolsListChanged();
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
    public McpServerFeatures.AsyncToolSpecification getTool(String name){
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
                sseTransport = HttpServletSseServerTransportProvider.builder()
                        .contextExtractor(CONTEXT_EXTRACTOR)
                        .sseEndpoint(secretKey.substring(4)+SSE_ENDPOINT)
                        .messageEndpoint(secretKey.substring(4) + SSE_MSG_ENDPOINT)
//                        .keepAliveInterval(Duration.ofSeconds(180))
                        .build();

                sseServer = McpServer.async(sseTransport)
                        .serverInfo(keyName, "2.2.0")
                        .capabilities(SERVER_CAPABILITIES)
                        .tools(toolSpecs.values().stream().toList())
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
                streamTransport = HttpServletStreamableServerTransportProvider.builder()
                        .contextExtractor(CONTEXT_EXTRACTOR)
                        .mcpEndpoint(secretKey.substring(4) + MCP_ENDPOINT)
//                        .keepAliveInterval(Duration.ofSeconds(180))
                        .build();
                streamServer = McpServer.async(streamTransport)
                        .serverInfo(keyName, "2.2.0")
                        .capabilities(SERVER_CAPABILITIES)
                        .tools(toolSpecs.values().stream().toList())
                        .build();
//                for (McpServerFeatures.AsyncToolSpecification tool : tools.values()) {
//                    streamServer.addTool(tool);
//                }
            }
        }
        streamTransport.service(req,resp);
    }
}
