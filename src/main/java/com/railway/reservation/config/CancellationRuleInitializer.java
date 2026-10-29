package com.railway.reservation.config;

import com.railway.reservation.model.CancellationRule;
import com.railway.reservation.repository.CancellationRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the default refund tiers used by RRS-F-013 when the table is empty.
 * These are business rules rather than demo data, so they are always seeded.
 */
@Component
public class CancellationRuleInitializer implements CommandLineRunner {

    private final CancellationRuleRepository cancellationRuleRepository;

    public CancellationRuleInitializer(CancellationRuleRepository cancellationRuleRepository) {
        this.cancellationRuleRepository = cancellationRuleRepository;
    }

    @Override
    public void run(String... args) {
        if (cancellationRuleRepository.count() == 0) {
            cancellationRuleRepository.saveAll(List.of(
                    new CancellationRule(48, new BigDecimal("90.00")),
                    new CancellationRule(24, new BigDecimal("75.00")),
                    new CancellationRule(12, new BigDecimal("50.00")),
                    new CancellationRule(4, new BigDecimal("25.00")),
                    new CancellationRule(0, new BigDecimal("0.00"))
            ));
        }
    }
}
