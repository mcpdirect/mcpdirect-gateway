package ai.mcpdirect.gateway.util;

import appnet.hstp.ServiceDescription;
import appnet.hstp.USL;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.*;

public class AIToolDirectory {
    public static class Description{
        public long toolId;
        public int toolHash;
        public String name;
        public String makerName;
//        public String tags;
//        public ServiceDescription metaData;
        public McpSchema.Tool.Builder mcpToolBuilder = McpSchema.Tool.builder();
        public USL usl;
    }
    public static class Tools{
        public String engineId;
        public List<Description> descriptions;
    }

    public long userId;
    public long keyId;
    public String keyName;
    public Map<String,Tools> tools = new HashMap<>();

    public static AIToolDirectory create(long userId,long keyId){
        AIToolDirectory provider = new AIToolDirectory();
        provider.userId=userId;
        provider.keyId = keyId;
        return provider;
    }
}