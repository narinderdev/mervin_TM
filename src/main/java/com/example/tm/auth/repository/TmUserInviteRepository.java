package com.example.tm.auth.repository;

import com.example.tm.auth.entity.TmUserInvite;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Defines operations for tm user invite repository.
 */
public interface TmUserInviteRepository extends JpaRepository<TmUserInvite, Long> {

    Optional<TmUserInvite> findByTokenAndAcceptedFalseAndExpiresAtAfter(String token, Instant now);

    Optional<TmUserInvite> findByEmailAndAcceptedFalseAndExpiresAtAfter(String email, Instant now);

    Optional<TmUserInvite> findByEmail(String email);

    boolean existsByEmailAndAcceptedFalse(String email);

    boolean existsByEmailAndAcceptedFalseAndExpiresAtAfter(String email, Instant now);
}
