package com.example.tm.auth.integration.eam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates eam app permission functionality.
 */
@Getter
@Setter
@Entity
@Table(name = "app_permissions")
public class EamAppPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "module", nullable = false, length = 64)
    private String module;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
