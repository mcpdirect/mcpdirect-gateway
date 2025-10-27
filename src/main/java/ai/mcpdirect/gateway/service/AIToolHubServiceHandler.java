package ai.mcpdirect.gateway.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ai.mcpdirect.gateway.dao.MCPToolDataHelper;
import ai.mcpdirect.gateway.dao.MCPAccessKeyDataHelper;
import ai.mcpdirect.gateway.dao.entity.account.AIPortAccessKeyCredential;
import ai.mcpdirect.gateway.dao.entity.aitool.AIPortTeamToolMaker;
import ai.mcpdirect.gateway.dao.entity.aitool.AIPortTool;
import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolAgent;
import ai.mcpdirect.gateway.dao.entity.aitool.AIPortToolMaker;
import ai.mcpdirect.gateway.dao.mapper.account.MCPAccessKeyMapper;
import ai.mcpdirect.gateway.dao.mapper.aitool.MCPToolMapper;
import ai.mcpdirect.gateway.mcp.MCPdirectTransportProvider;
import ai.mcpdirect.util.MCPdirectAccessKeyValidator;
import appnet.hstp.*;
import appnet.hstp.annotation.*;
import appnet.hstp.engine.util.JSON;
import ai.mcpdirect.gateway.MCPdirectGatewayApplication;
import ai.mcpdirect.gateway.mcp.MCPdirectTransportProviderFactory;
import ai.mcpdirect.gateway.util.AIToolDirectory;
import ai.mcpdirect.gateway.util.MCPdirectAccessKeyCache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceName("aitools.hub")
@ServiceRequestMapping("/aitools/hub/")
public class AIToolHubServiceHandler implements MCPdirectTransportProviderFactory,ServiceBroadcastListener {
    private static final Logger LOG = LoggerFactory.getLogger(AIToolHubServiceHandler.class);
//    aitools.discovery@mcpdirect.ai/list/user/tools
    public static final USL USL_LIST_USER_TOOLS = new USL("aitools.discovery","mcpdirect.ai","list/user/tools");
    private ServiceEngine engine;
    private MCPAccessKeyMapper accessKeyMapper;
    private MCPToolMapper toolMapper;
    @ServiceRequestInit
    public void init(ServiceEngine engine) {
        this.engine = engine;
        accessKeyMapper = MCPAccessKeyDataHelper.getInstance().getMCPAccessKeyMapper();
        toolMapper = MCPToolDataHelper.getInstance().getMCPToolMapper();
        MCPdirectGatewayApplication.setFactory(this);
//        executorService = Executors.newFixedThreadPool(200);
        engine.joinBroadcastGroup(new USL("aitools","mcpdirect.ai"),
                "", AUDIENCES_PEERS|AUDIENCES_LOCAL, this);
    }

