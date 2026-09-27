package com.movietickets.domain;

import com.movietickets.api.DomainException;
import com.movietickets.repository.DiscountCodeRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private final DiscountCodeRepository discountCodes;
    private final double weekendMultiplier;

    public PricingService(
            DiscountCodeRepository discountCodes,
            @Value("${pricing.weekend-multiplier:1.20}") double weekendMultiplier) {
        this.discountCodes = discountCodes;
        this.weekendMultiplier = weekendMultiplier;
    }

    public PriceBreakdown calculatePrice(List<ShowSeat> seats, String discountCodeInput, Instant now) {
        if (seats == null || seats.isEmpty()) {
            return new PriceBreakdown(0, 0, 0, null);
        }

        Show show = seats.get(0).getShow();
        boolean isWeekend = isWeekendShow(show.getStartsAt());

        int originalTotal = 0;
        for (ShowSeat seat : seats) {
            int basePrice = seat.getSeat().isPremium()
                    ? show.getPremiumPriceCents()
                    : show.getRegularPriceCents();

            if (isWeekend) {
                basePrice = (int) Math.round(basePrice * weekendMultiplier);
            }
            originalTotal += basePrice;
        }

        int discountAmount = 0;
        DiscountCode validDiscount = null;

        if (discountCodeInput != null && !discountCodeInput.trim().isEmpty()) {
            String codeStr = discountCodeInput.trim().toUpperCase();
            validDiscount = discountCodes.findByCodeIgnoreCase(codeStr)
                    .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "Invalid discount code: " + codeStr));

            if (!validDiscount.isValid(now)) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "Discount code '" + codeStr + "' is expired or no longer valid");
            }

            if (validDiscount.getDiscountPercentage() != null) {
                discountAmount += (originalTotal * validDiscount.getDiscountPercentage()) / 100;
            }
            if (validDiscount.getFlatDiscountCents() != null) {
                discountAmount += validDiscount.getFlatDiscountCents();
            }

            if (discountAmount > originalTotal) {
                discountAmount = originalTotal;
            }
        }

        int finalTotal = originalTotal - discountAmount;
        String appliedCode = validDiscount != null ? validDiscount.getCode() : null;

        return new PriceBreakdown(originalTotal, discountAmount, finalTotal, appliedCode);
    }

    public boolean isWeekendShow(Instant startsAt) {
        ZonedDateTime zdt = startsAt.atZone(ZoneId.of("UTC"));
        DayOfWeek dow = zdt.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public record PriceBreakdown(int originalTotalCents, int discountCents, int finalTotalCents, String discountCode) {}
}
