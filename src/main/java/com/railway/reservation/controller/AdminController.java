package com.railway.reservation.controller;

import com.railway.reservation.dto.CreateTrainDto;
import com.railway.reservation.dto.TrainResponseDto;
import com.railway.reservation.dto.UpdateTrainDto;
import com.railway.reservation.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/trains")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<TrainResponseDto> createTrain(@Valid @RequestBody CreateTrainDto createTrainDto) {
        TrainResponseDto created = adminService.createTrain(createTrainDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainResponseDto> updateTrain(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTrainDto updateTrainDto) {
        TrainResponseDto updated = adminService.updateTrain(id, updateTrainDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrain(@PathVariable("id") Long id) {
        adminService.deleteTrain(id);
        return ResponseEntity.noContent().build();
    }
}
