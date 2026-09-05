package com.example.jobaggregator.writer;

import com.example.jobaggregator.domain.Account;
import com.example.jobaggregator.domain.Advantage;
import com.example.jobaggregator.domain.Article;
import com.example.jobaggregator.domain.Condition;
import com.example.jobaggregator.domain.ContractBlock;
import com.example.jobaggregator.domain.ContractHeader;
import com.example.jobaggregator.domain.ExternalId;
import com.example.jobaggregator.domain.Ikac;
import com.example.jobaggregator.domain.Offer;
import com.example.jobaggregator.domain.MarketedObject;
import com.example.jobaggregator.domain.Role;
import com.example.jobaggregator.domain.Tarif;
import com.example.jobaggregator.persistence.ContractAccountEntity;
import com.example.jobaggregator.persistence.ContractAdvantageEntity;
import com.example.jobaggregator.persistence.ContractArticleEntity;
import com.example.jobaggregator.persistence.ContractConditionEntity;
import com.example.jobaggregator.persistence.ContractEntity;
import com.example.jobaggregator.persistence.ContractEntityRepository;
import com.example.jobaggregator.persistence.ContractExternalIdEntity;
import com.example.jobaggregator.persistence.ContractIkacEntity;
import com.example.jobaggregator.persistence.ContractMarketedObjectEntity;
import com.example.jobaggregator.persistence.ContractOfferEntity;
import com.example.jobaggregator.persistence.ContractRoleEntity;
import com.example.jobaggregator.persistence.ContractTarifEntity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persists validated {@link ContractBlock} aggregate roots and their normalized child entities
 * directly into relational tables using Spring Data JDBC.
 */
@Component
public class ContractPersistenceWriter implements ItemWriter<ContractBlock> {

    private final ContractEntityRepository repository;

    public ContractPersistenceWriter(ContractEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends ContractBlock> items) {
        List<ContractEntity> entities = items.getItems().stream()
                .map(this::toEntity)
                .toList();
        repository.saveAll(entities);
    }

    // -----------------------------------------------------------------------
    // Mapping: domain model → persistence model
    // -----------------------------------------------------------------------

    private ContractEntity toEntity(ContractBlock contract) {
        ContractEntity entity = new ContractEntity();
        entity.setId(contract.id());

        ContractHeader ctr = contract.header();
        if (ctr != null) {
            entity.setDevise(ctr.devise());
            entity.setState(ctr.state());
            entity.setMotif(blankToNull(ctr.motif()));
            entity.setOuDistribution(blankToNull(ctr.ouDistribution()));
            entity.setOuManagement(ctr.ouManagement());
            entity.setAddressId(blankToNull(ctr.addressId()));
            entity.setBusinessRelationship(ctr.businessRelationship());
            entity.setEffectiveDate(blankToNull(ctr.effectiveDate()));
            entity.setPeriodeFacturation(blankToNull(ctr.periodeFacturation()));
            entity.setDatesFacturation(blankToNull(ctr.datesFacturation()));
            entity.setXB3TraceId(ctr.xB3TraceId());
            entity.setXB3SpanId(ctr.xB3SpanId());
            entity.setUserId(ctr.userId());
            entity.setChannel(ctr.channel());
            entity.setMedia(ctr.media());
        }

        entity.setAccounts(contract.accounts().stream()
                .map(a -> new ContractAccountEntity(a.subType(), a.bic(), a.iban(), blankToNull(a.rib())))
                .collect(Collectors.toSet()));

        entity.setRoles(contract.roles().stream()
                .map(r -> new ContractRoleEntity(r.role(), r.brand(), r.scope(), r.holderId(), r.ikpi()))
                .collect(Collectors.toSet()));

        entity.setOffers(contract.offers().stream()
                .map(o -> new ContractOfferEntity(o.offerId(), o.provider(), blankToNull(o.personalizedLabel())))
                .collect(Collectors.toSet()));

        entity.setMarketedObjects(contract.marketedObjects().stream()
                .map(om -> new ContractMarketedObjectEntity(om.omId(), om.businessRelationship()))
                .collect(Collectors.toSet()));

        entity.setExternalIds(contract.externalIds().stream()
                .map(oid -> new ContractExternalIdEntity(oid.externalId()))
                .collect(Collectors.toSet()));

        entity.setArticles(contract.articles().stream()
                .map(art -> new ContractArticleEntity(art.sequentialIndex()))
                .collect(Collectors.toSet()));

        entity.setIkacLines(contract.ikacs().stream()
                .map(ikac -> new ContractIkacEntity(ikac.ikacValue()))
                .collect(Collectors.toSet()));

        entity.setConditions(contract.conditions().stream()
                .map(c -> new ContractConditionEntity(c.conditionId(), c.conditionValue()))
                .collect(Collectors.toSet()));

        entity.setTarifs(contract.tarifs().stream()
                .map(this::toTarifEntity)
                .collect(Collectors.toSet()));

        entity.setAdvantages(contract.advantages().stream()
                .map(avt -> new ContractAdvantageEntity(
                        blankToNull(avt.idOpraAvantage()),
                        avt.dateDebut(),
                        blankToNull(avt.dateFin()),
                        avt.codeAvantage(),
                        blankToNull(avt.valeurAvantage()),
                        blankToNull(avt.deviseAvantage())))
                .collect(Collectors.toSet()));

        return entity;
    }

    private ContractTarifEntity toTarifEntity(Tarif t) {
        ContractTarifEntity te = new ContractTarifEntity();
        te.setIdOpraTarif(blankToNull(t.idOpraTarif()));
        te.setTypeFrais(blankToNull(t.typeFrais()));
        te.setDateCreationTarif(blankToNull(t.dateCreationTarif()));
        te.setDateEffetTarif(blankToNull(t.dateEffetTarif()));
        te.setDeviseTarif(blankToNull(t.deviseTarif()));
        te.setIndicTarifPaliers(blankToNull(t.indicTarifPaliers()));
        te.setFormatTarif(blankToNull(t.formatTarif()));
        te.setPeriodiciteFacturation(blankToNull(t.periodiciteFacturation()));
        te.setTypeTaxation(blankToNull(t.typeTaxation()));
        te.setTypeTauxTarif(blankToNull(t.typeTauxTarif()));
        te.setTauxTarif(blankToNull(t.tauxTarif()));
        te.setMontantBase(blankToNull(t.montantBase()));
        te.setRatioTarif(blankToNull(t.ratioTarif()));
        te.setMontantUnite(blankToNull(t.montantUnite()));
        te.setTypeUnite(blankToNull(t.typeUnite()));
        te.setIndicLimiteHaute(blankToNull(t.indicLimiteHaute()));
        te.setLimiteHauteMontant(blankToNull(t.limiteHauteMontant()));
        te.setIndicLimiteBasse(blankToNull(t.indicLimiteBasse()));
        te.setLimiteBasseMontant(blankToNull(t.limiteBasseMontant()));
        return te;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
