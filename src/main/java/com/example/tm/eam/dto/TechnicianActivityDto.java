package com.example.tm.eam.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Transfers technician activity dto data between layers.
 */
@Value
@Builder
public class TechnicianActivityDto {

    String technicianName;
    String activity;
    String timeAgo;
}
