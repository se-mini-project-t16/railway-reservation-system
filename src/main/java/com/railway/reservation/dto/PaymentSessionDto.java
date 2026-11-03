package com.railway.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentSessionDto {

    private Long paymentId;
    private String gatewayRef;
    private String pnr;
    private BigDecimal amount;
    private String status;
    private String paymentUrl;
    private String gatewayProvider;

    // Journey context for payment display
    private String trainNo;
    private String trainName;
    private String sourceStation;
    private String destinationStation;
    private LocalDate journeyDate;
    private Integer passengerCount;
    private LocalDateTime createdAt;
    private String sessionToken;

    public PaymentSessionDto() {}

    public PaymentSessionDto(
            Long paymentId,
            String gatewayRef,
            String pnr,
            BigDecimal amount,
            String status,
            String paymentUrl,
            String gatewayProvider,
            String trainNo,
            String trainName,
            String sourceStation,
            String destinationStation,
            LocalDate journeyDate,
            Integer passengerCount,
            LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.gatewayRef = gatewayRef;
        this.pnr = pnr;
        this.amount = amount;
        this.status = status;
        this.paymentUrl = paymentUrl;
        this.gatewayProvider = gatewayProvider;
        this.trainNo = trainNo;
        this.trainName = trainName;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.journeyDate = journeyDate;
        this.passengerCount = passengerCount;
        this.createdAt = createdAt;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public String getGatewayProvider() { return gatewayProvider; }
    public void setGatewayProvider(String gatewayProvider) { this.gatewayProvider = gatewayProvider; }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getSourceStation() { return sourceStation; }
    public void setSourceStation(String sourceStation) { this.sourceStation = sourceStation; }

    public String getDestinationStation() { return destinationStation; }
    public void setDestinationStation(String destinationStation) { this.destinationStation = destinationStation; }

    public LocalDate getJourneyDate() { return journeyDate; }
    public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }

    public Integer getPassengerCount() { return passengerCount; }
    public void setPassengerCount(Integer passengerCount) { this.passengerCount = passengerCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}

