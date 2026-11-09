# Software Test Plan (STP)

**Project:** Railway Reservation System

This folder holds the test plan, the list of automated test cases and the traceability matrix from SRS requirements to tests.

## Deliverables

- Test plan document: [`test-plan-railway-reservation-system.pdf`](./test-plan-railway-reservation-system.pdf) | [`test-plan-railway-reservation-system.docx`](./test-plan-railway-reservation-system.docx)
- Search performance benchmark (issue #17): [`issue17-search-benchmark.md`](../performance/issue17-search-benchmark.md)

## Latest run

We ran `./mvnw clean test` on 09 November 2026 at 13:20 on `dev/shakthi`, which matched `main` after PR #91 plus these updates. Maven reported:

```text
Tests run: 199, Failures: 0, Errors: 0, Skipped: 0
```

All 199 tests passed. The suite had 256 tests before the rebuild in PR #68 and 146 right after it, when the old classes (`BookingControllerTest`, `PaymentServiceTest`, `AdminServiceTest` and others) were replaced. The Sprint 3 spill-over (PR #74 to PR #88) added the tests for Google sign-in, email codes, Razorpay, booking emails and password reset.

## Tests per class

| Package | Test class | Tests | Failed | Requirements |
|---|---|---:|---:|---|
| `controller` | `RailJourneyIntegrationTest` | 44 | 0 | RRS-F-004, RRS-F-005, RRS-F-006, RRS-F-007, RRS-F-008, RRS-F-009, RRS-F-010, RRS-F-011, RRS-F-012, RRS-F-013, RRS-F-014, RRS-F-015, RRS-F-016, RRS-SR-003, RRS-SR-004, RRS-SR-006 |
| `validation` | `SafeTextRulesTest` | 32 | 0 | RRS-SR-003 |
| `service` | `CancellationRefundTest` | 13 | 0 | RRS-F-013 |
| `controller` | `InputValidationSecurityTest` | 11 | 0 | RRS-SR-003 |
| `service` | `SeatLedgerTest` | 11 | 0 | RRS-F-005, RRS-F-006 |
| `controller` | `GoogleSignInTest` | 10 | 0 | RRS-F-002, RRS-F-003, RRS-SR-002 |
| `controller` | `EmailVerificationTest` | 9 | 0 | RRS-F-001 |
| `controller` | `UserLoginControllerTest` | 9 | 0 | RRS-F-001, RRS-F-002, RRS-F-003, RRS-SR-003 |
| `service` | `FareCalculatorTest` | 8 | 0 | RRS-F-006 |
| `controller` | `SystemReliabilityAndLoggingTest` | 8 | 0 | RRS-NF-002, RRS-NF-004, RRS-NF-005 |
| `controller` | `PasswordResetTest` | 7 | 0 | RRS-F-002, RRS-F-003 |
| `service/payment` | `RazorpayGatewayTest` | 5 | 0 | RRS-F-008, RRS-F-009 |
| `controller` | `SearchBoundaryTest` | 5 | 0 | RRS-F-004 |
| `controller` | `SessionTimeoutSecurityTest` | 5 | 0 | RRS-SR-005 |
| `controller` | `UserRegistrationControllerTest` | 5 | 0 | RRS-F-001, RRS-SR-001 |
| `service` | `DemandSimulatorTest` | 4 | 0 | RRS-F-005 |
| `controller` | `PasswordProtectionSecurityTest` | 4 | 0 | RRS-SR-001 |
| `service/google` | `GoogleIdTokenVerifierTest` | 3 | 0 | RRS-F-002 |
| `security` | `QueryParameterizationTest` | 2 | 0 | RRS-SR-003 |
| `service/payment` | `RazorpaySignatureTest` | 2 | 0 | RRS-F-008, RRS-F-009 |
| `(root)` | `RailwayReservationApplicationTests` | 1 | 0 | App start-up |
| `config` | `SchemaUpgradesTest` | 1 | 0 | RRS-F-008, RRS-F-009 |
| | **Total** | **199** | **0** | |

Parameterised tests count once per input row, which is why `SafeTextRulesTest` shows 32 tests from 4 methods.

## Traceability matrix

The SRS has 28 requirements. 27 are covered, 1 partly covered and 0 not covered. Most are covered by the automated tests; search speed, load, accessibility and the 5 minute booking target are covered by the manual runs named in the table. Where the rebuilt app behaves differently from the SRS acceptance criteria we wrote that in the Notes column instead of counting it as a pass.

| Requirement | Description | Covering tests | Tests run | Status | Notes |
|---|---|---|---:|---|---|
| RRS-F-001 | User registration with email verification | TC-REG-01 to 09, UserLoginControllerTest, UserRegistrationControllerTest | 15 | Covered | The account is created only after the 6-digit code emailed to the address is entered; field rules, duplicate email (any case) and BCrypt storage are also tested. |
| RRS-F-002 | User login (password, linked Google account, password reset) | TC-GGL-01 to 12, TC-PWD-01 to 07, UserLoginControllerTest | 28 | Covered | Email and password give a signed JWT; a Google account linked from the account page can sign in but never creates an account; a forgotten password is reset with an emailed code. |
| RRS-F-003 | Account lock after 5 failed logins | TC-GGL-07, TC-PWD-06, UserLoginControllerTest | 6 | Covered | The lock applies to Google sign-in too, and a password reset lifts it. The API answers "Invalid credentials." instead of the lock message in the SRS, so the response does not reveal that the account exists. |
| RRS-F-004 | Train search by source, destination and date | TC-SRC-01 to 11 | 11 | Covered | Includes intermediate stops, running days, nearby stations and bad input (SAME_STATION, past date, more than 60 days). |
| RRS-F-005 | Seat availability | TC-DEM-01 to 04, TC-SEAT-01, TC-SEAT-05, TC-SEAT-06, TC-SEAT-11, TC-SRC-01, TC-SRC-06 | 10 | Covered | Availability per class, the availability calendar, and berths freed at an intermediate station. |
| RRS-F-006 | Book ticket with PNR | TC-ACC-02, TC-BK-01, TC-BK-04, TC-FARE-01 to 08, TC-SEAT-01 to 11, TC-TQ-01 | 23 | Covered | Berth allocation, RAC and waiting list, Tatkal window, server-side fares, a fare quote that matches the booking, saved travellers and a 10-digit PNR. |
| RRS-F-007 | Passenger details for each seat | TC-BK-02 | 1 | Covered | Missing or invalid name, age or gender is rejected field by field. |
| RRS-F-008 | Payment through the payment gateway | TC-PAY-01 to 17; Manual run on 08 Nov 2026: bookings paid through Razorpay's own test Checkout in a browser | 17 | Covered | Razorpay runs in test mode. The server creates one order per booking, checks Checkout's signature and fetches the payment from Razorpay before booking. TC-PAY-12 to 16 run the real Razorpay client against a local server that answers like Razorpay's API. |
| RRS-F-009 | Confirm only after successful payment | TC-PAY-01 to 17, TC-BK-03 / TC-HIS-01 | 18 | Covered | A booking is confirmed only after Razorpay confirms the payment. A declined payment keeps the hold for a retry instead of marking the booking FAILED, and money that arrives after a hold ended is refunded in full. |
| RRS-F-010 | E-ticket after payment | TC-MAIL-01, TC-MAIL-04, TC-BK-03 / TC-HIS-01 | 3 | Covered | The PDF is refused before payment. After payment it is emailed as an attachment, and its text carries the PNR, train, travellers and payment ID. |
| RRS-F-011 | PNR status check | TC-PNR-01 | 1 | Covered | A paid PNR shows each traveller's status but no names; unpaid and unknown PNRs return not found. |
| RRS-F-012 | Cancel ticket | TC-CAN-01, TC-CAN-03, TC-CAN-04, TC-CAN-12, TC-CAN-13, TC-MAIL-02, TC-SEC-07 | 7 | Covered | Partial cancellation, RAC promotion, automatic cancellation of waitlisted tickets after charting, refunds through Razorpay (nothing is cancelled if the refund cannot start), refusal of unpaid bookings and of travellers from another ticket, and the cancellation email. |
| RRS-F-013 | Refund as per cancellation rules | TC-ADM-05, TC-CAN-02, TC-CAN-05, TC-CAN-06, TC-CAN-07, TC-CAN-08, TC-CAN-09, TC-CAN-10, TC-CAN-11 | 15 | Covered | Refund tiers and the preview shown before confirming. |
| RRS-F-014 | Booking history | TC-HIS-02, TC-BK-03 / TC-HIS-01 | 2 | Covered | History lists the newest journey first. The Upcoming tab lists the soonest trip first, which is the order a traveller needs; this is listed in the deviations document. |
| RRS-F-015 | Admin add, update, delete trains, routes, fares | TC-ADM-01, TC-ADM-03, TC-ADM-06, TC-ADM-07 | 4 | Covered | Creating, editing (name, running days, classes and route), stopping a train, and the locks once a train has tickets: no deleting, no new route, coaches can be added but not removed. |
| RRS-F-016 | Admin bookings report by date and train | TC-ADM-04 | 1 | Covered | Report filtered by train 12901 and date, plus the chart and its CSV. |
| RRS-NF-001 | Search within 3 s for 95% of requests | TC-PERF-01, load test on 09 Nov 2026 (docs/reports/system-validation-report.pdf, section 3.3); earlier JMeter run on 29 Oct 2026 (docs/performance/issue17-search-benchmark.md) | - | Covered | 100 searches sent together, three rounds, on the final build: p95 663 ms and slowest 736 ms, well inside 3 s. The 29 Oct JMeter run (p95 200 ms and 290 ms) was on the old two-train version. |
| RRS-NF-002 | 99% availability during demo | TC-OPS-01; Uptime monitor on the live site from 09 Nov 2026, one health check a minute, until the demo | 3 | Partly covered | The health endpoint and a burst of parallel health checks are tested. Real uptime is being measured on the live site from 09 Nov up to the demo; the figure will be added when the period ends. |
| RRS-NF-003 | New user books within 5 minutes | TC-UX-01, usability test on 08 Nov 2026 with the three other team members on the live site, timed from the database (docs/reports/system-validation-report.pdf, section 3.6) | - | Covered | The three other team members each signed up with a new account on the live site and were not helped. They know the project, and Kishan had used an earlier build on 04 Nov, so this is weaker evidence than the outside classmates the SRS asks for. One went from a new account to a paid ticket in 2 min 29 s. One took 13 min 36 s for the first ticket (this included linking Google and a refused attempt with 'book only if all confirmed' ticked) and 3 min 28 s for a second one. One reserved seats but did not pay, so the hold expired. The 5 minute target was met by one of three new users on the first try. |
| RRS-NF-004 | 100 concurrent users | TC-OPS-01; TC-PERF-02, load test on 09 Nov 2026 (docs/reports/system-validation-report.pdf, section 3.3) | 1 | Covered | 300 of 300 searches succeeded at 100 concurrent users on the final build, with no errors. The 29 Oct JMeter run with 100 threads also had 0 errors but predates PR #68. |
| RRS-NF-005 | Log errors with timestamp and user id | TC-OPS-02 | 5 | Covered | ERROR level logs with user id and timestamp, daily rotation in logs/, and no stack traces in responses. |
| RRS-SR-001 | BCrypt password hashing | PasswordProtectionSecurityTest, UserRegistrationControllerTest | 5 | Covered | Hash format and salt, and the password never appears in responses or logs. Email and reset codes are stored as BCrypt hashes too. |
| RRS-SR-002 | HTTPS (TLS 1.2+) | TC-SEC-08; Manual check on 09 Nov 2026: https://railway-reservation.srspace.dev negotiates TLS 1.3 and http:// answers 301 to https:// | 1 | Covered | The public address is served over TLS 1.3 through the Cloudflare tunnel, plain HTTP is redirected to HTTPS, and the app sends HSTS on HTTPS requests. TLS ends at the tunnel; a local run on localhost is HTTP. |
| RRS-SR-003 | Server-side input validation (SQL injection, XSS) | TC-OPS-03, TC-PAY-05, InputValidationSecurityTest, QueryParameterizationTest, SafeTextRulesTest, UserLoginControllerTest | 48 | Covered | Payloads on real endpoints, the SafeText rules on their own, bound query parameters, the CSP header, and forged or malformed payment callbacks. |
| RRS-SR-004 | Role based access control | TC-ADM-02, TC-SEC-06, TC-SEC-07 | 3 | Covered | A passenger gets 403 and a guest 401 on admin routes; passengers cannot open each other's bookings. |
| RRS-SR-005 | Session timeout after 15 minutes | TC-SEC-05; Browser check on 09 Nov 2026 with the limit set to 1 minute (docs/reports/system-validation-report.pdf, section 3.4) | 5 | Covered | The API side (401 Session Expired, token renewal) is tested in the suite. In a real browser, activity kept the session alive past the limit, going idle sent the passenger to sign in with the 'session ended' note, and signing in returned them to the same page. |
| RRS-SR-006 | Guests can search; booking needs sign-in | TC-SEC-06, TC-SRC-01 | 2 | Covered | Guests search trains and check PNRs; booking, paying, history and tickets answer 401 until they sign in. |
| RRS-NF-006 | Accessibility (keyboard, focus, contrast, labels) | accessibility-audit.md; axe-core sweep on 09 Nov 2026 (docs/reports/system-validation-report.pdf, section 3.5) | - | Covered | The audit's 4 axe findings were fixed in commit 9274510. On 09 Nov 2026 axe-core (WCAG 2.1 A and AA) found 0 violations on all 24 pages as guest, passenger and admin, light and dark, at 1280 and 375 px wide (96 runs), after three more fixes (board contrast on phones, keyboard access to scrolling tables, berth label contrast in the dark reservation chart). There is no automated test in the suite. |

### Gaps

- **RRS-NF-002** (partly covered): The health endpoint and a burst of parallel health checks are tested. Real uptime is being measured on the live site from 09 Nov up to the demo; the figure will be added when the period ends.

### Tests with no SRS requirement

These tests check behaviour beyond the SRS v2.0 list: TC-ACC-01, TC-MAIL-03.

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

### `EmailVerificationTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-REG-01 | Sign-up emails a 6-digit code and creates nothing until it is confirmed | 1 | Pass | RRS-F-001 |
| TC-REG-02 | The right code creates the account and signs the passenger in | 1 | Pass | RRS-F-001 |
| TC-REG-03 | Wrong codes count down, and the fifth wrong code uses the code up | 1 | Pass | RRS-F-001 |
| TC-REG-04 | A code expires after 10 minutes | 1 | Pass | RRS-F-001 |
| TC-REG-05 | A new code can be sent after a minute, and only the newest code works | 1 | Pass | RRS-F-001 |
| TC-REG-06 | At most five codes an hour go to one address | 1 | Pass | RRS-F-001 |
| TC-REG-07 | When the email cannot be sent, the passenger is told and nothing is kept | 1 | Pass | RRS-F-001 |
| TC-REG-08 | Confirming an email nobody signed up with is refused | 1 | Pass | RRS-F-001 |
| TC-REG-09 | Sign-ups left unconfirmed for a day are forgotten | 1 | Pass | RRS-F-001 |

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

### `GoogleIdTokenVerifierTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-GGL-10 | A current token from Google for this client is accepted | 1 | Pass | RRS-F-002 |
| TC-GGL-11 | Tokens for another app, from another issuer, or expired are refused | 1 | Pass | RRS-F-002 |
| TC-GGL-12 | Without a client ID, Google sign-in is off and refuses every credential | 1 | Pass | RRS-F-002 |

### `GoogleSignInTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-GGL-01 | The sign-in page is told the Google client ID | 1 | Pass | RRS-F-002 |
| TC-GGL-02 | An unlinked Google account cannot sign in and no account is created | 1 | Pass | RRS-F-002 |
| TC-GGL-03 | After linking from the account page, Google signs in to that same account | 1 | Pass | RRS-F-002 |
| TC-GGL-04 | Linking needs a signed-in user | 1 | Pass | RRS-F-002 |
| TC-GGL-05 | One Google account can be linked to only one account here | 1 | Pass | RRS-F-002 |
| TC-GGL-06 | A forged or malformed Google credential is rejected | 1 | Pass | RRS-F-002 |
| TC-GGL-07 | A locked account stays locked for Google sign-in too | 1 | Pass | RRS-F-002, RRS-F-003 |
| TC-GGL-08 | After unlinking, the Google account no longer signs in | 1 | Pass | RRS-F-002 |
| TC-GGL-09 | Only the sign-in and account pages may load Google's script | 1 | Pass | RRS-F-002 |
| TC-SEC-08 | Requests that arrive over HTTPS are told to stay on HTTPS; plain local HTTP is not | 1 | Pass | RRS-SR-002 |

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

### `PasswordResetTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-PWD-01 | A reset code goes only to registered addresses, and the answer never reveals which | 1 | Pass | RRS-F-002 |
| TC-PWD-02 | The right code sets the new password, signs the passenger in and emails a notice | 1 | Pass | RRS-F-002 |
| TC-PWD-03 | Wrong codes are refused the same way, and five of them use the code up | 1 | Pass | RRS-F-002 |
| TC-PWD-04 | A reset code expires after 10 minutes | 1 | Pass | RRS-F-002 |
| TC-PWD-05 | A new code can be sent after a minute, at most five an hour, and only the newest works | 1 | Pass | RRS-F-002 |
| TC-PWD-06 | Resetting the password lifts a lockout from wrong passwords | 1 | Pass | RRS-F-002, RRS-F-003 |
| TC-PWD-07 | The new password follows the sign-up rules; reset codes left unused for a day are removed | 1 | Pass | RRS-F-002 |

### `QueryParameterizationTest` (Parameterized queries only [RRS-SR-003])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `everyCustomQueryUsesNamedParameters` | 1 | Pass | RRS-SR-003 |
| No ID | `productionCodeDoesNotUseRawSqlApis` | 1 | Pass | RRS-SR-003 |

### `RailJourneyIntegrationTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-ACC-01 | A passenger edits their profile, changes password and keeps a private list of travellers | 1 | Pass | - |
| TC-ACC-02 | 'Save these travellers' keeps each name once, with tidy spacing, and updates it on the next booking | 1 | Pass | RRS-F-006 |
| TC-ADM-01 | An admin creates a train that appears in the next search; bad routes are rejected | 1 | Pass | RRS-F-015 |
| TC-ADM-02 | Admin routes refuse passengers and guests | 1 | Pass | RRS-SR-004 |
| TC-ADM-03 | A train with tickets keeps its route, cannot be deleted, and can be stopped from taking bookings | 1 | Pass | RRS-F-015 |
| TC-ADM-04 | The bookings report filters by train and date, and the chart places the traveller in a berth | 1 | Pass | RRS-F-016 |
| TC-ADM-05 | Changing the rule table changes the next refund | 1 | Pass | RRS-F-013 |
| TC-ADM-06 | An admin edits a train and the change shows in the timetable and search | 1 | Pass | RRS-F-015 |
| TC-ADM-07 | Editing a train: classes and route change freely without tickets; with tickets, coaches can only be added | 1 | Pass | RRS-F-015 |
| TC-BK-01 | Booking holds confirmed berths, prices them on the server and gives a 10-digit PNR owned by the caller | 1 | Pass | RRS-F-006 |
| TC-BK-02 | Missing or invalid traveller details are rejected field by field | 1 | Pass | RRS-F-007 |
| TC-BK-03 / TC-HIS-01 | The paid ticket downloads as PDF and appears in the owner's history | 1 | Pass | RRS-F-009, RRS-F-010, RRS-F-014 |
| TC-BK-04 | The fare quote before booking matches what the booking then charges, part by part | 1 | Pass | RRS-F-006 |
| TC-CAN-01 | Cancelling some travellers refunds them, leaves the rest booked, and refuses a repeat | 1 | Pass | RRS-F-012 |
| TC-CAN-02 | Refunds follow the rule table: flat charge beyond 48 h, 25% from 12 h, 50% from 4 h, nothing under 4 h | 1 | Pass | RRS-F-013 |
| TC-CAN-03 | A cancellation hands its berth to the first RAC traveller | 1 | Pass | RRS-F-012 |
| TC-CAN-04 | A fully waitlisted ticket is cancelled and refunded automatically once the chart is prepared | 1 | Pass | RRS-F-012 |
| TC-CAN-12 | If Razorpay cannot start the refund, nothing is cancelled | 1 | Pass | RRS-F-012 |
| TC-CAN-13 | Cancelling an unpaid or already cancelled ticket, nobody, a traveller from another ticket, or after departure is refused | 1 | Pass | RRS-F-012 |
| TC-HIS-02 | History lists the newest journey first; upcoming trips list the soonest first | 1 | Pass | RRS-F-014 |
| TC-MAIL-01 | A paid booking emails the e-ticket as a PDF to the passenger | 1 | Pass | RRS-F-010 |
| TC-MAIL-02 | Cancelling travellers emails the refund amount and the Razorpay refund id | 1 | Pass | RRS-F-012 |
| TC-MAIL-03 | A mail server failure never undoes a booking; a declined payment sends no ticket | 1 | Pass | - |
| TC-MAIL-04 | A payment found later (closed tab) still emails the ticket | 1 | Pass | RRS-F-010 |
| TC-OPS-03 | Unknown API paths are 401 to guests and 404 JSON when signed in, unknown pages are the 404 page, and pages carry a CSP | 1 | Pass | RRS-SR-003 |
| TC-PAY-01 | A verified Razorpay payment books the ticket; reporting it again does not record it twice | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-02 | A declined payment is recorded and keeps the hold, so the passenger can pay again | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-03 | An unpaid hold expires after 10 minutes, its berths return, and it can no longer be paid | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-04 | Releasing a hold frees the berths at once | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-05 | A forged signature, or a payment from another booking's order, books nothing | 1 | Pass | RRS-F-008, RRS-F-009, RRS-SR-003 |
| TC-PAY-06 | Paid but the tab was closed: the payment is found and books the ticket instead of the hold expiring | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-07 | Money that arrives after the hold was released is refunded in full automatically | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-08 | If Razorpay cannot be asked, a lapsed hold waits up to 30 minutes before its berths are released | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-09 | An authorised payment is captured before the ticket is booked; without keys, payment is unavailable | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PNR-01 | Public PNR status shows statuses but never names; unpaid and unknown PNRs are not found | 1 | Pass | RRS-F-011 |
| TC-SEC-06 | A guest cannot book, pay, list bookings or open a ticket | 1 | Pass | RRS-SR-004, RRS-SR-006 |
| TC-SEC-07 | Another passenger cannot see, pay, cancel or download someone else's booking | 1 | Pass | RRS-F-012, RRS-SR-004 |
| TC-SRC-01 | Guests can search; results carry times, classes, fares and availability | 1 | Pass | RRS-F-004, RRS-F-005, RRS-SR-006 |
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

### `RazorpayGatewayTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-PAY-12 | Orders are created in paise with the key as Basic auth, and read back | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-13 | Payments are read with masked card details; an order's attempts are listed | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-14 | Refunds and captures are sent for the exact amount | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-15 | Razorpay errors and bad ids become a gateway failure; no keys means payment is off | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-16 | Every payment method is described without exposing full numbers | 1 | Pass | RRS-F-008, RRS-F-009 |

### `RazorpaySignatureTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-PAY-10 | The payment signature is HMAC-SHA256 of order id \| payment id with the key secret | 1 | Pass | RRS-F-008, RRS-F-009 |
| TC-PAY-11 | Any change to the order, payment, secret or signature fails the check | 1 | Pass | RRS-F-008, RRS-F-009 |

### `SafeTextRulesTest` (SafeTextRules: SQL injection and XSS rejection [RRS-SR-003])

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| No ID | `allowsNormalInput` | 14 | Pass | RRS-SR-003 |
| No ID | `rejectsMaliciousInput` | 16 | Pass | RRS-SR-003 |
| No ID | `requireSafeThrowsWithoutEchoingThePayload` | 1 | Pass | RRS-SR-003 |
| No ID | `treatsNullAsSafeSoNotBlankCanReportMissingValues` | 1 | Pass | RRS-SR-003 |

### `SchemaUpgradesTest`

| ID | Test | Runs | Result | Requirements |
|---|---|---:|---|---|
| TC-PAY-17 | Old ENUM payment columns are widened on start, and a second start changes nothing | 1 | Pass | RRS-F-008, RRS-F-009 |

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
