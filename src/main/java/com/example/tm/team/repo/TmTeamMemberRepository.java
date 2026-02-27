package com.example.tm.team.repo;

import com.example.tm.team.entity.TmTeamMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TmTeamMemberRepository extends JpaRepository<TmTeamMember, Long> {

    List<TmTeamMember> findByTeam_Id(Long teamId);
}
