package com.railway.reservation.repository;

import com.railway.reservation.model.CancellationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationRuleRepository extends JpaRepository<CancellationRule, Long> {
    List<CancellationRule> findAllByOrderByHoursBeforeDesc();
}

