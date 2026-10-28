package com.example.tm.auth.security;

import com.example.tm.auth.integration.eam.EamCompanyRepository;
import com.example.tm.auth.integration.eam.EamUser;
import com.example.tm.auth.integration.eam.EamUserCompanyRepository;
import com.example.tm.auth.integration.eam.EamUserRepository;
import com.example.tm.auth.integration.eam.EamUserStatus;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Resolves active company membership from EAM mappings with the TM-technician mapping fallback used at login. */
@Service
public class TmCompanyAuthorizationService {

    private final EamUserRepository eamUserRepository;
    private final EamUserCompanyRepository eamUserCompanyRepository;
    private final EamCompanyRepository eamCompanyRepository;
    private final JdbcTemplate tmJdbcTemplate;

    public TmCompanyAuthorizationService(
            EamUserRepository eamUserRepository,
            EamUserCompanyRepository eamUserCompanyRepository,
            EamCompanyRepository eamCompanyRepository,
            @Qualifier("tmJdbcTemplate") JdbcTemplate tmJdbcTemplate) {
        this.eamUserRepository = eamUserRepository;
        this.eamUserCompanyRepository = eamUserCompanyRepository;
        this.eamCompanyRepository = eamCompanyRepository;
        this.tmJdbcTemplate = tmJdbcTemplate;
    }

    public boolean isActiveMember(TmUserPrincipal principal, Long companyId) {
        if (principal == null || companyId == null || companyId <= 0) {
            return false;
        }
        if (eamCompanyRepository.findById(companyId)
                .filter(company -> Boolean.TRUE.equals(company.getActive()))
                .isEmpty()) {
            return false;
        }

        EamUser eamUser = eamUserRepository.findByEmailAndDeletedFalse(principal.email()).orElse(null);
        if (eamUser != null) {
            if (eamUser.getStatus() != EamUserStatus.ACTIVE) {
                return false;
            }
            boolean mapped = eamUserCompanyRepository.findByUser_IdAndCompany_ActiveTrue(eamUser.getId()).stream()
                    .map(mapping -> mapping.getCompany())
                    .filter(Objects::nonNull)
                    .anyMatch(company -> Objects.equals(company.getId(), companyId));
            if (mapped) {
                return true;
            }
        }

        Number count = tmJdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM technicians
                        WHERE company_id = ?
                          AND is_deleted = 0
                          AND LOWER(email) = LOWER(?)
                        """,
                Number.class,
                companyId,
                principal.email());
        return count != null && count.longValue() > 0;
    }
}
