package com.bnpparibas.dsibddf.ap23992.modelevente.batch.injector.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_roles")
public class ContractRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String role;
    private String brand;
    private String scope;
    private String holderId;
    private String ikpi;

    public ContractRoleEntity() {}

    public ContractRoleEntity(String role, String brand, String scope, String holderId, String ikpi) {
        this.role = role;
        this.brand = brand;
        this.scope = scope;
        this.holderId = holderId;
        this.ikpi = ikpi;
    }

    public Long getId() { return id; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getHolderId() { return holderId; }
    public void setHolderId(String holderId) { this.holderId = holderId; }
    public String getIkpi() { return ikpi; }
    public void setIkpi(String ikpi) { this.ikpi = ikpi; }
}
