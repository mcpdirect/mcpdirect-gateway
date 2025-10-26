package ai.mcpdirect.gateway.dao.entity.account;

public class AIPortUser {
    public long id;
    public String name;
    public String language;
    public long created;
    public int type;
    public String account;
    public AIPortUser(){}
    public AIPortUser(long id, String name, String language, long created, int type) {
        this.id = id;
        this.name = name;
        this.language = language;
        this.created = created;
        this.type = type;
    }
}
