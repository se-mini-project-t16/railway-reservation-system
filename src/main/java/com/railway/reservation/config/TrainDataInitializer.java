package com.railway.reservation.config;

import com.railway.reservation.model.Train;
import com.railway.reservation.model.TrainClass;
import com.railway.reservation.model.TrainStop;
import com.railway.reservation.repository.TrainRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Portable seed data initializer for Indian Railway routes, station halts, and coach classes (RRS-F-004-EXT).
 * Configures official station codes (MAS, SBC, NDLS, MMCT, HWH, PUNE) and intermediate halt stops.
 */
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
            seedTrains();
        }
    }

    private void seedTrains() {
        // 1. Vande Bharat Express (Chennai -> Bengaluru)
        Train vbUp = new Train("20607", "Vande Bharat Express", "Chennai", "Bengaluru",
                LocalTime.of(5, 45), LocalTime.of(10, 10));
        vbUp.addClass(new TrainClass("CC", 100, new BigDecimal("920.00")));
        vbUp.addClass(new TrainClass("EC", 40, new BigDecimal("1880.00")));
        vbUp.addStop(new TrainStop(1, "MAS", "Chennai Central", null, LocalTime.of(5, 45), 0, 2, 0));
        vbUp.addStop(new TrainStop(2, "KPD", "Katpadi Junction", LocalTime.of(7, 13), LocalTime.of(7, 15), 2, 1, 130));
        vbUp.addStop(new TrainStop(3, "JTJ", "Jolarpettai Junction", LocalTime.of(8, 8), LocalTime.of(8, 10), 2, 2, 214));
        vbUp.addStop(new TrainStop(4, "SBC", "KSR Bengaluru", LocalTime.of(10, 10), null, 0, 1, 359));
        trainRepository.save(vbUp);

        // 2. Vande Bharat Express (Bengaluru -> Chennai)
        Train vbDown = new Train("20608", "Vande Bharat Express", "Bengaluru", "Chennai",
                LocalTime.of(14, 20), LocalTime.of(18, 45));
        vbDown.addClass(new TrainClass("CC", 100, new BigDecimal("920.00")));
        vbDown.addClass(new TrainClass("EC", 40, new BigDecimal("1880.00")));
        vbDown.addStop(new TrainStop(1, "SBC", "KSR Bengaluru", null, LocalTime.of(14, 20), 0, 1, 0));
        vbDown.addStop(new TrainStop(2, "JTJ", "Jolarpettai Junction", LocalTime.of(16, 18), LocalTime.of(16, 20), 2, 1, 145));
        vbDown.addStop(new TrainStop(3, "KPD", "Katpadi Junction", LocalTime.of(17, 13), LocalTime.of(17, 15), 2, 2, 229));
        vbDown.addStop(new TrainStop(4, "MAS", "Chennai Central", LocalTime.of(18, 45), null, 0, 2, 359));
        trainRepository.save(vbDown);

        // 3. Kovai Express (Chennai -> Bengaluru)
        Train kovai = new Train("12675", "Kovai Express", "Chennai", "Bengaluru",
                LocalTime.of(6, 10), LocalTime.of(11, 30));
        kovai.addClass(new TrainClass("CC", 120, new BigDecimal("450.00")));
        kovai.addClass(new TrainClass("2S", 200, new BigDecimal("180.00")));
        kovai.addStop(new TrainStop(1, "MAS", "Chennai Central", null, LocalTime.of(6, 10), 0, 4, 0));
        kovai.addStop(new TrainStop(2, "AJJ", "Arakkonam Junction", LocalTime.of(7, 8), LocalTime.of(7, 10), 2, 1, 69));
        kovai.addStop(new TrainStop(3, "KPD", "Katpadi Junction", LocalTime.of(7, 58), LocalTime.of(8, 0), 2, 1, 130));
        kovai.addStop(new TrainStop(4, "JTJ", "Jolarpettai Junction", LocalTime.of(9, 8), LocalTime.of(9, 10), 2, 3, 214));
        kovai.addStop(new TrainStop(5, "SBC", "KSR Bengaluru", LocalTime.of(11, 30), null, 0, 3, 359));
        trainRepository.save(kovai);

        // 4. Tejas Rajdhani Express (Mumbai -> Delhi)
        Train rajdhaniUp = new Train("12951", "Tejas Rajdhani", "Mumbai", "Delhi",
                LocalTime.of(17, 0), LocalTime.of(8, 32));
        rajdhaniUp.addClass(new TrainClass("3A", 120, new BigDecimal("1800.00")));
        rajdhaniUp.addClass(new TrainClass("2A", 60, new BigDecimal("2400.00")));
        rajdhaniUp.addClass(new TrainClass("1A", 30, new BigDecimal("3200.00")));
        rajdhaniUp.addStop(new TrainStop(1, "MMCT", "Mumbai Central", null, LocalTime.of(17, 0), 0, 1, 0));
        rajdhaniUp.addStop(new TrainStop(2, "ST", "Surat", LocalTime.of(19, 43), LocalTime.of(19, 48), 5, 1, 263));
        rajdhaniUp.addStop(new TrainStop(3, "BRC", "Vadodara Junction", LocalTime.of(21, 6), LocalTime.of(21, 16), 10, 2, 392));
        rajdhaniUp.addStop(new TrainStop(4, "RTM", "Ratlam Junction", LocalTime.of(0, 55), LocalTime.of(0, 58), 3, 1, 653));
        rajdhaniUp.addStop(new TrainStop(5, "KOTA", "Kota Junction", LocalTime.of(3, 55), LocalTime.of(4, 0), 5, 1, 920));
        rajdhaniUp.addStop(new TrainStop(6, "NDLS", "New Delhi", LocalTime.of(8, 32), null, 0, 2, 1386));
        trainRepository.save(rajdhaniUp);

        // 5. Tejas Rajdhani Express (Delhi -> Mumbai)
        Train rajdhaniDown = new Train("12952", "Tejas Rajdhani", "Delhi", "Mumbai",
                LocalTime.of(16, 55), LocalTime.of(8, 35));
        rajdhaniDown.addClass(new TrainClass("3A", 120, new BigDecimal("1800.00")));
        rajdhaniDown.addClass(new TrainClass("2A", 60, new BigDecimal("2400.00")));
        rajdhaniDown.addClass(new TrainClass("1A", 30, new BigDecimal("3200.00")));
        rajdhaniDown.addStop(new TrainStop(1, "NDLS", "New Delhi", null, LocalTime.of(16, 55), 0, 2, 0));
        rajdhaniDown.addStop(new TrainStop(2, "KOTA", "Kota Junction", LocalTime.of(21, 30), LocalTime.of(21, 35), 5, 2, 466));
        rajdhaniDown.addStop(new TrainStop(3, "RTM", "Ratlam Junction", LocalTime.of(0, 35), LocalTime.of(0, 38), 3, 2, 733));
        rajdhaniDown.addStop(new TrainStop(4, "BRC", "Vadodara Junction", LocalTime.of(4, 18), LocalTime.of(4, 28), 10, 1, 994));
        rajdhaniDown.addStop(new TrainStop(5, "ST", "Surat", LocalTime.of(5, 48), LocalTime.of(5, 53), 5, 2, 1123));
        rajdhaniDown.addStop(new TrainStop(6, "MMCT", "Mumbai Central", LocalTime.of(8, 35), null, 0, 1, 1386));
        trainRepository.save(rajdhaniDown);

        // 6. Deccan Queen Express (Mumbai -> Pune)
        Train dqUp = new Train("12123", "Deccan Queen Express", "Mumbai", "Pune",
                LocalTime.of(17, 10), LocalTime.of(20, 25));
        dqUp.addClass(new TrainClass("CC", 150, new BigDecimal("380.00")));
        dqUp.addClass(new TrainClass("2S", 250, new BigDecimal("130.00")));
        dqUp.addStop(new TrainStop(1, "MMCT", "Mumbai Central", null, LocalTime.of(17, 10), 0, 8, 0));
        dqUp.addStop(new TrainStop(2, "KJT", "Karjat Junction", LocalTime.of(18, 33), LocalTime.of(18, 35), 2, 1, 100));
        dqUp.addStop(new TrainStop(3, "LNL", "Lonavala", LocalTime.of(19, 18), LocalTime.of(19, 20), 2, 1, 128));
        dqUp.addStop(new TrainStop(4, "PUNE", "Pune Junction", LocalTime.of(20, 25), null, 0, 5, 192));
        trainRepository.save(dqUp);

        // 7. Deccan Queen Express (Pune -> Mumbai)
        Train dqDown = new Train("12124", "Deccan Queen Express", "Pune", "Mumbai",
                LocalTime.of(7, 15), LocalTime.of(10, 25));
        dqDown.addClass(new TrainClass("CC", 150, new BigDecimal("380.00")));
        dqDown.addClass(new TrainClass("2S", 250, new BigDecimal("130.00")));
        dqDown.addStop(new TrainStop(1, "PUNE", "Pune Junction", null, LocalTime.of(7, 15), 0, 5, 0));
        dqDown.addStop(new TrainStop(2, "LNL", "Lonavala", LocalTime.of(8, 8), LocalTime.of(8, 10), 2, 2, 64));
        dqDown.addStop(new TrainStop(3, "KJT", "Karjat Junction", LocalTime.of(8, 58), LocalTime.of(9, 0), 2, 2, 92));
        dqDown.addStop(new TrainStop(4, "MMCT", "Mumbai Central", LocalTime.of(10, 25), null, 0, 8, 192));
        trainRepository.save(dqDown);

        // 8. Howrah Rajdhani Express (Howrah -> Delhi)
        Train howrahRajUp = new Train("12301", "Howrah Rajdhani Express", "Howrah", "Delhi",
                LocalTime.of(16, 50), LocalTime.of(10, 5));
        howrahRajUp.addClass(new TrainClass("3A", 120, new BigDecimal("1950.00")));
        howrahRajUp.addClass(new TrainClass("2A", 60, new BigDecimal("2650.00")));
        howrahRajUp.addClass(new TrainClass("1A", 30, new BigDecimal("3450.00")));
        howrahRajUp.addStop(new TrainStop(1, "HWH", "Howrah Junction", null, LocalTime.of(16, 50), 0, 9, 0));
        howrahRajUp.addStop(new TrainStop(2, "ASN", "Asansol Junction", LocalTime.of(18, 57), LocalTime.of(19, 0), 3, 1, 200));
        howrahRajUp.addStop(new TrainStop(3, "DHN", "Dhanbad Junction", LocalTime.of(20, 0), LocalTime.of(20, 5), 5, 1, 259));
        howrahRajUp.addStop(new TrainStop(4, "GAYA", "Gaya Junction", LocalTime.of(22, 19), LocalTime.of(22, 22), 3, 1, 458));
        howrahRajUp.addStop(new TrainStop(5, "DDU", "Pt. Deen Dayal Upadhyaya", LocalTime.of(0, 45), LocalTime.of(0, 55), 10, 2, 663));
        howrahRajUp.addStop(new TrainStop(6, "PRYJ", "Prayagraj Junction", LocalTime.of(2, 33), LocalTime.of(2, 35), 2, 1, 816));
        howrahRajUp.addStop(new TrainStop(7, "CNB", "Kanpur Central", LocalTime.of(4, 40), LocalTime.of(4, 45), 5, 1, 1010));
        howrahRajUp.addStop(new TrainStop(8, "NDLS", "New Delhi", LocalTime.of(10, 5), null, 0, 4, 1451));
        trainRepository.save(howrahRajUp);

        // 9. Howrah Rajdhani Express (Delhi -> Howrah)
        Train howrahRajDown = new Train("12302", "Howrah Rajdhani Express", "Delhi", "Howrah",
                LocalTime.of(16, 55), LocalTime.of(9, 55));
        howrahRajDown.addClass(new TrainClass("3A", 120, new BigDecimal("1950.00")));
        howrahRajDown.addClass(new TrainClass("2A", 60, new BigDecimal("2650.00")));
        howrahRajDown.addClass(new TrainClass("1A", 30, new BigDecimal("3450.00")));
        howrahRajDown.addStop(new TrainStop(1, "NDLS", "New Delhi", null, LocalTime.of(16, 55), 0, 4, 0));
        howrahRajDown.addStop(new TrainStop(2, "CNB", "Kanpur Central", LocalTime.of(21, 32), LocalTime.of(21, 37), 5, 2, 441));
        howrahRajDown.addStop(new TrainStop(3, "PRYJ", "Prayagraj Junction", LocalTime.of(23, 43), LocalTime.of(23, 45), 2, 2, 635));
        howrahRajDown.addStop(new TrainStop(4, "DDU", "Pt. Deen Dayal Upadhyaya", LocalTime.of(1, 37), LocalTime.of(1, 47), 10, 1, 788));
        howrahRajDown.addStop(new TrainStop(5, "GAYA", "Gaya Junction", LocalTime.of(3, 58), LocalTime.of(4, 1), 3, 2, 993));
        howrahRajDown.addStop(new TrainStop(6, "DHN", "Dhanbad Junction", LocalTime.of(6, 18), LocalTime.of(6, 23), 5, 2, 1192));
        howrahRajDown.addStop(new TrainStop(7, "ASN", "Asansol Junction", LocalTime.of(7, 28), LocalTime.of(7, 30), 2, 2, 1251));
        howrahRajDown.addStop(new TrainStop(8, "HWH", "Howrah Junction", LocalTime.of(9, 55), null, 0, 9, 1451));
        trainRepository.save(howrahRajDown);

        // 10. Howrah Mail (Chennai -> Howrah)
        Train howrahMail = new Train("12840", "Howrah Mail", "Chennai", "Howrah",
                LocalTime.of(19, 0), LocalTime.of(23, 0));
        howrahMail.addClass(new TrainClass("SL", 240, new BigDecimal("620.00")));
        howrahMail.addClass(new TrainClass("3A", 120, new BigDecimal("1650.00")));
        howrahMail.addClass(new TrainClass("2A", 40, new BigDecimal("2350.00")));
        howrahMail.addStop(new TrainStop(1, "MAS", "Chennai Central", null, LocalTime.of(19, 0), 0, 5, 0));
        howrahMail.addStop(new TrainStop(2, "HWH", "Howrah Junction", LocalTime.of(23, 0), null, 0, 12, 1661));
        trainRepository.save(howrahMail);
    }
}
