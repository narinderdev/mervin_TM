package com.example.tm.eam.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers permission catalog response data between layers.
 */
@Data
@Builder
public class PermissionCatalogResponse {

    private List<PermissionCatalogClassResponse> classes;
}
