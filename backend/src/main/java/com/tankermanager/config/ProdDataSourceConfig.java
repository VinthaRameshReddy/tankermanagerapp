package com.tankermanager.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Render free Postgres often provides DATABASE_URL as postgres://user:pass@host/db.
 * Spring expects jdbc:postgresql://... — convert when needed.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties() {
            @Override
            public void setUrl(String url) {
                super.setUrl(toJdbc(url));
            }
        };
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    static String toJdbc(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("jdbc:")) {
            return url;
        }
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:postgresql://" + url.substring("postgresql://".length());
        }
        return url;
    }
}
