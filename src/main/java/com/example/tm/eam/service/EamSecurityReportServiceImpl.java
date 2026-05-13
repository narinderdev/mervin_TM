package com.example.tm.eam.service;

import com.example.tm.auth.integration.eam.EamAppPermission;
import com.example.tm.auth.integration.eam.EamCompany;
import com.example.tm.auth.integration.eam.EamCompanyRepository;
import com.example.tm.auth.integration.eam.EamRole;
import com.example.tm.auth.integration.eam.EamRoleRepository;
import com.example.tm.eam.dto.SecurityRoleReportItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements service logic for eam security report service.
 */
@Service
public class EamSecurityReportServiceImpl implements EamSecurityReportService {

    private final EamRoleRepository eamRoleRepository;
    private final EamCompanyRepository eamCompanyRepository;

    /** Creates a new instance of eam security report service impl. */
    public EamSecurityReportServiceImpl(
            EamRoleRepository eamRoleRepository,
            EamCompanyRepository eamCompanyRepository) {
        this.eamRoleRepository = eamRoleRepository;
        this.eamCompanyRepository = eamCompanyRepository;
    }

    /** Returns security report grouped by role and permission object(module/action). */
    @Override
    @Transactional(readOnly = true, transactionManager = "eamTransactionManager")
    public List<SecurityRoleReportItem> getRolePermissionReport(Long companyId) {
        Long resolvedCompanyId = requireActiveCompanyId(companyId);
        List<EamRole> roles = eamRoleRepository.findByActiveTrueAndCompanyIdOrderByNameAsc(resolvedCompanyId);
        return roles.stream().map(this::toReportItem).toList();
    }

    /** Converts one role to report row. */
    private SecurityRoleReportItem toReportItem(EamRole role) {
        Map<String, Set<String>> moduleActions = new TreeMap<>();
        if (role.getPermissions() != null) {
            for (EamAppPermission permission : role.getPermissions()) {
                if (permission == null || !Boolean.TRUE.equals(permission.getActive())) {
                    continue;
                }
                String module = safeTrim(permission.getModule());
                String action = safeTrim(permission.getAction());
                if (module == null || module.isBlank() || action == null || action.isBlank()) {
                    continue;
                }
                moduleActions.computeIfAbsent(module, key -> new TreeSet<>()).add(action);
            }
        }

        Map<String, List<String>> objects = moduleActions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new));

        return SecurityRoleReportItem.builder()
                .role(role.getName())
                .objects(objects)
                .build();
    }

    /** Validates company and returns normalized id. */
    private Long requireActiveCompanyId(Long companyId) {
        if (companyId == null || companyId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId query parameter is required");
        }
        EamCompany company = eamCompanyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found in EAM"));
        if (!Boolean.TRUE.equals(company.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company is inactive in EAM");
        }
        return companyId;
    }

    /** Safely trims nullable strings. */
    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
