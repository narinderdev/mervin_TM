package com.example.tm.eam.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TechnicianLeaveResponse {

    Long id;
    Long technicianId;
    String technicianName;
    LocalDate startDate;
    LocalDate endDate;
    String reason;
    LocalDateTime createdAt;
}
