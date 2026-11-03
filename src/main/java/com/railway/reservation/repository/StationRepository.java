package com.railway.reservation.repository;

import com.railway.reservation.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByCodeIgnoreCase(String code);
    List<Station> findByCityIgnoreCase(String city);
    List<Station> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
}
