package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers permission catalog class response data between layers.
 */
@Data
@Builder
public class PermissionCatalogClassResponse {

    private String name;
    private List<PermissionCatalogObjectResponse> objects;
}
