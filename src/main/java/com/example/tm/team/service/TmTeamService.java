package com.example.tm.team.service;

import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.team.dto.TmTeamCreateRequest;
import com.example.tm.team.dto.TmTeamDetailsResponse;
import com.example.tm.team.entity.TmTeam;
import com.example.tm.team.entity.TmTeamMember;
import com.example.tm.team.repo.TmTeamMemberRepository;
import com.example.tm.team.repo.TmTeamRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tmTransactionManager")
public class TmTeamService {

    private final TmTeamRepository teamRepository;
    private final TmTeamMemberRepository memberRepository;
    private final TmUserRepository tmUserRepository;

    public TmTeamDetailsResponse createTeam(TmTeamCreateRequest request) {
        String name = request.getTeamName().trim();
        if (teamRepository.existsByTeamNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Team name already exists");
        }

        List<Long> technicians = request.getTechnicianIds();
        if (technicians == null || technicians.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one technician is required");
        }

        Set<Long> uniqueTechnicians = new HashSet<>();
        for (Long id : technicians) {
            if (id == null || !tmUserRepository.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Technician not found: " + id);
            }
            uniqueTechnicians.add(id);
        }

        Long leaderId = request.getTeamLeaderId();
        if (leaderId != null && !uniqueTechnicians.contains(leaderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team leader must be in technician list");
        }

        String status = (request.getStatus() == null || request.getStatus().isBlank())
                ? "ACTIVE"
                : request.getStatus().trim();

        TmTeam team = new TmTeam();
        team.setTeamName(name);
        team.setTeamDescription(request.getTeamDescription());
        team.setStatus(status);
        team.setCreatedAt(Instant.now());
        team.setUpdatedAt(Instant.now());

        // Persist team first to get ID
        TmTeam saved = teamRepository.save(team);

        for (Long techId : uniqueTechnicians) {
            TmTeamMember member = new TmTeamMember();
            member.setTeam(saved);
            member.setTechnicianId(techId);
            member.setTeamLeader(leaderId != null && leaderId.equals(techId));
            memberRepository.save(member);
        }

        return TmTeamDetailsResponse.builder()
                .id(saved.getId())
                .teamName(saved.getTeamName())
                .teamDescription(saved.getTeamDescription())
                .status(saved.getStatus())
                .teamLeaderId(leaderId)
                .technicianIds(uniqueTechnicians.stream().toList())
                .build();
    }
}
