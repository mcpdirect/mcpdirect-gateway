//package ai.mcpdirect.gateway.mcp.server;
//
//import java.util.List;
//
//import io.modelcontextprotocol.server.McpServerFeatures;
//import io.modelcontextprotocol.spec.McpSchema;
//import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
//import io.modelcontextprotocol.util.Assert;
//
//public class MCPdirectServer {
//
//	/**
//	 * The async server to wrap.
//	 */
//	private final McpAsyncServer asyncServer;
//
//	private final boolean immediateExecution;
//
//	/**
//	 * Creates a new synchronous server that wraps the provided async server.
//	 * @param asyncServer The async server to wrap
//	 */
//	public MCPdirectServer(McpAsyncServer asyncServer) {
//		this(asyncServer, false);
//	}
//
//	/**
//	 * Creates a new synchronous server that wraps the provided async server.
//	 * @param asyncServer The async server to wrap
//	 * @param immediateExecution Tools, prompts, and resources handlers execute work
//	 * without blocking code offloading. Do NOT set to true if the {@code asyncServer}'s
//	 * transport is non-blocking.
//	 */
//	public MCPdirectServer(McpAsyncServer asyncServer, boolean immediateExecution) {
//		Assert.notNull(asyncServer, "Async server must not be null");
//		this.asyncServer = asyncServer;
//		this.immediateExecution = immediateExecution;
//	}
//
//	/**
//	 * Add a new tool handler.
//	 * @param toolHandler The tool handler to add
//	 */
//	public void addTool(McpServerFeatures.SyncToolSpecification toolHandler) {
//		this.asyncServer
//			.addTool(McpServerFeatures.AsyncToolSpecification.fromSync(toolHandler, this.immediateExecution))
//			.block();
//	}
//
//	/**
//	 * List all registered tools.
//	 * @return A list of all registered tools
//	 */
//	public List<McpSchema.Tool> listTools() {
//		return this.asyncServer.listTools().collectList().block();
//	}
//
//	/**
//	 * Remove a tool handler.
//	 * @param toolName The name of the tool handler to remove
//	 */
//	public void removeTool(String toolName) {
//		this.asyncServer.removeTool(toolName).block();
//	}
//
//	/**
//	 * Add a new resource handler.
//	 * @param resourceSpecification The resource specification to add
//	 */
//	public void addResource(McpServerFeatures.SyncResourceSpecification resourceSpecification) {
//		this.asyncServer
//			.addResource(McpServerFeatures.AsyncResourceSpecification.fromSync(resourceSpecification,
//					this.immediateExecution))
//			.block();
//	}
//
//	/**
//	 * List all registered resources.
//	 * @return A list of all registered resources
//	 */
//	public List<McpSchema.Resource> listResources() {
//		return this.asyncServer.listResources().collectList().block();
//	}
//
//	/**
//	 * Remove a resource handler.
//	 * @param resourceUri The URI of the resource handler to remove
//	 */
//	public void removeResource(String resourceUri) {
//		this.asyncServer.removeResource(resourceUri).block();
//	}
//
//	/**
//	 * Add a new resource template.
//	 * @param resourceTemplateSpecification The resource template specification to add
//	 */
//	public void addResourceTemplate(McpServerFeatures.SyncResourceTemplateSpecification resourceTemplateSpecification) {
//		this.asyncServer
//			.addResourceTemplate(McpServerFeatures.AsyncResourceTemplateSpecification
//				.fromSync(resourceTemplateSpecification, this.immediateExecution))
//			.block();
//	}
//
//	/**
//	 * List all registered resource templates.
//	 * @return A list of all registered resource templates
//	 */
//	public List<McpSchema.ResourceTemplate> listResourceTemplates() {
//		return this.asyncServer.listResourceTemplates().collectList().block();
//	}
//
//	/**
//	 * Remove a resource template.
//	 * @param uriTemplate The URI template of the resource template to remove
//	 */
//	public void removeResourceTemplate(String uriTemplate) {
//		this.asyncServer.removeResourceTemplate(uriTemplate).block();
//	}
//
//	/**
//	 * Add a new prompt handler.
//	 * @param promptSpecification The prompt specification to add
//	 */
//	public void addPrompt(McpServerFeatures.SyncPromptSpecification promptSpecification) {
//		this.asyncServer
//			.addPrompt(
//					McpServerFeatures.AsyncPromptSpecification.fromSync(promptSpecification, this.immediateExecution))
//			.block();
//	}
//
//	/**
//	 * List all registered prompts.
//	 * @return A list of all registered prompts
//	 */
//	public List<McpSchema.Prompt> listPrompts() {
//		return this.asyncServer.listPrompts().collectList().block();
//	}
//
//	/**
//	 * Remove a prompt handler.
//	 * @param promptName The name of the prompt handler to remove
//	 */
//	public void removePrompt(String promptName) {
//		this.asyncServer.removePrompt(promptName).block();
//	}
//
//	/**
//	 * Notify clients that the list of available tools has changed.
//	 */
//	public void notifyToolsListChanged() {
//		this.asyncServer.notifyToolsListChanged().block();
//	}
//
//	/**
//	 * Get the server capabilities that define the supported features and functionality.
//	 * @return The server capabilities
//	 */
//	public McpSchema.ServerCapabilities getServerCapabilities() {
//		return this.asyncServer.getServerCapabilities();
//	}
//
//	/**
//	 * Get the server implementation information.
//	 * @return The server implementation details
//	 */
//	public McpSchema.Implementation getServerInfo() {
//		return this.asyncServer.getServerInfo();
//	}
//
//	/**
//	 * Notify clients that the list of available resources has changed.
//	 */
//	public void notifyResourcesListChanged() {
//		this.asyncServer.notifyResourcesListChanged().block();
//	}
//
//	/**
//	 * Notify clients that the resources have updated.
//	 */
//	public void notifyResourcesUpdated(McpSchema.ResourcesUpdatedNotification resourcesUpdatedNotification) {
//		this.asyncServer.notifyResourcesUpdated(resourcesUpdatedNotification).block();
//	}
//
//	/**
//	 * Notify clients that the list of available prompts has changed.
//	 */
//	public void notifyPromptsListChanged() {
//		this.asyncServer.notifyPromptsListChanged().block();
//	}
//
//	/**
//	 * This implementation would, incorrectly, broadcast the logging message to all
//	 * connected clients, using a single minLoggingLevel for all of them. Similar to the
//	 * sampling and roots, the logging level should be set per client session and use the
//	 * ServerExchange to send the logging message to the right client.
//	 * @param loggingMessageNotification The logging message to send
//	 * @deprecated Use
//	 * {@link McpSyncServerExchange#loggingNotification(LoggingMessageNotification)}
//	 * instead.
//	 */
//	@Deprecated
//	public void loggingNotification(LoggingMessageNotification loggingMessageNotification) {
//		this.asyncServer.loggingNotification(loggingMessageNotification).block();
//	}
//
//	/**
//	 * Close the server gracefully.
//	 */
//	public void closeGracefully() {
//		this.asyncServer.closeGracefully().block();
//	}
//
//	/**
//	 * Close the server immediately.
//	 */
//	public void close() {
//		this.asyncServer.close();
//	}
//
//}