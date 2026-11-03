package com.railway.reservation.repository;

import com.railway.reservation.model.ClassInventory;
import com.railway.reservation.model.Quota;
import com.railway.reservation.model.TrainClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ClassInventoryRepository extends JpaRepository<ClassInventory, Long> {
    Optional<ClassInventory> findByTrainClassAndTrainStartDateAndQuota(TrainClass trainClass, LocalDate date, Quota quota);
}
