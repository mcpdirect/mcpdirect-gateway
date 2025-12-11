package ai.mcpdirect.gateway.util;

import appnet.hstp.ServiceDescription;

import java.util.*;

public class AIToolDirectory {
    public static class Description{
        public long toolId;
        public String name;
//        public List<String> tags;
        public String tags;
        public ServiceDescription metaData;
//        public MCPToolSchema metaData;
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