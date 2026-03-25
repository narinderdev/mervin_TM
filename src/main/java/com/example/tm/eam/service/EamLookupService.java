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

public interface EamLookupService {

    TechnicianDashboardResponse getDashboardTechnicians(Integer limit, Long companyId);

    TechnicianDetailsResponse createTechnician(TechnicianCreateRequest request);

    TechnicianDetailsResponse getTechnicianById(Long technicianId, Long companyId);

    TechnicianDetailsResponse patchTechnician(Long technicianId, Long companyId, TechnicianPatchRequest request);

    void deleteTechnician(Long technicianId, Long companyId);

    TechnicianListResponse getTechnicians(int page, int size, Long companyId);

    List<DailyAvailabilityDto> getTechnicianAvailabilityMonthly(Long technicianId, Integer days, Long companyId);

    TechnicianTeamDetailsResponse createTechnicianTeam(TechnicianTeamCreateRequest request);

    TechnicianTeamDetailsResponse getTechnicianTeamById(Long teamId, Long companyId);

    TechnicianTeamDetailsResponse patchTechnicianTeam(Long teamId, Long companyId, TechnicianTeamPatchRequest request);

    void deleteTechnicianTeam(Long teamId, Long companyId);

    TechnicianTeamListResponse getTechnicianTeams(int page, int size, Long companyId);

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

    TechnicianLeaveListResponse getTechniciansLeaves(int page, int size, Long companyId);

    TechnicianLeaveListResponse getTechnicianLeaves(Long technicianId, Long companyId);

    TechnicianLeaveResponse getTechnicianLeaveById(Long technicianId, Long leaveId, Long companyId);
}
