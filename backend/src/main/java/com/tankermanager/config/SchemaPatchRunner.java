package com.tankermanager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures MPIN / biometric columns & tables exist on older Postgres DBs
 * where Hibernate ddl-auto=update may not have applied yet.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class SchemaPatchRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE tanker_users ADD COLUMN IF NOT EXISTS mpin_hash varchar(100)");
            jdbc.execute("ALTER TABLE tanker_users ADD COLUMN IF NOT EXISTS mpin_enabled boolean DEFAULT false");
            jdbc.execute("UPDATE tanker_users SET mpin_enabled = false WHERE mpin_enabled IS NULL");

            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS tanker_biometric_devices (
                        id bigserial PRIMARY KEY,
                        user_id bigint NOT NULL REFERENCES tanker_users(id),
                        device_id varchar(64) NOT NULL UNIQUE,
                        public_key_base64 varchar(1000) NOT NULL,
                        device_label varchar(120),
                        active boolean DEFAULT true,
                        last_used_at timestamp with time zone,
                        created_at timestamp with time zone NOT NULL DEFAULT now(),
                        updated_at timestamp with time zone
                    )
                    """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_bio_user ON tanker_biometric_devices(user_id)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_bio_device ON tanker_biometric_devices(device_id)");

            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS tanker_auth_challenges (
                        id bigserial PRIMARY KEY,
                        challenge_id varchar(64) NOT NULL UNIQUE,
                        user_id bigint NOT NULL REFERENCES tanker_users(id),
                        device_id varchar(64) NOT NULL,
                        nonce varchar(128) NOT NULL,
                        expires_at timestamp with time zone NOT NULL,
                        used boolean DEFAULT false,
                        created_at timestamp with time zone NOT NULL DEFAULT now()
                    )
                    """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_challenge_id ON tanker_auth_challenges(challenge_id)");

            log.info("Schema patch applied (mpin + biometric tables)");
        } catch (Exception e) {
            log.warn("Schema patch skipped or partial: {}", e.getMessage());
        }
    }
}
