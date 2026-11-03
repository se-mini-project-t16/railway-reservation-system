package com.railway.reservation.repository;

import com.railway.reservation.model.CancellationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancellationRuleRepository extends JpaRepository<CancellationRule, Long> {
    List<CancellationRule> findAllByOrderByMinHoursBeforeDesc();
}
