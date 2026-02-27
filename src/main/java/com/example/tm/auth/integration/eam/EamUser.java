package com.example.tm.auth.integration.eam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class EamUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "password", length = 150)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EamUserStatus status;

    @Column(nullable = true)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<EamUserRole> userRoles = new ArrayList<>();
}
