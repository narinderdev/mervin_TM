package com.example.tm.auth.integration.eam;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates eam company functionality.
 */
@Getter
@Setter
@Entity
@Table(name = "companies")
public class EamCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_legal_name", nullable = false, length = 255)
    private String companyLegalName;

    @Column(name = "company_trade_name", nullable = false, length = 255)
    private String companyTradeName;

    @Column(name = "company_number", nullable = false, length = 100)
    private String companyNumber;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city", nullable = false, length = 150)
    private String city;

    @Column(name = "country", nullable = false, length = 150)
    private String country;

    @Column(name = "postal_code", nullable = false, length = 40)
    private String postalCode;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
