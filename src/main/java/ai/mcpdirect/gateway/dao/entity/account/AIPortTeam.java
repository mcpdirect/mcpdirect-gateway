package ai.mcpdirect.gateway.dao.entity.account;

public class AIPortTeam {
    public long id;
    public String name;
    public long created;
    public long ownerId;
    public Integer status;
    public long lastUpdated;
    public String ownerName;
    public String ownerAccount;

    public AIPortTeam id(long id) {
        this.id = id;
        return this;
    }

    public AIPortTeam name(String name) {
        this.name = name;
        return this;
    }

    public AIPortTeam created(long created) {
        this.created = created;
        return this;
    }

    public AIPortTeam ownerId(long ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public AIPortTeam status(Integer status) {
        this.status = status;
        return this;
    }

    public AIPortTeam lastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public static AIPortTeam build(){
        return new AIPortTeam();
    }
}