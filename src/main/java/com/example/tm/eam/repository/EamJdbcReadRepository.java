package com.example.tm.eam.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EamJdbcReadRepository implements EamReadRepository {

    private static final String TECHNICIAN_EXISTS_SQL = """
            SELECT COUNT(1)
            FROM technicians
            WHERE id = ?
            """;

    @Qualifier("eamJdbcTemplate")
    private final JdbcTemplate eamJdbcTemplate;

    @Override
    public boolean technicianExists(Long technicianId) {
        Integer count = eamJdbcTemplate.queryForObject(TECHNICIAN_EXISTS_SQL, Integer.class, technicianId);
        return count != null && count > 0;
    }
}
