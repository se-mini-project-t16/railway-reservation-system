package com.railway.reservation.controller;

import com.railway.reservation.dto.TrainDtos.*;
import com.railway.reservation.service.rail.CancellationService;
import com.railway.reservation.service.rail.TimetableService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Public timetable endpoints: open to guests (RRS-F-004, RRS-F-005). */
@RestController
@RequestMapping("/api/v1")
public class TrainController {

    private final TimetableService timetableService;
    private final CancellationService cancellationService;

    public TrainController(TimetableService timetableService, CancellationService cancellationService) {
        this.timetableService = timetableService;
        this.cancellationService = cancellationService;
    }

    @GetMapping("/stations")
    public List<StationDto> stations() {
        return timetableService.stations();
    }

    @GetMapping("/stations/{code}/departures")
    public StationBoard departures(@PathVariable String code,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return timetableService.departures(code, date);
    }

    @GetMapping("/trains/search")
    public SearchResponse search(@RequestParam String from, @RequestParam String to,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(required = false, defaultValue = "GN") String quota,
                                 @RequestParam(required = false, defaultValue = "false") boolean nearby) {
        return timetableService.search(from, to, date, quota, nearby);
    }

    @GetMapping("/trains")
    public List<TrainSummary> trains() {
        return timetableService.trains();
    }

    @GetMapping("/trains/{number}")
    public TrainDetail train(@PathVariable String number) {
        return timetableService.trainDetail(number);
    }

    @GetMapping("/trains/{number}/availability")
    public AvailabilityCalendar availability(@PathVariable String number, @RequestParam String from,
                                             @RequestParam String to, @RequestParam("class") String classCode,
                                             @RequestParam(required = false, defaultValue = "GN") String quota,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestParam(required = false, defaultValue = "6") int days) {
        return timetableService.calendar(number, from, to, classCode, quota, date, days);
    }

    @GetMapping("/policies/refunds")
    public RefundPolicy refundPolicy() {
        return cancellationService.policy();
    }
}
