package com.example.tm.auth.integration.eam;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EamUserCompanyRepository extends JpaRepository<EamUserCompany, Long> {

    List<EamUserCompany> findByUser_IdAndCompany_ActiveTrue(Long userId);
}
