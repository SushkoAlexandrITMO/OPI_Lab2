package ru.itmo.lab1.config;

import jakarta.persistence.EntityManagerFactory;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 Уровень хранения на JPA + EclipseLink.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "ru.itmo.lab1.repository")
public class JpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            @Value("${app.jpa.show-sql:false}") boolean showSql) {

        EclipseLinkJpaVendorAdapter vendorAdapter = new EclipseLinkJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.POSTGRESQL);
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(showSql);

        Map<String, Object> properties = new HashMap<>();
        // Без load-time weaving: связи грузятся жадно, для небольших объектов это приемлемо
        properties.put(PersistenceUnitProperties.WEAVING, "false");
        // Схему создаёт Flyway, EclipseLink её не трогает
        properties.put(PersistenceUnitProperties.DDL_GENERATION, PersistenceUnitProperties.NONE);
        // Общий кэш выключен: иначе изменения, сделанные функциями БД или другим узлом, не будут видны
        properties.put(PersistenceUnitProperties.CACHE_SHARED_DEFAULT, "false");
        // Bean Validation перед INSERT/UPDATE (ограничения на уровне ORM)
        properties.put(PersistenceUnitProperties.VALIDATION_MODE, "CALLBACK");

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("ru.itmo.lab1.domain");
        emf.setPersistenceUnitName("study-groups");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(properties);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
