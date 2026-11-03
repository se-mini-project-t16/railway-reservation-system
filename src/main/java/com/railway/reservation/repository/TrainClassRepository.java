package com.railway.reservation.repository;

import com.railway.reservation.model.TrainClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrainClassRepository extends JpaRepository<TrainClass, Long> {

    /** Serialises allocation for one class of one train so two bookings never take the same berth. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from TrainClass c where c.id = :id")
    Optional<TrainClass> findByIdForUpdate(@Param("id") Long id);
}
