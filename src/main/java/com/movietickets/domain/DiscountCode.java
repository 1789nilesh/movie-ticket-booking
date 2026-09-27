package com.movietickets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private Integer discountPercentage;

    private Integer flatDiscountCents;

    private Instant validUntil;

    private Integer maxUses;

    private int currentUses;

    private boolean active = true;

    protected DiscountCode() {
    }

    public DiscountCode(String code, Integer discountPercentage, Integer flatDiscountCents, Instant validUntil, Integer maxUses) {
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.discountPercentage = discountPercentage;
        this.flatDiscountCents = flatDiscountCents;
        this.validUntil = validUntil;
        this.maxUses = maxUses;
        this.currentUses = 0;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public Integer getFlatDiscountCents() {
        return flatDiscountCents;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isValid(Instant now) {
        if (!active) {
            return false;
        }
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }
        if (maxUses != null && currentUses >= maxUses) {
            return false;
        }
        return true;
    }

    public void incrementUses() {
        this.currentUses++;
    }
}
