package com.movietickets.service;

import com.movietickets.api.DomainException;
import com.movietickets.domain.Booking;
import com.movietickets.domain.BookingStatus;
import com.movietickets.domain.DiscountCode;
import com.movietickets.domain.HoldStatus;
import com.movietickets.domain.PricingService;
import com.movietickets.domain.PricingService.PriceBreakdown;
import com.movietickets.domain.RefundPolicy;
import com.movietickets.domain.SeatHold;
import com.movietickets.domain.SeatStatus;
import com.movietickets.domain.Show;
import com.movietickets.domain.ShowSeat;
import com.movietickets.repository.BookingRepository;
import com.movietickets.repository.DiscountCodeRepository;
import com.movietickets.repository.RefundPolicyRepository;
import com.movietickets.repository.SeatHoldRepository;
import com.movietickets.repository.ShowRepository;
import com.movietickets.repository.ShowSeatRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final ShowRepository shows;
    private final ShowSeatRepository showSeats;
    private final SeatHoldRepository holds;
    private final BookingRepository bookings;
    private final DiscountCodeRepository discountCodes;
    private final RefundPolicyRepository refundPolicies;
    private final PricingService pricingService;
    private final Duration holdDuration;

    public BookingService(
            ShowRepository shows,
            ShowSeatRepository showSeats,
            SeatHoldRepository holds,
            BookingRepository bookings,
            DiscountCodeRepository discountCodes,
            RefundPolicyRepository refundPolicies,
            PricingService pricingService,
            @Value("${booking.hold-minutes:5}") long holdMinutes) {
        this.shows = shows;
        this.showSeats = showSeats;
        this.holds = holds;
        this.bookings = bookings;
        this.discountCodes = discountCodes;
        this.refundPolicies = refundPolicies;
        this.pricingService = pricingService;
        this.holdDuration = Duration.ofMinutes(holdMinutes);
    }

    @Transactional(readOnly = true)
    public List<ShowSeat> seats(Long showId) {
        requireShow(showId);
        return showSeats.findByShowIdOrderBySeatRowLabelAscSeatNumberAsc(showId);
    }

    @Transactional(noRollbackFor = DomainException.class)
    public SeatHold createHold(Long showId, String customerEmail, List<Long> showSeatIds) {
        if (showSeatIds == null || showSeatIds.isEmpty()) {
            throw badRequest("At least one seat must be selected");
        }
        if (new HashSet<>(showSeatIds).size() != showSeatIds.size()) {
            throw badRequest("A seat can only be selected once");
        }
        Show show = requireShow(showId);
        Instant now = Instant.now();
        List<ShowSeat> selected = showSeats.lockForUpdate(showId, showSeatIds);
        if (selected.size() != showSeatIds.size()) {
            throw badRequest("One or more seats do not belong to this show");
        }
        selected.forEach(seat -> {
            if (seat.isExpired(now)) {
                seat.release();
            }
        });
        if (selected.stream().anyMatch(seat -> seat.getStatus() != SeatStatus.AVAILABLE)) {
            throw new DomainException(HttpStatus.CONFLICT, "One or more requested seats are no longer available");
        }
        Instant expiresAt = now.plus(holdDuration);
        String token = UUID.randomUUID().toString();
        selected.forEach(seat -> seat.hold(token, expiresAt));
        return holds.save(new SeatHold(token, customerEmail, show, expiresAt));
    }

    @Transactional(noRollbackFor = DomainException.class)
    public Booking confirm(String token, String customerEmail, String discountCode) {
        SeatHold hold = holds.findByToken(token)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Hold was not found"));
        if (!hold.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "This hold belongs to another customer");
        }
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new DomainException(HttpStatus.CONFLICT, "Hold has already been used");
        }
        List<ShowSeat> heldSeats = showSeats.findByHoldTokenOrderById(token);
        if (heldSeats.isEmpty()) {
            throw new DomainException(HttpStatus.CONFLICT, "Hold does not own any selectable seats");
        }
        List<Long> ids = heldSeats.stream().map(ShowSeat::getId).toList();
        heldSeats = showSeats.lockForUpdate(hold.getShow().getId(), ids);
        Instant now = Instant.now();
        boolean expired = hold.isExpired(now)
                || heldSeats.size() != ids.size()
                || heldSeats.stream().anyMatch(seat -> !token.equals(seat.getHoldToken()) || seat.isExpired(now));
        if (expired) {
            heldSeats.forEach(ShowSeat::release);
            hold.expire();
            throw new DomainException(HttpStatus.GONE, "Hold has expired; please select seats again");
        }

        PriceBreakdown breakdown = pricingService.calculatePrice(heldSeats, discountCode, now);

        if (breakdown.discountCode() != null) {
            DiscountCode dc = discountCodes.findByCodeIgnoreCase(breakdown.discountCode()).orElse(null);
            if (dc != null) {
                dc.incrementUses();
                discountCodes.save(dc);
            }
        }

        Booking booking = bookings.save(new Booking(
                "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                customerEmail,
                hold.getShow(),
                now,
                breakdown.originalTotalCents(),
                breakdown.discountCents(),
                breakdown.discountCode(),
                breakdown.finalTotalCents()));

        heldSeats.forEach(seat -> seat.book(booking));
        hold.confirm();
        booking.getShow().getId();
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> customerBookings(String customerEmail) {
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw badRequest("Customer email must be provided");
        }
        List<Booking> list = bookings.findByCustomerEmailIgnoreCaseOrderByBookedAtDesc(customerEmail.trim());
        list.forEach(b -> {
            b.getShow().getMovie().getTitle();
            b.getShow().getAuditorium().getTheater().getName();
            b.getSeats().size();
        });
        return list;
    }

    @Transactional
    public Booking cancelBooking(String reference, String customerEmail) {
        Booking booking = bookings.findByReference(reference)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Booking " + reference + " was not found"));

        if (!booking.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Booking does not belong to this customer");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new DomainException(HttpStatus.CONFLICT, "Booking is already cancelled");
        }

        Instant now = Instant.now();
        Instant showStart = booking.getShow().getStartsAt();
        long hoursUntilShow = Duration.between(now, showStart).toHours();

        int refundPercentage = calculateRefundPercentage(hoursUntilShow);
        int refundCents = (booking.getTotalPriceCents() * refundPercentage) / 100;

        booking.cancel(now, refundCents);

        List<ShowSeat> bookedSeats = showSeats.findByBookingId(booking.getId());
        if (!bookedSeats.isEmpty()) {
            List<Long> ids = bookedSeats.stream().map(ShowSeat::getId).toList();
            List<ShowSeat> locked = showSeats.lockForUpdate(booking.getShow().getId(), ids);
            locked.forEach(ShowSeat::release);
        }

        return bookings.save(booking);
    }

    private int calculateRefundPercentage(long hoursUntilShow) {
        List<RefundPolicy> policies = refundPolicies.findAllByOrderByHoursBeforeShowDesc();
        if (!policies.isEmpty()) {
            for (RefundPolicy policy : policies) {
                if (hoursUntilShow >= policy.getHoursBeforeShow()) {
                    return policy.getRefundPercentage();
                }
            }
            return 0;
        }

        if (hoursUntilShow >= 24) {
            return 100;
        } else if (hoursUntilShow >= 2) {
            return 50;
        } else {
            return 0;
        }
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void expireHolds() {
        Instant now = Instant.now();
        showSeats.findExpired(now).forEach(ShowSeat::release);
        holds.findExpired(now).forEach(SeatHold::expire);
    }

    private Show requireShow(Long id) {
        return shows.findById(id).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Show " + id + " was not found"));
    }

    private static DomainException badRequest(String message) {
        return new DomainException(HttpStatus.BAD_REQUEST, message);
    }
}
