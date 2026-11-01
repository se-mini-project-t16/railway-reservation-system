# Railway Reservation System

Mini project for the Software Engineering course (`UE24CS341A`).

A comprehensive, production-grade web-based Railway Reservation System that allows passengers to search trains, check real-time seat availability, book tickets with automatic seat allocation and online payment simulation, download printable PDF e-tickets, track booking history, query PNR status, and cancel bookings with automated rule-based refund calculation. The system also includes an administrative management module for configuring trains, routes, schedules, and fares, alongside filtered booking reports and audit logging.

## Team Members (Team 16)

- **Shakthi Raja G P** - `PES1UG25CS843` (`@shakthi-raja`)
- **Kishan S** - `PES2UG23CS277` (`@DARKDRAGON02`)
- **Sujeet V Bire** - `PES1UG24CS716` (`@sujeet0329`)
- **Likhith S** - `PES1UG24CS720` (`@likhith76342`)

## Technology Stack

- **Backend:** Java 25 (LTS), Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Hibernate, Bean Validation)
- **Security:** Spring Security, JWT (with sliding window renewal and 15-minute inactivity timeout), BCrypt password hashing, OWASP HTML Sanitizer
- **Frontend:** HTML5, CSS3, Bootstrap 5, Modern Vanilla JavaScript
- **Reporting & Documents:** iText 7 (PDF E-Ticket generation)
- **Database:** MySQL / H2 In-Memory (for zero-setup local testing and automated builds)
- **Observability:** Spring Boot Actuator, Custom Database Health Indicator, SLF4J/Logback with MDC correlation IDs and structured JSON audit logging
- **Performance:** Apache JMeter 5.6.3 (load testing suite)
- **Build & Testing:** Maven, JUnit 5, Mockito, Spring Test

> **Platform Modernization Note:** The initial software requirements and design specifications (SRS/SAD/STP) originally designated Java 17 and Spring Boot 3.3.4 as the project baseline. The active implementation platform has been modernized to **Java 25 (LTS)** and **Spring Boot 4.1.1**, maintaining 100% functional traceability with all specification requirements.

## System Capabilities & Feature Overview

### 1. Passenger Subsystem
- **User Authentication & Session Management (`RRS-F-001`, `RRS-F-002`, `RRS-F-003`, `RRS-SR-001`, `RRS-SR-002`, `RRS-SR-005`):** Secure signup and login with BCrypt password hashing, JWT token issuance with sliding window renewal, client-side inactivity tracking, and warning banner on session timeout.
- **Train Search & Fare Calculation (`RRS-F-004`, `RRS-F-005`):** Station-to-station schedule and route discovery with distance and class-based fare calculation.
- **Seat Availability & Allocation (`RRS-F-006`):** Real-time seat availability checking and dynamic allocation of lowest available seat numbers per coach class.
- **Booking & Passenger Details (`RRS-F-007`, `RRS-F-010`):** Multi-passenger details collection with age/gender validation, confirmed booking allocation with unique 10-digit alphanumeric PNR, and downloadable PDF E-Tickets.
- **Payment Gateway Simulation (`RRS-F-008`, `RRS-F-009`):** Online payment initiation, simulated payment gateway redirect, asynchronous payment callback processing with SHA-256 HMAC signature verification, and automated rollback with seat release upon payment failure.
- **PNR Inquiry & Booking History (`RRS-F-011`, `RRS-F-014`):** Real-time PNR status lookup for any booking, alongside authenticated passenger booking history with chronological sorting and status filtering (`ALL`, `CONFIRMED`, `CANCELLED`).
- **Ticket Cancellation & Refunds (`RRS-F-012`, `RRS-F-013`):** Pre-departure ticket cancellation with instant refund calculation preview based on tiered cancellation rules (hours before departure), immediate seat release back to available inventory, and audit logging.

