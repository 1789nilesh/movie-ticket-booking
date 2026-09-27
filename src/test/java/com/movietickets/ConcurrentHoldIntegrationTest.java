package com.movietickets;

import static org.assertj.core.api.Assertions.assertThat;

import com.movietickets.api.DomainException;
import com.movietickets.domain.Show;
import com.movietickets.domain.ShowSeat;
import com.movietickets.service.BookingService;
import com.movietickets.service.CatalogService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest
class ConcurrentHoldIntegrationTest {

    @Autowired
    CatalogService catalog;

    @Autowired
    BookingService bookings;

    @Test
    void onlyOneConcurrentHoldSucceeds() throws Exception {
        Show show = TestCatalog.seedShow(catalog);
        Long seatId = bookings.seats(show.getId()).stream().map(ShowSeat::getId).findFirst().orElseThrow();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        Runnable attempt = () -> {
            try {
                ready.countDown();
                start.await();
                bookings.createHold(show.getId(), "sam@example.com", List.of(seatId));
                successes.incrementAndGet();
            } catch (DomainException ex) {
                if (ex.getStatus() == HttpStatus.CONFLICT) {
                    conflicts.incrementAndGet();
                } else {
                    throw ex;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        };

        pool.submit(attempt);
        pool.submit(attempt);
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
    }
}
