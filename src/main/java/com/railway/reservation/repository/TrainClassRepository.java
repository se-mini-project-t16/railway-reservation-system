package com.railway.reservation.repository;

import com.railway.reservation.model.TrainClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainClassRepository extends JpaRepository<TrainClass, Long> {
    List<TrainClass> findByTrainId(Long trainId);
    Optional<TrainClass> findByTrainIdAndClassNameIgnoreCase(Long trainId, String className);
}

