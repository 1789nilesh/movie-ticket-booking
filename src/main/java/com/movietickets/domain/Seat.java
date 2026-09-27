package com.movietickets.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(columnNames = {"auditorium_id", "row_label", "seat_number"}))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "auditorium_id")
    private Auditorium auditorium;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer number;

    @Column(nullable = false)
    private String tier;

    protected Seat() {
    }

    public Seat(Auditorium auditorium, String rowLabel, Integer number, String tier) {
        this.auditorium = auditorium;
        this.rowLabel = rowLabel;
        this.number = number;
        this.tier = tier.toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public Integer getNumber() {
        return number;
    }

    public String getTier() {
        return tier;
    }

    public boolean isPremium() {
        return "PREMIUM".equalsIgnoreCase(tier);
    }
}
