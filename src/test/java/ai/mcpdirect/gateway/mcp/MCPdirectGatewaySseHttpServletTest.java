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
    public void testToolNameSuffix(){
        long[] id = new long[]{
                2280016759005184L,
                2280016756432896L,
                2280016746143744L,
                2280016743571456L,
                2280016748716032L,
                2280016738435072L,
                2280016725532672L,
                2280016717774848L,
                2280016741007360L,
                2280016712597504L,
                2280016735870976L,
                2280016730693632L,
                2280016722952192L,
                2280016753860608L,
                2280016764141568L,
                2280016710008832L,
                2280016733290496L,
                2280016720371712L,
                2280016728113152L,
                2280016761569280L,
                2280016715186176L,
                2280016751280128L
        };
        for (long l : id) {
            System.out.println(Long.toHexString(l));
            System.out.println(Integer.toHexString(Long.toString(l).hashCode()));
            System.out.println(Integer.toString(Long.toString(l).hashCode()&0x3FF,32));
        }

    }
}