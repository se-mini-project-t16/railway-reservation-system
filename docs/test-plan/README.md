# Software Test Plan (STP)

**Project:** Railway Reservation System (`v1.0`)

Outlines the test strategy, testing levels (unit, integration, system, performance, and security), test schedule, test environment, and test cases mapped to the SRS requirements.

## Deliverables

- **Test Plan Document:** [`test-plan-railway-reservation-system.pdf`](./test-plan-railway-reservation-system.pdf) | [`test-plan-railway-reservation-system.docx`](./test-plan-railway-reservation-system.docx)
- **Issue #17 Search Performance Benchmark:** [`issue17-search-benchmark.md`](../performance/issue17-search-benchmark.md)

## Automated Test Suite Breakdown

The repository includes a comprehensive regression suite of **256 automated unit and integration tests** executing via Maven Surefire and JUnit 5 with **100% pass rate (0 failures, 0 errors)**:

| Test Class | Test Count | Key Requirements Covered | Scope & Validation |
|---|---:|---|---|
| `BookingControllerTest` | 36 | `RRS-F-006`, `RRS-F-007`, `RRS-F-010`, `RRS-F-011`, `RRS-F-014` | Booking creation, passenger details, PNR lookup, PDF download endpoint, booking history |
| `SafeTextRulesTest` | 32 | `RRS-SR-003` | SQL injection prevention and XSS tag rejection unit rules |
| `BookingServiceTest` | 22 | `RRS-F-006`, `RRS-F-007`, `RRS-F-010` | Seat inventory hold, lowest seat assignment, reuse of released seats, PNR generation |
| `CancellationServiceTest` | 18 | `RRS-F-012`, `RRS-F-013` | Tiered refund rules (48h/24h/12h/4h), HALF_UP rounding, seat release, audit records |
| `PaymentServiceTest` | 18 | `RRS-F-008`, `RRS-F-009` | Payment initiation, HMAC verification, refund execution, failure rollback |
| `SearchControllerTest` | 17 | `RRS-F-004`, `RRS-F-005` | Search API endpoint parameters, validation, station matching, date parsing |
| `SearchServiceTest` | 14 | `RRS-F-004`, `RRS-F-005` | Route resolution, intermediate stations, class fare calculation |
| `CancellationControllerTest` | 11 | `RRS-F-012`, `RRS-F-013`, `TC-CAN-01`, `TC-CAN-02` | End-to-end cancellation, refund preview, authorization, XSS rejection in PNR |
| `InputValidationSecurityTest` | 11 | `RRS-SR-003`, `TC-SEC-01`, `TC-SEC-02` | API controller injection rejection on query params and request bodies |
| `AdminControllerTest` | 9 | `RRS-F-015`, `RRS-SR-004`, `TC-ADM-01`, `TC-SEC-04` | Admin train/route/fare CRUD, RBAC authorization enforcement (`403 Forbidden`) |
| `AdminServiceTest` | 9 | `RRS-F-015`, `RRS-F-016` | Admin operations business logic and report metric calculations |
| `PaymentControllerTest` | 9 | `RRS-F-008`, `RRS-F-009` | Payment initiation and webhook callback endpoints |
| `UserLoginControllerTest` | 9 | `RRS-F-002`, `RRS-SR-001`, `TC-LOG-01` | Login validation, bad credentials handling, JWT token generation |
| `BookingUiIntegrationTest` | 8 | `RRS-F-006`, `RRS-F-007`, `RRS-NF-003` | Multi-step booking UI flow, passenger auto-fill, ERS generation |
| `SystemReliabilityAndLoggingTest` | 8 | `RRS-NF-002`, `RRS-NF-005` | Health diagnostics actuator, custom DB health indicator, MDC correlation ID logging |
| `SessionTimeoutSecurityTest` | 5 | `RRS-SR-005`, `TC-SEC-05` | 15-minute inactivity session expiration and sliding window token renewal |
| `UserRegistrationControllerTest` | 5 | `RRS-F-001`, `RRS-SR-001`, `TC-REG-01` | User registration constraints, duplicate email rejection, BCrypt hashing |
| `AdminBookingReportTest` | 4 | `RRS-F-016`, `TC-ADM-02` | Admin filtered bookings report by train & date, summary metrics |
| `PasswordProtectionSecurityTest` | 4 | `RRS-SR-001`, `TC-SEC-03` | BCrypt strength, salt generation, non-reversible storage |
| `TicketPdfServiceTest` | 2 | `RRS-F-010` | iText 7 PDF generation, layout structure, PNR and passenger rendering |
| `PaymentIntegrationTest` | 2 | `RRS-F-008`, `RRS-F-009` | End-to-end payment gateway transaction simulation and confirmation |
| `QueryParameterizationTest` | 2 | `RRS-SR-003` | Hibernate JPA parameterized query enforcement across repositories |
| `RailwayReservationApplicationTests` | 1 | Foundation | Spring Boot context load, database schema migration, bean wiring |
| **Total** | **256** | **All SRS Requirements** | **100% Pass Rate** |

### Executing the Tests

```bash
./mvnw clean test
```
