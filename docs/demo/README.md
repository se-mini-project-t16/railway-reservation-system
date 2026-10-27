# Sprint 01 Demonstration Video

Mini-project demonstration for Software Engineering course (`UE24CS341A`) — Team 16.

## Video Overview

- **File:** [`sprint-01-demo.mp4`](sprint-01-demo.mp4)
- **Duration:** 1 minute 15 seconds
- **Resolution:** 1080p Full HD (Silent Walkthrough)
- **Sprint Scope:** Sprint 01 Deliverables (RRS-F-001, RRS-F-002, RRS-F-003, RRS-F-004, RRS-F-005, RRS-F-006, RRS-F-007, RRS-F-008, RRS-NF-003)

## Demonstrated Workflow

1. **Application Startup (`0:00 - 0:15`)**
   - Launched the Spring Boot application using `./mvnw spring-boot:run`.
   - Verified clean startup and database initialization on Java 25 LTS and Spring Boot 4.1.1.

2. **Train Search & Route Filtering (`0:15 - 0:35`)**
   - Navigated to `http://localhost:8080`.
   - Selected Departure Station (`Chennai (MAS)`) and Arrival Station (`Bengaluru (SBC)`) using the station dropdowns.
   - Executed search to query scheduled trains and active classes.

3. **Real-Time Seat Availability Check (`0:35 - 0:50`)**
   - Viewed search results for *Kovai Express (#12675)*.
   - Checked live seat availability for Chair Car (`CC`), returning 100 available seats.
   - Selected class to proceed to the passenger reservation view.

4. **Passenger Details & Ticket Reservation (`0:50 - 1:05`)**
   - Leveraged the `Auto-Fill Demo Account` feature to register a demo passenger account (`User ID #1`) and auto-populate passenger details.
   - Submitted the booking request to reserve seats.

5. **Confirmed E-Ticket & Electronic Reservation Slip (`1:05 - 1:15`)**
   - Displayed confirmed booking banner with the 10-character unique PNR and allocated seat numbers.
   - Demonstrated the clipboard copy feature (`Copy PNR`).
   - Triggered the print view demonstrating the official printable Electronic Reservation Slip (ERS).

6. **PNR Inquiry Status Lookup (`1:15 - 1:15+`)**
   - Opened the top navigation `Check PNR Status` modal.
   - Queried the generated PNR to verify real-time booking retrieval from the backend database.
