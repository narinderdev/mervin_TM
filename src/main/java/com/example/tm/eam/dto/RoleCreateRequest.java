package com.example.tm.eam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Data;

/**
 * Transfers role create request data between layers.
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;
    private Boolean technicianRole;

    @NotEmpty(message = "permissionCodes cannot be empty")
    private Set<String> permissionCodes;
}
