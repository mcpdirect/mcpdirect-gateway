package ai.mcpdirect.gateway.dao.entity.account;

public class AIPortAccessKey {
    public static final int STATUS_ENABLE = 1;
    public static final int STATUS_DISABLE = 0;

    public long id;
    public long effectiveDate;
    public long expirationDate;
    public long userId;
    public int userRoles;
    public long created;
    public int status;
    public String name;
    public int usageAmount;

    public AIPortAccessKey(){}
    public AIPortAccessKey(long id, long userId, int userRoles,String name, String secretKey,int status,long effectiveDate, long expirationDate, long created) {
        this.id = id;
        this.effectiveDate = effectiveDate;
        this.expirationDate = expirationDate;
        this.userId = userId;
        this.userRoles = userRoles;
        this.created = created;
        this.status = status;
        this.name = name;
    }
}
