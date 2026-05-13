package com.example.tm.eam.dto;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers role response data between layers.
 */
@Data
@Builder
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private boolean active;
    private boolean technicianRole;
    private Set<String> permissionCodes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
