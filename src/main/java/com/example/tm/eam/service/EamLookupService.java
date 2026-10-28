package com.example.tm.eam.service;

import com.example.tm.eam.dto.DailyAvailabilityDto;
import com.example.tm.eam.dto.TechnicianCreateRequest;
import com.example.tm.eam.dto.TechnicianDashboardResponse;
import com.example.tm.eam.dto.TechnicianHolidayListResponse;
import com.example.tm.eam.dto.TechnicianHolidayResponse;
import com.example.tm.eam.dto.TechnicianLeaveListResponse;
import com.example.tm.eam.dto.TechnicianLeaveResponse;
import com.example.tm.eam.dto.TechnicianListResponse;
import com.example.tm.eam.dto.TechnicianPatchRequest;
import com.example.tm.eam.dto.TechnicianDetailsResponse;
import com.example.tm.eam.dto.TechnicianTeamCreateRequest;
import com.example.tm.eam.dto.TechnicianTeamPatchRequest;
import com.example.tm.eam.dto.TechnicianTeamDetailsResponse;
import com.example.tm.eam.dto.TechnicianTeamListResponse;
import com.example.tm.eam.dto.WorkOrderDetailsResponse;
import com.example.tm.eam.dto.WorkOrderGlAccountListResponse;
import com.example.tm.eam.dto.WorkOrderListResponse;
import com.example.tm.eam.dto.WorkOrderNumberListResponse;
import com.example.tm.eam.dto.WorkOrderTypeListResponse;
import com.example.tm.eam.dto.WorkRequestTypePropertyUnitListResponse;
import java.util.List;

/**
 * Defines operations for eam lookup service.
 */
public interface EamLookupService {

    TechnicianDashboardResponse getDashboardTechnicians(Integer limit);

    TechnicianDetailsResponse createTechnician(TechnicianCreateRequest request);

    TechnicianDetailsResponse getTechnicianById(Long technicianId);

    TechnicianDetailsResponse patchTechnician(Long technicianId, TechnicianPatchRequest request);

    void deleteTechnician(Long technicianId);

    TechnicianListResponse getTechnicians(int page, int size);

    List<DailyAvailabilityDto> getTechnicianAvailabilityMonthly(Long technicianId, Integer days);

    TechnicianTeamDetailsResponse createTechnicianTeam(TechnicianTeamCreateRequest request);

    TechnicianTeamDetailsResponse getTechnicianTeamById(Long teamId);

    TechnicianTeamDetailsResponse patchTechnicianTeam(Long teamId, TechnicianTeamPatchRequest request);

    void deleteTechnicianTeam(Long teamId);

    TechnicianTeamListResponse getTechnicianTeams(int page, int size);

    WorkOrderNumberListResponse getWorkOrderNumbers(int page, int size, Long technicianId, Long companyId);

    WorkOrderNumberListResponse getCapexWorkOrderNumbers(int page, int size, Long technicianId, Long companyId);

    WorkOrderGlAccountListResponse getWorkOrderGlAccounts(int page, int size, Long companyId);

    WorkOrderTypeListResponse getWorkOrderTypes(int page, int size, Long companyId);

    WorkRequestTypePropertyUnitListResponse getWorkRequestTypePropertyUnits(int page, int size, Long companyId);

    WorkOrderListResponse getWorkOrders(int page, int size, Long companyId);

    WorkOrderDetailsResponse getWorkOrderById(Long workOrderId, Long companyId);

    WorkOrderDetailsResponse addWorkOrderToFavourites(Long technicianId, Long workOrderId, Long companyId);

    TechnicianHolidayListResponse getHolidays(int page, int size, Long companyId);

    TechnicianHolidayResponse getHolidayById(Long holidayId, Long companyId);

    TechnicianLeaveListResponse getTechniciansLeaves(int page, int size);

    TechnicianLeaveListResponse getTechnicianLeaves(Long technicianId);

    TechnicianLeaveResponse getTechnicianLeaveById(Long technicianId, Long leaveId);
}
