package com.example.jobaggregator.persistence;

import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC aggregate root for a contract.
 *
 * <p>A single {@code save()} call on {@link ContractEntityRepository} will insert
 * into {@code contracts} and batch-insert all child rows into {@code contract_lines}
 * — no manual key management required.
 */
@Table("contracts")
public class ContractEntity {

    @Id
    private Long id;

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

    /** All raw business lines belonging to this contract (CTR + children). */
    @MappedCollection(idColumn = "contract_id")
    private Set<ContractLineEntity> lines;

    /** Required by Spring Data JDBC for instantiation. */
    public ContractEntity() {}

    public Long getId() { return id; }

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

    public void setLines(Set<ContractLineEntity> lines) { this.lines = lines; }
    public Set<ContractLineEntity> getLines() { return lines; }
}
