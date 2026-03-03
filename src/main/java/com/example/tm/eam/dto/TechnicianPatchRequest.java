package com.example.tm.eam.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TechnicianPatchRequest {

    private String firstName;

    private String lastName;

    private String badgeNumber;

    private String technicianId;

    private String technicianType;

    private String skills;

    private String phoneNumber;

    @Email
    private String email;

    private String address;

    private String status;

    private LocalDate hireDate;

    private String workShift;

    private String technicianPhotoUrl;
    private String certificateUrl;
    private LocalDate certificateIssueDate;
    private LocalDate certificateExpiryDate;

    private LocalDate terminationDate;

    private String certifications;

    private String notes;
}
