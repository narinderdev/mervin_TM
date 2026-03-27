package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers technician team membership response data between layers.
 */
@Data
@Builder
public class TechnicianTeamMembershipResponse {

    private Long teamId;
    private String teamName;
    private boolean teamLeader;
    private List<String> teamLeaderNames;
}
