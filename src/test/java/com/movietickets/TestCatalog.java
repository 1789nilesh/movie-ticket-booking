package com.movietickets;

import com.movietickets.domain.Show;
import com.movietickets.service.CatalogService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

final class TestCatalog {

    private TestCatalog() {
    }

    static Show seedShow(CatalogService catalog) {
        var city = catalog.createCity("Bengaluru-" + UUID.randomUUID());
        var theater = catalog.createTheater("PVR Koramangala", city.getId());
        var auditorium = catalog.createAuditorium("Screen 1", theater.getId());
        catalog.createSeat(auditorium.getId(), "A", 1, "REGULAR");
        catalog.createSeat(auditorium.getId(), "A", 2, "PREMIUM");
        var movie = catalog.createMovie("Dune", 155);
        return catalog.createShow(
                movie.getId(),
                auditorium.getId(),
                Instant.now().plus(2, ChronoUnit.DAYS),
                25000,
                45000);
    }
}
