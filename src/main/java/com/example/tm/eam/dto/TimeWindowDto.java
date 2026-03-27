package com.example.tm.eam.dto;

import java.time.LocalTime;
import lombok.Builder;
import lombok.Value;

/**
 * Transfers time window dto data between layers.
 */
@Value
@Builder
public class TimeWindowDto {

    LocalTime start;
    LocalTime end;
}
