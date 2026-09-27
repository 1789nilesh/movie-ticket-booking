package com.movietickets.api;

import com.movietickets.domain.Auditorium;
import com.movietickets.domain.City;
import com.movietickets.domain.DiscountCode;
import com.movietickets.domain.Movie;
import com.movietickets.domain.RefundPolicy;
import com.movietickets.domain.Seat;
import com.movietickets.domain.Show;
import com.movietickets.domain.Theater;
import com.movietickets.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;
    private final RoleGuard roles;

    public CatalogController(CatalogService catalog, RoleGuard roles) {
        this.catalog = catalog;
        this.roles = roles;
    }

    @GetMapping("/cities")
    List<CityView> cities() {
        return catalog.cities().stream().map(city -> new CityView(city.getId(), city.getName())).toList();
    }

    @GetMapping("/cities/{cityId}/shows")
    List<ShowView> shows(@PathVariable Long cityId) {
        return catalog.showsForCity(cityId).stream().map(this::showView).toList();
    }

    @PostMapping("/admin/cities")
    @ResponseStatus(HttpStatus.CREATED)
    CityView city(@RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateCity request) {
        roles.requireAdmin(role);
        City city = catalog.createCity(request.name());
        return new CityView(city.getId(), city.getName());
    }

    @PostMapping("/admin/theaters")
    @ResponseStatus(HttpStatus.CREATED)
    TheaterView theater(
            @RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateTheater request) {
        roles.requireAdmin(role);
        Theater theater = catalog.createTheater(request.name(), request.cityId());
        return new TheaterView(theater.getId(), theater.getName(), theater.getCity().getId());
    }

    @PostMapping("/admin/auditoriums")
    @ResponseStatus(HttpStatus.CREATED)
    AuditoriumView auditorium(
            @RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateAuditorium request) {
        roles.requireAdmin(role);
        Auditorium auditorium = catalog.createAuditorium(request.name(), request.theaterId());
        return new AuditoriumView(auditorium.getId(), auditorium.getName(), auditorium.getTheater().getId());
    }

    @PostMapping("/admin/seats")
    @ResponseStatus(HttpStatus.CREATED)
    SeatView seat(@RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateSeat request) {
        roles.requireAdmin(role);
        Seat created = catalog.createSeat(request.auditoriumId(), request.rowLabel(), request.number(), request.tier());
        return new SeatView(created.getId(), created.getRowLabel(), created.getNumber(), created.getTier());
    }

    @PostMapping("/admin/movies")
    @ResponseStatus(HttpStatus.CREATED)
    MovieView movie(@RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateMovie request) {
        roles.requireAdmin(role);
        Movie created = catalog.createMovie(request.title(), request.durationMinutes());
        return new MovieView(created.getId(), created.getTitle(), created.getDurationMinutes());
    }

    @PostMapping("/admin/shows")
    @ResponseStatus(HttpStatus.CREATED)
    ShowView show(@RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateShow request) {
        roles.requireAdmin(role);
        return showView(catalog.createShow(
                request.movieId(),
                request.auditoriumId(),
                request.startsAt(),
                request.regularPriceCents(),
                request.premiumPriceCents()));
    }

    @PostMapping("/admin/discounts")
    @ResponseStatus(HttpStatus.CREATED)
    DiscountView discount(
            @RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateDiscount request) {
        roles.requireAdmin(role);
        DiscountCode created = catalog.createDiscountCode(
                request.code(),
                request.discountPercentage(),
                request.flatDiscountCents(),
                request.validUntil(),
                request.maxUses());
        return discountView(created);
    }

    @GetMapping("/admin/discounts")
    List<DiscountView> discounts(@RequestHeader(value = "X-Role", defaultValue = "") String role) {
        roles.requireAdmin(role);
        return catalog.discountCodes().stream().map(this::discountView).toList();
    }

    @PostMapping("/admin/refund-policies")
    @ResponseStatus(HttpStatus.CREATED)
    RefundPolicyView refundPolicy(
            @RequestHeader(value = "X-Role", defaultValue = "") String role, @Valid @RequestBody CreateRefundPolicy request) {
        roles.requireAdmin(role);
        RefundPolicy policy = catalog.createRefundPolicy(request.hoursBeforeShow(), request.refundPercentage());
        return new RefundPolicyView(policy.getId(), policy.getHoursBeforeShow(), policy.getRefundPercentage());
    }

    @GetMapping("/admin/refund-policies")
    List<RefundPolicyView> refundPolicies(@RequestHeader(value = "X-Role", defaultValue = "") String role) {
        roles.requireAdmin(role);
        return catalog.refundPolicies().stream()
                .map(p -> new RefundPolicyView(p.getId(), p.getHoursBeforeShow(), p.getRefundPercentage()))
                .toList();
    }

    private ShowView showView(Show show) {
        return new ShowView(
                show.getId(),
                show.getMovie().getId(),
                show.getMovie().getTitle(),
                show.getAuditorium().getId(),
                show.getAuditorium().getTheater().getName(),
                show.getStartsAt(),
                show.getRegularPriceCents(),
                show.getPremiumPriceCents());
    }

    private DiscountView discountView(DiscountCode d) {
        return new DiscountView(
                d.getId(),
                d.getCode(),
                d.getDiscountPercentage(),
                d.getFlatDiscountCents(),
                d.getValidUntil(),
                d.getMaxUses(),
                d.getCurrentUses(),
                d.isActive());
    }

    record CreateCity(@NotBlank String name) {}

    record CreateTheater(@NotBlank String name, @NotNull Long cityId) {}

    record CreateAuditorium(@NotBlank String name, @NotNull Long theaterId) {}

    record CreateSeat(
            @NotNull Long auditoriumId,
            @NotBlank String rowLabel,
            @NotNull @Positive Integer number,
            @NotBlank @Pattern(regexp = "REGULAR|PREMIUM", flags = Pattern.Flag.CASE_INSENSITIVE) String tier) {}

    record CreateMovie(@NotBlank String title, @NotNull @Positive Integer durationMinutes) {}

    record CreateShow(
            @NotNull Long movieId,
            @NotNull Long auditoriumId,
            @NotNull @Future Instant startsAt,
            @Positive int regularPriceCents,
            @Positive int premiumPriceCents) {}

    record CreateDiscount(
            @NotBlank String code,
            @Min(0) @Max(100) Integer discountPercentage,
            @PositiveOrZero Integer flatDiscountCents,
            Instant validUntil,
            @Positive Integer maxUses) {}

    record CreateRefundPolicy(
            @NotNull @PositiveOrZero Integer hoursBeforeShow,
            @NotNull @Min(0) @Max(100) Integer refundPercentage) {}

    record CityView(Long id, String name) {}

    record TheaterView(Long id, String name, Long cityId) {}

    record AuditoriumView(Long id, String name, Long theaterId) {}

    record SeatView(Long id, String rowLabel, Integer number, String tier) {}

    record MovieView(Long id, String title, Integer durationMinutes) {}

    record ShowView(
            Long id,
            Long movieId,
            String movieTitle,
            Long auditoriumId,
            String theaterName,
            Instant startsAt,
            int regularPriceCents,
            int premiumPriceCents) {}

    record DiscountView(
            Long id,
            String code,
            Integer discountPercentage,
            Integer flatDiscountCents,
            Instant validUntil,
            Integer maxUses,
            int currentUses,
            boolean active) {}

    record RefundPolicyView(Long id, Integer hoursBeforeShow, Integer refundPercentage) {}
}
