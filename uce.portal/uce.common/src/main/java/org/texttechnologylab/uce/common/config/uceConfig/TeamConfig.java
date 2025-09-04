package org.texttechnologylab.uce.common.config.uceConfig;

import java.util.List;

public class TeamConfig {
    private String description;
    private List<TeamMember> members;
    public TeamConfig(){}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }
}
