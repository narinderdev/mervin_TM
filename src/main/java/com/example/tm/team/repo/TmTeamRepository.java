package com.example.tm.team.repo;

import com.example.tm.team.entity.TmTeam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmTeamRepository extends JpaRepository<TmTeam, Long> {

    boolean existsByTeamNameIgnoreCase(String teamName);
}
