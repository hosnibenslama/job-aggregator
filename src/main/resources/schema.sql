-- Drop and recreate all tables for development / batch runs
DROP TABLE IF EXISTS contract_advantages;
DROP TABLE IF EXISTS contract_tarifs;
DROP TABLE IF EXISTS contract_tariffs;
DROP TABLE IF EXISTS contract_conditions;
DROP TABLE IF EXISTS contract_ikac;
DROP TABLE IF EXISTS contract_articles;
DROP TABLE IF EXISTS contract_external_ids;
DROP TABLE IF EXISTS contract_marketed_objects;
DROP TABLE IF EXISTS contract_operations;
DROP TABLE IF EXISTS contract_offers;
DROP TABLE IF EXISTS contract_roles;
DROP TABLE IF EXISTS contract_accounts;
DROP TABLE IF EXISTS contract_lines;
DROP TABLE IF EXISTS contracts CASCADE;

CREATE TABLE contracts (
    id                      UUID          PRIMARY KEY,
    -- CTR fields (section 4.2 of the input file specification)
    devise                  VARCHAR(3)    NOT NULL,          -- ISO 4217 currency code (e.g. EUR)
    state                   VARCHAR(2)    NOT NULL,          -- Internal state code (e.g. 16)
    motif                   VARCHAR(50),                     -- Reason code (optional, e.g. 003)
    ou_distribution         VARCHAR(255),                    -- Organisational distribution unit (optional)
    ou_management           VARCHAR(50)   NOT NULL,          -- Client agency code
    address_id              VARCHAR(50),                     -- Address identifier (optional)
    business_relationship   VARCHAR(100)  NOT NULL,          -- Commercial relationship identifier
    effective_date          VARCHAR(35),                     -- YYYY-MM-DDTHH:MM:SS.ssssssZ (optional)
    periode_facturation     VARCHAR(20),                     -- QUOTIDIENNE / HEBDOMADAIRE / MENSUELLE / ANNUELLE (optional)
    dates_facturation       VARCHAR(100),                    -- Billing dates (optional)
    x_b3_trace_id           VARCHAR(16)   NOT NULL,          -- 16 hex chars correlation trace id
    x_b3_span_id            VARCHAR(16)   NOT NULL,          -- 16 hex chars correlation span id
    user_id                 VARCHAR(16)   NOT NULL,          -- 16 hex chars user identifier
    channel                 VARCHAR(3)    NOT NULL,          -- 001/007/008/012
    media                   VARCHAR(3)    NOT NULL,          -- Interaction media
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_accounts (
    id          BIGSERIAL     PRIMARY KEY,
    contract_id UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    sub_type    VARCHAR(10)   NOT NULL,
    bic         VARCHAR(11)   NOT NULL,
    iban        VARCHAR(34)   NOT NULL,
    rib         VARCHAR(50),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_roles (
    id          BIGSERIAL     PRIMARY KEY,
    contract_id UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    role        VARCHAR(10)   NOT NULL,
    brand       VARCHAR(10)   NOT NULL,
    scope       VARCHAR(10)   NOT NULL,
    holder_id   VARCHAR(50)   NOT NULL,
    ikpi        VARCHAR(50)   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_offers (
    id                  BIGSERIAL     PRIMARY KEY,
    contract_id         UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    offer_id            VARCHAR(50)   NOT NULL,
    provider            VARCHAR(50)   NOT NULL,
    personalized_label  VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_marketed_objects (
    id                      BIGSERIAL     PRIMARY KEY,
    contract_id             UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    om_id                   VARCHAR(50)   NOT NULL,
    business_relationship   VARCHAR(100)  NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_external_ids (
    id          BIGSERIAL     PRIMARY KEY,
    contract_id UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    external_id VARCHAR(50)   NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_articles (
    id                BIGSERIAL     PRIMARY KEY,
    contract_id       UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    sequential_index  INT           NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_ikac (
    id          BIGSERIAL     PRIMARY KEY,
    contract_id UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    ikac_value  VARCHAR(100)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_conditions (
    id              BIGSERIAL     PRIMARY KEY,
    contract_id     UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    condition_id    VARCHAR(50)   NOT NULL,
    condition_value VARCHAR(100)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_tarifs (
    id                      BIGSERIAL     PRIMARY KEY,
    contract_id             UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    id_opra_tarif           VARCHAR(50),
    type_frais              VARCHAR(10),
    date_creation_tarif     VARCHAR(35),
    date_effet_tarif        VARCHAR(35),
    devise_tarif            VARCHAR(3),
    indic_tarif_paliers     VARCHAR(1),
    format_tarif            VARCHAR(10),
    periodicite_facturation VARCHAR(10),
    type_taxation           VARCHAR(10),
    type_taux_tarif         VARCHAR(10),
    taux_tarif              VARCHAR(25),
    montant_base            VARCHAR(25),
    ratio_tarif             VARCHAR(25),
    montant_unite           VARCHAR(25),
    type_unite              VARCHAR(10),
    indic_limite_haute      VARCHAR(1),
    limite_haute_montant    VARCHAR(25),
    indic_limite_basse      VARCHAR(1),
    limite_basse_montant    VARCHAR(25),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE contract_advantages (
    id                BIGSERIAL     PRIMARY KEY,
    contract_id       UUID          NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    id_opra_avantage  VARCHAR(50),
    date_debut        VARCHAR(35)   NOT NULL,
    date_fin          VARCHAR(35),
    code_avantage     VARCHAR(5)    NOT NULL,
    valeur_avantage   VARCHAR(25),
    devise_avantage   VARCHAR(3),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indices for foreign keys and frequent lookups
CREATE INDEX idx_contracts_business_rel        ON contracts (business_relationship);
CREATE INDEX idx_contracts_ou_management       ON contracts (ou_management);
CREATE INDEX idx_contract_accounts_cid         ON contract_accounts (contract_id);
CREATE INDEX idx_contract_roles_cid            ON contract_roles (contract_id);
CREATE INDEX idx_contract_offers_cid           ON contract_offers (contract_id);
CREATE INDEX idx_contract_marketed_objects_cid ON contract_marketed_objects (contract_id);
CREATE INDEX idx_contract_external_ids_cid  ON contract_external_ids (contract_id);
CREATE INDEX idx_contract_articles_cid      ON contract_articles (contract_id);
CREATE INDEX idx_contract_ikac_cid          ON contract_ikac (contract_id);
CREATE INDEX idx_contract_conditions_cid    ON contract_conditions (contract_id);
CREATE INDEX idx_contract_tarifs_cid        ON contract_tarifs (contract_id);
CREATE INDEX idx_contract_advantages_cid    ON contract_advantages (contract_id);
