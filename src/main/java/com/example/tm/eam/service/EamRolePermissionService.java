package com.example.tm.eam.service;

import com.example.tm.eam.dto.PermissionCatalogResponse;
import com.example.tm.eam.dto.RoleCreateRequest;
import com.example.tm.eam.dto.RoleListResponse;
import com.example.tm.eam.dto.RoleResponse;

/**
 * Defines operations for eam role permission service.
 */
public interface EamRolePermissionService {

    PermissionCatalogResponse getPermissionCatalog();

    RoleListResponse getRoles(Long companyId, int page, int size);

    RoleResponse getRoleById(Long companyId, Long roleId);

    RoleResponse createRole(Long companyId, RoleCreateRequest request);
}
