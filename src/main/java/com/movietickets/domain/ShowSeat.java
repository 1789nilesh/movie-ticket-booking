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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "show_seats", uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_id"}))
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private String holdToken;

    private Instant holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    protected ShowSeat() {
    }

    public ShowSeat(Show show, Seat seat) {
        this.show = show;
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public Show getShow() {
        return show;
    }

    public Seat getSeat() {
        return seat;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getHoldToken() {
        return holdToken;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public Booking getBooking() {
        return booking;
    }

    public boolean isExpired(Instant now) {
        return status == SeatStatus.HELD && holdExpiresAt != null && !holdExpiresAt.isAfter(now);
    }

    public void hold(String token, Instant expiresAt) {
        this.status = SeatStatus.HELD;
        this.holdToken = token;
        this.holdExpiresAt = expiresAt;
        this.booking = null;
    }

    public void book(Booking booking) {
        this.status = SeatStatus.BOOKED;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.booking = booking;
    }

    public void release() {
        this.status = SeatStatus.AVAILABLE;
        this.holdToken = null;
        this.holdExpiresAt = null;
        this.booking = null;
    }
}
