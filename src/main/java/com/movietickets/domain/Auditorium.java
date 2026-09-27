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
@Table(name = "auditoriums", uniqueConstraints = @UniqueConstraint(columnNames = {"theater_id", "name"}))
public class Auditorium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    protected Auditorium() {
    }

    public Auditorium(String name, Theater theater) {
        this.name = name;
        this.theater = theater;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Theater getTheater() {
        return theater;
    }
}
