//package ai.mcpdirect.gateway.mcp;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.servlet.ServletContextHandler;
//import org.eclipse.jetty.servlet.ServletHolder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * Java server implementing Model Context Protocol (MCP) for filesystem operations.
// *
// * @author codeboyzhou
// */
//public class MCPdirectGatewaySseServer {
//    /**
//     * The logger.
//     */
//    private static final Logger LOG = LoggerFactory.getLogger(MCPdirectGatewaySseServer.class);
//
//    /**
//     * The JSON object mapper.
//     */
//    private static final ObjectMapper JSON = new ObjectMapper();
//
//    /**
//     * The MCP message endpoint.
//     */
//    public static final String MSG_ENDPOINT = "/mcp/message";
//
//    /**
//     * The MCP SSE endpoint.
//     */
//    public static final String SSE_ENDPOINT = "/sse";
//    public static final String BASE_URL = "/mcpdirect/1/";
//
//    /**
//     * The MCP sync server instance.
//     */
//    // private static MCPdirectServer server;
//    private final MCPdirectGatewaySseHttpServlet servlet;
////    private final ConcurrentHashMap<Long, MCPdirectTransportProvider> mcpServers = new ConcurrentHashMap<>();
//    // private final Map<String, AIPortMcpServerTransportProvider> providers = new ConcurrentHashMap<>();
//    public MCPdirectGatewaySseServer() {
//        // McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
//        //     .tools(true)
//        //     .prompts(true)
//        //     .resources(true, true)
//        //     .build();
//
//        // HttpServletSseServerTransportProvider transport = new HttpServletSseServerTransportProvider(
//        //     JSON, MSG_ENDPOINT, SSE_ENDPOINT
//        // );
//        // server = McpServer.sync(transport)
//        //     .serverInfo(ServerInfo.SERVER_NAME, ServerInfo.SERVER_VERSION)
//        //     .capabilities(serverCapabilities)
//        //     .build();
//
//        // // Add resources, prompts, and tools to the MCP server
//        // McpResources.addAllTo(server);
//        // McpPrompts.addAllTo(server);
//        // McpTools.addAllTo(server);
////        servlet = new MCPdirectGatewaySseHttpServlet(mcpServers,JSON, MSG_ENDPOINT);
//        servlet = new MCPdirectGatewaySseHttpServlet();
//        // Start the HTTP server
//    }
//
//    /**
//     * Start the HTTP server with Jetty.
//     */
//    public void start() {
//        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
//        servletContextHandler.setContextPath("/");
//
//        ServletHolder servletHolder = new ServletHolder(servlet);
//        servletContextHandler.addServlet(servletHolder, "/*");
//
//        Server httpserver = new Server(8081);
//        httpserver.setHandler(servletContextHandler);
//
//        try {
//            httpserver.start();
//
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                try {
//                    LOG.info("Shutting down HTTP server");
//                    httpserver.stop();
//                    // server.close();
//                } catch (Exception e) {
//                    LOG.error("Error stopping HTTP server", e);
//                }
//            }));
//
//            // Wait for the HTTP server to stop
//            // httpserver.join();
//        } catch (Exception e) {
//            LOG.error("Error starting HTTP server on http://0.0.0.0:8081", e);
//            // server.close();
//        }
//    }
////    public void addMcpServer(String user,String apiKey){
////        // McpSchema.ServerCapabilities serverCapabilities = McpSchema.ServerCapabilities.builder()
////        //     .tools(true)
////        //     .prompts(true)
////        //     .resources(true, true)
////        //     .build();
////
////        // AIPortMcpServer transport = new AIPortMcpServer(user, secretKey, servlet, JSON);
////
////        // MCPdirectServer server = McpServer.sync(transport)
////        //     .serverInfo(user, "1.0.0")
////        //     .capabilities(serverCapabilities)
////        //     .build();
////        mcpServers.put(AIPortApiKeyGenerator.hashCode(apiKey), new MCPdirectTransportProvider(user, apiKey, servlet, JSON));
////
////        // servlet.addAIPortMcpServerTransportProvider(user,transport);
////    }
////    public MCPdirectTransportProvider getMcpServer(String auth){
////        // if (auth != null) {
////        //     AIPortMcpServer provider;
////        //     String[] split = auth.split(":");
////        //     if (split.length == 2 && (provider = mcpServers.get(split[0])) != null
////        //             && provider.verifyToken(split[1])) {
////        //         return provider;
////        //     }
////        // }
////        // return null;
////        // String username = JwtTokenService.extractUsername(auth);
////		// return mcpServers.get(username);
////        if(AIPortApiKeyGenerator.validateApiKey(auth)){
////            return mcpServers.get(AIPortApiKeyGenerator.hashCode(auth));
////        }
////        return null;
////    }
//    /**
//     * Main entry point for the HTTP SSE MCP server.
//     */
//    // public static void main(String[] args) {
//    //     // Initialize MCP server
//    //     McpSseServer mcpSseServer = new McpSseServer();
//    //     mcpSseServer.initialize();
//    // }
//
//}