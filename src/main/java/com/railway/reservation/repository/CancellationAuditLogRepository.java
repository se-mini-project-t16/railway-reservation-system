package com.railway.reservation.repository;

import com.railway.reservation.model.CancellationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CancellationAuditLogRepository extends JpaRepository<CancellationAuditLog, Long> {
    List<CancellationAuditLog> findByPnrOrderByCancelledAtAsc(String pnr);
    List<CancellationAuditLog> findByCancelledAtBetween(LocalDateTime from, LocalDateTime to);
}
