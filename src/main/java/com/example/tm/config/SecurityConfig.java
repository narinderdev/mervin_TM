package com.example.tm.config;

import com.example.tm.auth.security.TmActiveCompanyFilter;
import com.example.tm.auth.security.TmJwtAuthenticationFilter;
import com.example.tm.auth.security.TmSecurityErrorWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures application components for security config.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TmJwtAuthenticationFilter jwtAuthenticationFilter;
    private final TmActiveCompanyFilter activeCompanyFilter;
    private final TmSecurityErrorWriter errorWriter;

    /** Handles security filter chain. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/auth",
                                "/auth/signup",
                                "/auth/login/mfa",
                                "/auth/mfa/email/send",
                                "/auth/mfa/email/verify",
                                "/users/forgot-password",
                                "/api/invitations/set-password").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/invitations/accept",
                                "/actuator/health",
                                "/actuator/health/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(request, response, org.springframework.http.HttpStatus.UNAUTHORIZED,
                                        "Authentication required"))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(request, response, org.springframework.http.HttpStatus.FORBIDDEN,
                                        "Access denied")))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(activeCompanyFilter, TmJwtAuthenticationFilter.class);

        return http.build();
    }
}
