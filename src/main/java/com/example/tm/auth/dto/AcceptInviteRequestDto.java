package com.example.tm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcceptInviteRequestDto {

    @NotBlank
    private String token;

    @NotBlank
    private String password;
}
