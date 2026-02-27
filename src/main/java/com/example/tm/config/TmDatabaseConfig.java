package com.example.tm.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.example.tm.timesheet.repo",
                "com.example.tm.auth.repository",
                "com.example.tm.team.repo"
        },
        entityManagerFactoryRef = "tmEntityManagerFactory",
        transactionManagerRef = "tmTransactionManager")
public class TmDatabaseConfig {

    @Bean(name = "tmDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.tm")
    public DataSource tmDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "tmEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean tmEntityManagerFactory(
            @Qualifier("tmDataSource") DataSource tmDataSource,
            Environment environment) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(tmDataSource);
        factory.setPackagesToScan(
                "com.example.tm.timesheet.entity",
                "com.example.tm.auth.entity",
                "com.example.tm.team.entity");
        factory.setPersistenceUnitName("tm");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(Boolean.parseBoolean(environment.getProperty("spring.jpa.show-sql", "false")));
        String databasePlatform = environment.getProperty("spring.jpa.properties.hibernate.dialect");
        if (databasePlatform != null && !databasePlatform.isBlank()) {
            vendorAdapter.setDatabasePlatform(databasePlatform);
        }
        factory.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        properties.put("hibernate.format_sql", environment.getProperty("spring.jpa.properties.hibernate.format_sql", "false"));
        String defaultSchema = environment.getProperty("spring.jpa.properties.hibernate.default_schema");
        if (defaultSchema != null && !defaultSchema.isBlank()) {
            properties.put("hibernate.default_schema", defaultSchema);
        }
        factory.setJpaPropertyMap(properties);
        return factory;
    }

    @Bean(name = "tmTransactionManager")
    @Primary
    public PlatformTransactionManager tmTransactionManager(
            @Qualifier("tmEntityManagerFactory") EntityManagerFactory tmEntityManagerFactory) {
        return new JpaTransactionManager(tmEntityManagerFactory);
    }
}
