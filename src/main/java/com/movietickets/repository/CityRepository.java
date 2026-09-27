package com.movietickets.repository;

import com.movietickets.domain.City;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
