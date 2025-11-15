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
        name = "MCPdirectGatewayHttpServlet",
        urlPatterns = "/*",
        asyncSupported = true
)
public class MCPdirectGatewayHttpServlet extends HttpServlet {
    public static final String SSE_ENDPOINT = "/sse";
    public static final String SSE_MSG_ENDPOINT = "/sse/message";
    public static final String MCP_ENDPOINT = "/mcp";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String auth = req.getHeader("Authorization");
        if (auth != null && (auth.startsWith("Bearer ")||auth.startsWith("bearer "))) {
            auth = auth.substring("Bearer ".length()); // Remove "Bearer " prefix
        }
        if(auth==null) {
            auth = req.getHeader("X-MCPdirect-Key");
        }
        boolean sse=false;
        boolean mcp=false;
        if(auth==null){
            String path = req.getPathInfo();
            String[] split = path.split("/");
            if((sse=path.endsWith(SSE_ENDPOINT))||(mcp=path.endsWith(MCP_ENDPOINT))){
                auth = split[split.length-2];
            }else if(path.endsWith(SSE_MSG_ENDPOINT)){
                sse=true;
                auth = split[split.length-3];
            }else{
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            auth = MCPdirectAccessKeyValidator.PREFIX_AIK+"-"+auth;
        }
        MCPdirectToolProviderFactory factory = MCPdirectGatewayApplication.getMCPdirectToolProviderFactory();
        MCPdirectToolProvider provider = factory.getMCPdirectToolProvider(auth);
        if (provider == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if(sse){
            provider.sse(req,resp);
        }else if(mcp){
            provider.streamable(req,resp);
        }else{
            System.err.println(req.getRequestURI());
        }

    }

}
