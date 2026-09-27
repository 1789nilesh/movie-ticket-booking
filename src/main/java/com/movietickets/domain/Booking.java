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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String customerEmail;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    @Column(nullable = false)
    private Instant bookedAt;

    @Column(nullable = false)
    private int originalTotalPriceCents;

    private int discountAmountCents;

    private String discountCode;

    @Column(nullable = false)
    private int totalPriceCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    private Instant cancelledAt;

    private int refundCents;

    @OneToMany(mappedBy = "booking")
    private List<ShowSeat> seats = new ArrayList<>();

    protected Booking() {
    }

    public Booking(String reference, String customerEmail, Show show, Instant bookedAt, int originalTotalPriceCents, int discountAmountCents, String discountCode, int totalPriceCents) {
        this.reference = reference;
        this.customerEmail = customerEmail;
        this.show = show;
        this.bookedAt = bookedAt;
        this.originalTotalPriceCents = originalTotalPriceCents;
        this.discountAmountCents = discountAmountCents;
        this.discountCode = discountCode;
        this.totalPriceCents = totalPriceCents;
        this.status = BookingStatus.CONFIRMED;
    }

    public Booking(String reference, String customerEmail, Show show, Instant bookedAt, int totalPriceCents) {
        this(reference, customerEmail, show, bookedAt, totalPriceCents, 0, null, totalPriceCents);
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public Show getShow() {
        return show;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public int getOriginalTotalPriceCents() {
        return originalTotalPriceCents > 0 ? originalTotalPriceCents : totalPriceCents;
    }

    public int getDiscountAmountCents() {
        return discountAmountCents;
    }

    public String getDiscountCode() {
        return discountCode;
    }

    public int getTotalPriceCents() {
        return totalPriceCents;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public int getRefundCents() {
        return refundCents;
    }

    public List<ShowSeat> getSeats() {
        return seats;
    }

    public void cancel(Instant cancelledAt, int refundCents) {
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.refundCents = refundCents;
    }
}
