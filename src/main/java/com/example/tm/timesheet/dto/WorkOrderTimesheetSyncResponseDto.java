package com.example.tm.timesheet.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Transfers work order timesheet sync response dto data between layers.
 */
@Data
@Builder
public class WorkOrderTimesheetSyncResponseDto {

    private Long workOrderId;
    private String workOrderNumber;
    private int rowsUpserted;
    private int timesheetsUpdated;
    private int techniciansUpdated;
}
