package com.railway.reservation.repository;

import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("SELECT DISTINCT b FROM Booking b JOIN FETCH b.train t " +
           "LEFT JOIN FETCH b.passengers LEFT JOIN FETCH b.payment " +
           "WHERE (:trainNo IS NULL OR t.trainNo = :trainNo) " +
           "AND (:journeyDate IS NULL OR b.journeyDate = :journeyDate) " +
           "ORDER BY b.createdAt")
    List<Booking> findForAdminReport(
        @Param("trainNo") String trainNo,
        @Param("journeyDate") LocalDate journeyDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.pnr = :pnr")
    Optional<Booking> findByPnrForUpdate(@Param("pnr") String pnr);

    @Query("SELECT p.seatNo FROM Booking b JOIN b.passengers p " +
           "WHERE b.train.id = :trainId AND b.trainClass.id = :classId " +
           "AND b.journeyDate = :journeyDate AND b.status IN (:activeStatuses)")
    List<String> findActiveSeatNumbers(
        @Param("trainId") Long trainId,
        @Param("classId") Long classId,
        @Param("journeyDate") LocalDate journeyDate,
        @Param("activeStatuses") List<BookingStatus> activeStatuses
    );

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

