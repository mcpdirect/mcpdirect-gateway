package ai.mcpdirect.gateway.dao.entity.aitool;

public class AIPortToolLog {
    public long userId;
    public long keyId;
    public long toolId;
    public long created;

    public AIPortToolLog(){}
    public AIPortToolLog(long userId, long keyId, long toolId) {
        this.userId = userId;
        this.keyId = keyId;
        this.toolId = toolId;
        this.created = System.currentTimeMillis();
    }
}
