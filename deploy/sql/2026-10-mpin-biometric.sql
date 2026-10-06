-- Manual fix if API is still on an old deploy (PostgreSQL).
-- Safe to re-run (IF NOT EXISTS).

ALTER TABLE tanker_users ADD COLUMN IF NOT EXISTS mpin_hash varchar(100);
ALTER TABLE tanker_users ADD COLUMN IF NOT EXISTS mpin_enabled boolean DEFAULT false;
UPDATE tanker_users SET mpin_enabled = false WHERE mpin_enabled IS NULL;

CREATE TABLE IF NOT EXISTS tanker_biometric_devices (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES tanker_users(id),
    device_id varchar(64) NOT NULL UNIQUE,
    public_key_base64 varchar(1000) NOT NULL,
    device_label varchar(120),
    active boolean DEFAULT true,
    last_used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_bio_user ON tanker_biometric_devices(user_id);

CREATE TABLE IF NOT EXISTS tanker_auth_challenges (
    id bigserial PRIMARY KEY,
    challenge_id varchar(64) NOT NULL UNIQUE,
    user_id bigint NOT NULL REFERENCES tanker_users(id),
    device_id varchar(64) NOT NULL,
    nonce varchar(128) NOT NULL,
    expires_at timestamptz NOT NULL,
    used boolean DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_challenge_id ON tanker_auth_challenges(challenge_id);
