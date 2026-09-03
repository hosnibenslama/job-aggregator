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

    // -----------------------------------------------------------------------
    // Constructor used by ContractWriter
    // -----------------------------------------------------------------------
    public ContractEntity(
            String devise, String state, String motif, String ouDistribution,
            String ouManagement, String addressId, String businessRelationship,
            String effectiveDate, String periodeFacturation, String datesFacturation,
            String xB3TraceId, String xB3SpanId, String userId, String channel, String media,
            Set<ContractLineEntity> lines) {
        this.devise = devise;
        this.state = state;
        this.motif = motif;
        this.ouDistribution = ouDistribution;
        this.ouManagement = ouManagement;
        this.addressId = addressId;
        this.businessRelationship = businessRelationship;
        this.effectiveDate = effectiveDate;
        this.periodeFacturation = periodeFacturation;
        this.datesFacturation = datesFacturation;
        this.xB3TraceId = xB3TraceId;
        this.xB3SpanId = xB3SpanId;
        this.userId = userId;
        this.channel = channel;
        this.media = media;
        this.lines = lines;
    }

    public Long getId() { return id; }
    public String getDevise() { return devise; }
    public String getState() { return state; }
    public String getMotif() { return motif; }
    public String getOuDistribution() { return ouDistribution; }
    public String getOuManagement() { return ouManagement; }
    public String getAddressId() { return addressId; }
    public String getBusinessRelationship() { return businessRelationship; }
    public String getEffectiveDate() { return effectiveDate; }
    public String getPeriodeFacturation() { return periodeFacturation; }
    public String getDatesFacturation() { return datesFacturation; }
    public String getXB3TraceId() { return xB3TraceId; }
    public String getXB3SpanId() { return xB3SpanId; }
    public String getUserId() { return userId; }
    public String getChannel() { return channel; }
    public String getMedia() { return media; }
    public Set<ContractLineEntity> getLines() { return lines; }
}
