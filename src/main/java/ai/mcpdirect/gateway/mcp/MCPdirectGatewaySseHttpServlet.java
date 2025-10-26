package ai.mcpdirect.gateway.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import ai.mcpdirect.gateway.MCPdirectGatewayApplication;
import ai.mcpdirect.util.MCPdirectAccessKeyValidator;

import appnet.hstp.engine.HstpServiceEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.ProtocolVersions;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(
        name = "MCPdirectGatewaySseHttpServlet",
        urlPatterns = "/*",
        asyncSupported = true
)
public class MCPdirectGatewaySseHttpServlet extends HttpServlet {

	/** Logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(MCPdirectGatewaySseHttpServlet.class);

	public static final String UTF_8 = "UTF-8";

	public static final String APPLICATION_JSON = "application/json";

	public static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

    public static final String BASE_URL = "/mcpdirect/1/";

    public static final String VERSION_ENDPOINT = "/version";
	/** Default endpoint path for SSE connections */
    public static final String SSE_ENDPOINT = "/sse";
    public static final String MCP_ENDPOINT = "/mcp";
    public static final String MSG_ENDPOINT = "/mcp/message";

    public static final String TOOL_API_ENDPOINT = "/tool/";

	/** Event type for endpoint information */
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    private static final McpJsonMapper objectMapper;
    static {
        objectMapper = McpJsonMapper.getDefault();
    }
	/** JSON object mapper for serialization/deserialization */
//	private final ObjectMapper objectMapper = new ObjectMapper();

