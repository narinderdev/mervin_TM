package com.example.tm.eam.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Transfers security role report data between layers.
 */
@Data
@Builder
public class SecurityRoleReportItem {

    private String role;
    private Map<String, List<String>> objects;
}
