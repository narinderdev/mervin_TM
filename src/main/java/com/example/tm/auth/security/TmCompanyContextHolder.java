package com.example.tm.auth.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Holds the company that was authorized for the current request. */
public final class TmCompanyContextHolder {

    private static final ThreadLocal<Long> COMPANY_ID = new ThreadLocal<>();

    private TmCompanyContextHolder() {
    }

    public static void setCompanyId(Long companyId) {
        COMPANY_ID.set(companyId);
    }

    public static Long requireCompanyId() {
        Long companyId = COMPANY_ID.get();
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active company context is required");
        }
        return companyId;
    }

    public static void clear() {
        COMPANY_ID.remove();
    }
}
