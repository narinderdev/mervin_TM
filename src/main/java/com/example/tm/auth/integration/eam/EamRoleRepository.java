package com.example.tm.auth.integration.eam;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Defines operations for eam role repository.
 */
public interface EamRoleRepository extends JpaRepository<EamRole, Long> {

    boolean existsByNameIgnoreCaseAndCompanyId(String name, Long companyId);

    @EntityGraph(attributePaths = "permissions")
    Optional<EamRole> findByIdAndActiveTrueAndCompanyId(Long id, Long companyId);

    @EntityGraph(attributePaths = "permissions")
    Page<EamRole> findByActiveTrueAndCompanyId(Long companyId, Pageable pageable);

    @EntityGraph(attributePaths = "permissions")
    List<EamRole> findByActiveTrueAndCompanyIdOrderByNameAsc(Long companyId);
}
