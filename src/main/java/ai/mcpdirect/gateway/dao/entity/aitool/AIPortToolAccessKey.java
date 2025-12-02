package ai.mcpdirect.gateway.dao.entity.aitool;

public class AIPortToolAccessKey {
    public static final int STATUS_ENABLE = 1;
    public static final int STATUS_DISABLE = 0;

    public long id;
    public long effectiveDate;
    public long expirationDate;
    public long userId;
    public long created;
    public int status;
    public String name;
    public int usage;

    public AIPortToolAccessKey(){}
    public AIPortToolAccessKey(long id, long userId, String name, String secretKey, int status, long effectiveDate, long expirationDate, long created) {
        this.id = id;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.userId = userId;
        this.created = created;
        this.status = status;
        this.name = name;
    }
}
