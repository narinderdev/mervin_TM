package com.example.tm.auth.integration.eam;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Defines operations for eam user repository.
 */
public interface EamUserRepository extends JpaRepository<EamUser, Long> {

    @Query("select distinct u from EamUser u left join fetch u.userRoles ur left join fetch ur.role r where lower(u.email) = lower(:email) and (u.deleted = false or u.deleted is null)")
    Optional<EamUser> findByEmailAndDeletedFalse(@Param("email") String email);

    @Query("""
            select distinct lower(u.email)
            from EamUserCompany uc
            join uc.user u
            join uc.company c
            where c.id = :companyId
              and c.active = true
              and (u.deleted = false or u.deleted is null)
              and u.status = :status
              and u.email is not null
            """)
    List<String> findActiveEmailsByCompanyId(
            @Param("companyId") Long companyId,
            @Param("status") EamUserStatus status);
}
