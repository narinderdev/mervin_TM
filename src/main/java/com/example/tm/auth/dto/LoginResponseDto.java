package com.example.tm.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {

    private String token;
    private UserSummaryDto user;

    @JsonProperty("mfa_required")
    private Boolean mfaRequired;

    @JsonProperty("mfa_token")
    private String mfaToken;
}
