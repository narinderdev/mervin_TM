package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TechnicianTeamMembershipResponse {

    private Long teamId;
    private String teamName;
    private boolean teamLeader;
    private List<String> teamLeaderNames;
}
