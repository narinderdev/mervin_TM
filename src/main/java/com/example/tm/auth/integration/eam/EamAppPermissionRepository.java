package com.example.tm.auth.integration.eam;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Defines operations for eam app permission repository.
 */
public interface EamAppPermissionRepository extends JpaRepository<EamAppPermission, Long> {

    List<EamAppPermission> findByActiveTrueOrderByModuleAscSortOrderAsc();

    List<EamAppPermission> findByCodeIn(Collection<String> codes);
}
