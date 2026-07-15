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
 * - Prefer DATABASE_URL
 * - Or DATABASE_HOST / DATABASE_NAME / DATABASE_USER / DATABASE_PASSWORD (buffalo / openplot style)
 * Converts postgres://… → jdbc:postgresql://… and appends sslmode=require.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties(Environment env) {
        DataSourceProperties props = new DataSourceProperties();

        String url = firstNonBlank(env.getProperty("DATABASE_URL"), env.getProperty("spring.datasource.url"));
        if (isBlank(url)) {
            String host = env.getProperty("DATABASE_HOST");
            String port = env.getProperty("DATABASE_PORT", "5432");
            String name = env.getProperty("DATABASE_NAME");
            if (!isBlank(host) && !isBlank(name)) {
                url = "jdbc:postgresql://" + host + ":" + port + "/" + name;
            }
        }

        if (!isBlank(url)) {
            props.setUrl(withSsl(toJdbc(url)));
        }

        String user = firstNonBlank(
                env.getProperty("DB_USERNAME"),
                env.getProperty("DATABASE_USER"),
                env.getProperty("spring.datasource.username"));
        String pass = firstNonBlank(
                env.getProperty("DB_PASSWORD"),
                env.getProperty("DATABASE_PASSWORD"),
                env.getProperty("spring.datasource.password"));
        if (!isBlank(user)) {
            props.setUsername(user);
        }
        if (!isBlank(pass)) {
            props.setPassword(pass);
        }
        props.setDriverClassName("org.postgresql.Driver");
        return props;
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        if (isBlank(properties.getUrl())) {
            throw new IllegalStateException(
                    "Database URL missing. Set DATABASE_URL or DATABASE_HOST + DATABASE_NAME + DATABASE_USER + DATABASE_PASSWORD");
        }
        return properties.initializeDataSourceBuilder().build();
    }

    static String toJdbc(String url) {
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

    static String withSsl(String jdbcUrl) {
        if (jdbcUrl.contains("sslmode=")) {
            return jdbcUrl;
        }
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (!isBlank(v)) {
                return v;
            }
        }
        return null;
    }
}
