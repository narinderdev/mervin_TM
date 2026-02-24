package com.example.tm.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Boolean active;
}
