package com.railway.reservation.repository;

import com.railway.reservation.model.CancellationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancellationAuditLogRepository extends JpaRepository<CancellationAuditLog, Long> {
    List<CancellationAuditLog> findByPnr(String pnr);
}
