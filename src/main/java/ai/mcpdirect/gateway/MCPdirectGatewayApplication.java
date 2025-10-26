package ai.mcpdirect.gateway;

import ai.mcpdirect.gateway.mcp.MCPdirectTransportProviderFactory;
import appnet.hstp.exception.ServiceEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appnet.hstp.ServiceEngine;
import appnet.hstp.ServiceEngineFactory;

import appnet.hstp.annotation.ServiceScan;

import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("ai.mcpdirect.gateway")
@ServletComponentScan("ai.mcpdirect.gateway")
@ServiceScan()
public class MCPdirectGatewayApplication implements CommandLineRunner {
	private static final Logger LOG = LoggerFactory.getLogger(MCPdirectGatewayApplication.class);
//	private static MCPdirectGatewaySseServer mcpSseServer;
    public static void main(String[] args) throws ServiceEngineException {
        SpringApplication app = new SpringApplication(MCPdirectGatewayApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    // {
	// "mcpServers": {
	// 	"remote-server": {
	// 		"url": "https://your-server-url.com/mcp",
	// 		"headers": {
	// 			"Authorization": "Bearer your-token"
	// 		},
	// 		"alwaysAllow": ["tool3"],
	// 		"disabled": false
	// 	}
	// }
	// }
        // HttpServletSseServerTransportProvider.builder().sseEndpoint("sse")
//		ServiceEngineFactory.setServiceEngineIdSeed("ai.mcpdirect.gateway");
//		ServiceEngine serviceEngine = ServiceEngineFactory.getServiceEngine();
//        LOG.info("ServiceEngine "+serviceEngine+" started");
//		mcpSseServer = new MCPdirectGatewaySseServer();
////		mcpSseServer.addMcpServer("robinshang", "aik-ZxLkym826ITJX9rcbOlPS7r0i1nrazeg9f58");
//		mcpSseServer.start();
        ServiceEngineFactory.setServiceEngineIdSeed("ai.mcpdirect.gateway");
        ServiceEngine serviceEngine = ServiceEngineFactory.getServiceEngine();
        LOG.info("ServiceEngine "+serviceEngine+" started");
    }
//	public static void addMcpServer(String user,String secretKey){
//		mcpSseServer.addMcpServer(user, secretKey);
//	}
//	public static MCPdirectTransportProvider getMcpServer(String user){
//		return mcpSseServer.getMcpServer(user);
//	}
	private static MCPdirectTransportProviderFactory factory;

	public static void setFactory(MCPdirectTransportProviderFactory factory) {
		MCPdirectGatewayApplication.factory = factory;
	}

	public static MCPdirectTransportProviderFactory getFactory() {
		return factory;
	}

    @Override
    public void run(String... args) throws Exception {
        LOG.info("MCPdirectGatewayApplication started");
//        ServiceEngineFactory.setServiceEngineIdSeed("ai.mcpdirect.gateway");
//        ServiceEngine serviceEngine = ServiceEngineFactory.getServiceEngine();
//        LOG.info("ServiceEngine "+serviceEngine+" started");
    }
}
