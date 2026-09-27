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
import java.time.Instant;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "auditorium_id")
    private Auditorium auditorium;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private int regularPriceCents;

    @Column(nullable = false)
    private int premiumPriceCents;

    protected Show() {
    }

    public Show(Movie movie, Auditorium auditorium, Instant startsAt, int regularPriceCents, int premiumPriceCents) {
        this.movie = movie;
        this.auditorium = auditorium;
        this.startsAt = startsAt;
        this.regularPriceCents = regularPriceCents;
        this.premiumPriceCents = premiumPriceCents;
    }

    public Long getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public int getRegularPriceCents() {
        return regularPriceCents;
    }

    public int getPremiumPriceCents() {
        return premiumPriceCents;
    }
}
