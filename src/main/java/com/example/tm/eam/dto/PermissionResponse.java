package com.example.tm.eam.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Transfers permission response data between layers.
 */
@Data
@Builder
public class PermissionResponse {

    private Long id;
    private String code;
    private String module;
    private String action;
    private String label;
    private String description;
}
