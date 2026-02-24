package com.example.tm.eam.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TechnicianDetailsResponse {

    private Long id;
    private String technicianId;
    private String badgeNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String technicianType;
    private String skills;
    private String phoneNumber;
    private String email;
    private String address;
    private String status;
    private String workStatus;
    private LocalDate hireDate;
    private String workShift;
    private String technicianPhotoUrl;
    private String certificateUrl;
    private LocalDate certificateIssueDate;
    private LocalDate certificateExpiryDate;
    private LocalDate terminationDate;
    private String certifications;
    private String notes;
    private boolean teamLeader;
    private List<TechnicianTeamMembershipResponse> teamMemberships;
}
