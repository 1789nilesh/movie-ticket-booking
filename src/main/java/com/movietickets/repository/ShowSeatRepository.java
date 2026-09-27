package com.movietickets.repository;

import com.movietickets.domain.ShowSeat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {
    @Query("""
            select ss from ShowSeat ss
            join fetch ss.seat
            where ss.show.id = :showId
            order by ss.seat.rowLabel, ss.seat.number
            """)
    List<ShowSeat> findByShowIdOrderBySeatRowLabelAscSeatNumberAsc(@Param("showId") Long showId);

    List<ShowSeat> findByHoldTokenOrderById(String holdToken);

    List<ShowSeat> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowSeat ss where ss.show.id = :showId and ss.id in :ids order by ss.id")
    List<ShowSeat> lockForUpdate(@Param("showId") Long showId, @Param("ids") Collection<Long> ids);

    @Query("select ss from ShowSeat ss where ss.status = com.movietickets.domain.SeatStatus.HELD and ss.holdExpiresAt <= :now")
    List<ShowSeat> findExpired(@Param("now") Instant now);
}
