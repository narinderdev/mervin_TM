package com.example.tm.eam.service;

import com.example.tm.eam.dto.DailyAvailabilityDto;
import com.example.tm.eam.dto.TechnicianDashboardResponse;
import com.example.tm.eam.dto.TechnicianHolidayListResponse;
import com.example.tm.eam.dto.TechnicianHolidayResponse;
import com.example.tm.eam.dto.TechnicianLeaveListResponse;
import com.example.tm.eam.dto.TechnicianLeaveResponse;
import com.example.tm.eam.dto.TechnicianListResponse;
import com.example.tm.eam.dto.TechnicianTeamListResponse;
import com.example.tm.eam.dto.WorkOrderDetailsResponse;
import com.example.tm.eam.dto.WorkOrderListResponse;
import java.util.List;

public interface EamLookupService {

    TechnicianDashboardResponse getDashboardTechnicians(Integer limit);

    TechnicianListResponse getTechnicians(int page, int size);

    List<DailyAvailabilityDto> getTechnicianAvailabilityMonthly(Long technicianId, Integer days);

    TechnicianTeamListResponse getTechnicianTeams(int page, int size);

    WorkOrderListResponse getWorkOrders(int page, int size);

    WorkOrderDetailsResponse getWorkOrderById(Long workOrderId);

    TechnicianHolidayListResponse getHolidays(int page, int size);

    TechnicianHolidayResponse getHolidayById(Long holidayId);

    TechnicianLeaveListResponse getTechniciansLeaves(int page, int size);

    TechnicianLeaveListResponse getTechnicianLeaves(Long technicianId);

    TechnicianLeaveResponse getTechnicianLeaveById(Long technicianId, Long leaveId);
}
