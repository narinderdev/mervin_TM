package com.example.tm.eam.controller;

import com.example.tm.eam.service.EamLookupService;
import com.example.tm.shared.constants.HeaderConstants;
import com.example.tm.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class EamLookupController {

    private final EamLookupService eamLookupService;

    @GetMapping("/work-orders/numbers")
    public ResponseEntity<ApiResponse<?>> getWorkOrderNumbers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam("technicianId") Long technicianId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/numbers page={} size={} technicianId={} companyId={} cid={}",
                page, size, technicianId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Work order numbers fetched successfully",
                eamLookupService.getWorkOrderNumbers(page, size, technicianId, companyId)));
    }

    @GetMapping("/work-orders/numbers/capex")
    public ResponseEntity<ApiResponse<?>> getCapexWorkOrderNumbers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam(value = "technicianId", required = false) Long technicianId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/numbers/capex page={} size={} technicianId={} companyId={} cid={}",
                page, size, technicianId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "CAPEX work order numbers fetched successfully",
                eamLookupService.getCapexWorkOrderNumbers(page, size, technicianId, companyId)));
    }

    @GetMapping("/work-orders/gl-accounts")
    public ResponseEntity<ApiResponse<?>> getWorkOrderGlAccounts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/gl-accounts page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work order GL accounts fetched successfully", eamLookupService.getWorkOrderGlAccounts(page, size, companyId)));
    }

    @GetMapping("/work-orders/types")
    public ResponseEntity<ApiResponse<?>> getWorkOrderTypes(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/types page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Work order types fetched successfully",
                eamLookupService.getWorkOrderTypes(page, size, companyId)));
    }

    @GetMapping("/work-request-types/property-units")
    public ResponseEntity<ApiResponse<?>> getWorkRequestTypePropertyUnits(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-request-types/property-units page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Work request type property units fetched successfully",
                eamLookupService.getWorkRequestTypePropertyUnits(page, size, companyId)));
    }

    @GetMapping("/work-orders")
    public ResponseEntity<ApiResponse<?>> getWorkOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work orders fetched successfully", eamLookupService.getWorkOrders(page, size, companyId)));
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<ApiResponse<?>> getWorkOrderById(
            @PathVariable("id") Long workOrderId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/{} companyId={} cid={}", workOrderId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work order details fetched successfully", eamLookupService.getWorkOrderById(workOrderId, companyId)));
    }

    @PostMapping("/work-orders/{id}/favourites")
    public ResponseEntity<ApiResponse<?>> addWorkOrderToFavourites(
            @PathVariable("id") Long workOrderId,
            @RequestParam("technicianId") Long technicianId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM POST /work-orders/{}/favourites technicianId={} companyId={} cid={}",
                workOrderId, technicianId, companyId, correlationId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Work order added to favourites",
                eamLookupService.addWorkOrderToFavourites(technicianId, workOrderId, companyId)));
    }

    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<?>> getHolidays(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /holidays page={} size={} companyId={} cid={}", page, size, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holidays fetched successfully", eamLookupService.getHolidays(page, size, companyId)));
    }

    @GetMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<?>> getHolidayById(
            @PathVariable("id") Long holidayId,
            @RequestParam("companyId") Long companyId,
            HttpServletRequest request) {
        log.info("EAM GET /holidays/{} companyId={} cid={}", holidayId, companyId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holiday fetched successfully", eamLookupService.getHolidayById(holidayId, companyId)));
    }

    private String correlationId(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(HeaderConstants.CORRELATION_ID_HEADER);
        if (fromRequest instanceof String value && !value.isBlank()) {
            return value;
        }
        String fromHeader = request.getHeader(HeaderConstants.CORRELATION_ID_HEADER);
        return fromHeader == null ? "" : fromHeader;
    }
}
