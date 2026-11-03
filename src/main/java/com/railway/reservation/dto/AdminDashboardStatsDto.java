package com.railway.reservation.dto;

import java.math.BigDecimal;

public class AdminDashboardStatsDto {

    private long todayBookings;
    private BigDecimal todayRevenue;
    private long activeFleetTrains;
    private double averageOccupancyRate;
    private long totalBookingsAllTime;
    private BigDecimal totalRevenueAllTime;
    private long totalCancellationsToday;
    private BigDecimal totalRefundsToday;

    public AdminDashboardStatsDto() {}

    public AdminDashboardStatsDto(long todayBookings, BigDecimal todayRevenue, long activeFleetTrains,
                                  double averageOccupancyRate, long totalBookingsAllTime,
                                  BigDecimal totalRevenueAllTime, long totalCancellationsToday,
                                  BigDecimal totalRefundsToday) {
        this.todayBookings = todayBookings;
        this.todayRevenue = todayRevenue;
        this.activeFleetTrains = activeFleetTrains;
        this.averageOccupancyRate = averageOccupancyRate;
        this.totalBookingsAllTime = totalBookingsAllTime;
        this.totalRevenueAllTime = totalRevenueAllTime;
        this.totalCancellationsToday = totalCancellationsToday;
        this.totalRefundsToday = totalRefundsToday;
    }

    public long getTodayBookings() { return todayBookings; }
    public void setTodayBookings(long todayBookings) { this.todayBookings = todayBookings; }

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }

    public long getActiveFleetTrains() { return activeFleetTrains; }
    public void setActiveFleetTrains(long activeFleetTrains) { this.activeFleetTrains = activeFleetTrains; }

    public double getAverageOccupancyRate() { return averageOccupancyRate; }
    public void setAverageOccupancyRate(double averageOccupancyRate) { this.averageOccupancyRate = averageOccupancyRate; }

    public long getTotalBookingsAllTime() { return totalBookingsAllTime; }
    public void setTotalBookingsAllTime(long totalBookingsAllTime) { this.totalBookingsAllTime = totalBookingsAllTime; }

    public BigDecimal getTotalRevenueAllTime() { return totalRevenueAllTime; }
    public void setTotalRevenueAllTime(BigDecimal totalRevenueAllTime) { this.totalRevenueAllTime = totalRevenueAllTime; }

    public long getTotalCancellationsToday() { return totalCancellationsToday; }
    public void setTotalCancellationsToday(long totalCancellationsToday) { this.totalCancellationsToday = totalCancellationsToday; }

    public BigDecimal getTotalRefundsToday() { return totalRefundsToday; }
    public void setTotalRefundsToday(BigDecimal totalRefundsToday) { this.totalRefundsToday = totalRefundsToday; }
}

