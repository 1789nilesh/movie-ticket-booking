package com.movietickets.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.movietickets.domain.Auditorium;
import com.movietickets.domain.City;
import com.movietickets.domain.Movie;
import com.movietickets.domain.Seat;
import com.movietickets.domain.Show;
import com.movietickets.domain.ShowSeat;
import com.movietickets.domain.Theater;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PricingTest {

    @Test
    void premiumSeatUsesPremiumPrice() {
        ShowSeat seat = showSeat("PREMIUM");
        assertThat(Pricing.priceCents(seat)).isEqualTo(45000);
    }

    @Test
    void regularSeatUsesRegularPrice() {
        ShowSeat seat = showSeat("REGULAR");
        assertThat(Pricing.priceCents(seat)).isEqualTo(25000);
    }

    private static ShowSeat showSeat(String tier) {
        City city = new City("Bengaluru");
        Theater theater = new Theater("PVR", city);
        Auditorium auditorium = new Auditorium("Screen 1", theater);
        Seat seat = new Seat(auditorium, "A", 1, tier);
        Movie movie = new Movie("Dune", 155);
        Show show = new Show(movie, auditorium, Instant.parse("2026-10-01T18:30:00Z"), 25000, 45000);
        return new ShowSeat(show, seat);
    }
}
