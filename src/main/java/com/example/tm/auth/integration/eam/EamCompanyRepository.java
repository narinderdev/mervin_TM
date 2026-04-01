package com.example.tm.auth.integration.eam;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Defines operations for eam company repository.
 */
public interface EamCompanyRepository extends JpaRepository<EamCompany, Long> {

    List<EamCompany> findByIdInAndActiveTrue(Collection<Long> ids);
}
