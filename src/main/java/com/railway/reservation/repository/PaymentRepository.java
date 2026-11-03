package com.railway.reservation.repository;

import com.railway.reservation.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByTxnRef(String txnRef);
}
