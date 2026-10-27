# Railway Reservation System

Mini project for the Software Engineering course (`UE24CS341A`).

A web-based Railway Reservation System that allows passengers to search trains, check real-time seat availability, book tickets through an online payment gateway, check PNR status, and cancel bookings with automated refund calculation, alongside an administration module for managing trains, routes, schedules, and fares.

## Team Members

- **Shakthi Raja G P** - `PES1UG25CS843`
- **Kishan S** - `PES2UG23CS277`
- **Sujeet V Bire** - `PES1UG24CS716`
- **Likhith S** - `PES1UG24CS720`

## Technology Stack

- **Backend:** Java 25 (LTS), Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA, Hibernate, Bean Validation)
- **Security:** Spring Security, JWT, BCrypt
- **Frontend:** HTML5, CSS3, Bootstrap 5, Modern JavaScript
- **Database:** MySQL / H2 In-Memory (for zero-setup local testing)
- **Build & Testing:** Maven, JUnit 5, Mockito, Spring Test

> **Platform Modernization Note:** The initial software requirements and design specifications (SRS/SAD/STP) originally designated Java 17 and Spring Boot 3.3.4 as the project baseline. The active implementation platform has been modernized to **Java 25 (LTS)** and **Spring Boot 4.1.1**, maintaining 100% functional traceability with all specification requirements.

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

Execute the full regression test suite (127 unit, integration, RBAC security, and XSS/SQLi rejection tests):
```bash
./mvnw test
```

## Documentation

### [1. Software Requirements Specification (SRS)](docs/srs/)

Defines 16 functional requirements (`RRS-F-001` to `RRS-F-016`), 5 non-functional requirements (`RRS-NF-001` to `RRS-NF-005`), and 5 security requirements (`RRS-SR-001` to `RRS-SR-005`), along with UML use-case diagrams for the overall system and booking subsystem, and a Requirements Traceability Matrix (RTM).

### [2. Software Architecture and Design Specification (SAD)](docs/sad/)

Models the 3-tier layered architecture, component responsibilities, STRIDE threat model, database schema, REST API endpoints (`/api/v1`), UML component diagram, and UML sequence diagrams for ticket booking and cancellation flows.

### [3. Software Test Plan (STP)](docs/test-plan/)

Outlines the unit, integration, system, performance, and security testing strategy, test schedule, environment setup, and 12 test cases (`TC-REG-01` through `TC-SEC-03`) mapped to the SRS requirements.

### [4. Sprint 01 Demonstration Video](docs/demo/)

Complete end-to-end silent video demonstration (1 min 15 sec) illustrating the application startup, train search, real-time seat availability check, demo account auto-fill, ticket booking with PNR allocation, printable ERS slip, and PNR status lookup.

- Walkthrough documentation: [`docs/demo/README.md`](docs/demo/README.md)
- Video file: [`docs/demo/sprint-01-demo.mp4`](docs/demo/sprint-01-demo.mp4)
