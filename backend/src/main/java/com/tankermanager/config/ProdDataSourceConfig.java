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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Prod DataSource from Render DATABASE_URL.
 * Use External Database URL (host contains -postgres.render.com).
 * Credentials must be set separately from the JDBC URL for the PG driver.
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
        ds.setInitializationFailTimeout(30000);
        // Render Postgres requires TLS on external connections
        ds.addDataSourceProperty("ssl", "true");
        ds.addDataSourceProperty("sslmode", "require");
        ds.addDataSourceProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");

        log.info("Prod datasource jdbcUrl={} user={}", parts.jdbcUrl, parts.username);
        return ds;
    }

    private static DbParts resolve(Environment env) {
        String rawUrl = firstNonBlank(env.getProperty("DATABASE_URL"), env.getProperty("spring.datasource.url"));
        String user = firstNonBlank(env.getProperty("DATABASE_USER"), env.getProperty("DB_USERNAME"));
        String pass = firstNonBlank(env.getProperty("DATABASE_PASSWORD"), env.getProperty("DB_PASSWORD"));

        if (!isBlank(rawUrl)) {
            DbParts fromUrl = parseDatabaseUrl(rawUrl, env);
            if (isBlank(fromUrl.username) && !isBlank(user)) {
                fromUrl.username = user;
            }
            if (isBlank(fromUrl.password) && !isBlank(pass)) {
                fromUrl.password = pass;
            }
            if (isBlank(fromUrl.username) || isBlank(fromUrl.password)) {
                throw new IllegalStateException(
                        "DATABASE_URL is set but username/password are missing. "
                                + "Paste the full External Database URL from Render Connect.");
            }
            return fromUrl;
        }

        String host = firstNonBlank(env.getProperty("DATABASE_HOST"));
        String port = firstNonBlank(env.getProperty("DATABASE_PORT"), "5432");
        String name = firstNonBlank(env.getProperty("DATABASE_NAME"));
        if (isBlank(host) || isBlank(name) || isBlank(user) || isBlank(pass)) {
            throw new IllegalStateException(
                    "Set DATABASE_URL to External Database URL from Render Postgres → Connect "
                            + "(must include ...-postgres.render.com host).");
        }
        host = toExternalHost(host, env);
        String jdbc = withSsl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        return new DbParts(jdbc, user, pass);
    }

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
        } else if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }

        try {
            URI uri = new URI(normalized);
            String host = toExternalHost(uri.getHost(), env);
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String db = (path != null && path.startsWith("/")) ? path.substring(1) : path;
            if (db != null && db.contains("?")) {
                db = db.substring(0, db.indexOf('?'));
            }

            String username = null;
            String password = null;
            String userInfo = uri.getRawUserInfo() != null ? uri.getRawUserInfo() : uri.getUserInfo();
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    username = urlDecode(userInfo.substring(0, colon));
                    password = urlDecode(userInfo.substring(colon + 1));
                } else {
                    username = urlDecode(userInfo);
                }
            }

            String jdbc = withSsl("jdbc:postgresql://" + host + ":" + port + "/" + db);
            return new DbParts(jdbc, username, password);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid DATABASE_URL: " + e.getMessage(), e);
        }
    }

    /**
     * Short internal hosts (dpg-xxx-a) only work on Render private network same-region.
     * Expand to external hostname so TLS public connections work reliably.
     */
    static String toExternalHost(String host, Environment env) {
        if (host == null || host.contains(".")) {
            return host;
        }
        String override = env != null ? firstNonBlank(env.getProperty("DATABASE_EXTERNAL_HOST")) : null;
        if (!isBlank(override)) {
            return override;
        }
        String suffix = env != null
                ? firstNonBlank(env.getProperty("DATABASE_HOST_SUFFIX"), "singapore-postgres.render.com")
                : "singapore-postgres.render.com";
        String expanded = host + "." + suffix.replaceFirst("^\\.", "");
        log.warn("Expanded short DB host {} → {} (prefer External Database URL in Render env)", host, expanded);
        return expanded;
    }

    static String withSsl(String jdbcUrl) {
        String url = jdbcUrl;
        if (!url.contains("sslmode=")) {
            url = url + (url.contains("?") ? "&" : "?") + "sslmode=require";
        }
        if (!url.contains("sslfactory=")) {
            url = url + "&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        }
        return url;
    }

    private static String urlDecode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
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
