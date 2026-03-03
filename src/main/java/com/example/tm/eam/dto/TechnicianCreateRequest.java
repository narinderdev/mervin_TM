package com.example.tm.eam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TechnicianCreateRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String badgeNumber;

    private String technicianId;

    private String technicianType = "FULL_TIME";

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
