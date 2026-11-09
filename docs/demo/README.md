# Demonstration Videos

Mini-project demonstration videos for the Software Engineering course (`UE24CS341A`), Team 16.

| Sprint | File | Length | What it shows |
|---|---|---|---|
| Sprint 1 | [`sprint-01-demo.mp4`](sprint-01-demo.mp4) | 1 min 15 s | The first working product: search, availability, booking with a PNR, printable slip, PNR status |
| Sprint 2 | [`sprint-02-demo.mp4`](sprint-02-demo.mp4) | 2 min 00 s | The final product: Google sign-in, booking, Razorpay payment, e-ticket and emails, cancellation with refund, operations console |

Both are silent screen recordings at 1080p.

---

# Sprint 02 Demonstration Video

- **File:** [`sprint-02-demo.mp4`](sprint-02-demo.mp4)
- **Duration:** 2 minutes 0 seconds
- **Resolution:** 1080p Full HD, 30 fps (silent walkthrough)
- **Recorded on:** 9 November 2026, on the live site `https://railway-reservation.srspace.dev` (the app on the team laptop behind a Cloudflare tunnel), with Razorpay in test mode and real Gmail.
- **Requirements shown:** RRS-F-002, RRS-F-004 to RRS-F-010, RRS-F-012 to RRS-F-016, RRS-SR-004, RRS-SR-006

The recording was 3 min 08 s of real time. It was sped up evenly (about 1.57 times) to fit the two-minute limit; nothing was cut, so every step is still in it. The waits while Google and Razorpay load are therefore shorter than they would be for a user.

## Demonstrated Workflow

1. **Search as a guest (`0:00 - 0:10`)**
   - Home page with the "Leaving Chennai Central" board.
   - From `KSR Bengaluru City (SBC)` to `Chennai Central (MAS)` on Thursday 12 November, chosen with the station search.

2. **Results and class choice (`0:10 - 0:18`)**
   - Five trains with times, stops, fares and live availability for each class.
   - Opened Executive Chair Car on 12028 Chennai Shatabdi Express: six-day availability strip and fare breakdown, then "Book".

3. **Sign in with Google (`0:18 - 0:25`)**
   - Booking needs sign-in (RRS-SR-006), so the site asked to sign in and kept the chosen train, class and date.
   - Signed in with a Google account already linked to the passenger's account (RRS-F-002).

4. **Travellers and review (`0:25 - 0:36`)**
   - Two travellers with gender, seat preference and meal; the page also offers the account's saved travellers.
   - Review page with the fare for two (₹2,988) before anything is held.

5. **Payment through Razorpay (`0:36 - 0:53`)**
   - "Hold berths and pay": berths held for 10 minutes with a countdown (RRS-F-006).
   - Razorpay Checkout in test mode: Netbanking, the test bank page, Success (RRS-F-008, RRS-F-009).

6. **E-ticket and PDF (`0:53 - 1:01`)**
   - Ticket page with PNR `4885687971`, both travellers confirmed in coach E1, and Razorpay's payment ID (RRS-F-010).
   - The PDF e-ticket opened from "Download PDF".

7. **Booking email (`1:01 - 1:06`)**
   - The confirmation email in Gmail with the PDF attached.

8. **Cancellation and refund (`1:06 - 1:23`)**
   - Cancelled one traveller. The refund (₹1,242 after the flat charge) was shown before confirming (RRS-F-012, RRS-F-013).
   - Ticket marked "Partly cancelled" with the Razorpay refund ID, and the cancellation email in Gmail.

9. **Operations console (`1:23 - 1:49`)**
   - Signed in as the admin: overview with today's bookings, revenue and refunds, and the last seven days.
   - Bookings report with the new booking at the top (RRS-F-016), the reservation chart berth by berth, and the train list (RRS-F-015). Only the admin role can open these pages (RRS-SR-004).

10. **My trips and account (`1:49 - 2:00`)**
    - The ticket again, then My trips with the booking (RRS-F-014).
    - The account page showing the linked Google account, and back to the console overview.

---

# Sprint 01 Demonstration Video

## Video Overview

- **File:** [`sprint-01-demo.mp4`](sprint-01-demo.mp4)
- **Duration:** 1 minute 15 seconds
- **Resolution:** 1080p Full HD (Silent Walkthrough)
- **Sprint Scope:** Sprint 01 Deliverables (RRS-F-001, RRS-F-002, RRS-F-003, RRS-F-004, RRS-F-005, RRS-F-006, RRS-F-007, RRS-F-008, RRS-NF-003)
- **Note:** this video shows the first version of the product. The interface was rebuilt in Sprint 3 (PR #62 to PR #68), so it looks different from the Sprint 2 video above.

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

6. **PNR Inquiry Status Lookup (`1:15`)**
   - Opened the top navigation `Check PNR Status` modal.
   - Queried the generated PNR to verify real-time booking retrieval from the backend database.
