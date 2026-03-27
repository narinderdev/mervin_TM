package com.example.tm.eam.repository;

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

/**
 * Defines operations for eam lookup repository.
 */
public interface EamLookupRepository {

    TechnicianDashboardResponse getTechnicianDashboard(Integer limit, Long companyId);

    TechnicianListResponse getTechnicians(int page, int size, Long companyId);

    List<DailyAvailabilityDto> getTechnicianAvailabilityMonthly(Long technicianId, Integer days, Long companyId);

    TechnicianTeamListResponse getTechnicianTeams(int page, int size, Long companyId);

    WorkOrderListResponse getWorkOrders(int page, int size, Long companyId);

    WorkOrderDetailsResponse getWorkOrderById(Long workOrderId, Long companyId);

    TechnicianHolidayListResponse getHolidays(int page, int size, Long companyId);

    TechnicianHolidayResponse getHolidayById(Long holidayId, Long companyId);

    TechnicianLeaveListResponse getTechniciansLeaves(int page, int size, Long companyId);

    TechnicianLeaveListResponse getTechnicianLeaves(Long technicianId, Long companyId);

    TechnicianLeaveResponse getTechnicianLeaveById(Long technicianId, Long leaveId, Long companyId);
}
