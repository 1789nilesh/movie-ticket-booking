package com.movietickets.repository;

import com.movietickets.domain.DiscountCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
