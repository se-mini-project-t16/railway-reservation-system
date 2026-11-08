package com.railway.reservation.repository;

import com.railway.reservation.model.PasswordResetCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordResetCode r where r.userId = :userId")
    Optional<PasswordResetCode> findByUserIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Query("delete from PasswordResetCode r where r.lastSentAt < :before")
    int deleteStale(@Param("before") LocalDateTime before);
}
