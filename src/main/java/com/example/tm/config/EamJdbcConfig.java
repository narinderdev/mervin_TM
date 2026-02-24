package com.example.tm.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EamJdbcConfig {

    @Bean(name = "eamDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.eam")
    public DataSource eamDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "eamJdbcTemplate")
    public JdbcTemplate eamJdbcTemplate(@Qualifier("eamDataSource") DataSource eamDataSource) {
        return new JdbcTemplate(eamDataSource);
    }
}
