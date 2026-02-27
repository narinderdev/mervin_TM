package com.example.tm.auth.integration.eam;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EamUserRepository extends JpaRepository<EamUser, Long> {

    Optional<EamUser> findByEmailAndDeletedFalse(String email);
}
