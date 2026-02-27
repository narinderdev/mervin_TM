package com.example.tm.auth.integration.eam;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EamUserRepository extends JpaRepository<EamUser, Long> {

    @Query("select u from EamUser u left join fetch u.userRoles ur left join fetch ur.role r where lower(u.email) = lower(:email) and (u.deleted = false or u.deleted is null)")
    Optional<EamUser> findByEmailAndDeletedFalse(@Param("email") String email);
}
