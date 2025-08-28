package ai.mcpdirect.gateway.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ai.mcpdirect.gateway.mcp.MCPdirectTransportProvider;
import ai.mcpdirect.util.MCPdirectAccessKeyValidator;
import appnet.hstp.*;
import appnet.hstp.engine.util.JSON;
import ai.mcpdirect.gateway.MCPdirectGatewayApplication;
import ai.mcpdirect.gateway.mcp.MCPdirectTransportProviderFactory;
import ai.mcpdirect.gateway.util.AIToolDirectory;
import ai.mcpdirect.gateway.util.MCPdirectAccessKeyCache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appnet.hstp.annotation.ServiceName;
import appnet.hstp.annotation.ServiceRequestInit;
import appnet.hstp.annotation.ServiceRequestMapping;


@ServiceName("aitools.hub")
@ServiceRequestMapping("/aitools/hub/")
public class AIToolHubServiceHandler implements MCPdirectTransportProviderFactory,ServiceBroadcastListener {
    private static final Logger LOG = LoggerFactory.getLogger(AIToolHubServiceHandler.class);

    private ServiceEngine engine;
//    private ExecutorService executorService;
    @ServiceRequestInit
    public void init(ServiceEngine engine) {
        this.engine = engine;
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

    final
    @Override
    public MCPdirectTransportProvider getMCPwingsTransportProvider(String apiKey) {
//        Long userId = MCPdirectAccessKeyValidator.extractUserId(MCPdirectAccessKeyValidator.PREFIX_AIK,apiKey);
//        if(userId==null){
//            return null;
//        }
        MCPdirectTransportProvider provider;
        long keyId = MCPdirectAccessKeyValidator.hashCode(apiKey);
        MCPdirectAccessKeyCache.AccessKey accessKey = cache.getAccessKey(keyId);
        if(accessKey!=null&&accessKey.status<1){
            provider = providers.remove(keyId);
            if(provider!=null){
                provider.closeGracefully();
            }
            return null;
        }

        long apiKeyHash = MCPdirectAccessKeyValidator.hashCode(apiKey);
        provider = providers.get(apiKeyHash);
        if(provider==null||accessKey==null||cache.toolsAnnounced(accessKey.userId))try {
            if(provider!=null){
                providers.remove(apiKeyHash);
                provider.closeGracefully();
            }
            Service service = USL
                    .createServiceClient("aitools.management@mcpdirect.ai/list/user/tools")
                    .headers(new ServiceHeaders().addHeader("mcpdirect-auth",apiKey))
                    .content("{\"lastUpdated\":"+(accessKey==null?0:cache.toolsLastUpdated(accessKey.userId))+"}")
                    .request(engine);
            SimpleServiceResponseMessage<AIToolDirectory> resp;
            AIToolDirectory ap;
            if(service.getErrorCode()==0&&(resp=JSON.fromJson(service.getResponseMessage(),
                    new TypeReference<>() {})).code==0&&(ap=resp.data)!=null){
                provider = createMCPdirectTransportProvider(ap.userId,apiKey);
                for (AIToolDirectory.Tools tools : ap.tools.values())
                    for (AIToolDirectory.Description d : tools.descriptions) {
                        String name = d.name;
                        ServiceDescription s = d.metaData;
                        String description = s.description;
                        if(d.tags!=null&&!d.tags.isEmpty()){
                            description+="\n\n**This tool is associated with "+String.join(", ",d.tags)+"**";
                        }
                        USL usl = new USL(s.serviceName,tools.engineId,s.servicePath);
                        if(name==null||(name=name.trim()).isEmpty()){
                            String path = s.servicePath;;
                            name = "_"+path.substring(path.lastIndexOf("/")+1);
                        }else name="_"+name;

//                        int p = path.lastIndexOf("/");
//                        String prefix = Long.toString(((usl.getServiceAddress()+path.substring(0,p)).hashCode()&0xFFFFFFFFL),32);
//                        String name = "_"+prefix +"_"+path.substring(p+1);
                        if(name.length()>54) name = name.substring(0,54);
                        name += ("_"+Long.toString((usl.toString().hashCode()&0xFFFFFFFFL),32));
                        provider.addTool(name, description, s.requestSchema,usl, engine);
                    }
                cache.addAccessKey(ap.userId,keyId,1,apiKey);
                cache.toolsUpdate(ap.userId,System.currentTimeMillis());
            }else{
                cache.addAccessKey(0,keyId,-1,null);
            }
        }catch (Exception e){
            LOG.error("getMCPdirectTransportProvider({})",apiKey,e);
        }
        return provider;
    }

    private MCPdirectTransportProvider createMCPdirectTransportProvider(long userId, String apiKey){
        MCPdirectTransportProvider provider = new MCPdirectTransportProvider(userId, apiKey);
        providers.put(MCPdirectAccessKeyValidator.hashCode(apiKey), provider);
        return provider;
    }
}
