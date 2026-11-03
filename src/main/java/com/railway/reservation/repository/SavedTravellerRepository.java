package com.railway.reservation.repository;

import com.railway.reservation.model.SavedTraveller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedTravellerRepository extends JpaRepository<SavedTraveller, Long> {
    List<SavedTraveller> findByUserIdOrderByNameAsc(Long userId);
    Optional<SavedTraveller> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
