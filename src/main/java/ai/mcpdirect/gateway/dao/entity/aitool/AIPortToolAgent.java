package ai.mcpdirect.gateway.dao.entity.aitool;

import ai.mcpdirect.backend.util.ID;

public class AIPortToolAgent {
    public long id;
    public long userId;
    public String engineId;
    public long created;
    public long deviceId;
    public String device;
    public String name;
    public String tags;
    public int status;

    public AIPortToolAgent() {}

    public AIPortToolAgent(long userId, String engineId, long created,long deviceId,
                           String device, String name, String tags, int status) {
        this.id = ID.nextId();
        this.userId = userId;
        this.engineId = engineId;
        this.created = created;
        this.deviceId = deviceId;
        this.device = device;
        this.name = name;
        this.tags = tags;
        this.status = status;
    }
}