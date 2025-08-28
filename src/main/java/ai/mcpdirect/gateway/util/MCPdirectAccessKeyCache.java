package ai.mcpdirect.gateway.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MCPdirectAccessKeyCache {
    public static class AccessKey{
        public long id;
        public long userId;
        public int status;
        public String aik;
        private long created;
        public AccessKey(){}
        public AccessKey(long id,long userId,String aik){
            this.id = id;
            this.userId = userId;
            this.aik = aik;
            this.created = System.currentTimeMillis();
        }
    }
    public static class AccessKeyUser{
        public long id;
        public int status=1;
        public long toolsLastAnnounced;
        public long toolsLastUpdated;
        public AccessKeyUser(long id){
            this.id = id;
        }
//        public ConcurrentHashMap<Integer,AccessKey> keys = new ConcurrentHashMap<>();
    }
//    private final ConcurrentHashMap<Long, AccessKeyUser> secretKeyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AccessKey> secretKeyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AccessKeyUser> secretKeyUserCache = new ConcurrentHashMap<>();
    public MCPdirectAccessKeyCache(){
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(()->{
            long now = System.currentTimeMillis();
//            for (Map.Entry<Long, AccessKeyUser> en : secretKeyCache.entrySet()) {
//                ConcurrentHashMap<Integer,AccessKey> keys = en.getValue().keys;
//                for (Map.Entry<Integer, AccessKey> uen : keys.entrySet()) {
//                    AccessKey key = uen.getValue();
//                    if(key.status<0&&(now-key.created)>24*3600*1000){
//                        keys.remove(uen.getKey());
//                    }
//                }
//                if(keys.isEmpty()){
//                    secretKeyCache.remove(en.getKey());
//                }
//            }
            for (Map.Entry<Long, AccessKey> uen : secretKeyCache.entrySet()) {
                AccessKey key = uen.getValue();
                if(key.status<0&&(now-key.created)>24*3600*1000){
                    secretKeyCache.remove(uen.getKey());
                }
            }
        },24,24, TimeUnit.HOURS);
    }
//    public void add(long userId,int keyId,int keyStatus,String aik){
//        AccessKeyUser accessKeyUser = secretKeyCache.computeIfAbsent(userId, id -> new AccessKeyUser());
//        accessKeyUser.keys.put(keyId,new AccessKey(keyStatus,aik));
//    }

    public void addAccessKey(long userId,long keyId,int keyStatus,String aik){
        AccessKey accessKey = secretKeyCache.computeIfAbsent(keyId, id -> new AccessKey(keyId,userId,aik));
        accessKey.status = keyStatus;
        secretKeyUserCache.computeIfAbsent(userId, id -> new AccessKeyUser(userId));
    }

//    public AccessKey get(long userId,int keyId){
//        AccessKeyUser accessKeyUser = secretKeyCache.get(userId);
//        return accessKeyUser!=null?accessKeyUser.keys.get(keyId):null;
//    }
    public AccessKey getAccessKey(long keyId){
        return secretKeyCache.get(keyId);
    }

    public Collection<AccessKey> get(long userId){
        return secretKeyCache.values().stream().filter(k->k.userId==userId).toList();
//        AccessKeyUser accessKeyUser = secretKeyCache.get(userId);
//        return accessKeyUser!=null?accessKeyUser.keys.values(): Collections.emptyList();
    }
    public boolean toolsAnnounced(long userId){
        AccessKeyUser user = secretKeyUserCache.get(userId);
        return user==null||user.toolsLastAnnounced>user.toolsLastUpdated;
    }
    public void toolsUpdate(long userId,long lastUpdated){
        AccessKeyUser user = secretKeyUserCache.get(userId);
        if(user!=null){
            user.toolsLastUpdated = lastUpdated;
        }
    }
    public void toolsAnnounce(long userId, long toolsLastAnnounced) {
        AccessKeyUser user = secretKeyUserCache.get(userId);
        if(user!=null){
            user.toolsLastAnnounced = toolsLastAnnounced;
        }
    }
    public long toolsLastUpdated(long userId){
        AccessKeyUser user = secretKeyUserCache.get(userId);
        if(user!=null){
            return user.toolsLastUpdated;
        }
        return 0;
    }
}