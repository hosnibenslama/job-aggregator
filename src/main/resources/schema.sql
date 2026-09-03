-- Drop and recreate to pick up schema changes during development
DROP TABLE IF EXISTS contract_lines;
DROP TABLE IF EXISTS contracts;

CREATE TABLE contracts (
    id                      BIGSERIAL PRIMARY KEY,
    -- CTR fields (section 4.2 of the input file specification)
    devise                  VARCHAR(3)    NOT NULL,          -- ISO 4217 currency code (e.g. EUR)
    state                   VARCHAR(2)    NOT NULL,          -- Internal state code (e.g. 16)
    motif                   VARCHAR(50),                     -- Reason code (optional, e.g. 003)
    ou_distribution         VARCHAR(255),                    -- Organisational distribution unit (optional)
    ou_management           VARCHAR(50)   NOT NULL,          -- Client agency code
    address_id              VARCHAR(50),                     -- Address identifier (optional)
    business_relationship   VARCHAR(100)  NOT NULL,          -- Commercial relationship identifier
    effective_date          VARCHAR(30),                     -- YYYY-MM-DDTHH:MM:SS.ssssssZ (optional)
    periode_facturation     VARCHAR(20),                     -- QUOTIDIENNE / HEBDOMADAIRE / MENSUELLE / ANNUELLE (optional)
    dates_facturation       VARCHAR(100),                    -- Billing dates (optional)
    x_b3_trace_id           VARCHAR(16)   NOT NULL,          -- 16 hex chars correlation trace id
    x_b3_span_id            VARCHAR(16)   NOT NULL,          -- 16 hex chars correlation span id
    user_id                 VARCHAR(16)   NOT NULL,          -- 16 hex chars user identifier
    channel                 VARCHAR(3)    NOT NULL,          -- 001/007/008/012
    media                   VARCHAR(3)    NOT NULL,          -- Interaction media
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_lines (
    id          BIGSERIAL PRIMARY KEY,
    contract_id BIGINT      NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    line_number BIGINT      NOT NULL,
    line_type   VARCHAR(50) NOT NULL,
    raw_line    TEXT        NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contract_lines_contract_id ON contract_lines (contract_id);
CREATE INDEX idx_contract_lines_line_type   ON contract_lines (line_type);
CREATE INDEX idx_contracts_business_rel     ON contracts (business_relationship);
CREATE INDEX idx_contracts_ou_management    ON contracts (ou_management);
