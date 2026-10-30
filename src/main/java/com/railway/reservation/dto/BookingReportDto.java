package com.railway.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class BookingReportDto {

    private String trainNo;
    private LocalDate date;
    private long totalBookings;
    private long failedPayments;
    private long cancellations;
    private BigDecimal totalRefunds;
    private List<BookingReportRowDto> bookings;

    public BookingReportDto() {}

    public BookingReportDto(String trainNo, LocalDate date, long totalBookings, long failedPayments,
                            long cancellations, BigDecimal totalRefunds, List<BookingReportRowDto> bookings) {
        this.trainNo = trainNo;
        this.date = date;
        this.totalBookings = totalBookings;
        this.failedPayments = failedPayments;
        this.cancellations = cancellations;
        this.totalRefunds = totalRefunds;
        this.bookings = bookings;
    }

    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getFailedPayments() { return failedPayments; }
    public void setFailedPayments(long failedPayments) { this.failedPayments = failedPayments; }

    public long getCancellations() { return cancellations; }
    public void setCancellations(long cancellations) { this.cancellations = cancellations; }

    public BigDecimal getTotalRefunds() { return totalRefunds; }
    public void setTotalRefunds(BigDecimal totalRefunds) { this.totalRefunds = totalRefunds; }

    public List<BookingReportRowDto> getBookings() { return bookings; }
    public void setBookings(List<BookingReportRowDto> bookings) { this.bookings = bookings; }
}
