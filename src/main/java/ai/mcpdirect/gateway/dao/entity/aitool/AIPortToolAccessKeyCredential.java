package ai.mcpdirect.gateway.dao.entity.aitool;


public class AIPortToolAccessKeyCredential extends AIPortToolAccessKey {
    public String secretKey;

    public AIPortToolAccessKeyCredential() {}

    public AIPortToolAccessKeyCredential(long id, long userId,String name, String secretKey, int status, long effectiveDate, long expirationDate, long created) {
        super(id, userId,name, secretKey, status, effectiveDate, expirationDate, created);
        this.secretKey = secretKey;
    }
}
