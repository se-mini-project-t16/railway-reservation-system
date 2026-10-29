package com.railway.reservation.repository;

import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPnr(String pnr);
    Optional<Booking> findByPnrAndUserId(String pnr, Long userId);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByTrainIdAndJourneyDate(Long trainId, LocalDate journeyDate);
    boolean existsByTrainId(Long trainId);

    @Query("SELECT COALESCE(SUM(SIZE(b.passengers)), 0) FROM Booking b " +
           "WHERE b.train.id = :trainId AND b.trainClass.id = :classId " +
           "AND b.journeyDate = :journeyDate AND b.status IN (:activeStatuses)")
    int countBookedSeats(
        @Param("trainId") Long trainId,
        @Param("classId") Long classId,
        @Param("journeyDate") LocalDate journeyDate,
        @Param("activeStatuses") List<BookingStatus> activeStatuses
    );
}

