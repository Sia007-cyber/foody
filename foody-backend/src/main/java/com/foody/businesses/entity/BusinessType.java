package com.foody.businesses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Reference row backing {@link BusinessTypeCode}. The businesses table stores only
 * the VARCHAR code (a FK to this table), so a new business type is purely data + an
 * enum constant — never a schema change.
 */
@Entity
@Table(name = "business_types")
public class BusinessType {

    @Id
    @Column(name = "code", length = 50)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
