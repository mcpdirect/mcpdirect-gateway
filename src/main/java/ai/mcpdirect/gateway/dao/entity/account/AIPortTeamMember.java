package ai.mcpdirect.gateway.dao.entity.account;

public class AIPortTeamMember{
    public long teamId;
    public long memberId;

    public Integer status;
    public long created;
    public Long expirationDate;
    public long lastUpdated;
    public int teamStatus;

    public AIPortTeamMember teamId(long teamId) {
        this.teamId = teamId;
        return this;
    }

    public AIPortTeamMember memberId(long memberId) {
        this.memberId = memberId;
        return this;
    }

    public AIPortTeamMember status(Integer status) {
        this.status = status;
        return this;
    }

    public AIPortTeamMember created(long created) {
        this.created = created;
        return this;
    }

    public AIPortTeamMember expirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }
    public AIPortTeamMember lastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public static AIPortTeamMember build(){
        return new AIPortTeamMember();
    }
}