package com.example.tm.team.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TmTeamDetailsResponse {

    private Long id;
    private String teamName;
    private String teamDescription;
    private String status;
    private Long teamLeaderId;
    private List<Long> technicianIds;
}
