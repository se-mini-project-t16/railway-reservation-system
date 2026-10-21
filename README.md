# Railway Reservation System

Mini project for the Software Engineering course (`UE24CS341A`).

A web-based Railway Reservation System that allows passengers to search trains, check real-time seat availability, book tickets through an online payment gateway, check PNR status, and cancel bookings with automated refund calculation, alongside an administration module for managing trains, routes, schedules, and fares.

## Team Members

- **Shakthi Raja G P** - `PES1UG25CS843`
- **Kishan S** - `PES2UG23CS277`
- **Sujeet V Bire** - `PES1UG24CS716`
- **Likhith S** - `PES1UG24CS720`

## Technology Stack

- **Backend:** Java, Spring Boot (Spring Web MVC, Spring Data JPA, Hibernate, Bean Validation)
- **Security:** Spring Security, JWT, BCrypt
- **Frontend:** Thymeleaf / React, Tailwind CSS / Bootstrap
- **Database:** MySQL / H2 (for local testing)
- **Build & Testing:** Maven, JUnit, Mockito, Postman, Apache JMeter

## Local Setup

The application requires `APP_JWT_SECRET` at startup. It must contain at least 32 UTF-8 bytes. In PowerShell, generate a random value for the current session and start the application:

```powershell
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 32
$jwtRandom.GetBytes($jwtBytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRandom.Dispose()
.\mvnw.cmd spring-boot:run
```

The value above remains in the current PowerShell session only. For deployments, provide `APP_JWT_SECRET` through the deployment platform's secret manager. Never put a real signing secret in source files or commit it to Git.

## Documentation

### [1. Software Requirements Specification (SRS)](docs/srs/)

Defines 16 functional requirements (`RRS-F-001` to `RRS-F-016`), 5 non-functional requirements (`RRS-NF-001` to `RRS-NF-005`), and 5 security requirements (`RRS-SR-001` to `RRS-SR-005`), along with UML use-case diagrams for the overall system and booking subsystem, and a Requirements Traceability Matrix (RTM).

- [`srs-railway-reservation-system.pdf`](docs/srs/srs-railway-reservation-system.pdf) | [`srs-railway-reservation-system.docx`](docs/srs/srs-railway-reservation-system.docx)

### [2. Software Architecture and Design Specification (SAD)](docs/sad/)

Models the 3-tier layered architecture, component responsibilities, STRIDE threat model, database schema, REST API endpoints (`/api/v1`), UML component diagram, and UML sequence diagrams for ticket booking and cancellation flows.

- [`sad-railway-reservation-system.pdf`](docs/sad/sad-railway-reservation-system.pdf) | [`sad-railway-reservation-system.docx`](docs/sad/sad-railway-reservation-system.docx)

### [3. Software Test Plan (STP)](docs/test-plan/)

Outlines the unit, integration, system, performance, and security testing strategy, test schedule, environment setup, and 12 test cases (`TC-REG-01` through `TC-SEC-03`) mapped to the SRS requirements.

- [`test-plan-railway-reservation-system.pdf`](docs/test-plan/test-plan-railway-reservation-system.pdf) | [`test-plan-railway-reservation-system.docx`](docs/test-plan/test-plan-railway-reservation-system.docx)
