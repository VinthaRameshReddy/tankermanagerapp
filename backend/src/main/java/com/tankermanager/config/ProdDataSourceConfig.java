package com.tankermanager.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Production datasource for Render:
 * - Prefer DATABASE_URL (Blueprint postgres connection string)
 * - Or DATABASE_HOST / DATABASE_NAME / DATABASE_USER / DATABASE_PASSWORD (openplot-style)
 * Converts postgres://… → jdbc:postgresql://…
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties(Environment env) {
        DataSourceProperties props = new DataSourceProperties() {
            @Override
            public void setUrl(String url) {
                super.setUrl(toJdbc(url));
            }
        };

        String url = env.getProperty("DATABASE_URL");
        if (url == null || url.isBlank()) {
            String host = env.getProperty("DATABASE_HOST");
            String port = env.getProperty("DATABASE_PORT", "5432");
            String name = env.getProperty("DATABASE_NAME");
            if (host != null && name != null) {
                url = "jdbc:postgresql://" + host + ":" + port + "/" + name;
            }
        }
        if (url != null && !url.isBlank()) {
            props.setUrl(toJdbc(url));
        }

        String user = env.getProperty("DB_USERNAME", env.getProperty("DATABASE_USER"));
        String pass = env.getProperty("DB_PASSWORD", env.getProperty("DATABASE_PASSWORD"));
        if (user != null) {
            props.setUsername(user);
        }
        if (pass != null) {
            props.setPassword(pass);
        }
        props.setDriverClassName("org.postgresql.Driver");
        return props;
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
