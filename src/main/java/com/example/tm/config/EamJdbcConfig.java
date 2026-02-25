package com.example.tm.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EamJdbcConfig {

    @Bean(name = "eamDataSource")
    public DataSource eamDataSource(Environment env) {
        String rawUrl = env.getProperty("spring.datasource.eam.jdbc-url");
        String url = enforceTrust(rawUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(env.getProperty("spring.datasource.eam.username"));
        config.setPassword(env.getProperty("spring.datasource.eam.password"));
        config.setDriverClassName(env.getProperty("spring.datasource.eam.driver-class-name"));
        config.setPoolName(env.getProperty("spring.datasource.eam.pool-name", "eam-hikari-pool"));
        config.setMaximumPoolSize(Integer.parseInt(env.getProperty("spring.datasource.eam.maximum-pool-size", "20")));
        config.setMinimumIdle(Integer.parseInt(env.getProperty("spring.datasource.eam.minimum-idle", "6")));
        config.setConnectionTimeout(Long.parseLong(env.getProperty("spring.datasource.eam.connection-timeout", "20000")));
        config.setIdleTimeout(Long.parseLong(env.getProperty("spring.datasource.eam.idle-timeout", "300000")));
        config.setMaxLifetime(Long.parseLong(env.getProperty("spring.datasource.eam.max-lifetime", "1800000")));
        config.setValidationTimeout(Long.parseLong(env.getProperty("spring.datasource.eam.validation-timeout", "5000")));
        config.setAutoCommit(Boolean.parseBoolean(env.getProperty("spring.datasource.eam.auto-commit", "false")));
        // Ensure we never fail on cert issues in lower environments; prod should use a trusted cert.
        config.addDataSourceProperty("trustServerCertificate", "true");
        config.addDataSourceProperty("encrypt", "false");
        return new HikariDataSource(config);
    }

    @Bean(name = "eamJdbcTemplate")
    public JdbcTemplate eamJdbcTemplate(@Qualifier("eamDataSource") DataSource eamDataSource) {
        return new JdbcTemplate(eamDataSource);
    }

    private String enforceTrust(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String lower = url.toLowerCase();
        boolean hasTrust = lower.contains("trustservercertificate=");
        boolean hasEncrypt = lower.contains("encrypt=");
        StringBuilder sb = new StringBuilder(url);
        if (!hasTrust) {
            sb.append(url.contains(";") ? ";" : ";").append("trustServerCertificate=true");
        }
        if (!hasEncrypt) {
            sb.append(";encrypt=false");
        }
        return sb.toString();
    }
}
