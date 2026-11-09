# Railway Reservation System

Mini project for the Software Engineering course (`UE24CS341A`).

A comprehensive, production-grade web-based Railway Reservation System that allows passengers to search trains, check real-time seat availability, book tickets with automatic seat allocation and online payment through Razorpay (test mode), download printable PDF e-tickets, track booking history, query PNR status, and cancel bookings with automated rule-based refund calculation. The system also includes an administrative management module for configuring trains, routes, schedules, and fares, alongside filtered booking reports and audit logging.

## Team Members (Team 16)

- **Shakthi Raja G P** - `PES1UG25CS843` (`@shakthi-raja`)
- **Kishan S** - `PES2UG23CS277` (`@DARKDRAGON02`)
- **Sujeet V Bire** - `PES1UG24CS716` (`@sujeet0329`)
- **Likhith S** - `PES1UG24CS720` (`@likhith76342`)

## Technology Stack

- **Backend:** Java 25, Spring Boot 4.1.1 (Spring Web MVC, Spring Data JPA with Hibernate, Bean Validation, scheduling)
- **Security:** Spring Security with stateless JWT (sliding renewal, 15-minute inactivity timeout), BCrypt password hashing, account lockout, server-side ownership checks on every booking, a strict Content Security Policy per page, HSTS over HTTPS, keys kept in a git-ignored file
- **Integrations:** Razorpay Checkout and API (test mode) for payments and refunds, Gmail SMTP for email, Google Identity Services for sign-in with a linked account
- **Frontend:** semantic HTML, hand-written CSS on a token system (Hallmark "Wayfind" theme, light and dark), vanilla JavaScript ES modules. No build step and no CDN: the font is self-hosted.
- **Documents:** Apache PDFBox (e-ticket PDF), CSV export of reservation charts
- **Database:** H2 (file database by default, in-memory for tests); MySQL driver included
- **Observability:** SLF4J/Logback with correlation IDs and user IDs on every log line, daily rotated log files, a health endpoint
- **Build and tests:** Maven wrapper, JUnit 5, Spring MockMvc, JaCoCo coverage; Apache JMeter for load tests

> **Design changes:** the SRS and SAD were updated to v2.0 and the test plan to v3.1 to match the final product. Every change from the submitted v1.0 documents, with the reason, is listed in [`docs/deviations`](docs/deviations/).

## Development Sprints & Milestones

### Sprint 1: Core Foundation [19th Oct – 23rd Oct] (10 Issues — 100% Done)
- **User Registration & Login (`RRS-F-001`, `RRS-F-002`):** Account registration with BCrypt hashing (`RRS-SR-001`) and JWT issuance.
- **Train Search & Schedule Discovery (`RRS-F-004`, `RRS-F-005`):** Station-to-station schedule lookups and dynamic distance/class fare calculation.
- **Real-Time Seat Availability (`RRS-F-006`):** Live coach availability check and temporary seat hold.
- **Passenger Details & Ticket Reservation (`RRS-F-007`):** Multi-passenger details collection with age/gender validation.
- **Admin Train Management (`RRS-F-015`):** Full CRUD for trains, routes, schedules, and class fares.
- **Baseline Security (`RRS-SR-002`, `RRS-SR-003`):** Password protection and SQL injection/XSS parameter sanitization.
- **Sprint 1 Test Suite:** 127 automated regression tests.

### Sprint 2: Advanced Flows & Hardening [26th Oct – 30th Oct] (11 Issues — 100% Done)
- **Booking History (`RRS-F-014`):** Chronological booking history list for authenticated passengers with status filtering (`ALL`, `CONFIRMED`, `CANCELLED`), passenger details expansion, and ticket retrieval.
- **Ticket Cancellation & Automated Refunds (`RRS-F-012`, `RRS-F-013`):** Pre-departure ticket cancellation with instant refund calculation preview based on configurable tiered rules (hours before departure), immediate seat inventory release, and cancellation audit record logging (`TC-CAN-01`, `TC-CAN-02`).
- **Filtered Admin Bookings Report (`RRS-F-016`):** Filtered administrative report by train number and journey date with passenger rosters and aggregate financial metrics (total bookings, failed payments, cancellations, total refunds) (`TC-ADM-02`).
- **Downloadable PDF E-Tickets (`RRS-F-010`):** Secure iText 7 (Apache PDFBox since the Sprint 3 rebuild) downloadable PDF e-ticket with embedded 10-character PNR, passenger manifest, coach/seat allocation, and journey metadata.
- **Payment Gateway Simulation & Asynchronous Webhooks (`RRS-F-008`, `RRS-F-009`):** Online payment initiation, simulated payment gateway redirect, asynchronous payment callback processing with SHA-256 HMAC signature verification, and automated rollback with seat release upon payment failure. (Replaced by Razorpay test mode in the Sprint 3 spill-over.)
- **PNR Status Inquiry Lookup (`RRS-F-011`):** Real-time public PNR lookup endpoint and navigation modal.
- **RBAC & 15-Minute Session Inactivity Timeout (`RRS-SR-004`, `RRS-SR-005`):** Strict role-based access control protecting all `/api/v1/admin/**` endpoints (`ROLE_ADMIN`), sliding window JWT token renewal on user activity, and client-side inactivity warning with auto re-login prompt (`TC-SEC-04`, `TC-SEC-05`).
- **System Health Diagnostics & 99% Availability (`RRS-NF-002`):** Spring Boot Actuator `/actuator/health` integration (an own `/api/v1/health` endpoint since the Sprint 3 rebuild) with custom `DatabaseHealthIndicator` providing database connectivity verification, latency, and thread pool metrics.
- **Structured Error Logging & Observability (`RRS-NF-005`):** MDC-based request correlation IDs (`corr`) and authenticated user ID tracking across all log statements with centralized JSON exception responses.
- **Search Performance Benchmark (`RRS-NF-001`, `RRS-NF-004`):** 100 concurrent virtual user JMeter load test achieving 200–290 ms p95 latency (sub-3s target) and 0% errors.
- **Sprint 2 Test Suite:** Expanded from 127 to **256 automated regression tests** (100% pass rate).

