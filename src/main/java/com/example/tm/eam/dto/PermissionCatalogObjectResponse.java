package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers permission catalog object response data between layers.
 */
@Data
@Builder
public class PermissionCatalogObjectResponse {

    private String name;
    private List<PermissionResponse> permissions;
}