	public MCPdirectTransportProvider getMCPdirectTransportProvider(HttpServletRequest request) {
		String auth = request.getHeader("Authorization");
		if (auth != null && (auth.startsWith("Bearer ")||auth.startsWith("bearer "))) {
			auth = auth.substring("Bearer ".length()); // Remove "Bearer " prefix
		}
		if(auth==null) {
			auth = request.getHeader("X-MCPdirect-Key");
		}
		if(auth==null){
			String path = request.getPathInfo();
			String[] split = path.split("/");
			if(path.endsWith(SSE_ENDPOINT)||path.endsWith(MCP_ENDPOINT)){
				auth = split[split.length-2];
			}else if(path.endsWith(MSG_ENDPOINT)||split[split.length-2].equals("tool")){
				auth = split[split.length-3];
			}else{
				return null;
			}
			auth = MCPdirectAccessKeyValidator.PREFIX_AIK+"-"+auth;
		}
		logger.info("getMCPdirectTransportProvider({})",auth);
		return MCPdirectGatewayApplication.getFactory().getMCPdirectTransportProvider(auth);
	}
    private String version;
	/**
	 * Handles GET requests to establish SSE connections.
	 * <p>
	 * This method sets up a new SSE connection when a client connects to the SSE
	 * endpoint. It configures the response headers for SSE, creates a new session,
	 * and
	 * sends the initial endpoint information to the client.
	 * 
	 * @param request  The HTTP servlet request
	 * @param response The HTTP servlet response
	 * @throws ServletException If a servlet-specific error occurs
	 * @throws IOException      If an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String requestURI = request.getRequestURI();

		if (!requestURI.endsWith(SSE_ENDPOINT)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		MCPdirectTransportProvider provider = getMCPdirectTransportProvider(request);
		if (provider == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		if (provider.isClosing()) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
			return;
		}

		String ip = request.getHeader("X-Real-IP");
		if(ip==null){
			ip = request.getRemoteHost();
		}
		int count = provider.count(ip);
		if(count>50){
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Too must connections from "+ip);
			return;
		}

		response.setContentType("text/event-stream");
		response.setCharacterEncoding(UTF_8);
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");
		response.setHeader("Access-Control-Allow-Origin", "*");

		String sessionId = UUID.randomUUID().toString();

		AsyncContext asyncContext = request.startAsync();
		asyncContext.setTimeout(0);
		PrintWriter writer = response.getWriter();

		provider.createSession(sessionId, ip,asyncContext, writer);

		String apiKey = provider.getApiKey()
                .substring(MCPdirectAccessKeyValidator.PREFIX_AIK.length()+1);
		// Send initial endpoint event
		this.sendEvent(writer, ENDPOINT_EVENT_TYPE,
		BASE_URL+ apiKey+ MSG_ENDPOINT + "?sessionId=" + sessionId);
	}

	/**
	 * Handles POST requests for client messages.
	 * <p>
	 * This method processes incoming messages from clients, routes them through the
	 * session handler, and sends back the appropriate response. It handles error
	 * cases
	 * and formats error responses according to the MCP specification.
	 * 
	 * @param request  The HTTP servlet request
	 * @param response The HTTP servlet response
	 * @throws ServletException If a servlet-specific error occurs
	 * @throws IOException      If an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String sessionId = request.getParameter("sessionId");
		MCPdirectTransportProvider provider ;
		if(sessionId!=null) {
			String requestURI = request.getRequestURI();
			if (!requestURI.endsWith(MSG_ENDPOINT)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			provider = getMCPdirectTransportProvider(request);
			if (provider == null) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			if (provider.isClosing()) {
				response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
				return;
			}
			// Get the session ID from the request parameter

            // Get the session from the sessions map
			McpServerSession session = provider.getSession(sessionId);
			if (session == null) {
				response.setContentType(APPLICATION_JSON);
				response.setCharacterEncoding(UTF_8);
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				String jsonError = objectMapper.writeValueAsString(
                        new McpError("Session not found: " + sessionId)
                );
				PrintWriter writer = response.getWriter();
				writer.write(jsonError);
				writer.flush();
				return;
			}

			try {
				BufferedReader reader = request.getReader();
				StringBuilder body = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					body.append(line);
				}

				McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body.toString());
				// Process the message through the session's handle method
				session.handle(message).block(); // Block for Servlet compatibility

				response.setStatus(HttpServletResponse.SC_OK);
			} catch (Exception e) {
				logger.error("Error processing message: {}", e.getMessage());
				try {
					McpError mcpError = new McpError(e.getMessage());
					response.setContentType(APPLICATION_JSON);
					response.setCharacterEncoding(UTF_8);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					String jsonError = objectMapper.writeValueAsString(mcpError);
					PrintWriter writer = response.getWriter();
					writer.write(jsonError);
					writer.flush();
				} catch (IOException ex) {
					logger.error(FAILED_TO_SEND_ERROR_RESPONSE, ex.getMessage());
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing message");
				}
			}
		}else if((provider = getMCPdirectTransportProvider(request))!=null){
			Optional<String> inputParam = getBody(request);
			if (inputParam.isEmpty()) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
            String path = request.getPathInfo();
            if(path.contains("/tool/")){
                McpServerFeatures.SyncToolSpecification toolSpecification;
                String[] split = path.split("/");
                String tool = split[split.length-1];
                if(tool.equals("list")){
                    List<McpSchema.Tool> tools = provider.getTools();
                    logger.debug("Received tool/list request: {}", tools);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().write(objectMapper.writeValueAsString(tools));
                }else if((toolSpecification=provider.getTool(tool))!=null){
//                    Map<String,Object> params = objectMapper.readValue(
//                            inputParam.get(), new TypeRef<>() {}
//                    );
                    McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(inputParam.get(),
                            new TypeRef<>() {});
                    Object mcpCallResult = toolSpecification.callHandler().apply(
                            null, callToolRequest
                    );
                    logger.debug("Received tool/call request: {}", callToolRequest);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().write(objectMapper.writeValueAsString(mcpCallResult));
                }else{
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }else {
                McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, inputParam.get());
                if (message instanceof McpSchema.JSONRPCRequest mcpRequest) {
                    logger.debug("Received request: {}", mcpRequest);
                    response.setContentType(APPLICATION_JSON);
                    response.setCharacterEncoding(UTF_8);
                    response.getWriter().write(objectMapper.writeValueAsString(handleIncomingRequest(mcpRequest, provider)));
                } else if (message instanceof McpSchema.JSONRPCNotification notification) {
                    logger.debug("Received notification: {}", notification);
                    response.setStatus(HttpServletResponse.SC_ACCEPTED);
                    response.getWriter().write(handleIncomingNotification(notification));
                } else {
                    logger.warn("Received unknown message type: {}", message);
                    response.getWriter().write("{}");
                }
            }
		}else{
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

	}
	private Optional<String> getBody(HttpServletRequest request) {
		try (ServletInputStream servletInputStream = request.getInputStream()) {
			byte[] data = servletInputStream.readAllBytes();
			return Optional.of(new String(data));
		} catch (IOException e) {
			logger.error("read data error ", e);
		}
		return Optional.empty();
	}

	private final List<String> protocolVersions = List.of(ProtocolVersions.MCP_2025_03_26);
	private Object handleIncomingRequest(McpSchema.JSONRPCRequest request, MCPdirectTransportProvider provider) {
		if (McpSchema.METHOD_INITIALIZE.equals(request.method())) {

			McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(
                    request.params(), new TypeRef<>() {
			});

			logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
					initializeRequest.protocolVersion(), initializeRequest.capabilities(),
					initializeRequest.clientInfo());

			String serverProtocolVersion = this.protocolVersions.get(0);

			if (this.protocolVersions.contains(initializeRequest.protocolVersion())) {
				// If the server supports the requested protocol version, it MUST
				// respond
				// with the same version.
				serverProtocolVersion = initializeRequest.protocolVersion();
			}
			else {
				logger.warn(
						"Client requested unsupported protocol version: {}, so the server will suggest the {} version instead",
						initializeRequest.protocolVersion(), serverProtocolVersion);
			}
			McpSyncServer mcpSyncServer = provider.getMcpSyncServer();
			McpSchema.InitializeResult mcpInit = new McpSchema.InitializeResult(serverProtocolVersion,
					mcpSyncServer.getServerCapabilities(),
					mcpSyncServer.getServerInfo(),
					"");
			return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), mcpInit, null);

		} else if (McpSchema.METHOD_TOOLS_LIST.equals(request.method())) {

			List<McpSchema.Tool> tools = provider.getTools();
			McpSchema.ListToolsResult mcpListTool = new McpSchema.ListToolsResult(tools, null);
			return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), mcpListTool, null);

		} else if (McpSchema.METHOD_TOOLS_CALL.equals(request.method())) {
			McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(request.params(),
                    new TypeRef<>() {});

//			Optional<McpServerFeatures.AsyncToolSpecification> toolSpecification = this.tools.stream()
//					.filter(tr -> callToolRequest.name().equals(tr.tool().name()))
//					.findAny();
			McpServerFeatures.SyncToolSpecification toolSpecification = provider.getTool(callToolRequest.name());

			if (toolSpecification==null) {
				return new McpError("Tool not found: " + callToolRequest.name());
			}

			Object mcpCallResult = toolSpecification.callHandler().apply(null, callToolRequest);
			return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), mcpCallResult, null);
		}
		return "{}";
	}

	private String handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
		if (McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(notification.method())) {

		}
		return "";
	}
//
//	private List<McpServerFeatures.SyncToolSpecification> toSyncToolSpecifications(List<ToolCallback> tools,
//																				   McpServerProperties serverProperties) {
//
//		// De-duplicate tools by their name, keeping the first occurrence of each tool
//		// name
//		return tools.stream() // Key: tool name
//				.collect(Collectors.toMap(tool -> tool.getToolDefinition().name(), tool -> tool, // Value:
//						// the
//						// tool
//						// itself
//						(existing, replacement) -> existing)) // On duplicate key, keep the
//				// existing tool
//				.values()
//				.stream()
//				.map(tool -> {
//					String toolName = tool.getToolDefinition().name();
//					MimeType mimeType = (serverProperties.getToolResponseMimeType().containsKey(toolName))
//							? MimeType.valueOf(serverProperties.getToolResponseMimeType().get(toolName)) : null;
//					return McpToolUtils.toSyncToolSpecification(tool, mimeType);
//				})
//				.toList();
//	}
	/**
	 * Initiates a graceful shutdown of the transport.
	 * <p>
	 * This method marks the transport as closing and closes all active client
	 * sessions.
	 * New connection attempts will be rejected during shutdown.
	 * 
	 * @return A Mono that completes when all sessions have been closed
	 */
	// @Override
	// public Mono<Void> closeGracefully() {
	// isClosing.set(true);
	// logger.debug("Initiating graceful shutdown with {} active sessions",
	// sessions.size());

	// return
	// Flux.fromIterable(sessions.values()).flatMap(McpServerSession::closeGracefully).then();
	// }

	/**
	 * Sends an SSE event to a client.
	 * 
	 * @param writer    The writer to send the event through
	 * @param eventType The type of event (message or endpoint)
	 * @param data      The event data
	 * @throws IOException If an error occurs while writing the event
	 */
	public void sendEvent(PrintWriter writer, String eventType, String data) throws IOException {
		writer.write("event: " + eventType + "\n");
		writer.write("data: " + data + "\n\n");
		writer.flush();

		if (writer.checkError()) {
			throw new IOException("Client disconnected");
		}
	}

	/**
	 * Cleans up resources when the servlet is being destroyed.
	 * <p>
	 * This method ensures a graceful shutdown by closing all client connections
	 * before
	 * calling the parent's destroy method.
	 */
//	@Override
//	public void destroy() {
//		for (MCPdirectTransportProvider provider : providers.values()) {
//			provider.closeGracefully();
//		}
//		super.destroy();
//	}

}
