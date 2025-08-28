package ai.mcpdirect.gateway.mcp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.modelcontextprotocol.server.McpServerFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import appnet.hstp.ServiceEngine;
import appnet.hstp.USL;
import ai.mcpdirect.gateway.util.AITool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerSession.Factory;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.servlet.AsyncContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MCPdirectTransportProvider implements McpServerTransportProvider{
    public static final String MESSAGE_EVENT_TYPE = "message";
    private static final Logger LOG = LoggerFactory.getLogger(MCPdirectTransportProvider.class);

    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();
    private McpServerSession.Factory sessionFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();
     private final long userId;
    private final String secretKey;
	private final McpSyncServer server;
	private final ConcurrentHashMap<String,SyncToolSpecification> tools=new ConcurrentHashMap<>();
    public MCPdirectTransportProvider(long userId, String secretKey) {
		this.userId = userId;
		this.secretKey = secretKey;
		McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
				.tools(true)
				.prompts(true)
				.resources(true, true)
				.build();

		server = McpServer.sync(this)
				.serverInfo(Long.toString(userId), "1.0.0")
				.capabilities(serverCapabilities)
				.build();
    }
	public long getUserId(){
		return userId;
	}
	public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";
	public static SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback, MimeType mimeType) {

		var tool = new McpSchema.Tool(toolCallback.getToolDefinition().name(),
				toolCallback.getToolDefinition().description(), toolCallback.getToolDefinition().inputSchema());

		return new McpServerFeatures.SyncToolSpecification(tool, (exchange, request) -> {
			try {
				ToolContext toolContext = exchange!=null?new ToolContext(Map.of(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange)):null;
				String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request), toolContext);
				if (mimeType != null && mimeType.toString().startsWith("image")) {
					return new McpSchema.CallToolResult(List
							.of(new McpSchema.ImageContent(List.of(McpSchema.Role.ASSISTANT), null, callResult, mimeType.toString())),
							false);
				}
				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
			}
			catch (Exception e) {
				return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(e.getMessage())), true);
			}
		});
	}
	public void addTool(String name,String description,String inputSchema,USL usl,ServiceEngine engine){

		AITool aiTool = new AITool(secretKey, server, name, description, inputSchema, usl, engine);
		SyncToolSpecification newTool = toSyncToolSpecification(aiTool, null);
		try {
			server.addTool(newTool);
		} catch (Exception e) {
			server.removeTool(name);
			server.addTool(newTool);
		}
		tools.put(name,newTool);
//		List<SyncToolSpecification> newTools = McpToolUtils
//					.toSyncToolSpecifications(aiTool);
//			for (SyncToolSpecification newTool : newTools) {
//				LOG.info("Add new tool: " + newTool);
//				try {
//					server.addTool(newTool);
//				} catch (Exception e) {
//					server.removeTool(name);
//					server.addTool(newTool);
//				}
//				tools.put(name,aiTool);
//			}
	}
	public SyncToolSpecification getTool(String name){
		return tools.get(name);
	}
	public String getApiKey(){
		return secretKey;
	}
	
    // public String verifyToken(String token) {
	// 	return JwtTokenService.extractUsername(token);
    // }
    @Override
    public void setSessionFactory(Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

	@Override
	public Mono<Void> notifyClients(String method, Object params) {
		if (sessions.isEmpty()) {
			LOG.debug("No active sessions to broadcast message to");
			return Mono.empty();
		}

		LOG.debug("Attempting to broadcast message to {} active sessions", sessions.size());

		return Flux.fromIterable(sessions.values())
			.flatMap(session -> session.sendNotification(method, params)
				.doOnError(
						e -> LOG.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
				.onErrorComplete())
			.then();
	}

	@Override
	public Mono<Void> closeGracefully() {
		isClosing.set(true);
		LOG.debug("Initiating graceful shutdown with {} active sessions", sessions.size());

		return Flux.fromIterable(sessions.values()).flatMap(McpServerSession::closeGracefully).then();
	}

    public boolean isClosing(){
        return isClosing.get();
    }
	// public void sendEvent(PrintWriter writer, String eventType, String data) throws IOException {
	// 	writer.write("event: " + eventType + "\n");
	// 	writer.write("data: " + data + "\n\n");
	// 	writer.flush();

	// 	if (writer.checkError()) {
	// 		throw new IOException("Client disconnected");
	// 	}
	// }
	private final ConcurrentHashMap<String, AtomicInteger> clientIpCount = new ConcurrentHashMap<>();
	public int count(String ip){
		AtomicInteger atomicInteger = clientIpCount.get(ip);
		return atomicInteger!=null?atomicInteger.get():0;
	}
    public McpServerSession createSession(String sessionId, String ip,AsyncContext asyncContext, PrintWriter writer){

		// Create a new session transport
		HttpServletMcpSessionTransport sessionTransport =
				new HttpServletMcpSessionTransport(ip,sessionId, asyncContext, writer);

		// Create a new session using the session factory
		McpServerSession session = sessionFactory.create(sessionTransport);
		this.sessions.put(sessionId, session);
        return session;
    }

    public McpServerSession getSession(String sessionId){
        return this.sessions.get(sessionId);
    }

	public McpSyncServer getMcpSyncServer() {
		return server;
	}

	public List<McpSchema.Tool> getTools() {
		return this.tools.values().stream().map(SyncToolSpecification::tool).toList();
	}

	/**
	 * Implementation of McpServerTransport for HttpServlet SSE sessions. This class
	 * handles the transport-level communication for a specific client session.
	 */
	private class HttpServletMcpSessionTransport implements McpServerTransport {
		private final String ip;

		private final String sessionId;

		private final AsyncContext asyncContext;

		private final PrintWriter writer;

		/**
		 * Creates a new session transport with the specified ID and SSE writer.
		 * @param sessionId The unique identifier for this session
		 * @param asyncContext The async context for the session
		 * @param writer The writer for sending server events to the client
		 */
		HttpServletMcpSessionTransport(String ip,String sessionId, AsyncContext asyncContext, PrintWriter writer) {
			AtomicInteger count = clientIpCount.computeIfAbsent(ip, key->new AtomicInteger(0));
			count.incrementAndGet();
			this.ip = ip;
			this.sessionId = sessionId;
			this.asyncContext = asyncContext;
			this.writer = writer;
			LOG.debug("Session transport {} initialized with SSE writer", sessionId);
		}
		private void count(){
			AtomicInteger count = clientIpCount.get(ip);
			if(count!=null&&count.decrementAndGet()==0){
				clientIpCount.remove(ip);

			}
		}
		/**
		 * Sends a JSON-RPC message to the client through the SSE connection.
		 * @param message The JSON-RPC message to send
		 * @return A Mono that completes when the message has been sent
		 */
		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return Mono.fromRunnable(() -> {
				try {
					String jsonText = objectMapper.writeValueAsString(message);
//					servlet.sendEvent(writer, MESSAGE_EVENT_TYPE, jsonText);
					writer.write("event: " + MESSAGE_EVENT_TYPE + "\n");
					writer.write("data: " + jsonText + "\n\n");
					writer.flush();

					if (writer.checkError()) {
						count();
						throw new IOException("Client disconnected");
					}
					LOG.debug("Message sent to session {}", sessionId);
				}
				catch (Exception e) {
					LOG.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
					sessions.remove(sessionId);
					asyncContext.complete();
				}
			});
		}

		/**
		 * Converts data from one type to another using the configured ObjectMapper.
		 * @param data The source data object to convert
		 * @param typeRef The target type reference
		 * @return The converted object of type T
		 * @param <T> The target type
		 */
		@Override
		public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
			return objectMapper.convertValue(data, typeRef);
		}

		/**
		 * Initiates a graceful shutdown of the transport.
		 * @return A Mono that completes when the shutdown is complete
		 */
		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(() -> {
				LOG.debug("Closing session transport: {}", sessionId);
				try {
					sessions.remove(sessionId);
					asyncContext.complete();
					count();
					LOG.debug("Successfully completed async context for session {}", sessionId);
				}
				catch (Exception e) {
					LOG.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
				}
			});
		}

		/**
		 * Closes the transport immediately.
		 */
		@Override
		public void close() {
			try {
				sessions.remove(sessionId);
				asyncContext.complete();
				count();
				LOG.debug("Successfully completed async context for session {}", sessionId);
			}
			catch (Exception e) {
				LOG.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
			}
		}

	}
}
