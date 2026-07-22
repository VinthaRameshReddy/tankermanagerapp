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
 *
 * Prefer Render External Database URL when the web service and Postgres are not
 * in the same region (short internal hosts like dpg-xxx-a will not resolve).
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
        ds.setConnectionTimeout(60000);

        log.info("Prod datasource jdbcUrl={} user={}", parts.jdbcUrl, parts.username);
        return ds;
    }

    private static DbParts resolve(Environment env) {
        String rawUrl = firstNonBlank(env.getProperty("DATABASE_URL"), env.getProperty("spring.datasource.url"));
        String user = firstNonBlank(env.getProperty("DATABASE_USER"), env.getProperty("DB_USERNAME"));
        String pass = firstNonBlank(env.getProperty("DATABASE_PASSWORD"), env.getProperty("DB_PASSWORD"));

        if (!isBlank(rawUrl)) {
            DbParts fromUrl = parseDatabaseUrl(rawUrl, env);
            // Prefer credentials embedded in DATABASE_URL. Only fill gaps from
            // DATABASE_USER / DATABASE_PASSWORD so stale overrides cannot win.
            if (isBlank(fromUrl.username) && !isBlank(user)) {
                fromUrl.username = user;
            }
            if (isBlank(fromUrl.password) && !isBlank(pass)) {
                fromUrl.password = pass;
            }
            if (isBlank(fromUrl.username) || isBlank(fromUrl.password)) {
                throw new IllegalStateException(
                        "DATABASE_URL is set but username/password are missing. "
                                + "Paste the full External Database URL from Render, or set "
                                + "DATABASE_USER and DATABASE_PASSWORD.");
            }
            return fromUrl;
        }

        String host = firstNonBlank(env.getProperty("DATABASE_HOST"));
        String port = firstNonBlank(env.getProperty("DATABASE_PORT"), "5432");
        String name = firstNonBlank(env.getProperty("DATABASE_NAME"));
        if (isBlank(host) || isBlank(name) || isBlank(user) || isBlank(pass)) {
            throw new IllegalStateException(
                    "Set DATABASE_URL (Render External Database URL) OR "
                            + "DATABASE_HOST + DATABASE_NAME + DATABASE_USER + DATABASE_PASSWORD");
        }
        host = expandShortRenderHost(host, env);
        String jdbc = withSsl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        return new DbParts(jdbc, user, pass);
    }

    /**
     * Accepts:
     * - postgresql://user:pass@host/db
     * - postgres://user:pass@host:5432/db
     * - jdbc:postgresql://host:5432/db
     */
    static DbParts parseDatabaseUrl(String raw, Environment env) {
        String value = raw.trim();
        if (value.startsWith("jdbc:postgresql://") && !value.contains("@")) {
            return new DbParts(withSsl(value), null, null);
        }

        String normalized = value;
        if (normalized.startsWith("jdbc:postgresql://")) {
            normalized = "postgresql://" + normalized.substring("jdbc:postgresql://".length());
        } else if (normalized.startsWith("jdbc:postgres://")) {
            normalized = "postgres://" + normalized.substring("jdbc:postgres://".length());
        }

        try {
            URI uri = new URI(normalized);
            String host = expandShortRenderHost(uri.getHost(), env);
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String db = (path != null && path.startsWith("/")) ? path.substring(1) : path;
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

    /**
     * Short Render internal hosts (dpg-xxx-a) only resolve on the private network
     * in the same region. Expand using DATABASE_HOST_SUFFIX when needed, e.g.
     * singapore-postgres.render.com → dpg-xxx-a.singapore-postgres.render.com
     */
    static String expandShortRenderHost(String host, Environment env) {
        if (host == null || host.contains(".")) {
            return host;
        }
        String override = env != null ? firstNonBlank(env.getProperty("DATABASE_EXTERNAL_HOST")) : null;
        if (!isBlank(override)) {
            return override;
        }
        String suffix = env != null ? firstNonBlank(env.getProperty("DATABASE_HOST_SUFFIX")) : null;
        if (!isBlank(suffix)) {
            String expanded = host + "." + suffix.replaceFirst("^\\.", "");
            log.warn("Expanded short Render DB host {} → {} (set External DATABASE_URL to avoid this)",
                    host, expanded);
            return expanded;
        }
        log.warn(
                "Using short DB host '{}'. If deploy fails with UnknownHostException, "
                        + "set DATABASE_URL to the Render External Database URL "
                        + "(host like dpg-xxx-a.REGION-postgres.render.com).",
                host);
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
