package com.railway.reservation.repository;

import com.railway.reservation.model.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNo(String trainNo);
    boolean existsByTrainNo(String trainNo);
    List<Train> findBySourceStationIgnoreCaseAndDestinationStationIgnoreCase(String sourceStation, String destinationStation);
}

