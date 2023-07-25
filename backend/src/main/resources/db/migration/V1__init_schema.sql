CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE patients (
    id            BIGSERIAL PRIMARY KEY,
    mrn           VARCHAR(32)  NOT NULL UNIQUE,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    date_of_birth DATE         NOT NULL,
    sex           VARCHAR(10)  NOT NULL,
    phone         VARCHAR(32),
    email         VARCHAR(160),
    notes         VARCHAR(1000),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_patients_last_name ON patients (lower(last_name));

CREATE TABLE assessments (
    id            BIGSERIAL PRIMARY KEY,
    patient_id    BIGINT        NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    features      JSONB         NOT NULL,
    probability   NUMERIC(6, 4) NOT NULL CHECK (probability BETWEEN 0 AND 1),
    risk_band     VARCHAR(10)   NOT NULL,
    prediction    VARCHAR(16)   NOT NULL,
    model_name    VARCHAR(64)   NOT NULL,
    model_version VARCHAR(32)   NOT NULL,
    top_features  JSONB         NOT NULL,
    notes         VARCHAR(1000),
    created_by    VARCHAR(160),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_assessments_patient ON assessments (patient_id, created_at DESC);
CREATE INDEX idx_assessments_created_at ON assessments (created_at);
CREATE INDEX idx_assessments_risk_band ON assessments (risk_band);

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor       VARCHAR(160) NOT NULL,
    action      VARCHAR(40)  NOT NULL,
    entity_type VARCHAR(40)  NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(2000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at DESC);

INSERT INTO roles (name) VALUES ('ADMIN'), ('DOCTOR'), ('ANALYST');
