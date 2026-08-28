CREATE TABLE IF NOT EXISTS contracts (
    id BIGSERIAL PRIMARY KEY,
    contract_id VARCHAR(255) NOT NULL UNIQUE,
    first_line BIGINT NOT NULL,
    last_line BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contracts_contract_id ON contracts (contract_id);

CREATE TABLE IF NOT EXISTS contract_lines (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    line_number BIGINT NOT NULL,
    line_type VARCHAR(50) NOT NULL,
    raw_line TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contract_lines_contract_id ON contract_lines (contract_id);
CREATE INDEX IF NOT EXISTS idx_contract_lines_line_type ON contract_lines (line_type);
