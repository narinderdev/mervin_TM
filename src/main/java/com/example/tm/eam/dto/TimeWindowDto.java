package com.example.tm.eam.dto;

import java.time.LocalTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeWindowDto {

    LocalTime start;
    LocalTime end;
}
