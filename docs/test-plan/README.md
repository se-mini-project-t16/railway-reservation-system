# Software Test Plan (STP)

**Project:** Railway Reservation System

This folder holds the test plan, the list of automated test cases and the traceability matrix from SRS requirements to tests.

## Deliverables

- Test plan document: [`test-plan-railway-reservation-system.pdf`](./test-plan-railway-reservation-system.pdf) | [`test-plan-railway-reservation-system.docx`](./test-plan-railway-reservation-system.docx)
- Search performance benchmark (issue #17): [`issue17-search-benchmark.md`](../performance/issue17-search-benchmark.md)
- Script that rebuilds this page and the docx tables: [`tools/update_test_plan.py`](./tools/update_test_plan.py)

## Latest run

We ran `./mvnw clean test` on 06 November 2026 at 22:56 on `dev/likhith` (merged with `main` after PR #68). Maven reported:

```text
Tests run: 146, Failures: 0, Errors: 0, Skipped: 0
```

All 146 tests passed. The number is lower than the 256 this page used to list because the app was rebuilt in PR #68 and the old test classes (`BookingControllerTest`, `PaymentServiceTest`, `AdminServiceTest` and others) were replaced by the classes below.

## Tests per class

| Package | Test class | Tests | Failed | Requirements |
|---|---|---:|---:|---|
| `validation` | `SafeTextRulesTest` | 32 | 0 | RRS-SR-003 |
| `controller` | `RailJourneyIntegrationTest` | 28 | 0 | RRS-F-004, RRS-F-005, RRS-F-006, RRS-F-007, RRS-F-008, RRS-F-009, RRS-F-010, RRS-F-011, RRS-F-012, RRS-F-013, RRS-F-014, RRS-F-015, RRS-F-016, RRS-SR-003, RRS-SR-004 |
| `service` | `CancellationRefundTest` | 13 | 0 | RRS-F-013 |
| `controller` | `InputValidationSecurityTest` | 11 | 0 | RRS-SR-003 |
| `service` | `SeatLedgerTest` | 11 | 0 | RRS-F-005, RRS-F-006 |
| `controller` | `UserLoginControllerTest` | 9 | 0 | RRS-F-001, RRS-F-002, RRS-F-003, RRS-SR-003 |
| `service` | `FareCalculatorTest` | 8 | 0 | RRS-F-006 |
| `controller` | `SystemReliabilityAndLoggingTest` | 8 | 0 | RRS-NF-002, RRS-NF-004, RRS-NF-005 |
| `controller` | `SearchBoundaryTest` | 5 | 0 | RRS-F-004 |
| `controller` | `SessionTimeoutSecurityTest` | 5 | 0 | RRS-SR-005 |
| `controller` | `UserRegistrationControllerTest` | 5 | 0 | RRS-F-001, RRS-SR-001 |
| `service` | `DemandSimulatorTest` | 4 | 0 | RRS-F-005 |
| `controller` | `PasswordProtectionSecurityTest` | 4 | 0 | RRS-SR-001 |
| `security` | `QueryParameterizationTest` | 2 | 0 | RRS-SR-003 |
| `(root)` | `RailwayReservationApplicationTests` | 1 | 0 | App start-up |
| | **Total** | **146** | **0** | |

Parameterised tests count once per input row, which is why `SafeTextRulesTest` shows 32 tests from 4 methods.

## Traceability matrix

The SRS has 26 requirements. In this run 17 were covered by automated tests, 7 were partly covered and 2 had no test. The last row, RRS-NF-006, is the accessibility check we added in Sprint 3; it is not in SRS v1.0, so it is not part of these counts. Where the rebuilt app behaves differently from the SRS acceptance criteria we wrote that in the Notes column instead of counting it as a pass.

| Requirement | Description | Covering tests | Tests run | Status | Notes |
|---|---|---|---:|---|---|
| RRS-F-001 | User registration | UserLoginControllerTest, UserRegistrationControllerTest | 6 | Covered | Sign up, field rules, duplicate email (any case) and BCrypt storage. |
| RRS-F-002 | User login | UserLoginControllerTest | 9 | Covered | Correct login returns a signed JWT; wrong password and unknown email get the same 401. |
| RRS-F-003 | Account lock after 5 failed logins | UserLoginControllerTest | 4 | Covered | The lock is tested, but the API answers "Invalid credentials." instead of the lock message in the SRS, so the response does not reveal that the account exists. |
| RRS-F-004 | Train search by source, destination and date | TC-SRC-01 to 11 | 11 | Covered | Includes intermediate stops, running days, nearby stations and bad input (SAME_STATION, past date, more than 60 days). |
| RRS-F-005 | Seat availability | TC-DEM-01 to 04, TC-SEAT-01, TC-SEAT-05, TC-SEAT-06, TC-SEAT-11, TC-SRC-01, TC-SRC-06 | 10 | Covered | Availability per class, the availability calendar, and berths freed at an intermediate station. |
| RRS-F-006 | Book ticket with PNR | TC-BK-01, TC-FARE-01 to 08, TC-SEAT-01 to 11, TC-TQ-01 | 21 | Covered | Berth allocation, RAC and waiting list, Tatkal window, server-side fares and a 10-digit PNR. |
| RRS-F-007 | Passenger details for each seat | TC-BK-02 | 1 | Covered | Missing or invalid name, age or gender is rejected field by field. |
| RRS-F-008 | Payment gateway after booking | TC-PAY-01, TC-PAY-02 | 2 | Partly covered | The app uses its own simulated gateway with test cards and UPI IDs, so there is no redirect to an external page to test. |
| RRS-F-009 | Confirm only after successful payment | TC-PAY-01 to 04, TC-BK-03 / TC-HIS-01 | 5 | Covered | A declined card keeps the hold for a retry instead of marking the booking FAILED; berths come back when the 10 minute hold expires or is released. |
| RRS-F-010 | E-ticket after payment | TC-BK-03 / TC-HIS-01 | 1 | Partly covered | The test checks the PDF is refused before payment and is a real PDF after it, but does not read the PNR or fare inside the file. |
| RRS-F-011 | PNR status check | TC-PNR-01 | 1 | Covered | A paid PNR shows each traveller's status but no names; unpaid and unknown PNRs return not found. |
| RRS-F-012 | Cancel ticket | TC-CAN-01, TC-CAN-03, TC-CAN-04, TC-SEC-07 | 4 | Covered | Partial cancellation, RAC promotion and automatic refund of waitlisted tickets after charting. |
| RRS-F-013 | Refund as per cancellation rules | TC-ADM-05, TC-CAN-02, TC-CAN-05, TC-CAN-06, TC-CAN-07, TC-CAN-08, TC-CAN-09, TC-CAN-10, TC-CAN-11 | 15 | Covered | Refund tiers and the preview shown before confirming. |
| RRS-F-014 | Booking history | TC-BK-03 / TC-HIS-01 | 1 | Partly covered | The paid booking appears in history, but newest-first ordering is not checked (the test has one booking). |
| RRS-F-015 | Admin add, update, delete trains, routes, fares | TC-ADM-01, TC-ADM-03 | 2 | Partly covered | Adding a train and blocking delete when it has bookings are tested; editing an existing train (PUT /api/v1/admin/trains/{number}) has no test. |
| RRS-F-016 | Admin bookings report by date and train | TC-ADM-04 | 1 | Covered | Report filtered by train 12901 and date, plus the chart and its CSV. |
| RRS-NF-001 | Search within 3 s for 95% of requests | TC-PERF-01, JMeter run on 29 Oct 2026 (docs/performance/issue17-search-benchmark.md) | - | Partly covered | p95 was 200 ms and 290 ms over two runs of 100 requests, but that was before the rebuild in PR #68 and has not been repeated since. |
| RRS-NF-002 | 99% availability during demo | TC-OPS-01 | 3 | Partly covered | The health endpoint and a burst of parallel health checks are tested; real uptime over the demo period is not measured. |
| RRS-NF-003 | New user books within 5 minutes | None | - | Not covered | No automated test. The user test with 3 classmates (TC-UX-01) has no record in the repository. |
| RRS-NF-004 | 100 concurrent users | TC-OPS-01; TC-PERF-02, JMeter run on 29 Oct 2026 with 100 threads, 0 errors | 1 | Partly covered | Same caveat as RRS-NF-001: the JMeter run predates PR #68. |
| RRS-NF-005 | Log errors with timestamp and user id | TC-OPS-02 | 5 | Covered | ERROR level logs with user id and timestamp, daily rotation in logs/, and no stack traces in responses. |
| RRS-SR-001 | BCrypt password hashing | PasswordProtectionSecurityTest, UserRegistrationControllerTest | 5 | Covered | Hash format and salt, and the password never appears in responses or logs. |
| RRS-SR-002 | HTTPS (TLS 1.2+) | None | - | Not covered | The app runs on plain HTTP at localhost:8080 and has no TLS settings in application.properties. |
| RRS-SR-003 | Server-side input validation (SQL injection, XSS) | TC-OPS-03, InputValidationSecurityTest, QueryParameterizationTest, SafeTextRulesTest, UserLoginControllerTest | 47 | Covered | Payloads on real endpoints, the SafeText rules on their own, bound query parameters and the CSP header. |
| RRS-SR-004 | Role based access control | TC-ADM-02, TC-SEC-06, TC-SEC-07 | 3 | Covered | A passenger gets 403 and a guest 401 on admin routes; passengers cannot open each other's bookings. |
| RRS-SR-005 | Session timeout after 15 minutes | TC-SEC-05 | 5 | Covered | The API side (401 Session Expired, token renewal) is tested; the redirect to the login page in the browser is not. |
| RRS-NF-006 (added in Sprint 3, not in SRS v1.0) | Accessibility (keyboard, focus, contrast, labels) | `docs/test-plan/accessibility-audit.md` | - | Covered | Checked by hand with Lighthouse, axe and keyboard only, in light and dark themes; there is no automated test. |

### Gaps

- **RRS-F-008** (partly covered): The app uses its own simulated gateway with test cards and UPI IDs, so there is no redirect to an external page to test.
- **RRS-F-010** (partly covered): The test checks the PDF is refused before payment and is a real PDF after it, but does not read the PNR or fare inside the file.
- **RRS-F-014** (partly covered): The paid booking appears in history, but newest-first ordering is not checked (the test has one booking).
- **RRS-F-015** (partly covered): Adding a train and blocking delete when it has bookings are tested; editing an existing train (PUT /api/v1/admin/trains/{number}) has no test.
- **RRS-NF-001** (partly covered): p95 was 200 ms and 290 ms over two runs of 100 requests, but that was before the rebuild in PR #68 and has not been repeated since.
- **RRS-NF-002** (partly covered): The health endpoint and a burst of parallel health checks are tested; real uptime over the demo period is not measured.
- **RRS-NF-003** (not covered): No automated test. The user test with 3 classmates (TC-UX-01) has no record in the repository.
- **RRS-NF-004** (partly covered): Same caveat as RRS-NF-001: the JMeter run predates PR #68.
- **RRS-SR-002** (not covered): The app runs on plain HTTP at localhost:8080 and has no TLS settings in application.properties.

### Tests with no SRS requirement

These tests check behaviour the rebuilt app added beyond the SRS v1.0 list: TC-ACC-01.

## Test cases

Every JUnit test that ran, grouped by class. The ID and description come from the `@DisplayName` on each test. Classes written before we used per-test IDs show the method name instead.

### `CancellationRefundTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-CAN-05 | Confirmed Sleeper Rs 1000 ticket refund boundaries across 48h, 12h, and 4h tiers | 7 | Pass | RRS-F-013 |
| TC-CAN-06 | 3A cancellation includes 5% GST on the flat cancellation charge beyond 48 hours | 1 | Pass | RRS-F-013 |
| TC-CAN-07 | Flat charge takes precedence when percentage deduction is lower | 1 | Pass | RRS-F-013 |
| TC-CAN-08 | RAC traveller cancellation retains only Rs 60 clerkage charge | 1 | Pass | RRS-F-013 |
| TC-CAN-09 | Waitlisted traveller cancellation retains only Rs 60 clerkage charge | 1 | Pass | RRS-F-013 |
| TC-CAN-10 | Confirmed Tatkal tickets receive zero refund regardless of time remaining | 1 | Pass | RRS-F-013 |
| TC-CAN-11 | Train already departed receives zero refund | 1 | Pass | RRS-F-013 |

### `DemandSimulatorTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-DEM-01 | The same departure always gets the same simulated sales | 1 | Pass | RRS-F-005 |
| TC-DEM-02 | A departure tomorrow is busier than one seven weeks away | 1 | Pass | RRS-F-005 |
| TC-DEM-03 | Simulated Tatkal sales stay inside the Tatkal berths | 1 | Pass | RRS-F-005 |
| TC-DEM-04 | Switched off, nothing is pre-sold | 1 | Pass | RRS-F-005 |

### `FareCalculatorTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-FARE-01 | Sleeper fare is distance x rate plus reservation and superfast, with no GST | 1 | Pass | RRS-F-006 |
| TC-FARE-02 | AC classes pay 5% GST on the fare before tax | 1 | Pass | RRS-F-006 |
| TC-FARE-03 | Short journeys are charged the class's minimum distance | 1 | Pass | RRS-F-006 |
| TC-FARE-04 | Long journeys taper: the second 1000 km costs less per km than the first | 1 | Pass | RRS-F-006 |
| TC-FARE-05 | Non-superfast express trains carry no superfast charge | 1 | Pass | RRS-F-006 |
| TC-FARE-06 | Tatkal charge is a percentage of base fare, held between the class minimum and maximum | 1 | Pass | RRS-F-006 |
| TC-FARE-07 | Premium trains include catering and a higher base rate | 1 | Pass | RRS-F-006 |
| TC-FARE-08 | A booking's total is the per-traveller fare times the travellers | 1 | Pass | RRS-F-006 |

### `InputValidationSecurityTest` (TC-SEC-03 input validation security [RRS-SR-003])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| (TC-SEC-03) | `adminCreateTrainRejectsScriptTagInNameWith400` | 1 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `adminCreateTrainRejectsSqlInjectionInStationWith400` | 1 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `adminCreateTrainStillAcceptsNormalNames` | 1 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `registrationRejectsMaliciousNameWith400` | 4 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `registrationStillAcceptsApostropheInName` | 1 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `registrationStillAcceptsHyphenatedName` | 1 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `searchRejectsSqlInjectionInSourceWith400` | 1 | Pass | RRS-SR-003 |
| (TC-SEC-03) | `searchRejectsXssInDestinationWith400` | 1 | Pass | RRS-SR-003 |

### `PasswordProtectionSecurityTest` (TC-SEC-01 password protection [RRS-SR-001])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| (TC-SEC-01) | `passwordAndHashNeverAppearInRegisterOrLoginResponses` | 1 | Pass | RRS-SR-001 |
| (TC-SEC-01) | `passwordIsStoredAsBcryptHashWithSalt` | 1 | Pass | RRS-SR-001 |
| (TC-SEC-01) | `passwordsAreNeverWrittenToLogs` | 1 | Pass | RRS-SR-001 |
| (TC-SEC-01) | `sameRawPasswordProducesDifferentHashesBecauseOfSalt` | 1 | Pass | RRS-SR-001 |

### `QueryParameterizationTest` (Parameterized queries only [RRS-SR-003])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `everyCustomQueryUsesNamedParameters` | 1 | Pass | RRS-SR-003 |
| No ID | `productionCodeDoesNotUseRawSqlApis` | 1 | Pass | RRS-SR-003 |

### `RailJourneyIntegrationTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-ACC-01 | A passenger edits their profile, changes password and keeps a private list of travellers | 1 | Pass | - |
| TC-ADM-01 | An admin creates a train that appears in the next search; bad routes are rejected | 1 | Pass | RRS-F-015 |
| TC-ADM-02 | Admin routes refuse passengers and guests | 1 | Pass | RRS-SR-004 |
| TC-ADM-03 | A train with tickets keeps its route, cannot be deleted, and can be stopped from taking bookings | 1 | Pass | RRS-F-015 |
| TC-ADM-04 | The bookings report filters by train and date, and the chart places the traveller in a berth | 1 | Pass | RRS-F-016 |
| TC-ADM-05 | Changing the rule table changes the next refund | 1 | Pass | RRS-F-013 |
| TC-BK-01 | Booking holds confirmed berths, prices them on the server and gives a 10-digit PNR owned by the caller | 1 | Pass | RRS-F-006 |
| TC-BK-02 | Missing or invalid traveller details are rejected field by field | 1 | Pass | RRS-F-007 |
| TC-BK-03 / TC-HIS-01 | The paid ticket downloads as PDF and appears in the owner's history | 1 | Pass | RRS-F-009, RRS-F-010, RRS-F-014 |
| TC-CAN-01 | Cancelling some travellers refunds them, leaves the rest booked, and refuses a repeat | 1 | Pass | RRS-F-012 |
| TC-CAN-02 | Refunds follow the rule table: flat charge beyond 48 h, 25% from 12 h, 50% from 4 h, nothing under 4 h | 1 | Pass | RRS-F-013 |
| TC-CAN-03 | A cancellation hands its berth to the first RAC traveller | 1 | Pass | RRS-F-012 |
| TC-CAN-04 | A fully waitlisted ticket is cancelled and refunded automatically once the chart is prepared | 1 | Pass | RRS-F-012 |
| TC-OPS-03 | Unknown API paths are 401 to guests and 404 JSON when signed in, unknown pages are the 404 page, and pages carry a CSP | 1 | Pass | RRS-SR-003 |
| TC-PAY-01 | Paying books the ticket; paying again returns the same receipt without charging twice | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-02 | A declined card keeps the hold for a retry; an invalid card is refused; card numbers are masked | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-03 | An unpaid hold expires after 10 minutes, its berths return, and it can no longer be paid | 1 | Pass | RRS-F-009 |
| TC-PAY-04 | Releasing a hold frees the berths at once | 1 | Pass | RRS-F-009 |
| TC-PNR-01 | Public PNR status shows statuses but never names; unpaid and unknown PNRs are not found | 1 | Pass | RRS-F-011 |
| TC-SEC-06 | A guest cannot book, pay, list bookings or open a ticket | 1 | Pass | RRS-SR-004 |
| TC-SEC-07 | Another passenger cannot see, pay, cancel or download someone else's booking | 1 | Pass | RRS-F-012, RRS-SR-004 |
| TC-SRC-01 | Guests can search; results carry times, classes, fares and availability | 1 | Pass | RRS-F-004, RRS-F-005 |
| TC-SRC-02 | Search finds a train between two intermediate stops, with that stretch's distance | 1 | Pass | RRS-F-004 |
| TC-SRC-03 | Trains are listed only on the days they run | 1 | Pass | RRS-F-004 |
| TC-SRC-04 | Same station, a past date, a date past 60 days and an unknown station are rejected | 1 | Pass | RRS-F-004 |
| TC-SRC-05 | 'Nearby' widens a search to other stations in the same city | 1 | Pass | RRS-F-004 |
| TC-SRC-06 | A train's page, its availability calendar and a station board are public | 1 | Pass | RRS-F-004, RRS-F-005 |
| TC-TQ-01 | Tatkal opens at 10:00 (AC) and 11:00 (non-AC) the day before, for at most 4 travellers | 1 | Pass | RRS-F-006 |

### `RailwayReservationApplicationTests`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `contextLoads` | 1 | Pass | - |

### `SafeTextRulesTest` (SafeTextRules: SQL injection and XSS rejection [RRS-SR-003])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `allowsNormalInput` | 14 | Pass | RRS-SR-003 |
| No ID | `rejectsMaliciousInput` | 16 | Pass | RRS-SR-003 |
| No ID | `requireSafeThrowsWithoutEchoingThePayload` | 1 | Pass | RRS-SR-003 |
| No ID | `treatsNullAsSafeSoNotBlankCanReportMissingValues` | 1 | Pass | RRS-SR-003 |

### `SearchBoundaryTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-SRC-07 | Swapping the stations returns trains only in their scheduled direction | 1 | Pass | RRS-F-004 |
| TC-SRC-08 | Swapping an intermediate journey returns no reverse train | 1 | Pass | RRS-F-004 |
| TC-SRC-09 | Station codes and full names resolve before same-station validation | 1 | Pass | RRS-F-004 |
| TC-SRC-10 | Today and day 60 are allowed; yesterday and day 61 are rejected | 1 | Pass | RRS-F-004 |
| TC-SRC-11 | Missing and blank station or date inputs return clear errors | 1 | Pass | RRS-F-004 |

### `SeatLedgerTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-SEAT-01 | An empty coach offers all 58 general berths | 1 | Pass | RRS-F-005, RRS-F-006 |
| TC-SEAT-02 | A berth preference is honoured when such a berth is free | 1 | Pass | RRS-F-006 |
| TC-SEAT-03 | Travellers aged 60 or more are given a lower berth when they state no preference | 1 | Pass | RRS-F-006 |
| TC-SEAT-04 | A group is seated together, close to the first traveller | 1 | Pass | RRS-F-006 |
| TC-SEAT-05 | A berth freed at an intermediate station is sold again for the rest of the route | 1 | Pass | RRS-F-005, RRS-F-006 |
| TC-SEAT-06 | When berths run out travellers join RAC, then the waiting list, then the class regrets | 1 | Pass | RRS-F-005, RRS-F-006 |
| TC-SEAT-07 | 'Book only if confirmed' refuses a booking that would not be fully confirmed | 1 | Pass | RRS-F-006 |
| TC-SEAT-08 | After chart preparation no RAC or waiting-list tickets are issued | 1 | Pass | RRS-F-006 |
| TC-SEAT-09 | A cancellation confirms the first RAC traveller and moves the first waitlisted one into RAC | 1 | Pass | RRS-F-006 |
| TC-SEAT-10 | Other channels' earlier RAC passengers clear before this system's | 1 | Pass | RRS-F-006 |
| TC-SEAT-11 | Tatkal and general quotas draw on separate berth pools | 1 | Pass | RRS-F-005, RRS-F-006 |

### `SessionTimeoutSecurityTest` (TC-SEC-05 session inactivity timeout [RRS-SR-005])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-SEC-05 | Activity renews the session; renewed token has later expiry, same identity, and works on admin routes | 1 | Pass | RRS-SR-005 |
| TC-SEC-05 | Token idle past 15 minutes -> 401 Session Expired with inactivity notice | 1 | Pass | RRS-SR-005 |
| TC-SEC-05 | Missing or tampered token -> no renewal and a plain 401 (not a timeout) | 1 | Pass | RRS-SR-005 |
| TC-SEC-05 | JwtService.renewToken keeps identity claims and moves expiry forward | 1 | Pass | RRS-SR-005 |
| TC-SEC-05 | Renewed token grants exactly a fresh 15-minute inactivity window | 1 | Pass | RRS-SR-005 |

### `SystemReliabilityAndLoggingTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-OPS-01 | Database disconnection reports DEGRADED availability status | 1 | Pass | RRS-NF-002 |
| TC-OPS-01 | Health check reports UP status with 99% SLA availability and uptime diagnostics | 1 | Pass | RRS-NF-002 |
| TC-OPS-01 | High-concurrency health check burst maintains 100% uptime with zero crashes | 1 | Pass | RRS-NF-002, RRS-NF-004 |
| TC-OPS-02 | Correlation ID is generated or echoed in header for distributed tracing | 1 | Pass | RRS-NF-005 |
| TC-OPS-02 | Rotating log files are configured in /logs/ directory with daily policy | 1 | Pass | RRS-NF-005 |
| TC-OPS-02 | Database error sanitizes response and prevents raw SQL leakage to client | 1 | Pass | RRS-NF-005 |
| TC-OPS-02 | Log all exceptions at ERROR level with user ID and timestamp | 1 | Pass | RRS-NF-005 |
| TC-OPS-02 | Uncaught exception returns standard JSON without stack trace leakage | 1 | Pass | RRS-NF-005 |

### `UserLoginControllerTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `concurrentFailedLoginsAreCountedWithoutLostUpdates` | 1 | Pass | RRS-F-002, RRS-F-003 |
| No ID | `locksAccountAfterFiveFailedAttemptsAndBlocksCorrectPassword` | 1 | Pass | RRS-F-002, RRS-F-003 |
| No ID | `logsInWithBcryptPasswordAndReturnsSignedJwtWithExpectedClaims` | 1 | Pass | RRS-F-002 |
| No ID | `persistsLockoutAcrossCommittedLoginRequests` | 1 | Pass | RRS-F-002, RRS-F-003 |
| No ID | `registeredUserCanImmediatelyLogIn` | 1 | Pass | RRS-F-001, RRS-F-002 |
| No ID | `rejectsIncorrectPasswordWithUnauthorizedResponse` | 1 | Pass | RRS-F-002 |
| No ID | `rejectsSqlInjectionPayloadInLoginEmail` | 1 | Pass | RRS-F-002, RRS-SR-003 |
| No ID | `rejectsUnknownEmailWithSameUnauthorizedResponse` | 1 | Pass | RRS-F-002 |
| No ID | `successfulLoginResetsFailuresAndExpiredLock` | 1 | Pass | RRS-F-002, RRS-F-003 |

### `UserRegistrationControllerTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `registersUserWithBcryptAndDoesNotExposePasswordData` | 1 | Pass | RRS-F-001, RRS-SR-001 |
| No ID | `rejectsDuplicateEmailWithConflictIncludingCaseDifferences` | 1 | Pass | RRS-F-001 |
| No ID | `rejectsMalformedJsonWithBadRequest` | 1 | Pass | RRS-F-001 |
| No ID | `rejectsMissingAndInvalidFieldsWithBadRequest` | 1 | Pass | RRS-F-001 |
| No ID | `rejectsPasswordLongerThanBcryptUtf8Limit` | 1 | Pass | RRS-F-001 |

## Updating this page

```bash
git checkout dev/likhith
git fetch origin
git merge origin/main
export APP_JWT_SECRET=$(openssl rand -base64 32)
./mvnw clean test
python3 docs/test-plan/tools/update_test_plan.py
```

The script rewrites this README and the tables in the docx. Export the PDF again from Word afterwards. New tests are picked up by their ID prefix (for example `TC-CAN-*`); a test with a new prefix or a new requirement needs a line in `REQUIREMENTS` inside the script.
