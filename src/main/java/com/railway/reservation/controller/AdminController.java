package com.railway.reservation.controller;

import com.railway.reservation.dto.AdminDashboardStatsDto;
import com.railway.reservation.dto.CoachSeatMatrixDto;
import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.ReservationChartDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.UpdateTrainDto;
import com.railway.reservation.service.AdminService;
import com.railway.reservation.validation.SafeTextRules;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/trains")
    public ResponseEntity<List<TrainResponseDto>> getAllTrains() {
        return ResponseEntity.ok(adminService.getAllTrains());
    }

    @PostMapping("/trains")
    public ResponseEntity<TrainResponseDto> createTrain(@Valid @RequestBody CreateTrainDto createTrainDto) {
        TrainResponseDto created = adminService.createTrain(createTrainDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/trains/{id}")
    public ResponseEntity<TrainResponseDto> updateTrain(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTrainDto updateTrainDto) {
        TrainResponseDto updated = adminService.updateTrain(id, updateTrainDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/trains/{id}")
    public ResponseEntity<Void> deleteTrain(@PathVariable("id") Long id) {
        adminService.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trains/{id}/seat-matrix")
    public ResponseEntity<CoachSeatMatrixDto> getCoachSeatMatrix(
            @PathVariable("id") Long id,
            @RequestParam(name = "date", required = false) String dateStr,
            @RequestParam(name = "className", required = false) String className) {

        LocalDate journeyDate = parseDate(dateStr);
        String safeClassName = (className != null && !className.isBlank()) ? className.trim() : null;
        if (safeClassName != null) {
            SafeTextRules.requireSafe("Class name ('className')", safeClassName);
        }

        CoachSeatMatrixDto matrix = adminService.getCoachSeatMatrix(id, journeyDate, safeClassName);
        return ResponseEntity.ok(matrix);
    }

    @GetMapping("/trains/{id}/chart")
    public ResponseEntity<ReservationChartDto> getReservationChart(
            @PathVariable("id") Long id,
            @RequestParam(name = "date", required = false) String dateStr) {

        LocalDate journeyDate = parseDate(dateStr);
        ReservationChartDto chart = adminService.getReservationChart(id, journeyDate);
        return ResponseEntity.ok(chart);
    }

    @GetMapping("/trains/{id}/chart/csv")
    public ResponseEntity<String> getReservationChartCsv(
            @PathVariable("id") Long id,
            @RequestParam(name = "date", required = false) String dateStr) {

        LocalDate journeyDate = parseDate(dateStr);
        String csv = adminService.generateReservationChartCsv(id, journeyDate);

        String filename = "reservation-chart-train-" + id + "-" + (journeyDate != null ? journeyDate : LocalDate.now()) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format. Expected format: YYYY-MM-DD.");
        }
    }
}
