package ai.mcpdirect.gateway.mcp;

import junit.framework.TestCase;

import static ai.mcpdirect.gateway.mcp.MCPdirectGatewaySseHttpServlet.MSG_ENDPOINT;
import static ai.mcpdirect.gateway.mcp.MCPdirectGatewaySseHttpServlet.SSE_ENDPOINT;

public class MCPdirectGatewaySseHttpServletTest extends TestCase {
    public void test(){
        String path = "/key/sse";
        String[] split = path.split("/");
        System.out.println(split.length+","+getAuth(path));

        path = "key/sse";
        split = path.split("/");
        System.out.println(split.length+","+getAuth(path));

        path = "/sse";
        split = path.split("/");
        System.out.println(split.length+","+getAuth(path));


    }
    public String getAuth(String path){
        String auth = null;
        String[] split = path.split("/");
        if(path.endsWith(SSE_ENDPOINT)){
            auth = split[split.length-2];
        }else if(path.endsWith(MSG_ENDPOINT)){
            auth = split[split.length-3];
        }
        return auth;
    }
}