    public static class UpdatedTool{
        public long userId;
        public long lastUpdated;
    }
    public static class BroadcastOfAnnounce{
        public List<Long> users;
        public List<UpdatedTool> tools;
    }
    @Override
    public void onServiceBroadcastEvent(int event, USL group, String sourceEngineId, String message) {
        LOG.info("onServiceBroadcastEvent({},{},{},{})",event,group,sourceEngineId,message);
        if (event == ServiceBroadcastListener.BROADCAST_GROUP_MESSAGE_RECEIVED){
            String path = group.getPath();
            try {
                if (path.equals("/aitools/publish")||path.equals("/aitools/announce")) {
                    BroadcastOfAnnounce req = JSON.fromJson(message, BroadcastOfAnnounce.class);
                    if(req.tools!=null) for (UpdatedTool tool : req.tools) {
                        cache.toolsAnnounce(tool.userId,tool.lastUpdated);
                    }
                }else if(path.equals("/access_key/update")){
                    List<MCPdirectAccessKeyCache.AccessKey> list = JSON.fromJson(message, new TypeReference<>() {});
                    for (MCPdirectAccessKeyCache.AccessKey accessKey : list) {
//                        MCPdirectAccessKeyCache.AccessKey old = cache.get(accessKey.userId, accessKey.id);
                        MCPdirectAccessKeyCache.AccessKey old = cache.getAccessKey(accessKey.id);
                        if(old!=null){
                            old.status = accessKey.status;
                            if(accessKey.status<1){
                                MCPdirectTransportProvider provider = providers.remove(MCPdirectAccessKeyValidator.hashCode(old.aik));
                                if(provider!=null){
                                    provider.closeGracefully();
                                    cache.addAccessKey(accessKey.userId,accessKey.id,-1,null);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("onServiceBroadcastEvent({},{})",group,message,e);
            }
        }
    }
    final ConcurrentHashMap<Long, MCPdirectTransportProvider> providers = new ConcurrentHashMap<>();
    final MCPdirectAccessKeyCache cache = new MCPdirectAccessKeyCache();

    public AIToolDirectory listUserTools(
            String aiportAuth
    ) throws Exception {
        AIPortAccessKeyCredential key = accessKeyMapper.selectAccessKeyCredentialById(MCPdirectAccessKeyValidator.hashCode(aiportAuth));
        AIToolDirectory directory = AIToolDirectory.create(key.userId);
        Map<Long,AIPortTool> aiPortToolMap = new HashMap<>();
        List<AIPortTool> aiPortTools = toolMapper.selectPermittedTools(key.id);
        for (AIPortTool aiPortTool : aiPortTools) {
            aiPortToolMap.put(aiPortTool.id,aiPortTool);
        }
        aiPortTools = toolMapper.selectVirtualPermittedTools(key.id);
        for (AIPortTool aiPortTool : aiPortTools) {
            aiPortToolMap.put(aiPortTool.id,aiPortTool);
        }
        aiPortTools = aiPortToolMap.values().stream().sorted(Comparator.comparingLong(t-> t.agentId)).toList();
        HashSet<Long> agentIds = new HashSet<>();
        HashSet<Long> makerIds = new HashSet<>();
        for (AIPortTool aiPortTool : aiPortTools){
            agentIds.add(aiPortTool.agentId);
            makerIds.add(aiPortTool.makerId);
        }

        if(agentIds.isEmpty()){
            directory.tools = Map.of();
            return directory;
        }
        Map<Long, AIPortToolAgent> agentMap = toolMapper.selectToolAgentByIds(agentIds.stream().toList()).stream()
                .collect(Collectors.toMap(a -> a.id, a -> a));
        Map<Long, AIPortToolMaker> makerMap = toolMapper.selectToolMakerByIds(makerIds.stream().toList()).stream()
                .collect(Collectors.toMap(a -> a.id, a -> a));
        Map<Long,List<AIPortTeamToolMaker>> teamToolMakerMap = new HashMap<>();
        List<AIPortTeamToolMaker> teamToolMakers = toolMapper.selectTeamToolMakerByMemberId(key.userId);
        for (AIPortTeamToolMaker teamToolMaker : teamToolMakers) {
            List<AIPortTeamToolMaker> list = teamToolMakerMap.computeIfAbsent(teamToolMaker.toolMakerId,
                    k -> new ArrayList<>());
            list.add(teamToolMaker);
        }
        long agentId = 0;
        AIToolDirectory.Tools tools=null;
        for (AIPortTool tool : aiPortTools) {
            List<AIPortTeamToolMaker> list = teamToolMakerMap.get(tool.makerId);
            boolean skip = false;
            if(list!=null) for (AIPortTeamToolMaker teamToolMaker : list) {
                if(teamToolMaker.status>0&&teamToolMaker.memberStatus>0&&teamToolMaker.teamStatus>0){
                    break;
                }
                skip = true;
            }
            if(!skip) {
                if (tool.agentId != agentId) {
                    agentId = tool.agentId;
                    if (tools != null) {
                        directory.tools.put(tools.engineId, tools);
                    }
                    tools = null;
                }
                if (tools == null) {
                    tools = new AIToolDirectory.Tools();
                    tools.descriptions = new ArrayList<>();
                    tools.engineId = agentMap.get(agentId).engineId;
                }
                AIToolDirectory.Description d = new AIToolDirectory.Description();
                d.name = tool.name;
                d.tags = makerMap.get(tool.makerId).tags;
                d.metaData = JSON.fromJson(tool.metaData, new TypeReference<>() {
                });
                tools.descriptions.add(d);
            }
        }
        if(tools!=null) directory.tools.put(tools.engineId,tools);
        return directory;
    }
    @Override
    public MCPdirectTransportProvider getMCPdirectTransportProvider(String apiKey) {
        MCPdirectTransportProvider provider;
        long apiKeyHash = MCPdirectAccessKeyValidator.hashCode(apiKey);
        MCPdirectAccessKeyCache.AccessKey accessKey = cache.getAccessKey(apiKeyHash);
        if(accessKey!=null&&accessKey.status<1){
            provider = providers.remove(apiKeyHash);
            if(provider!=null){
                provider.closeGracefully();
            }
            return null;
        }

        provider = providers.get(apiKeyHash);
        if(provider==null||accessKey==null||cache.toolsAnnounced(accessKey.userId))try {
            if(provider!=null){
                providers.remove(apiKeyHash);
                provider.closeGracefully();
            }

            AIToolDirectory ap = listUserTools(apiKey);
            provider = createMCPdirectTransportProvider(ap.userId,apiKey);
            for (AIToolDirectory.Tools tools : ap.tools.values())
                for (AIToolDirectory.Description d : tools.descriptions) {
                    String name = d.name;
                    ServiceDescription s = d.metaData;
                    String description = s.description;
                    if(d.tags!=null&&!(d.tags=d.tags.trim()).isEmpty()){
                        description+="\n\n**This tool is associated with "+d.tags+"**";
                    }
                    USL usl = new USL(s.serviceName,tools.engineId,s.servicePath);
                    if(name==null||(name=name.trim()).isEmpty()){
                        String path = s.servicePath;;
                        name = "_"+path.substring(path.lastIndexOf("/")+1);
                    }else name="_"+name;
                    if(name.length()>54) name = name.substring(0,54);
                    name += ("_"+Long.toString((usl.toString().hashCode()&0xFFFFFFFFL),32));
                    provider.addTool(name, description, s.requestSchema,usl, engine);
                }
            cache.addAccessKey(ap.userId,apiKeyHash,1,apiKey);
            cache.toolsUpdate(ap.userId,System.currentTimeMillis());
        }catch (Exception e){
            LOG.error("getMCPdirectTransportProvider({})",apiKey,e);
        }
        return provider;
    }
//    @Override
//    public MCPdirectTransportProvider getMCPdirectTransportProvider(String apiKey) {
//        MCPdirectTransportProvider provider;
//        long apiKeyHash = MCPdirectAccessKeyValidator.hashCode(apiKey);
//        MCPdirectAccessKeyCache.AccessKey accessKey = cache.getAccessKey(apiKeyHash);
//        if(accessKey!=null&&accessKey.status<1){
//            provider = providers.remove(apiKeyHash);
//            if(provider!=null){
//                provider.closeGracefully();
//            }
//            return null;
//        }
//
//        provider = providers.get(apiKeyHash);
//        if(provider==null||accessKey==null||cache.toolsAnnounced(accessKey.userId))try {
//            if(provider!=null){
//                providers.remove(apiKeyHash);
//                provider.closeGracefully();
//            }
//            Service service = USL_LIST_USER_TOOLS.createServiceClient()
//                    .headers(new ServiceHeaders().addHeader("mcpdirect-auth",apiKey))
//                    .content("{\"lastUpdated\":"+(accessKey==null?0:cache.toolsLastUpdated(accessKey.userId))+"}")
//                    .request(engine);
//            SimpleServiceResponseMessage<AIToolDirectory> resp;
//            AIToolDirectory ap;
//            if(service.getErrorCode()==0&&(resp=JSON.fromJson(service.getResponseMessage(),
//                    new TypeReference<>() {})).code==0&&(ap=resp.data)!=null){
//                provider = createMCPdirectTransportProvider(ap.userId,apiKey);
//                for (AIToolDirectory.Tools tools : ap.tools.values())
//                    for (AIToolDirectory.Description d : tools.descriptions) {
//                        String name = d.name;
//                        ServiceDescription s = d.metaData;
////                        MCPToolSchema s = d.metaData;
//                        String description = s.description;
//                        if(d.tags!=null&&!d.tags.isEmpty()){
//                            description+="\n\n**This tool is associated with "+String.join(", ",d.tags)+"**";
//                        }
//                        USL usl = new USL(s.serviceName,tools.engineId,s.servicePath);
//                        if(name==null||(name=name.trim()).isEmpty()){
//                            String path = s.servicePath;;
//                            name = "_"+path.substring(path.lastIndexOf("/")+1);
//                        }else name="_"+name;
//
////                        int p = path.lastIndexOf("/");
////                        String prefix = Long.toString(((usl.getServiceAddress()+path.substring(0,p)).hashCode()&0xFFFFFFFFL),32);
////                        String name = "_"+prefix +"_"+path.substring(p+1);
//                        if(name.length()>54) name = name.substring(0,54);
//                        name += ("_"+Long.toString((usl.toString().hashCode()&0xFFFFFFFFL),32));
//                        provider.addTool(name, description, s.requestSchema,usl, engine);
//                    }
//                cache.addAccessKey(ap.userId,apiKeyHash,1,apiKey);
//                cache.toolsUpdate(ap.userId,System.currentTimeMillis());
//            }else{
//                cache.addAccessKey(0,apiKeyHash,-1,null);
//            }
//        }catch (Exception e){
//            LOG.error("getMCPdirectTransportProvider({})",apiKey,e);
//        }
//        return provider;
//    }

    private MCPdirectTransportProvider createMCPdirectTransportProvider(long userId, String apiKey){
        MCPdirectTransportProvider provider = new MCPdirectTransportProvider(userId, apiKey);
        providers.put(MCPdirectAccessKeyValidator.hashCode(apiKey), provider);
        return provider;
    }
}
