package ai.mcpdirect.gateway.mcp;

import ai.mcpdirect.gateway.util.AITool;
import appnet.hstp.ServiceEngine;
import appnet.hstp.USL;
import appnet.hstp.engine.util.JSON;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ai.mcpdirect.gateway.mcp.MCPdirectGatewayHttpServlet.*;

public class MCPdirectToolProvider {
    private static final Logger LOG = LoggerFactory.getLogger(MCPdirectToolProvider.class);
    private static final McpJsonMapper jsonMapper = McpJsonMapper.getDefault();
    private static final McpTransportContextExtractor<HttpServletRequest> CONTEXT_EXTRACTOR
            = (r) -> McpTransportContext.create(Map.of("X-MCPdirect", "2.0.0"));

    private final long userId;
    private final String secretKey;
    private final String keyName;
    private McpSyncServer sseServer;
    private HttpServletSseServerTransportProvider sseTransport;

    private McpSyncServer streamServer;
    private HttpServletStreamableServerTransportProvider streamTransport;
    private final ConcurrentHashMap<String, McpServerFeatures.SyncToolSpecification> tools=new ConcurrentHashMap<>();
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
    public void addTool(long userId,long keyId,long toolId,
                        String name,String description,String inputSchema,
                        USL usl,ServiceEngine engine){

        AITool aiTool = new AITool(userId,keyId,toolId,
                secretKey, name, description, inputSchema, usl, engine);
        McpServerFeatures.SyncToolSpecification newTool = toSyncToolSpecification(aiTool, null);
        if(sseServer!=null) {
            try {
                sseServer.addTool(newTool);
            } catch (Exception e) {
                sseServer.removeTool(name);
                sseServer.addTool(newTool);
            }
        }
        if(streamServer!=null) {
            try {
                streamServer.addTool(newTool);
            } catch (Exception e) {
                streamServer.removeTool(name);
                streamServer.addTool(newTool);
            }
        }
        tools.put(name,newTool);
    }
    public McpServerFeatures.SyncToolSpecification getTool(String name){
        return tools.get(name);
    }
    public String getApiKey(){
        return secretKey;
    }

//    public HttpServlet getTransportHttpServlet() {
//        return transport;
//    }
    public void closeGracefully(){
        if(sseServer!=null) sseServer.closeGracefully();
        if(streamServer!=null) streamServer.closeGracefully();
    }
    public synchronized void sse(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(sseServer==null){
            sseTransport = HttpServletSseServerTransportProvider.builder()
                    .contextExtractor(CONTEXT_EXTRACTOR)
                    .sseEndpoint(SSE_ENDPOINT)
                    .messageEndpoint(secretKey.substring(4)+SSE_MSG_ENDPOINT)
                    .keepAliveInterval(Duration.ofSeconds(180))
                    .build();
            McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .prompts(true)
                    .resources(true, true)
                    .build();

            sseServer = McpServer.sync(sseTransport)
                    .serverInfo(keyName, "2.0.0")
                    .capabilities(serverCapabilities)
                    .build();
            for (McpServerFeatures.SyncToolSpecification tool : tools.values()) {
                sseServer.addTool(tool);
            }
        }
        sseTransport.service(req,resp);
    }
    public synchronized void streamable(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(streamServer==null) {
            streamTransport = HttpServletStreamableServerTransportProvider.builder()
                    .contextExtractor(CONTEXT_EXTRACTOR)
                    .mcpEndpoint(MCP_ENDPOINT)
                    .keepAliveInterval(Duration.ofSeconds(180))
                    .build();
            McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .prompts(true)
                    .resources(true, true)
                    .build();

            streamServer = McpServer.sync(streamTransport)
                    .serverInfo(keyName, "2.0.0")
                    .capabilities(serverCapabilities)
                    .build();
            for (McpServerFeatures.SyncToolSpecification tool : tools.values()) {
                streamServer.addTool(tool);
            }
        }
        streamTransport.service(req,resp);
    }
}
