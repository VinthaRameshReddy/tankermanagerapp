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
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Builds the prod DataSource from Render env vars.
 * Parses postgres://user:pass@host/db into jdbc URL + separate user/password
 * (PG JDBC does not accept credentials embedded in the jdbc URL).
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        DbParts parts = resolve(env);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(parts.jdbcUrl);
        ds.setUsername(parts.username);
        ds.setPassword(parts.password);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        ds.setConnectionTimeout(30000);

        log.info("Prod datasource jdbcUrl={} user={}", parts.jdbcUrl, parts.username);
        return ds;
    }

    private static DbParts resolve(Environment env) {
        String rawUrl = firstNonBlank(env.getProperty("DATABASE_URL"), env.getProperty("spring.datasource.url"));
        String user = firstNonBlank(env.getProperty("DATABASE_USER"), env.getProperty("DB_USERNAME"));
        String pass = firstNonBlank(env.getProperty("DATABASE_PASSWORD"), env.getProperty("DB_PASSWORD"));

        if (!isBlank(rawUrl)) {
            DbParts fromUrl = parseDatabaseUrl(rawUrl);
            // Env USER/PASSWORD override URL if both set
            if (!isBlank(user)) {
                fromUrl.username = user;
            }
            if (!isBlank(pass)) {
                fromUrl.password = pass;
            }
            return fromUrl;
        }

        String host = firstNonBlank(env.getProperty("DATABASE_HOST"));
        String port = firstNonBlank(env.getProperty("DATABASE_PORT"), "5432");
        String name = firstNonBlank(env.getProperty("DATABASE_NAME"));
        if (isBlank(host) || isBlank(name) || isBlank(user) || isBlank(pass)) {
            throw new IllegalStateException(
                    "Set DATABASE_URL (Internal URL from Render) OR "
                            + "DATABASE_HOST + DATABASE_NAME + DATABASE_USER + DATABASE_PASSWORD");
        }
        host = toRenderInternalHost(host);
        String jdbc = withSsl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        return new DbParts(jdbc, user, pass);
    }

    /**
     * Accepts:
     * - postgresql://user:pass@host/db
     * - postgres://user:pass@host:5432/db
     * - jdbc:postgresql://host:5432/db
     */
    static DbParts parseDatabaseUrl(String raw) {
        String value = raw.trim();
        if (value.startsWith("jdbc:postgresql://") && !value.contains("@")) {
            // Already a clean JDBC URL without credentials
            return new DbParts(withSsl(value), null, null);
        }

        // Normalize to URI-parseable form
        String normalized = value;
        if (normalized.startsWith("jdbc:postgresql://")) {
            normalized = "postgresql://" + normalized.substring("jdbc:postgresql://".length());
        } else if (normalized.startsWith("jdbc:postgres://")) {
            normalized = "postgres://" + normalized.substring("jdbc:postgres://".length());
        }

        try {
            URI uri = new URI(normalized);
            String host = toRenderInternalHost(uri.getHost());
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String db = (path != null && path.startsWith("/")) ? path.substring(1) : path;
            // strip query from db name if present
            if (db != null && db.contains("?")) {
                db = db.substring(0, db.indexOf('?'));
            }

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    username = userInfo.substring(0, colon);
                    password = userInfo.substring(colon + 1);
                } else {
                    username = userInfo;
                }
            }

            String jdbc = withSsl("jdbc:postgresql://" + host + ":" + port + "/" + db);
            return new DbParts(jdbc, username, password);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid DATABASE_URL: " + e.getMessage(), e);
        }
    }

    static String toRenderInternalHost(String host) {
        if (host == null) {
            return null;
        }
        // dpg-xxx-a.singapore-postgres.render.com → dpg-xxx-a
        if (host.contains("-postgres.render.com")) {
            int dot = host.indexOf('.');
            if (dot > 0) {
                return host.substring(0, dot);
            }
        }
        return host;
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

    static final class DbParts {
        final String jdbcUrl;
        String username;
        String password;

        DbParts(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}
