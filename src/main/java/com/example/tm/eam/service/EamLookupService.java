package com.example.tm.eam.service;

public interface EamLookupService {

    Object getDashboardTechnicians(Integer limit);

    Object getTechnicians(int page, int size);

    Object getTechnicianAvailabilityMonthly(Long technicianId, Integer days);

    Object getTechnicianTeams(int page, int size);

    Object getWorkOrders(int page, int size);

    Object getWorkOrderById(Long workOrderId);

    Object getHolidays(int page, int size);

    Object getHolidayById(Long holidayId);

    Object getTechniciansLeaves(int page, int size);

    Object getTechnicianLeaves(Long technicianId);

    Object getTechnicianLeaveById(Long technicianId, Long leaveId);
}
