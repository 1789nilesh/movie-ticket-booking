package com.movietickets.repository;

import com.movietickets.domain.Show;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowRepository extends JpaRepository<Show, Long> {
    @Query("""
            select s from Show s
            join fetch s.movie
            join fetch s.auditorium a
            join fetch a.theater
            where a.theater.city.id = :cityId and s.startsAt >= :from
            order by s.startsAt
            """)
    List<Show> findUpcomingByCity(@Param("cityId") Long cityId, @Param("from") Instant from);
}
