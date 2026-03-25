package com.example.tm.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponseDto {

    private String token;
    private UserSummaryDto user;
    private List<CompanyDto> companies;

    @JsonProperty("is_company_setup")
    private Boolean isCompanySetup;

    @JsonProperty("mfa_required")
    private Boolean mfaRequired;

    @JsonProperty("mfa_token")
    private String mfaToken;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompanyDto {
        private Long id;

        @JsonProperty("company_legal_name")
        private String companyLegalName;

        @JsonProperty("company_trade_name")
        private String companyTradeName;

        @JsonProperty("company_number")
        private String companyNumber;

        private String address;
        private String city;
        private String country;

        @JsonProperty("postal_code")
        private String postalCode;

        private Boolean active;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
    }
}
