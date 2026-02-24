package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TechnicianDashboardResponse {

    long totalTechnicians;
    long availableToday;
    long onLeave;
    long workOrders;
    List<TechnicianActivityDto> recentActivities;
}
