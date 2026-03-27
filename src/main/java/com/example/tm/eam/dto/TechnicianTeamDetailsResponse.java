package com.example.tm.eam.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers technician team details response data between layers.
 */
@Data
@Builder
public class TechnicianTeamDetailsResponse {

    private Long id;
    private Long companyId;
    private String teamName;
    private String teamDescription;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private Long teamLeaderId;
    private String teamLeaderName;
    private String availability;
    private List<TechnicianDetailsResponse> technicians;
}
