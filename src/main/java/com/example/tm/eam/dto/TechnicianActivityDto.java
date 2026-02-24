package com.example.tm.eam.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TechnicianActivityDto {

    String technicianName;
    String activity;
    String timeAgo;
}
