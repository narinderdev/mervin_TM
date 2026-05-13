package com.example.tm.eam.service;

import com.example.tm.auth.integration.eam.EamAppPermission;
import com.example.tm.auth.integration.eam.EamAppPermissionRepository;
import com.example.tm.auth.integration.eam.EamCompany;
import com.example.tm.auth.integration.eam.EamCompanyRepository;
import com.example.tm.auth.integration.eam.EamRole;
import com.example.tm.auth.integration.eam.EamRoleRepository;
import com.example.tm.eam.dto.PermissionCatalogClassResponse;
import com.example.tm.eam.dto.PermissionCatalogObjectResponse;
import com.example.tm.eam.dto.PermissionCatalogResponse;
import com.example.tm.eam.dto.PermissionResponse;
import com.example.tm.eam.dto.RoleCreateRequest;
import com.example.tm.eam.dto.RoleListResponse;
import com.example.tm.eam.dto.RoleResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements service logic for eam role permission service.
 */
@Service
public class EamRolePermissionServiceImpl implements EamRolePermissionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EamRoleRepository eamRoleRepository;
    private final EamAppPermissionRepository eamAppPermissionRepository;
    private final EamCompanyRepository eamCompanyRepository;
    private final JdbcTemplate eamJdbcTemplate;

    /** Creates a new instance of eam role permission service impl. */
    public EamRolePermissionServiceImpl(
            EamRoleRepository eamRoleRepository,
            EamAppPermissionRepository eamAppPermissionRepository,
            EamCompanyRepository eamCompanyRepository,
            @Qualifier("eamJdbcTemplate") JdbcTemplate eamJdbcTemplate) {
        this.eamRoleRepository = eamRoleRepository;
        this.eamAppPermissionRepository = eamAppPermissionRepository;
        this.eamCompanyRepository = eamCompanyRepository;
        this.eamJdbcTemplate = eamJdbcTemplate;
    }

    /** Returns permission catalog. */
    @Override
    @Transactional(readOnly = true, transactionManager = "eamTransactionManager")
    public PermissionCatalogResponse getPermissionCatalog() {
        List<EamAppPermission> permissions = eamAppPermissionRepository.findByActiveTrueOrderByModuleAscSortOrderAsc();
        Map<String, List<EamAppPermission>> byModule = permissions.stream()
                .collect(Collectors.groupingBy(
                        p -> normalizeKey(p.getModule()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<PermissionCatalogClassResponse> classes = loadCatalogFromDatabase(byModule);
        return PermissionCatalogResponse.builder()
                .classes(classes)
                .build();
    }

    /** Returns roles. */
    @Override
    @Transactional(readOnly = true, transactionManager = "eamTransactionManager")
    public RoleListResponse getRoles(Long companyId, int page, int size) {
        Long resolvedCompanyId = requireActiveCompanyId(companyId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        Page<EamRole> rolePage = eamRoleRepository.findByActiveTrueAndCompanyId(
                resolvedCompanyId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name")));

        return RoleListResponse.builder()
                .roles(rolePage.stream().map(this::toRoleResponse).toList())
                .page(rolePage.getNumber())
                .size(rolePage.getSize())
                .totalElements(rolePage.getTotalElements())
                .totalPages(rolePage.getTotalPages())
                .last(rolePage.isLast())
                .build();
    }

    /** Returns role by id. */
    @Override
    @Transactional(readOnly = true, transactionManager = "eamTransactionManager")
    public RoleResponse getRoleById(Long companyId, Long roleId) {
        Long resolvedCompanyId = requireActiveCompanyId(companyId);
        if (roleId == null || roleId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleId must be a positive number");
        }
        EamRole role = eamRoleRepository.findByIdAndActiveTrueAndCompanyId(roleId, resolvedCompanyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        return toRoleResponse(role);
    }

    /** Creates role. */
    @Override
    @Transactional(transactionManager = "eamTransactionManager")
    public RoleResponse createRole(Long companyId, RoleCreateRequest request) {
        Long resolvedCompanyId = requireActiveCompanyId(companyId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role payload is required");
        }
        String name = requireRoleName(request.getName());

        if (eamRoleRepository.existsByNameIgnoreCaseAndCompanyId(name, resolvedCompanyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists: " + name);
        }

        Set<String> normalizedCodes = normalizePermissionCodes(request.getPermissionCodes());
        Set<EamAppPermission> permissions = resolveActivePermissions(normalizedCodes);

        EamRole role = new EamRole();
        role.setCompanyId(resolvedCompanyId);
        role.setName(name);
        role.setDescription(safeTrim(request.getDescription()));
        role.setActive(Boolean.TRUE);
        role.setTechnicianRole(Boolean.TRUE.equals(request.getTechnicianRole()));
        role.setPermissions(permissions);

        EamRole saved = eamRoleRepository.save(role);
        return toRoleResponse(saved);
    }

    /** Loads permission catalog from database taxonomy tables when available. */
    private List<PermissionCatalogClassResponse> loadCatalogFromDatabase(Map<String, List<EamAppPermission>> byModule) {
        try {
            List<ClassRow> classRows = eamJdbcTemplate.query(
                    """
                            SELECT id, name
                            FROM dbo.permission_classes
                            WHERE active = 1
                            ORDER BY sort_order, name
                            """,
                    (rs, rowNum) -> new ClassRow(rs.getLong("id"), rs.getString("name")));

            List<ObjectRow> objectRows = eamJdbcTemplate.query(
                    """
                            SELECT class_id, name, module, view_only
                            FROM dbo.permission_objects
                            WHERE active = 1
                            ORDER BY class_id, sort_order, name
                            """,
                    (rs, rowNum) -> new ObjectRow(
                            rs.getLong("class_id"),
                            rs.getString("name"),
                            normalizeKey(rs.getString("module")),
                            rs.getBoolean("view_only")));

            Map<Long, List<ObjectRow>> byClassId = objectRows.stream()
                    .filter(row -> row.module() != null)
                    .collect(Collectors.groupingBy(ObjectRow::classId, LinkedHashMap::new, Collectors.toList()));

            return classRows.stream()
                    .map(classRow -> PermissionCatalogClassResponse.builder()
                            .name(classRow.name())
                            .objects(byClassId.getOrDefault(classRow.id(), List.of())
                                    .stream()
                                    .map(obj -> buildPermissionObject(obj.name(), obj.module(), obj.viewOnly(), byModule))
                                    .toList())
                            .build())
                    .toList();
        } catch (DataAccessException ignored) {
            return buildFallbackCatalog(byModule);
        }
    }

    /** Creates fallback catalog grouped by permission module. */
    private List<PermissionCatalogClassResponse> buildFallbackCatalog(Map<String, List<EamAppPermission>> byModule) {
        List<PermissionCatalogObjectResponse> objects = byModule.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> buildPermissionObject(toTitle(entry.getKey()), entry.getKey(), false, byModule))
                .toList();

        return List.of(PermissionCatalogClassResponse.builder()
                .name("Permissions")
                .objects(objects)
                .build());
    }

    /** Maps module permissions to a catalog object. */
    private PermissionCatalogObjectResponse buildPermissionObject(
            String objectName,
            String module,
            boolean viewOnly,
            Map<String, List<EamAppPermission>> byModule) {
        List<PermissionResponse> permissions = byModule.getOrDefault(module, List.of()).stream()
                .filter(permission -> !viewOnly || "VIEW".equalsIgnoreCase(permission.getAction()))
                .map(this::toPermissionResponse)
                .sorted(Comparator.comparing(PermissionResponse::getCode, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return PermissionCatalogObjectResponse.builder()
                .name(objectName)
                .permissions(permissions)
                .build();
    }

    /** Converts EAM permission entity to response dto. */
    private PermissionResponse toPermissionResponse(EamAppPermission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .module(permission.getModule())
                .action(permission.getAction())
                .label(permission.getLabel())
                .description(permission.getDescription())
                .build();
    }

    /** Converts EAM role entity to response dto. */
    private RoleResponse toRoleResponse(EamRole role) {
        Set<String> permissionCodes = role.getPermissions() == null
                ? Set.of()
                : role.getPermissions().stream()
                        .map(EamAppPermission::getCode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(Boolean.TRUE.equals(role.getActive()))
                .technicianRole(Boolean.TRUE.equals(role.getTechnicianRole()))
                .permissionCodes(permissionCodes)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    /** Resolves and validates active permissions from permission codes. */
    private Set<EamAppPermission> resolveActivePermissions(Set<String> requestedCodes) {
        List<EamAppPermission> activePermissions = eamAppPermissionRepository.findByActiveTrueOrderByModuleAscSortOrderAsc();
        Map<String, EamAppPermission> byCode = activePermissions.stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getActive()))
                .filter(permission -> permission.getCode() != null)
                .collect(Collectors.toMap(
                        permission -> normalizeKey(permission.getCode()),
                        permission -> permission,
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<String> missingCodes = new ArrayList<>();
        for (String code : requestedCodes) {
            if (!byCode.containsKey(code)) {
                missingCodes.add(code);
            }
        }

        if (!missingCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid permission codes: " + missingCodes);
        }

        return new LinkedHashSet<>(byCode.values());
    }

    /** Validates and normalizes permission code set. */
    private Set<String> normalizePermissionCodes(Set<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permissionCodes cannot be empty");
        }

        Set<String> normalized = permissionCodes.stream()
                .map(this::safeTrim)
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permissionCodes cannot be empty");
        }
        if (normalized.size() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many permission codes provided");
        }
        return normalized;
    }

    /** Validates and trims role name. */
    private String requireRoleName(String roleName) {
        String normalized = safeTrim(roleName);
        if (normalized == null || normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required");
        }
        if (normalized.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name must be at most 80 characters");
        }
        return normalized;
    }

    /** Resolves company id and ensures company is active in EAM. */
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

    /** Safely trims a string value. */
    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    /** Normalizes keys for case-insensitive comparisons. */
    private String normalizeKey(String value) {
        String trimmed = safeTrim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /** Converts constant-like labels to title case for fallback display. */
    private String toTitle(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private record ClassRow(Long id, String name) {
    }

    private record ObjectRow(Long classId, String name, String module, boolean viewOnly) {
    }
}
