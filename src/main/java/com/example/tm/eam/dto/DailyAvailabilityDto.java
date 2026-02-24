package com.example.tm.eam.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyAvailabilityDto {

    LocalDate date;
    String status;
    List<TimeWindowDto> busyWindows;
    List<TimeWindowDto> freeWindows;
}
