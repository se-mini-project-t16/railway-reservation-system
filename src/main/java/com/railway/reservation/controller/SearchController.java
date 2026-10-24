package com.railway.reservation.controller;

import com.railway.reservation.validation.SafeTextRules;

import com.railway.reservation.dto.SeatAvailabilityDto;
import com.railway.reservation.dto.TrainSearchResponseDto;
import com.railway.reservation.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trains")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<TrainSearchResponseDto>> searchTrains(
            @RequestParam(name = "src", required = false) String src,
            @RequestParam(name = "dst", required = false) String dst,
            @RequestParam(name = "date", required = false) String dateStr) {

        if (src == null || src.trim().isEmpty()) {
            throw new IllegalArgumentException("Source station ('src') parameter is required.");
        }
        if (dst == null || dst.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination station ('dst') parameter is required.");
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Journey date ('date') parameter is required.");
        }

        LocalDate journeyDate;
        try {
            journeyDate = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid journey date format. Expected format: YYYY-MM-DD.");
        }

        SafeTextRules.requireSafe("Source station ('src')", src);
        SafeTextRules.requireSafe("Destination station ('dst')", dst);

        List<TrainSearchResponseDto> results = searchService.searchTrains(src.trim(), dst.trim(), journeyDate);
        if (results.isEmpty()) {
            return ResponseEntity.ok()
                    .header("X-Search-Message", "No trains found for selected route and date")
                    .body(results);
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{trainId}/availability")
    public ResponseEntity<SeatAvailabilityDto> getSeatAvailability(
            @PathVariable Long trainId,
            @RequestParam(name = "class", required = false) String className,
            @RequestParam(name = "date", required = false) String dateStr) {

        if (className == null || className.trim().isEmpty()) {
            throw new IllegalArgumentException("Class name ('class') parameter is required.");
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Journey date ('date') parameter is required.");
        }

        LocalDate journeyDate;
        try {
            journeyDate = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid journey date format. Expected format: YYYY-MM-DD.");
        }

        SafeTextRules.requireSafe("Class name ('class')", className);

        SeatAvailabilityDto availability = searchService.getSeatAvailability(trainId, className.trim(), journeyDate);
        return ResponseEntity.ok(availability);
    }
}
