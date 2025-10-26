package ai.mcpdirect.gateway.dao.entity.account;

public class AIPortAccessKeyCredential extends AIPortAccessKey{
    public String secretKey;

    public AIPortAccessKeyCredential() {}

    public AIPortAccessKeyCredential(long id,long userId, int userRoles,String name, String secretKey, int status, long effectiveDate, long expirationDate, long created) {
        super(id, userId, userRoles,name, secretKey, status, effectiveDate, expirationDate, created);
        this.secretKey = secretKey;
    }
}
