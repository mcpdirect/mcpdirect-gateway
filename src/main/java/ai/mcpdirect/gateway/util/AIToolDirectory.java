package ai.mcpdirect.gateway.util;

import appnet.hstp.ServiceDescription;

import java.util.*;

public class AIToolDirectory {
    public static class Description{
        public String name;
        public List<String> tags;
        public ServiceDescription metaData;
    }
    public static class Tools{
        public String engineId;
        public List<Description> descriptions;
    }

    public long userId;
//    public String user;
    public String secretKey;
//    public HashSet<Tool> tools = new HashSet<>();
    public Map<String,Tools> tools = new HashMap<>();

    public static AIToolDirectory create(long id, String secretKey){
        AIToolDirectory provider = new AIToolDirectory();
        provider.userId=id;
//        provider.user = user;
        provider.secretKey = secretKey;
        return provider;
    }
}