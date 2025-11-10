//package ai.mcpdirect.gateway.mcp;
//
//import ai.mcpdirect.gateway.util.AITool;
//import appnet.hstp.ServiceEngine;
//import appnet.hstp.USL;
//import appnet.hstp.engine.util.JSON;
//import io.modelcontextprotocol.common.McpTransportContext;
//import io.modelcontextprotocol.json.McpJsonMapper;
//import io.modelcontextprotocol.server.McpServer;
//import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
//import io.modelcontextprotocol.server.McpSyncServer;
//import io.modelcontextprotocol.server.McpTransportContextExtractor;
//import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
//import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
//import io.modelcontextprotocol.spec.McpSchema;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.chat.model.ToolContext;
//import org.springframework.ai.model.ModelOptionsUtils;
//import org.springframework.ai.tool.ToolCallback;
//import org.springframework.util.MimeType;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static ai.mcpdirect.gateway.mcp.MCPdirectGatewayHttpServlet.MCP_ENDPOINT;
//import static ai.mcpdirect.gateway.mcp.MCPdirectGatewayHttpServlet.MSG_ENDPOINT;
//
//public class MCPdirectSseToolProvider implements MCPdirectToolProvider {
//    private static final Logger LOG = LoggerFactory.getLogger(MCPdirectSseToolProvider.class);
//    private static final McpJsonMapper jsonMapper = McpJsonMapper.getDefault();
//    private static final McpTransportContextExtractor<HttpServletRequest> CONTEXT_EXTRACTOR
//            = (r) -> McpTransportContext.create(Map.of("X-MCPdirect", "2.0.0"));
//
//     private final long userId;
//    private final String secretKey;
//	private final McpSyncServer server;
//    private final HttpServletSseServerTransportProvider transport;
//	private final ConcurrentHashMap<String,SyncToolSpecification> tools=new ConcurrentHashMap<>();
//    public MCPdirectSseToolProvider(long userId, String keyName, String secretKey) {
//
//		this.userId = userId;
//		this.secretKey = secretKey;
//        transport = HttpServletSseServerTransportProvider.builder()
//                .contextExtractor(CONTEXT_EXTRACTOR)
//                .sseEndpoint(MCP_ENDPOINT)
//                .messageEndpoint(MSG_ENDPOINT)
//                .keepAliveInterval(Duration.ofSeconds(1))
//                .build();
//		McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
//				.tools(true)
//				.prompts(true)
//				.resources(true, true)
//				.build();
//
//		server = McpServer.sync(transport)
//				.serverInfo(keyName, "2.0.0")
//				.capabilities(serverCapabilities)
//				.build();
//    }
//	public long getUserId(){
//		return userId;
//	}
//	public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";
//	public static SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback, MimeType mimeType) {
//        McpSchema.Tool.Builder builder = McpSchema.Tool.builder();
//        var tool = builder.name(toolCallback.getToolDefinition().name())
//                .description(toolCallback.getToolDefinition().description())
//                .inputSchema(jsonMapper,toolCallback.getToolDefinition().inputSchema())
//                .build();
//
//		return new SyncToolSpecification(tool, null,(exchange, request) -> {
//			try {
//				ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
//				String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request.arguments()), toolContext);
//				if (mimeType != null && mimeType.toString().startsWith("image")) {
//					return new McpSchema.CallToolResult(List
//							.of(new McpSchema.ImageContent(List.of(McpSchema.Role.ASSISTANT), null, callResult, mimeType.toString())),
//							false);
//				}
////				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
//                return JSON.fromJson(callResult,McpSchema.CallToolResult.class);
//			}
//			catch (Exception e) {
//				return new McpSchema.CallToolResult(e.getMessage(), true);
//			}
//		});
//	}
//	public void addTool(long userId,long keyId,long toolId,
//                        String name,String description,String inputSchema,
//                        USL usl,ServiceEngine engine){
//
//		AITool aiTool = new AITool(userId,keyId,toolId,
//                secretKey, server, name, description, inputSchema, usl, engine);
//		SyncToolSpecification newTool = toSyncToolSpecification(aiTool, null);
//		try {
//			server.addTool(newTool);
//		} catch (Exception e) {
//			server.removeTool(name);
//			server.addTool(newTool);
//		}
//		tools.put(name,newTool);
//	}
//	public SyncToolSpecification getTool(String name){
//		return tools.get(name);
//	}
//	public String getApiKey(){
//		return secretKey;
//	}
//
//    public HttpServlet getTransportHttpServlet() {
//        return transport;
//    }
//}
