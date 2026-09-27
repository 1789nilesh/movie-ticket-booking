package com.movietickets.repository;

import com.movietickets.domain.SeatHold;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {
    Optional<SeatHold> findByToken(String token);

    @Query("select h from SeatHold h where h.status = com.movietickets.domain.HoldStatus.ACTIVE and h.expiresAt <= :now")
    List<SeatHold> findExpired(@Param("now") Instant now);
}
