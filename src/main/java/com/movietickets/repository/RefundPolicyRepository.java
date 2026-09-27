package com.movietickets.repository;

import com.movietickets.domain.RefundPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
    List<RefundPolicy> findAllByOrderByHoursBeforeShowDesc();
    Optional<RefundPolicy> findByHoursBeforeShow(Integer hoursBeforeShow);
}
