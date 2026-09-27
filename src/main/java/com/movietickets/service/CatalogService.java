package com.movietickets.service;

import com.movietickets.api.DomainException;
import com.movietickets.domain.Auditorium;
import com.movietickets.domain.City;
import com.movietickets.domain.DiscountCode;
import com.movietickets.domain.Movie;
import com.movietickets.domain.RefundPolicy;
import com.movietickets.domain.Seat;
import com.movietickets.domain.Show;
import com.movietickets.domain.ShowSeat;
import com.movietickets.domain.Theater;
import com.movietickets.repository.AuditoriumRepository;
import com.movietickets.repository.CityRepository;
import com.movietickets.repository.DiscountCodeRepository;
import com.movietickets.repository.MovieRepository;
import com.movietickets.repository.RefundPolicyRepository;
import com.movietickets.repository.SeatRepository;
import com.movietickets.repository.ShowRepository;
import com.movietickets.repository.ShowSeatRepository;
import com.movietickets.repository.TheaterRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final CityRepository cities;
    private final TheaterRepository theaters;
    private final AuditoriumRepository auditoriums;
    private final SeatRepository seats;
    private final MovieRepository movies;
    private final ShowRepository shows;
    private final ShowSeatRepository showSeats;
    private final DiscountCodeRepository discountCodes;
    private final RefundPolicyRepository refundPolicies;

    public CatalogService(
            CityRepository cities,
            TheaterRepository theaters,
            AuditoriumRepository auditoriums,
            SeatRepository seats,
            MovieRepository movies,
            ShowRepository shows,
            ShowSeatRepository showSeats,
            DiscountCodeRepository discountCodes,
            RefundPolicyRepository refundPolicies) {
        this.cities = cities;
        this.theaters = theaters;
        this.auditoriums = auditoriums;
        this.seats = seats;
        this.movies = movies;
        this.shows = shows;
        this.showSeats = showSeats;
        this.discountCodes = discountCodes;
        this.refundPolicies = refundPolicies;
    }

    @Transactional(readOnly = true)
    public List<City> cities() {
        return cities.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Show> showsForCity(Long cityId) {
        requireCity(cityId);
        return shows.findUpcomingByCity(cityId, Instant.now());
    }

    @Transactional
    public City createCity(String name) {
        String trimmed = name.trim();
        if (cities.existsByNameIgnoreCase(trimmed)) {
            throw new DomainException(HttpStatus.CONFLICT, "City '" + trimmed + "' already exists");
        }
        return cities.save(new City(trimmed));
    }

    @Transactional
    public Theater createTheater(String name, Long cityId) {
        return theaters.save(new Theater(name.trim(), requireCity(cityId)));
    }

    @Transactional
    public Auditorium createAuditorium(String name, Long theaterId) {
        Theater theater = theaters.findById(theaterId)
                .orElseThrow(() -> notFound("Theater " + theaterId + " was not found"));
        return auditoriums.save(new Auditorium(name.trim(), theater));
    }

    @Transactional
    public Seat createSeat(Long auditoriumId, String rowLabel, Integer number, String tier) {
        Auditorium auditorium = requireAuditorium(auditoriumId);
        return seats.save(new Seat(auditorium, rowLabel.trim().toUpperCase(), number, tier));
    }

    @Transactional
    public Movie createMovie(String title, Integer durationMinutes) {
        return movies.save(new Movie(title.trim(), durationMinutes));
    }

    @Transactional
    public Show createShow(Long movieId, Long auditoriumId, Instant startsAt, int regularPriceCents, int premiumPriceCents) {
        Movie movie = movies.findById(movieId)
                .orElseThrow(() -> notFound("Movie " + movieId + " was not found"));
        Auditorium auditorium = requireAuditorium(auditoriumId);
        List<Seat> layout = seats.findByAuditoriumIdOrderByRowLabelAscNumberAsc(auditoriumId);
        if (layout.isEmpty()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Auditorium has no seats to snapshot for the show");
        }
        Show show = shows.save(new Show(movie, auditorium, startsAt, regularPriceCents, premiumPriceCents));
        showSeats.saveAll(layout.stream().map(seat -> new ShowSeat(show, seat)).toList());
        show.getMovie().getTitle();
        show.getAuditorium().getTheater().getName();
        return show;
    }

    @Transactional
    public DiscountCode createDiscountCode(String code, Integer discountPercentage, Integer flatDiscountCents, Instant validUntil, Integer maxUses) {
        String upperCode = code.trim().toUpperCase();
        if (discountCodes.existsByCodeIgnoreCase(upperCode)) {
            throw new DomainException(HttpStatus.CONFLICT, "Discount code '" + upperCode + "' already exists");
        }
        return discountCodes.save(new DiscountCode(upperCode, discountPercentage, flatDiscountCents, validUntil, maxUses));
    }

    @Transactional(readOnly = true)
    public List<DiscountCode> discountCodes() {
        return discountCodes.findAll();
    }

    @Transactional
    public RefundPolicy createRefundPolicy(Integer hoursBeforeShow, Integer refundPercentage) {
        RefundPolicy policy = refundPolicies.findByHoursBeforeShow(hoursBeforeShow)
                .orElseGet(() -> new RefundPolicy(hoursBeforeShow, refundPercentage));
        policy.setRefundPercentage(refundPercentage);
        return refundPolicies.save(policy);
    }

    @Transactional(readOnly = true)
    public List<RefundPolicy> refundPolicies() {
        return refundPolicies.findAllByOrderByHoursBeforeShowDesc();
    }

    private City requireCity(Long cityId) {
        return cities.findById(cityId).orElseThrow(() -> notFound("City " + cityId + " was not found"));
    }

    private Auditorium requireAuditorium(Long auditoriumId) {
        return auditoriums.findById(auditoriumId)
                .orElseThrow(() -> notFound("Auditorium " + auditoriumId + " was not found"));
    }

    private static DomainException notFound(String message) {
        return new DomainException(HttpStatus.NOT_FOUND, message);
    }
}
