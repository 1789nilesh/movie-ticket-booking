package com.movietickets.api;

import com.movietickets.domain.Booking;
import com.movietickets.domain.BookingStatus;
import com.movietickets.domain.HoldStatus;
import com.movietickets.domain.SeatHold;
import com.movietickets.domain.SeatStatus;
import com.movietickets.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookings;
    private final RoleGuard roles;

    public BookingController(BookingService bookings, RoleGuard roles) {
        this.bookings = bookings;
        this.roles = roles;
    }

    @GetMapping("/shows/{showId}/seats")
    List<ShowSeatView> seats(@PathVariable Long showId) {
        return bookings.seats(showId).stream()
                .map(seat -> new ShowSeatView(
                        seat.getId(),
                        seat.getSeat().getRowLabel(),
                        seat.getSeat().getNumber(),
                        seat.getSeat().getTier(),
                        seat.getStatus(),
                        seat.getHoldExpiresAt()))
                .toList();
    }

    @PostMapping("/shows/{showId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    HoldView hold(
            @RequestHeader(value = "X-Role", defaultValue = "") String role,
            @PathVariable Long showId,
            @Valid @RequestBody CreateHold request) {
        roles.requireCustomer(role);
        SeatHold created = bookings.createHold(showId, request.customerEmail(), request.showSeatIds());
        return new HoldView(created.getToken(), created.getExpiresAt(), created.getStatus());
    }

    @PostMapping("/holds/{token}/confirm")
    BookingView confirm(
            @RequestHeader(value = "X-Role", defaultValue = "") String role,
            @PathVariable String token,
            @Valid @RequestBody ConfirmHold request) {
        roles.requireCustomer(role);
        Booking booking = bookings.confirm(token, request.customerEmail(), request.discountCode());
        return bookingView(booking);
    }

    @GetMapping("/bookings")
    List<BookingDetailView> history(
            @RequestHeader(value = "X-Role", defaultValue = "") String role,
            @RequestParam @Email @NotBlank String email) {
        roles.requireCustomer(role);
        return bookings.customerBookings(email).stream().map(this::bookingDetailView).toList();
    }

    @PostMapping("/bookings/{reference}/cancel")
    BookingDetailView cancel(
            @RequestHeader(value = "X-Role", defaultValue = "") String role,
            @PathVariable String reference,
            @Valid @RequestBody CancelBooking request) {
        roles.requireCustomer(role);
        Booking cancelled = bookings.cancelBooking(reference, request.customerEmail());
        return bookingDetailView(cancelled);
    }

    private BookingView bookingView(Booking booking) {
        return new BookingView(
                booking.getId(),
                booking.getReference(),
                booking.getCustomerEmail(),
                booking.getShow().getId(),
                booking.getBookedAt(),
                booking.getOriginalTotalPriceCents(),
                booking.getDiscountAmountCents(),
                booking.getDiscountCode(),
                booking.getTotalPriceCents(),
                booking.getStatus());
    }

    private BookingDetailView bookingDetailView(Booking b) {
        List<SeatItemView> seatItems = b.getSeats().stream()
                .map(s -> new SeatItemView(s.getId(), s.getSeat().getRowLabel(), s.getSeat().getNumber(), s.getSeat().getTier()))
                .toList();

        return new BookingDetailView(
                b.getId(),
                b.getReference(),
                b.getCustomerEmail(),
                b.getShow().getId(),
                b.getShow().getMovie().getTitle(),
                b.getShow().getAuditorium().getTheater().getName(),
                b.getShow().getStartsAt(),
                seatItems,
                b.getBookedAt(),
                b.getOriginalTotalPriceCents(),
                b.getDiscountAmountCents(),
                b.getDiscountCode(),
                b.getTotalPriceCents(),
                b.getStatus(),
                b.getCancelledAt(),
                b.getRefundCents());
    }

    record CreateHold(@Email @NotBlank String customerEmail, @NotEmpty List<@NotNull Long> showSeatIds) {}

    record ConfirmHold(@Email @NotBlank String customerEmail, String discountCode) {}

    record CancelBooking(@Email @NotBlank String customerEmail) {}

    record ShowSeatView(Long id, String rowLabel, Integer number, String tier, SeatStatus status, Instant holdExpiresAt) {}

    record HoldView(String token, Instant expiresAt, HoldStatus status) {}

    record BookingView(
            Long id,
            String reference,
            String customerEmail,
            Long showId,
            Instant bookedAt,
            int originalTotalPriceCents,
            int discountAmountCents,
            String discountCode,
            int totalPriceCents,
            BookingStatus status) {}

    record BookingDetailView(
            Long id,
            String reference,
            String customerEmail,
            Long showId,
            String movieTitle,
            String theaterName,
            Instant showStartsAt,
            List<SeatItemView> seats,
            Instant bookedAt,
            int originalTotalPriceCents,
            int discountAmountCents,
            String discountCode,
            int totalPriceCents,
            BookingStatus status,
            Instant cancelledAt,
            int refundCents) {}

    record SeatItemView(Long showSeatId, String rowLabel, Integer number, String tier) {}
}
