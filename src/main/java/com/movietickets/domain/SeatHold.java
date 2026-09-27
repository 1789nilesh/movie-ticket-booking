package com.movietickets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "seat_holds")
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String customerEmail;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status = HoldStatus.ACTIVE;

    protected SeatHold() {
    }

    public SeatHold(String token, String customerEmail, Show show, Instant expiresAt) {
        this.token = token;
        this.customerEmail = customerEmail;
        this.show = show;
        this.expiresAt = expiresAt;
        this.status = HoldStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Show getShow() {
        return show;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public boolean isExpired(Instant now) {
        return status == HoldStatus.ACTIVE && !expiresAt.isAfter(now);
    }

    public void confirm() {
        this.status = HoldStatus.CONFIRMED;
    }

    public void expire() {
        this.status = HoldStatus.EXPIRED;
    }
}
