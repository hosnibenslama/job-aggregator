package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.Account;
import com.example.jobaggregator.domain.Advantage;
import com.example.jobaggregator.domain.Article;
import com.example.jobaggregator.domain.Condition;
import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.ContractHeader;
import com.example.jobaggregator.domain.ExternalId;
import com.example.jobaggregator.domain.Ikac;
import com.example.jobaggregator.domain.MarketedObject;
import com.example.jobaggregator.domain.Offer;
import com.example.jobaggregator.domain.Role;
import com.example.jobaggregator.domain.Tarif;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists validated hierarchical {@link ContractBlock} aggregate roots and their normalized child entities
 * directly into relational tables with hierarchical foreign keys.
 */
@Component
public class ContractPersistenceWriter implements ItemWriter<ContractBlock> {

    private final JdbcTemplate jdbcTemplate;

    public ContractPersistenceWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends ContractBlock> items) {
        for (ContractBlock contract : items) {
            persistContract(contract);
        }
    }

    private void persistContract(ContractBlock contract) {
        UUID contractId = contract.id();

        // 1. Insert Root Contract
        insertContractRoot(contractId, contract.header());

        // 2. Insert Contract-level Accounts
        for (Account acc : contract.accounts()) {
            insertAccount(contractId, null, "CONTRACT", acc);
        }

        // 3. Insert Contract-level Roles
        for (Role rol : contract.roles()) {
            insertRole(contractId, null, null, "CONTRACT", rol);
        }

        // 4. Insert Contract-level Offers
        for (Offer off : contract.offers()) {
            insertOffer(contractId, off);
        }

        // 5. Insert Contract-level Tarifs (if any)
        for (Tarif tar : contract.tarifs()) {
            insertTarif(contractId, null, null, "CONTRACT", tar);
        }

        // 6. Insert Contract-level Advantages (if any)
        for (Advantage avt : contract.advantages()) {
            insertAdvantage(contractId, null, null, "CONTRACT", avt);
        }

        // 7. Insert MarketedObjects (OM) and their descendants
        for (MarketedObject om : contract.marketedObjects()) {
            long omId = insertMarketedObject(contractId, om);

            // OM-level External IDs
            for (ExternalId oid : om.externalIds()) {
                insertExternalId(contractId, omId, null, "OM", oid);
            }

            // OM-level Roles
            for (Role rol : om.roles()) {
                insertRole(contractId, omId, null, "OM", rol);
            }

            // OM-level Tarifs (if any)
            for (Tarif tar : om.tarifs()) {
                insertTarif(contractId, omId, null, "OM", tar);
            }

            // OM-level Advantages (if any)
            for (Advantage avt : om.advantages()) {
                insertAdvantage(contractId, omId, null, "OM", avt);
            }

            // Articles attached to this OM
            for (Article art : om.articles()) {
                long articleId = insertArticle(contractId, omId, art);

                // Article-level External IDs
                for (ExternalId oid : art.externalIds()) {
                    insertExternalId(contractId, omId, articleId, "ARTICLE", oid);
                }

                // Article-level IKAC
                for (Ikac ikac : art.ikacs()) {
                    insertIkac(contractId, articleId, ikac);
                }

                // Article-level Conditions
                for (Condition cond : art.conditions()) {
                    insertCondition(contractId, articleId, cond);
                }

                // Article-level Accounts
                for (Account acc : art.accounts()) {
                    insertAccount(contractId, articleId, "ARTICLE", acc);
                }

                // Article-level Roles
                for (Role rol : art.roles()) {
                    insertRole(contractId, omId, articleId, "ARTICLE", rol);
                }

                // Article-level Tarifs
                for (Tarif tar : art.tarifs()) {
                    insertTarif(contractId, omId, articleId, "ARTICLE", tar);
                }

                // Article-level Advantages
                for (Advantage avt : art.advantages()) {
                    insertAdvantage(contractId, omId, articleId, "ARTICLE", avt);
                }
            }
        }
    }

    private void insertContractRoot(UUID contractId, ContractHeader ctr) {
        String sql = """
            INSERT INTO contracts (
                id, devise, state, motif, ou_distribution, ou_management,
                address_id, business_relationship, effective_date,
                periode_facturation, dates_facturation, x_b3_trace_id,
                x_b3_span_id, user_id, channel, media
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                contractId,
                ctr != null ? ctr.devise() : null,
                ctr != null ? ctr.state() : null,
                ctr != null ? blankToNull(ctr.motif()) : null,
                ctr != null ? blankToNull(ctr.ouDistribution()) : null,
                ctr != null ? ctr.ouManagement() : null,
                ctr != null ? blankToNull(ctr.addressId()) : null,
                ctr != null ? ctr.businessRelationship() : null,
                ctr != null ? blankToNull(ctr.effectiveDate()) : null,
                ctr != null ? blankToNull(ctr.periodeFacturation()) : null,
                ctr != null ? blankToNull(ctr.datesFacturation()) : null,
                ctr != null ? ctr.xB3TraceId() : null,
                ctr != null ? ctr.xB3SpanId() : null,
                ctr != null ? ctr.userId() : null,
                ctr != null ? ctr.channel() : null,
                ctr != null ? ctr.media() : null
        );
    }

    private void insertAccount(UUID contractId, Long articleId, String level, Account a) {
        String sql = """
            INSERT INTO contract_accounts (contract_id, article_id, level, sub_type, bic, iban, rib)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, contractId, articleId, level, a.subType(), a.bic(), a.iban(), blankToNull(a.rib()));
    }

    private void insertRole(UUID contractId, Long omId, Long articleId, String level, Role r) {
        String sql = """
            INSERT INTO contract_roles (contract_id, marketed_object_id, article_id, level, role, brand, scope, holder_id, ikpi)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, contractId, omId, articleId, level, r.role(), r.brand(), r.scope(), r.holderId(), r.ikpi());
    }

    private void insertOffer(UUID contractId, Offer o) {
        String sql = """
            INSERT INTO contract_offers (contract_id, offer_id, provider, personalized_label)
            VALUES (?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, contractId, o.offerId(), o.provider(), blankToNull(o.personalizedLabel()));
    }

    private long insertMarketedObject(UUID contractId, MarketedObject om) {
        String sql = """
            INSERT INTO contract_marketed_objects (contract_id, om_id, business_relationship)
            VALUES (?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, contractId);
            ps.setString(2, om.omId());
            ps.setString(3, om.businessRelationship());
            return ps;
        }, keyHolder);

        return extractGeneratedId(keyHolder);
    }

    private long insertArticle(UUID contractId, long omId, Article art) {
        String sql = """
            INSERT INTO contract_articles (contract_id, marketed_object_id, sequential_index)
            VALUES (?, ?, ?)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, contractId);
            ps.setLong(2, omId);
            ps.setInt(3, art.sequentialIndex());
            return ps;
        }, keyHolder);

        return extractGeneratedId(keyHolder);
    }

    private long extractGeneratedId(KeyHolder keyHolder) {
        try {
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null) {
                for (Map.Entry<String, Object> entry : keys.entrySet()) {
                    if ("id".equalsIgnoreCase(entry.getKey()) && entry.getValue() instanceof Number n) {
                        return n.longValue();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : 0L;
    }

    private void insertExternalId(UUID contractId, Long omId, Long articleId, String level, ExternalId oid) {
        String sql = """
            INSERT INTO contract_external_ids (contract_id, marketed_object_id, article_id, level, external_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, contractId, omId, articleId, level, oid.externalId());
    }

    private void insertIkac(UUID contractId, long articleId, Ikac ikac) {
        String sql = """
            INSERT INTO contract_ikac (contract_id, article_id, ikac_value)
            VALUES (?, ?, ?)
            """;
        jdbcTemplate.update(sql, contractId, articleId, ikac.ikacValue());
    }

    private void insertCondition(UUID contractId, long articleId, Condition c) {
        String sql = """
            INSERT INTO contract_conditions (contract_id, article_id, condition_id, condition_value)
            VALUES (?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, contractId, articleId, c.conditionId(), c.conditionValue());
    }

    private void insertTarif(UUID contractId, Long omId, Long articleId, String level, Tarif t) {
        String sql = """
            INSERT INTO contract_tarifs (
                contract_id, marketed_object_id, article_id, level,
                id_opra_tarif, type_frais, date_creation_tarif, date_effet_tarif,
                devise_tarif, indic_tarif_paliers, format_tarif, periodicite_facturation,
                type_taxation, type_taux_tarif, taux_tarif, montant_base, ratio_tarif,
                montant_unite, type_unite, indic_limite_haute, limite_haute_montant,
                indic_limite_basse, limite_basse_montant
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                contractId, omId, articleId, level,
                blankToNull(t.idOpraTarif()),
                blankToNull(t.typeFrais()),
                blankToNull(t.dateCreationTarif()),
                blankToNull(t.dateEffetTarif()),
                blankToNull(t.deviseTarif()),
                blankToNull(t.indicTarifPaliers()),
                blankToNull(t.formatTarif()),
                blankToNull(t.periodiciteFacturation()),
                blankToNull(t.typeTaxation()),
                blankToNull(t.typeTauxTarif()),
                blankToNull(t.tauxTarif()),
                blankToNull(t.montantBase()),
                blankToNull(t.ratioTarif()),
                blankToNull(t.montantUnite()),
                blankToNull(t.typeUnite()),
                blankToNull(t.indicLimiteHaute()),
                blankToNull(t.limiteHauteMontant()),
                blankToNull(t.indicLimiteBasse()),
                blankToNull(t.limiteBasseMontant())
        );
    }

    private void insertAdvantage(UUID contractId, Long omId, Long articleId, String level, Advantage a) {
        String sql = """
            INSERT INTO contract_advantages (
                contract_id, marketed_object_id, article_id, level,
                id_opra_avantage, date_debut, date_fin, code_avantage,
                valeur_avantage, devise_avantage
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
                contractId, omId, articleId, level,
                blankToNull(a.idOpraAvantage()),
                a.dateDebut(),
                blankToNull(a.dateFin()),
                a.codeAvantage(),
                blankToNull(a.valeurAvantage()),
                blankToNull(a.deviseAvantage())
        );
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
