package com.railway.reservation.repository;

import com.railway.reservation.model.Booking;
import com.railway.reservation.model.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPnr(String pnr);

    boolean existsByPnr(String pnr);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.pnr = :pnr")
    Optional<Booking> findByPnrForUpdate(@Param("pnr") String pnr);

    @Query("select b from Booking b where b.user.id = :userId and b.status in :statuses order by b.departureAt desc")
    List<Booking> findForUser(@Param("userId") Long userId, @Param("statuses") Collection<BookingStatus> statuses);

    @Query("select b.pnr from Booking b where b.status = com.railway.reservation.model.BookingStatus.PAYMENT_PENDING "
            + "and b.paymentDueAt < :now")
    List<String> findExpiredHolds(@Param("now") LocalDateTime now);

    /** Booked tickets whose train has prepared its chart (origin departure within the chart window). */
    @Query("select b.pnr from Booking b where b.status in :statuses and b.trainStartDate >= :fromDate "
            + "and b.departureAt > :now")
    List<String> findLiveDepartingFrom(@Param("statuses") Collection<BookingStatus> statuses,
                                       @Param("fromDate") LocalDate fromDate, @Param("now") LocalDateTime now);

    @Query("select b from Booking b where b.train.id = :trainId and b.coachClass = :cc and b.trainStartDate = :date "
            + "and b.status in :statuses")
    List<Booking> findForInventory(@Param("trainId") Long trainId,
                                   @Param("cc") com.railway.reservation.model.CoachClass cc,
                                   @Param("date") LocalDate date,
                                   @Param("statuses") Collection<BookingStatus> statuses);

    @Query("select b from Booking b join b.user u join b.train t where "
            + "(:pnr is null or b.pnr = :pnr) "
            + "and (:email is null or lower(u.email) like concat('%', :email, '%')) "
            + "and (:trainNo is null or t.number = :trainNo) "
            + "and (:date is null or b.journeyDate = :date) "
            + "and (:status is null or b.status = :status)")
    Page<Booking> search(@Param("pnr") String pnr, @Param("email") String email, @Param("trainNo") String trainNo,
                         @Param("date") LocalDate date, @Param("status") BookingStatus status, Pageable pageable);

    @Query("select b from Booking b where b.bookedAt >= :from and b.bookedAt < :to")
    List<Booking> findBookedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select b from Booking b where b.status in :statuses and b.departureAt >= :from and b.departureAt < :to "
            + "order by b.departureAt")
    List<Booking> findDepartingBetween(@Param("statuses") Collection<BookingStatus> statuses,
                                       @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatus(BookingStatus status);

    long countByTrainId(Long trainId);
}
