package com.group2.rms.core.config;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Preserve the PascalCase SQL identifiers declared by database/schema/db.sql. */
@Configuration
public class SchemaNamingConfig {

    @Bean
    public HibernatePropertiesCustomizer schemaPhysicalNamingStrategy() {
        return properties -> properties.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                PhysicalNamingStrategyStandardImpl.class.getName());
    }
}
