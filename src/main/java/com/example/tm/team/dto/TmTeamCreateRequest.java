package com.example.tm.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TmTeamCreateRequest {

    @NotBlank
    private String teamName;

    private String teamDescription;

    /**
     * Optional status; defaults to ACTIVE if null/blank.
     */
    private String status;

    @NotEmpty
    private List<Long> technicianIds;

    private Long teamLeaderId;
}
