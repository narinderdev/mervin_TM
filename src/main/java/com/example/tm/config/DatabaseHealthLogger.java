package com.example.tm.config;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthLogger {

    @Qualifier("tmDataSource")
    private final DataSource tmDataSource;

    @Qualifier("eamDataSource")
    private final DataSource eamDataSource;

    @Value("${app.startup.db-check.enabled:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!enabled) {
            log.info("DB startup check skipped (app.startup.db-check.enabled=false)");
            return;
        }
        check("TM", tmDataSource);
        check("EAM", eamDataSource);
    }

    private void check(String name, DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                log.info("DB check [{}]: OK (SELECT 1 -> {})", name, rs.getInt(1));
            } else {
                log.warn("DB check [{}]: SELECT 1 returned no rows", name);
            }
        } catch (Exception ex) {
            log.error("DB check [{}]: FAILED - {}", name, ex.getMessage(), ex);
        }
    }
}
