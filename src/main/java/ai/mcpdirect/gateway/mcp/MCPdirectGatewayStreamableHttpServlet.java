package ai.mcpdirect.gateway.mcp;

import ai.mcpdirect.gateway.MCPdirectGatewayApplication;
import ai.mcpdirect.util.MCPdirectAccessKeyValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(
        name = "MCPdirectGatewayStreamableHttpServlet",
        urlPatterns = "/mcp",
        asyncSupported = true
)
public class MCPdirectGatewayStreamableHttpServlet extends HttpServlet{
    public static final String MCP_ENDPOINT = "/mcp";
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String auth = req.getHeader("Authorization");
        if (auth != null && (auth.startsWith("Bearer ")||auth.startsWith("bearer "))) {
            auth = auth.substring(7); // Remove "Bearer " prefix
        }
        if(auth==null) {
            auth = req.getHeader("X-MCPdirect-Key");
        }
        if(auth==null){
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if(!auth.startsWith(MCPdirectAccessKeyValidator.PREFIX_AIK+"-")){
            auth = MCPdirectAccessKeyValidator.PREFIX_AIK+"-"+auth;
        }
        MCPdirectToolProviderFactory factory = MCPdirectGatewayApplication.getMCPdirectToolProviderFactory();
        MCPdirectToolProvider provider = factory.getMCPdirectToolProvider(auth);
        if (provider == null) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        provider.streamable(req,resp);
    }
}