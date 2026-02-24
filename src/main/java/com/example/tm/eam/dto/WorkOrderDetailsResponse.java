package com.example.tm.eam.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkOrderDetailsResponse {

    private Long id;
    private String workOrderNumber;
    private String workOrderId;
    private Long workRequestTypeId;
    private String workRequestTypeCode;
    private String workRequestTypeDescription;
    private String location;
    private String workType;
    private String priority;
    private String woTitle;
    private String descriptionScope;
    private String planner;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private Long assignedTeamId;
    private String assignedTeamName;
    private LocalDateTime plannedStartDateTime;
    private LocalDateTime plannedEndDateTime;
    private LocalDateTime actualStartDateTime;
    private LocalDateTime actualEndDateTime;
    private LocalDate targetCompletionDate;
    private String status;
    private String source;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
