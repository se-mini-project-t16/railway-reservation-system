package com.railway.reservation.config;

import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class TrainDataInitializer implements CommandLineRunner {

    private final TrainRepository trainRepository;

    public TrainDataInitializer(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Override
    public void run(String... args) {
        if (trainRepository.count() == 0) {
            Train vandeBharat = new Train("20607", "Vande Bharat Express", "Chennai", "Bengaluru",
                    LocalTime.of(5, 45), LocalTime.of(10, 10));
            vandeBharat.addClass(new TrainClass("CC", 100, new BigDecimal("920.00")));
            vandeBharat.addClass(new TrainClass("EC", 40, new BigDecimal("1880.00")));
            trainRepository.save(vandeBharat);

            Train rajdhani = new Train("12951", "Tejas Rajdhani", "Mumbai", "Delhi",
                    LocalTime.of(17, 0), LocalTime.of(8, 32));
            rajdhani.addClass(new TrainClass("3A", 120, new BigDecimal("1800.00")));
            rajdhani.addClass(new TrainClass("2A", 60, new BigDecimal("2400.00")));
            rajdhani.addClass(new TrainClass("1A", 30, new BigDecimal("3200.00")));
            trainRepository.save(rajdhani);
        }
    }
}
