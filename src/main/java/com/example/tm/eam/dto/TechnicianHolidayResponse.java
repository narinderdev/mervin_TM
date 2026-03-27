package com.example.tm.eam.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * Transfers technician holiday response data between layers.
 */
@Value
@Builder
public class TechnicianHolidayResponse {

    Long id;
    String holidayName;
    String holidayType;
    LocalDate holidayDate;
    String notes;
    LocalDateTime createdAt;
}
