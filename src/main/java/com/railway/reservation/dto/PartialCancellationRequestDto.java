package com.railway.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class PartialCancellationRequestDto {

    @NotEmpty(message = "At least one passenger ID must be selected for cancellation")
    private List<Long> passengerIds;

    public PartialCancellationRequestDto() {}

    public PartialCancellationRequestDto(List<Long> passengerIds) {
        this.passengerIds = passengerIds;
    }

    public List<Long> getPassengerIds() { return passengerIds; }
    public void setPassengerIds(List<Long> passengerIds) { this.passengerIds = passengerIds; }
}