### 2. Administrator Subsystem
- **Train, Route & Fare Management (`RRS-F-015`):** Full CRUD capabilities for administrators (`ROLE_ADMIN`) to add, update, and delete trains, intermediate routes, schedules, and class fares.
- **Filtered Bookings Report (`RRS-F-016`):** Comprehensive administrative reporting with optional train number and journey date filters, detailed passenger rosters, and aggregate financial metrics (total bookings, failed payments, cancellations, total refunds).
- **Role-Based Access Control (`RRS-SR-004`):** Strict authorization enforcing `ROLE_ADMIN` on all `/api/v1/admin/**` endpoints (`403 Forbidden` for standard passengers).

### 3. Non-Functional & Operational Hardening
- **Sub-3s Response Time under Concurrency (`RRS-NF-001`, `RRS-NF-004`):** Verified under a 100 concurrent virtual user benchmark with JMeter, achieving 200–290 ms p95 response latency and 0% errors.
- **High Availability & Health Diagnostics (`RRS-NF-002`):** System health monitoring via Spring Boot Actuator `/actuator/health` with a custom `DatabaseHealthIndicator` providing database connectivity checks, latency, and thread metrics.
- **Structured Error Logging (`RRS-NF-005`):** MDC-based request correlation IDs (`corr`) and user ID tracking propagated across all log statements with centralized exception handling and JSON error responses.
- **Security & Injection Rejection (`RRS-SR-003`):** Parameterized queries and strict `SafeTextRules` input validation rejecting SQL injection and XSS payloads.

## Local Setup

The application requires `APP_JWT_SECRET` at startup (minimum 32 UTF-8 bytes).

**Linux / macOS (Bash / Zsh):**
```bash
export APP_JWT_SECRET=$(openssl rand -base64 32)
./mvnw spring-boot:run
```

**Windows (PowerShell):**
```powershell
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$jwtRandom.GetBytes($jwtBytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
.\mvnw.cmd spring-boot:run
```

Once started, open `http://localhost:8080` in your web browser.

### Running Automated Tests

Execute the full regression test suite (256 unit, integration, RBAC security, cancellation, PDF generation, booking history, and admin reporting tests):
```bash
./mvnw clean test
```

## Documentation

### [1. Software Requirements Specification (SRS)](docs/srs/)

Defines 16 functional requirements (`RRS-F-001` to `RRS-F-016`), 5 non-functional requirements (`RRS-NF-001` to `RRS-NF-005`), and 5 security requirements (`RRS-SR-001` to `RRS-SR-005`), along with UML use-case diagrams for the overall system and booking subsystem, and a Requirements Traceability Matrix (RTM).

### [2. Software Architecture and Design Specification (SAD)](docs/sad/)

Models the 3-tier layered architecture, component responsibilities, STRIDE threat model, database schema, REST API endpoints (`/api/v1`), UML component diagram, and UML sequence diagrams for ticket booking and cancellation flows.

### [3. Software Test Plan (STP)](docs/test-plan/)

Outlines the unit, integration, system, performance, and security testing strategy, test schedule, environment setup, and test cases mapped to the SRS requirements. Includes a breakdown of all 256 automated regression tests.

### [4. Performance Benchmark](docs/performance/issue17-search-benchmark.md)

Empirical load test report validating `RRS-NF-001` and `RRS-NF-004` (100 concurrent virtual users, sub-300ms p95 latency, 0% error rate). Detailed reproduction instructions in [`perf/README.md`](perf/README.md).

### [5. Sprint 01 Demonstration Video](docs/demo/)

Complete end-to-end silent video demonstration (1 min 15 sec) illustrating the application startup, train search, real-time seat availability check, demo account auto-fill, ticket booking with PNR allocation, printable ERS slip, and PNR status lookup.

- Walkthrough documentation: [`docs/demo/README.md`](docs/demo/README.md)
- Video file: [`docs/demo/sprint-01-demo.mp4`](docs/demo/sprint-01-demo.mp4)
