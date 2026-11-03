package com.railway.reservation.controller;

import com.railway.reservation.dto.AdminDtos.*;
import com.railway.reservation.dto.BookingDtos.BookingView;
import com.railway.reservation.dto.BookingDtos.CancelPreview;
import com.railway.reservation.dto.BookingDtos.CancelRequest;
import com.railway.reservation.dto.TrainDtos.StationDto;
import com.railway.reservation.dto.TrainDtos.TrainDetail;
import com.railway.reservation.model.CancellationRule;
import com.railway.reservation.service.rail.AdminService;
import com.railway.reservation.service.rail.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Operations console API. Every route requires ROLE_ADMIN (also enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final BookingService bookingService;

    public AdminController(AdminService adminService, BookingService bookingService) {
        this.adminService = adminService;
        this.bookingService = bookingService;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return adminService.overview();
    }

    @GetMapping("/trains")
    public List<TrainRow> trains() {
        return adminService.trains();
    }

    @GetMapping("/trains/{number}")
    public TrainDetail train(@PathVariable String number) {
        return adminService.train(number);
    }

    @PostMapping("/trains")
    public ResponseEntity<TrainDetail> create(@Valid @RequestBody TrainForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTrain(form));
    }

    @PutMapping("/trains/{number}")
    public TrainDetail update(@PathVariable String number, @Valid @RequestBody TrainForm form) {
        return adminService.updateTrain(number, form);
    }

    @PatchMapping("/trains/{number}/status")
    public ResponseEntity<Void> status(@PathVariable String number, @RequestBody Map<String, Boolean> body) {
        adminService.setActive(number, Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/trains/{number}")
    public ResponseEntity<Void> delete(@PathVariable String number) {
        adminService.deleteTrain(number);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stations")
    public List<StationDto> stations() {
        return adminService.stations();
    }

    @PostMapping("/stations")
    public ResponseEntity<StationDto> createStation(@Valid @RequestBody StationForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createStation(form));
    }

    /** Bookings report filtered by PNR, email, train, journey date and status (RRS-F-016). */
    @GetMapping("/bookings")
    public PageOf<BookingRow> bookings(@RequestParam(required = false) String pnr,
                                       @RequestParam(required = false) String email,
                                       @RequestParam(required = false) String train,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "25") int size) {
        return adminService.bookings(pnr, email, train, date, status, page, size);
    }

    @GetMapping("/bookings/{pnr}")
    public BookingView booking(@PathVariable String pnr, Authentication auth) {
        return bookingService.view(pnr, CurrentUser.id(auth), true);
    }

    @PostMapping("/bookings/{pnr}/cancel/preview")
    public CancelPreview cancelPreview(@PathVariable String pnr, @Valid @RequestBody CancelRequest request, Authentication auth) {
        return bookingService.previewCancel(pnr, CurrentUser.id(auth), true, request.passengerIds());
    }

    @PostMapping("/bookings/{pnr}/cancel")
    public BookingView cancel(@PathVariable String pnr, @Valid @RequestBody CancelRequest request, Authentication auth) {
        return bookingService.cancel(pnr, CurrentUser.id(auth), true, request.passengerIds());
    }

    @GetMapping("/chart")
    public Chart chart(@RequestParam String train,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(name = "class", required = false) String classCode) {
        return adminService.chart(train, date, classCode);
    }

    @GetMapping(value = "/chart.csv", produces = "text/csv")
    public ResponseEntity<byte[]> chartCsv(@RequestParam String train,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam(name = "class", required = false) String classCode) {
        String csv = adminService.chartCsv(train, date, classCode);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chart-" + train + "-" + date + "-" + classCode + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/users")
    public PageOf<UserRow> users(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return adminService.users(page, size);
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long id) {
        adminService.unlockUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rules")
    public List<CancellationRule> rules() {
        return adminService.rules();
    }

    @PutMapping("/rules")
    public ResponseEntity<Void> updateRules(@RequestBody List<@Valid RuleForm> rules) {
        adminService.updateRules(rules);
        return ResponseEntity.noContent().build();
    }
}
