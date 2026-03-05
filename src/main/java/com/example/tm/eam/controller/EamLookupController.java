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
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/numbers page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work order numbers fetched successfully", eamLookupService.getWorkOrderNumbers(page, size)));
    }

    @GetMapping("/work-orders")
    public ResponseEntity<ApiResponse<?>> getWorkOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work orders fetched successfully", eamLookupService.getWorkOrders(page, size)));
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<ApiResponse<?>> getWorkOrderById(
            @PathVariable("id") Long workOrderId,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/{} cid={}", workOrderId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Work order details fetched successfully", eamLookupService.getWorkOrderById(workOrderId)));
    }

    @PostMapping({"/work-orders/{id}/favourites", "/work-orders/{id}/favorites"})
    public ResponseEntity<ApiResponse<?>> addWorkOrderToFavourites(
            @PathVariable("id") Long workOrderId,
            @RequestParam("technicianId") Long technicianId,
            HttpServletRequest request) {
        log.info("EAM POST /work-orders/{}/favourites technicianId={} cid={}", workOrderId, technicianId, correlationId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.successResponse(
                HttpStatus.CREATED.value(),
                "Work order added to favourites",
                eamLookupService.addWorkOrderToFavourites(technicianId, workOrderId)));
    }

    @GetMapping({"/work-orders/favourites", "/work-orders/favorites"})
    public ResponseEntity<ApiResponse<?>> getFavouriteWorkOrders(
            @RequestParam("technicianId") Long technicianId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpServletRequest request) {
        log.info("EAM GET /work-orders/favourites technicianId={} page={} size={} cid={}",
                technicianId, page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Favourite work orders fetched successfully",
                eamLookupService.getFavouriteWorkOrders(technicianId, page, size)));
    }

    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<?>> getHolidays(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            HttpServletRequest request) {
        log.info("EAM GET /holidays page={} size={} cid={}", page, size, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holidays fetched successfully", eamLookupService.getHolidays(page, size)));
    }

    @GetMapping("/holidays/{id}")
    public ResponseEntity<ApiResponse<?>> getHolidayById(
            @PathVariable("id") Long holidayId,
            HttpServletRequest request) {
        log.info("EAM GET /holidays/{} cid={}", holidayId, correlationId(request));
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(), "Holiday fetched successfully", eamLookupService.getHolidayById(holidayId)));
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
