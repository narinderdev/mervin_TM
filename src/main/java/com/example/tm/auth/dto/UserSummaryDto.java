package com.example.tm.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Transfers user summary dto data between layers.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Boolean active;
}