### Sprint 3: Product rebuild [2nd Nov – 6th Nov]
- Rebuilt the reservation engine and the whole interface. See the capabilities below.
- Search and refund boundary tests, an accessibility audit with its fixes, and the four Part 3 test reports. 146 automated tests, 80.0% line coverage and 63.2% branch coverage.
- **Spill-over (7th and 8th Nov, issues #80 to #86):** sign-in with a linked Google account, email verification at sign-up, Razorpay payments and refunds in place of the simulated gateway, e-ticket and cancellation emails, password reset, a full-site check with its fixes, and updated SRS, SAD, test plan and a deviations document. 195 automated tests, 83.0% line coverage and 65.7% branch coverage.
- **Testing on the final build (8th and 9th Nov):** a usability test on the live site with the other three team members (`RRS-NF-003`), then the system validation run again: the Postman collection (56 requests), a browser journey with real Razorpay test payments and refunds, a 100-user load test, security checks and an accessibility sweep of all 24 pages. The four Part 3 reports were rebuilt from it.

## System Capabilities

### Passengers
- **Search** trains between any two stations on a route, including intermediate stops, with running days, overnight arrivals (+1, +2), an optional search across all stations in a city, filters (class, departure time, train type, available now) and sorting.
- **Availability** per class as a railway shows it: `AVL n`, `RAC n`, `WL n` or `REGRET`, plus a six-day availability calendar. Other booking channels are simulated, so trains fill up realistically closer to departure.
- **Fares** calculated on the server from distance (tapering rates for long journeys), class, reservation and superfast charges, catering on premium trains, Tatkal charges and 5% GST on AC classes.
- **Booking** in three steps (travellers, review, payment) for up to six travellers (four for Tatkal), with berth and meal preferences, lower berths for travellers aged 60+ and women aged 45+, groups seated together, saved travellers, and an option to book only if every berth is confirmed. Booking needs sign-in; guests are returned to the same booking after signing in.
- **Quotas:** General, and Tatkal opening at 10:00 (AC) or 11:00 (non-AC) the day before the train leaves its origin.
- **Payment through Razorpay** (test mode, so no real money moves) by card, net banking, wallet or UPI where enabled, with a 10-minute seat hold and a live countdown. The server creates the Razorpay order, checks Checkout's signature and confirms the payment with Razorpay before booking. A payment whose page never reported back is picked up when the pay page reopens or when the hold would lapse, and money that arrives after a hold ended is refunded in full automatically. Card numbers, UPI PINs and bank passwords never reach this server.
- **Refunds through Razorpay** to the original payment method whenever travellers are cancelled, including the automatic cancellation of waitlisted tickets at chart time. If Razorpay cannot start a refund, nothing is cancelled.
- **E-ticket** page and PDF, print view, public **PNR status** (statuses only, never names), and **My trips** (upcoming, past, cancelled).
- **Cancellation** of some or all travellers, with a refund preview from the rule table before confirming, and the Razorpay refund reference on the ticket. Freed berths go to RAC, and RAC places go to the waiting list automatically.
- **Train schedule** pages (route, halts, day numbers, coach composition), **station boards**, a refund rules page, and an account page with profile, password change and saved travellers.
- **Email verification at sign-up:** a 6-digit code is emailed and the account is created only once it is entered. Codes expire after 10 minutes, allow five wrong tries, and can be re-sent once a minute (five an hour). The mobile number is a contact number and is not verified.
- **Forgot password:** a 6-digit code is emailed to the account's address and sets a new password; the answer is the same whether or not the email has an account, and a notice is emailed when the password changes.
- **Emails:** the e-ticket PDF after payment, and a notice with the refund amount and Razorpay refund id after a cancellation.
- **Sign in with Google** for passengers who have linked a Google account from their account page. Google only signs in to an existing account; it never creates one, and a locked account stays locked.

### Operations console (`/admin/`)
- Overview of today's bookings, revenue, refunds, pending holds, the last seven days, class mix and departures in the next 48 hours.
- Fleet management: create trains with a full route editor, edit coaches and running days, stop or restart bookings, delete unused trains. The route is locked once tickets exist.
- Bookings search by PNR, email, train, date and status, with cancellation on a passenger's behalf.
- **Reservation chart:** coach-by-coach berth map for any departure, showing who holds each berth and for which stretch, plus RAC and waiting lists and a CSV export.
- Stations, users (unlock locked accounts) and the editable refund rule table.

### Background jobs
- Unpaid holds are released when their 10 minutes run out.
- When the chart is prepared (four hours before the train leaves its origin), fully waitlisted e-tickets are cancelled and refunded less a clerkage charge.

## Local Setup

```bash
./mvnw spring-boot:run
```

Open http://localhost:8080. The first start seeds 98 stations and 66 long-distance trains; bookings are kept in `./data` between restarts (delete that folder to start fresh).

### Keys and passwords

Keys are never committed. Copy [`secrets.example.properties`](secrets.example.properties) to `railway-secrets.properties` in the project folder and fill it in; git ignores that file. To keep it somewhere else, set `RRS_SECRETS_FILE` to its path. The app reads it on start. Anything left empty is switched off cleanly:

| Key | Turns on |
| --- | --- |
| `spring.mail.username`, `spring.mail.password` | Emailing sign-up and password reset codes, e-tickets and cancellation notices through Gmail. Use the Gmail address that owns a Gmail app password (2-Step Verification must be on). Without them, emails are written to the server log so sign-up still works on a local copy. |
| `app.razorpay.key-id`, `app.razorpay.key-secret` | Online payment and refunds through Razorpay. Test keys (`rzp_test_...`) from the Razorpay dashboard move no real money. Without them, the pay page says online payment is unavailable. |
| `app.google.client-id` | "Sign in with Google". Create a Web application OAuth client in Google Cloud Console and add `http://localhost:8080` (and any public address you use) under Authorized JavaScript origins. |

Useful settings (environment variables):

| Variable | Purpose | Default |
| --- | --- | --- |
| `APP_JWT_SECRET` | JWT signing key (32+ bytes) | a development key |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | the seeded operations account | `admin@railway.gov.in` / `Admin@123` |
| `DB_URL` | JDBC URL, for example `jdbc:h2:mem:railwaydb` for a throwaway database | `./data/railwaydb` |
| `H2_CONSOLE` | `true` to enable the H2 console at `/h2-console` | `false` |

Paying in Razorpay test mode: in the payment window choose **Netbanking**, pick any bank, then press **Success** on Razorpay's test bank page (**Failure** shows a declined payment). Razorpay's [test card numbers](https://razorpay.com/docs/payments/payments/test-card-details/) also work.

### Running Automated Tests

```bash
./mvnw clean test                       # tests, plus JaCoCo coverage in target/site/jacoco
./mvnw surefire-report:report-only      # HTML test report in target/reports/surefire.html
```

## Documentation

### [1. Software Requirements Specification (SRS)](docs/srs/)

Version 2.0. Defines 16 functional requirements (`RRS-F-001` to `RRS-F-016`), 6 non-functional requirements (`RRS-NF-001` to `RRS-NF-006`), and 6 security requirements (`RRS-SR-001` to `RRS-SR-006`), along with UML use-case diagrams for the overall system and booking subsystem, and a Requirements Traceability Matrix (RTM).

### [2. Software Architecture and Design Specification (SAD)](docs/sad/)

Version 2.0. Models the 3-tier layered architecture, the Razorpay, Gmail and Google integrations, component responsibilities, STRIDE threat model, database schema, REST API endpoints (`/api/v1`), UML component diagram, and UML sequence diagrams for ticket booking and cancellation flows.

### [3. Software Test Plan (STP)](docs/test-plan/)

Outlines the unit, integration, system, performance, and security testing strategy, test schedule, environment setup, and test cases mapped to the SRS requirements. Version 3.1 lists all 195 automated tests and maps each of the 28 requirements to them, or to the manual runs for load, accessibility and usability.

### [4. Performance Benchmark](docs/performance/issue17-search-benchmark.md)

Empirical load test report validating `RRS-NF-001` and `RRS-NF-004` (100 concurrent virtual users, sub-300ms p95 latency, 0% error rate). Detailed reproduction instructions in [`perf/README.md`](perf/README.md).

### [5. Part 3 Test Reports](docs/reports/)

Unit test report, system validation report, code coverage report and branch coverage report, each as a Word document and a PDF, built from the latest test run.

### [6. Deviations from the Submitted Design](docs/deviations/)

Every place where the final product differs from the submitted SRS, SAD and test plan v1.0, and why, as a Word document and a PDF.

### [7. Sprint 01 Demonstration Video](docs/demo/)

Complete end-to-end silent video demonstration (1 min 15 sec) illustrating the application startup, train search, real-time seat availability check, demo account auto-fill, ticket booking with PNR allocation, printable ERS slip, and PNR status lookup.

- Walkthrough documentation: [`docs/demo/README.md`](docs/demo/README.md)
- Video file: [`docs/demo/sprint-01-demo.mp4`](docs/demo/sprint-01-demo.mp4)
