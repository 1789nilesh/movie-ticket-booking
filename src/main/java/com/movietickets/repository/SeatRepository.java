package com.movietickets.repository;

import com.movietickets.domain.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByAuditoriumIdOrderByRowLabelAscNumberAsc(Long auditoriumId);
}
