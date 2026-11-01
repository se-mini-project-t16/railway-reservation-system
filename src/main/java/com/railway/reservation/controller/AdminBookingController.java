package com.railway.reservation.controller;

import com.railway.reservation.dto.BookingReportDto;
import com.railway.reservation.service.AdminService;
import com.railway.reservation.validation.SafeTextRules;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final AdminService adminService;

    public AdminBookingController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<BookingReportDto> getBookingsReport(
            @RequestParam(name = "trainNo", required = false) String trainNo,
            @RequestParam(name = "date", required = false) String dateStr) {

        String trainNoFilter = (trainNo == null || trainNo.isBlank()) ? null : trainNo.trim();
        SafeTextRules.requireSafe("Train number ('trainNo')", trainNoFilter);

        LocalDate journeyDate = null;
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                journeyDate = LocalDate.parse(dateStr.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid journey date format. Expected format: YYYY-MM-DD.");
            }
        }

        return ResponseEntity.ok(adminService.getBookingsReport(trainNoFilter, journeyDate));
    }
}
