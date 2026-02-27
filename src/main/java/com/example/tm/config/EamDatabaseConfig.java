package com.example.tm.config;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.tm.auth.integration.eam",
        entityManagerFactoryRef = "eamEntityManagerFactory",
        transactionManagerRef = "eamTransactionManager")
public class EamDatabaseConfig {

    @Bean(name = "eamEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean eamEntityManagerFactory(
            @Qualifier("eamDataSource") DataSource eamDataSource,
            Environment environment) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(eamDataSource);
        factory.setPackagesToScan("com.example.tm.auth.integration.eam");
        factory.setPersistenceUnitName("eam");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(Boolean.parseBoolean(environment.getProperty("spring.jpa.show-sql", "false")));
        String databasePlatform = environment.getProperty("spring.jpa.properties.hibernate.dialect");
        if (databasePlatform != null && !databasePlatform.isBlank()) {
            vendorAdapter.setDatabasePlatform(databasePlatform);
        }
        factory.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        // Avoid accidental schema modification on EAM DB
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.format_sql", environment.getProperty("spring.jpa.properties.hibernate.format_sql", "false"));
        String defaultSchema = environment.getProperty("spring.jpa.properties.hibernate.default_schema");
        if (defaultSchema != null && !defaultSchema.isBlank()) {
            properties.put("hibernate.default_schema", defaultSchema);
        }
        factory.setJpaPropertyMap(properties);
        return factory;
    }

    @Bean(name = "eamTransactionManager")
    public PlatformTransactionManager eamTransactionManager(
            @Qualifier("eamEntityManagerFactory") EntityManagerFactory eamEntityManagerFactory) {
        return new JpaTransactionManager(eamEntityManagerFactory);
    }
}
