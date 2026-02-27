package com.example.tm.auth.repository;

import com.example.tm.auth.entity.TmUser;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmUserRepository extends JpaRepository<TmUser, Long> {

    Optional<TmUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<TmUser> findByActiveTrue();
}
