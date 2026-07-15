package com.tankermanager.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Builds the prod DataSource from Render env vars.
 * Supports:
 * - DATABASE_URL (postgres:// or jdbc:postgresql://)
 * - DATABASE_HOST + DATABASE_NAME + DATABASE_USER + DATABASE_PASSWORD (buffalo / openplot style)
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String jdbcUrl = resolveJdbcUrl(env);
        String user = firstNonBlank(
                env.getProperty("DATABASE_USER"),
                env.getProperty("DB_USERNAME"),
                env.getProperty("spring.datasource.username"));
        String pass = firstNonBlank(
                env.getProperty("DATABASE_PASSWORD"),
                env.getProperty("DB_PASSWORD"),
                env.getProperty("spring.datasource.password"));

        // If credentials are embedded in DATABASE_URL, user/pass may be null — that's OK
        if (isBlank(jdbcUrl)) {
            throw new IllegalStateException(
                    "Database URL missing. On Render set either DATABASE_URL, or "
                            + "DATABASE_HOST + DATABASE_NAME + DATABASE_USER + DATABASE_PASSWORD "
                            + "(same values as openplot-api / buffalo_db).");
        }

        log.info("Prod datasource jdbcUrl={} user={}", maskUrl(jdbcUrl), user);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        if (!isBlank(user)) {
            ds.setUsername(user);
        }
        if (!isBlank(pass)) {
            ds.setPassword(pass);
        }
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        ds.setConnectionTimeout(30000);
        return ds;
    }

    private static String resolveJdbcUrl(Environment env) {
        String url = firstNonBlank(
                env.getProperty("DATABASE_URL"),
                env.getProperty("spring.datasource.url"));

        if (isBlank(url)) {
            String host = firstNonBlank(env.getProperty("DATABASE_HOST"));
            String port = firstNonBlank(env.getProperty("DATABASE_PORT"), "5432");
            String name = firstNonBlank(env.getProperty("DATABASE_NAME"));
            if (!isBlank(host) && !isBlank(name)) {
                host = toRenderInternalHost(host);
                // Internal Render Postgres uses host only (default 5432)
                url = "jdbc:postgresql://" + host + ":" + port + "/" + name;
            }
        }

        if (isBlank(url)) {
            return null;
        }
        return withSsl(toJdbc(toRenderInternalJdbc(url.trim())));
    }

    /**
     * Render free Postgres: from another Render service, prefer the internal hostname
     * dpg-xxxxx-a  instead of  dpg-xxxxx-a.singapore-postgres.render.com
     * External host often fails auth with EOFException between Render services.
     */
    static String toRenderInternalHost(String host) {
        if (host == null) {
            return null;
        }
        // dpg-xxx-a.oregon-postgres.render.com → dpg-xxx-a
        int idx = host.indexOf(".postgres.render.com");
        if (idx > 0) {
            // also matches region-postgres.render.com after stripping "-region"
            String before = host.substring(0, idx);
            int lastDot = before.lastIndexOf('.');
            if (lastDot > 0) {
                return before.substring(0, lastDot); // shouldn't happen
            }
        }
        // Pattern: dpg-....-a.singapore-postgres.render.com
        if (host.contains("-postgres.render.com")) {
            int dot = host.indexOf('.');
            if (dot > 0) {
                return host.substring(0, dot);
            }
        }
        return host;
    }

    static String toRenderInternalJdbc(String jdbcUrl) {
        // jdbc:postgresql://dpg-xxx-a.singapore-postgres.render.com:5432/db
        // → jdbc:postgresql://dpg-xxx-a:5432/db
        try {
            String withoutScheme = jdbcUrl;
            String prefix = "";
            if (jdbcUrl.startsWith("jdbc:postgresql://")) {
                prefix = "jdbc:postgresql://";
                withoutScheme = jdbcUrl.substring(prefix.length());
            } else if (jdbcUrl.startsWith("jdbc:postgres://")) {
                prefix = "jdbc:postgresql://";
                withoutScheme = jdbcUrl.substring("jdbc:postgres://".length());
            } else {
                return jdbcUrl;
            }
            // strip credentials if present user:pass@host
            String creds = "";
            int at = withoutScheme.lastIndexOf('@');
            String hostPart = withoutScheme;
            if (at >= 0) {
                creds = withoutScheme.substring(0, at + 1);
                hostPart = withoutScheme.substring(at + 1);
            }
            int slash = hostPart.indexOf('/');
            String path = slash >= 0 ? hostPart.substring(slash) : "";
            String hostPort = slash >= 0 ? hostPart.substring(0, slash) : hostPart;
            int colon = hostPort.indexOf(':');
            String host = colon >= 0 ? hostPort.substring(0, colon) : hostPort;
            String port = colon >= 0 ? hostPort.substring(colon) : ":5432";
            host = toRenderInternalHost(host);
            return prefix + creds + host + port + path;
        } catch (Exception e) {
            return jdbcUrl;
        }
    }

    static String toJdbc(String url) {
        if (url.startsWith("jdbc:")) {
            return url;
        }
        // postgres://user:pass@host:port/db → jdbc:postgresql://user:pass@host:port/db
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

    private static String maskUrl(String url) {
        return url.replaceAll("://([^:/@]+):([^@/]+)@", "://$1:***@");
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
