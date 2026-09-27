package com.movietickets.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShowSeatExpiryTest {

    @Test
    void heldSeatExpiresAtDeadline() {
        ShowSeat seat = new ShowSeat();
        Instant deadline = Instant.parse("2026-10-01T12:00:00Z");
        seat.hold("token", deadline);

        assertThat(seat.isExpired(deadline)).isTrue();
        assertThat(seat.isExpired(deadline.minusSeconds(1))).isFalse();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void releaseReturnsSeatToAvailable() {
        ShowSeat seat = new ShowSeat();
        seat.hold("token", Instant.parse("2026-10-01T12:00:00Z"));
        seat.release();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seat.getHoldToken()).isNull();
        assertThat(seat.isExpired(Instant.parse("2026-10-01T13:00:00Z"))).isFalse();
    }
}
