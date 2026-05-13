package com.example.tm.eam.service;

import com.example.tm.eam.dto.SecurityRoleReportItem;
import java.util.List;

/**
 * Defines operations for eam security report service.
 */
public interface EamSecurityReportService {

    List<SecurityRoleReportItem> getRolePermissionReport(Long companyId);
}
