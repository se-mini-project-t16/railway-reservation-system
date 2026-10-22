package com.railway.reservation.service;

import com.railway.reservation.dto.BookingRequestDto;
import com.railway.reservation.dto.BookingResponseDto;
import com.railway.reservation.dto.PassengerDto;
import com.railway.reservation.exception.ResourceNotFoundException;
import com.railway.reservation.exception.SeatUnavailableException;
import com.railway.reservation.model.*;
import com.railway.reservation.repository.*;
import com.railway.reservation.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private TrainClassRepository trainClassRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User sampleUser;
    private Train sampleTrain;
    private TrainClass sampleClass;

    @BeforeEach
    void setUp() {
        sampleUser = new User("Shakthi Raja", "shakthi@example.com", "9876543210", "hashedPassword", Role.ROLE_PASSENGER);
        sampleUser.setId(1L);

        sampleTrain = new Train("12601", "Mangalore Mail", "Chennai Central", "Mangalore Central",
                LocalTime.of(20, 15), LocalTime.of(12, 5));
        sampleTrain.setId(10L);

        sampleClass = new TrainClass("AC 3 Tier", 64, BigDecimal.valueOf(1150.00));
        sampleClass.setId(100L);
        sampleClass.setTrain(sampleTrain);
    }

    @Test
    @DisplayName("TC-BK-01: Successfully book ticket and generate unique PNR")
    void testCreateBooking_Success() {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(2),
                List.of(
                        new PassengerDto("Alice Smith", 28, "Female"),
                        new PassengerDto("Bob Smith", 32, "Male")
                )
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(trainRepository.findById(10L)).thenReturn(Optional.of(sampleTrain));
        when(trainClassRepository.findById(100L)).thenReturn(Optional.of(sampleClass));
        when(bookingRepository.countBookedSeats(eq(10L), eq(100L), any(LocalDate.class), anyList())).thenReturn(10);
        when(bookingRepository.findByPnr(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponseDto response = bookingService.createBooking(request);

        assertNotNull(response);
        assertNotNull(response.getPnr());
        assertEquals(10, response.getPnr().length());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertEquals("NOT_VERIFIED", response.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(2300.00), response.getTotalFare());
        assertEquals(2, response.getPassengers().size());
        assertEquals("AC3-11", response.getPassengers().get(0).getSeatNo());
        assertEquals("AC3-12", response.getPassengers().get(1).getSeatNo());

        verify(passengerRepository, times(2)).save(any(Passenger.class));
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("TC-BK-01 (Negative): Fail when requested seats exceed availability")
    void testCreateBooking_InsufficientSeats() {
        BookingRequestDto request = new BookingRequestDto(
                1L, 10L, 100L, LocalDate.now().plusDays(2),
                List.of(
                        new PassengerDto("Alice Smith", 28, "Female"),
                        new PassengerDto("Bob Smith", 32, "Male")
                )
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(trainRepository.findById(10L)).thenReturn(Optional.of(sampleTrain));
        when(trainClassRepository.findById(100L)).thenReturn(Optional.of(sampleClass));
        // Total seats: 64, already booked: 63 -> only 1 seat available
        when(bookingRepository.countBookedSeats(eq(10L), eq(100L), any(LocalDate.class), anyList())).thenReturn(63);

        SeatUnavailableException exception = assertThrows(SeatUnavailableException.class, () ->
                bookingService.createBooking(request));

        assertTrue(exception.getMessage().contains("Insufficient seats available"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("TC-BK-01 (Negative): Fail when user is not found")
    void testCreateBooking_UserNotFound() {
        BookingRequestDto request = new BookingRequestDto(
                999L, 10L, 100L, LocalDate.now().plusDays(2),
                List.of(new PassengerDto("Alice Smith", 28, "Female"))
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("TC-PNR-01: Successfully retrieve booking by valid PNR")
    void testGetBookingByPnr_Success() {
        Booking booking = new Booking("TESTPNR123", sampleUser, sampleTrain, sampleClass, LocalDate.now().plusDays(2), BigDecimal.valueOf(1150.00));
        booking.setId(500L);
        booking.setStatus(BookingStatus.CONFIRMED);
        Passenger p = new Passenger("Alice Smith", 28, "Female", "AC3-1");
        booking.addPassenger(p);

        when(bookingRepository.findByPnr("TESTPNR123")).thenReturn(Optional.of(booking));

        BookingResponseDto response = bookingService.getBookingByPnr("TESTPNR123");

        assertNotNull(response);
        assertEquals("TESTPNR123", response.getPnr());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertEquals("12601", response.getTrainNo());
        assertEquals(1, response.getPassengers().size());
        assertEquals("Alice Smith", response.getPassengers().get(0).getName());
    }

    @Test
    @DisplayName("TC-PNR-01 (Negative): Return error when PNR does not exist")
    void testGetBookingByPnr_NotFound() {
        when(bookingRepository.findByPnr("INVALIDPNR")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingByPnr("INVALIDPNR"));
    }

    @Test
    @DisplayName("Ticket lookup scopes PNR to the authenticated owner")
    void testGetTicketByPnr_EnforcesOwnership() {
        when(bookingRepository.findByPnrAndUserId("OWNEDPNR", 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getTicketByPnr("OWNEDPNR", 1L));
        verify(bookingRepository).findByPnrAndUserId("OWNEDPNR", 1L);
        verify(bookingRepository, never()).findByPnr("OWNEDPNR");
    }
}

