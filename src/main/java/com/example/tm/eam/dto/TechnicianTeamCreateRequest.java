package com.example.tm.eam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TechnicianTeamCreateRequest {

    @NotBlank
    private String teamName;

    private String teamDescription;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private String notes;

    private List<Long> technicianIds;

    private Long teamLeaderId;
}
