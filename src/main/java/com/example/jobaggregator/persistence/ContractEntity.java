package com.example.jobaggregator.persistence;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC aggregate root for a contract.
 *
 * <p>Uses a code-generated {@link UUID} primary key. Implementing {@link Persistable}
 * with {@code isNew() == true} ensures Spring Data JDBC issues direct INSERT statements
 * without checking for prior existence, which is critical for high-throughput batch ingestion.
 */
@Table("contracts")
public class ContractEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    // -----------------------------------------------------------------------
    // CTR fields — section 4.2 of the input file specification
    // -----------------------------------------------------------------------
    private String devise;
    private String state;
    private String motif;
    private String ouDistribution;
    private String ouManagement;
    private String addressId;
    private String businessRelationship;
    private String effectiveDate;
    private String periodeFacturation;
    private String datesFacturation;
    private String xB3TraceId;
    private String xB3SpanId;
    private String userId;
    private String channel;
    private String media;

    // -----------------------------------------------------------------------
    // Dedicated child entities mapped via contract_id foreign key
    // -----------------------------------------------------------------------
    @MappedCollection(idColumn = "contract_id")
    private Set<ContractAccountEntity> accounts;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractRoleEntity> roles;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractOfferEntity> offers;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractOperationEntity> operations;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractExternalIdEntity> externalIds;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractArticleEntity> articles;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractIkacEntity> ikacLines;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractConditionEntity> conditions;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractTariffEntity> tariffs;

    @MappedCollection(idColumn = "contract_id")
    private Set<ContractAdvantageEntity> advantages;

    public ContractEntity() {}

    @Override
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Override
    public boolean isNew() { return isNew; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

    public void setDevise(String devise) { this.devise = devise; }
    public String getDevise() { return devise; }

    public void setState(String state) { this.state = state; }
    public String getState() { return state; }

    public void setMotif(String motif) { this.motif = motif; }
    public String getMotif() { return motif; }

    public void setOuDistribution(String ouDistribution) { this.ouDistribution = ouDistribution; }
    public String getOuDistribution() { return ouDistribution; }

    public void setOuManagement(String ouManagement) { this.ouManagement = ouManagement; }
    public String getOuManagement() { return ouManagement; }

    public void setAddressId(String addressId) { this.addressId = addressId; }
    public String getAddressId() { return addressId; }

    public void setBusinessRelationship(String businessRelationship) { this.businessRelationship = businessRelationship; }
    public String getBusinessRelationship() { return businessRelationship; }

    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getEffectiveDate() { return effectiveDate; }

    public void setPeriodeFacturation(String periodeFacturation) { this.periodeFacturation = periodeFacturation; }
    public String getPeriodeFacturation() { return periodeFacturation; }

    public void setDatesFacturation(String datesFacturation) { this.datesFacturation = datesFacturation; }
    public String getDatesFacturation() { return datesFacturation; }

    public void setXB3TraceId(String xB3TraceId) { this.xB3TraceId = xB3TraceId; }
    public String getXB3TraceId() { return xB3TraceId; }

    public void setXB3SpanId(String xB3SpanId) { this.xB3SpanId = xB3SpanId; }
    public String getXB3SpanId() { return xB3SpanId; }

    public void setUserId(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }

    public void setChannel(String channel) { this.channel = channel; }
    public String getChannel() { return channel; }

    public void setMedia(String media) { this.media = media; }
    public String getMedia() { return media; }

    public Set<ContractAccountEntity> getAccounts() { return accounts; }
    public void setAccounts(Set<ContractAccountEntity> accounts) { this.accounts = accounts; }

    public Set<ContractRoleEntity> getRoles() { return roles; }
    public void setRoles(Set<ContractRoleEntity> roles) { this.roles = roles; }

    public Set<ContractOfferEntity> getOffers() { return offers; }
    public void setOffers(Set<ContractOfferEntity> offers) { this.offers = offers; }

    public Set<ContractOperationEntity> getOperations() { return operations; }
    public void setOperations(Set<ContractOperationEntity> operations) { this.operations = operations; }

    public Set<ContractExternalIdEntity> getExternalIds() { return externalIds; }
    public void setExternalIds(Set<ContractExternalIdEntity> externalIds) { this.externalIds = externalIds; }

    public Set<ContractArticleEntity> getArticles() { return articles; }
    public void setArticles(Set<ContractArticleEntity> articles) { this.articles = articles; }

    public Set<ContractIkacEntity> getIkacLines() { return ikacLines; }
    public void setIkacLines(Set<ContractIkacEntity> ikacLines) { this.ikacLines = ikacLines; }

    public Set<ContractConditionEntity> getConditions() { return conditions; }
    public void setConditions(Set<ContractConditionEntity> conditions) { this.conditions = conditions; }

    public Set<ContractTariffEntity> getTariffs() { return tariffs; }
    public void setTariffs(Set<ContractTariffEntity> tariffs) { this.tariffs = tariffs; }

    public Set<ContractAdvantageEntity> getAdvantages() { return advantages; }
    public void setAdvantages(Set<ContractAdvantageEntity> advantages) { this.advantages = advantages; }
}
