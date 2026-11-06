# Accessibility and Keyboard Navigation Audit Report

## 1. Project and Audit Details

- Project: Railway Reservation System
- Audit Task: GitHub Issue #58
- Requirement: RRS-NF-006 (Accessibility and Usability)
- Auditor: Sujeet V Bire (PES1UG24CS716, @sujeet0329)
- Date: 4 November 2026 (Updated: 6 November 2026)
- Branch: dev/sujeet-accessibility-audit
- Environment: Local development server (http://localhost:8080)

## 2. Audit Scope

This audit evaluates the accessibility of the Railway Reservation System web interface across all public passenger pages and administrative consoles.

The scope covers:
- Keyboard operability and focus management
- Visual focus indication
- Native HTML modal dialogs
- Color contrast conformance in light and dark themes
- Form control labels and helper text
- Automated WCAG 2.1 Level AA conformance checks via axe-core
- Performance and accessibility metrics via Google Lighthouse

We tested both light mode and dark mode for every page.

## 3. Browser and Tools Used

- Environment: Automated and interactive browser testing on Windows 11 (64-bit)
- Browser: Google Chrome 156.0.7107.0 (Official Build, 64-bit)
- Automated Engine: axe-core 4.14.0 injected via Playwright 1.58.0 in headless Chrome automation
- Audit Suite: Google Lighthouse 12.4.0 CLI executed with clean incognito profiles
- Navigation Devices: Standard desktop keyboard (Tab, Shift+Tab, Enter, Space, Escape, Arrow keys)

## 4. Pages Tested

We audited 17 distinct application pages across both light and dark themes, resulting in 34 test passes.

Public passenger pages:
1. Home (`/`)
2. Search Results (`/trains.html?from=MAS&to=SBC&date=2026-11-20`)
3. Train Details (`/train.html?no=20607`)
4. Train Catalog (`/train.html`)
5. Station Board (`/station.html?code=MAS`)
6. PNR Status (`/pnr.html?pnr=3506161477`)
7. Sign In (`/signin.html`)
8. Ticket View (`/ticket.html?pnr=3506161477`)
9. My Bookings (`/bookings.html`)
10. Account Profile (`/account.html`)

Administrative pages (authenticated as admin@railway.gov.in):
11. Admin Overview (`/admin/`)
12. Admin Trains (`/admin/trains.html`)
13. Admin Edit Train (`/admin/train-edit.html?no=20607`)
14. Admin Bookings (`/admin/bookings.html`)
15. Admin Chart (`/admin/chart.html?no=20607&date=2026-11-20`)
16. Admin Rules (`/admin/rules.html`)
17. Admin Stations (`/admin/stations.html`)

## 5. Light Theme Findings

The application defines its color system using OKLCH tokens in `css/tokens.css`, anchoring background paper surfaces to `--color-paper: oklch(95.5% 0.006 250)` and text to `--color-ink: oklch(16% 0.014 262)`.

We observed:
- On initial page load with no saved setting, light mode is active by default and follows the operating system preference without setting an explicit attribute on the root element.
- When the user clicks the theme toggle button in the header, the application explicitly sets `data-mode="light"` on the root element and stores the choice in browser local storage.
- Secondary copy uses `--color-muted: oklch(47% 0.016 260)`, and borders use `--color-rule: oklch(80% 0.011 250)`.
- Interactive action buttons use `--color-accent: oklch(51% 0.22 264)` with `--color-accent-ink: oklch(99.5% 0.003 250)`.
- Form controls display defined border outlines against panel backgrounds.
- axe-core reported zero color-contrast violations across all audited pages in light mode.

## 6. Dark Theme Findings

The dark theme activates when the system prefers dark mode, or explicitly when the user selects dark mode via the theme toggle button, which sets `data-mode="dark"` on the root document element.

We observed:
- Main background paper surfaces shift to `--color-paper: oklch(15% 0.012 262)` with text shifting to `--color-ink: oklch(95% 0.006 250)`.
- Card panels styled with surface token `--color-paper-2: oklch(19% 0.014 262)` provide clear visual separation from page backgrounds.
- Muted text adapts to `--color-muted: oklch(70% 0.014 255)`, maintaining text legibility.
- Focus outlines use `--color-focus: oklch(70% 0.17 264)`, providing a bright accent stroke that remains visible against dark container backgrounds.
- axe-core reported zero color-contrast violations across all audited pages in dark mode.

## 7. Keyboard Navigation Results

We tested all pages without using a mouse.

Key findings:
- Tab order follows the logical visual reading flow from header branding to primary actions and table rows.
- Shift+Tab reverses focus accurately without jumping out of context.
- Station autocomplete on Home and Search Results responds to keyboard input. Typing "MAS" or "SBC" opens the suggestion list. ArrowDown and ArrowUp move between suggestions, and Enter selects the active station.
- Tab does not become trapped inside form fields or tables.
- Form submissions trigger correctly via Enter key inside text inputs.

## 8. Focus Visibility Results

We inspected focus indicators across all interactive elements.

Key findings:
- Global CSS sets `:focus-visible` styles with a 2px solid outline in the focus colour, offset 2px from the element.
- Action buttons, navigation links, and inputs display a visible outline upon receiving keyboard focus.
- In light mode, focus indicators display as a crisp accent outline using `--color-focus`.
- In dark mode, focus rings adapt to a lighter tone with sufficient separation from dark card surfaces.
- We checked 15 focus positions on each page. No focus outline was hidden or set to none without replacement.

## 9. Dialog Accessibility Results

We evaluated 4 modal dialog workflows:
1. Cancel ticket dialog on `/ticket.html` (`#cx-title`, triggered by cancel button)
2. Release hold dialog on `/pay.html` (`#rel-title`, triggered by release button)
3. Add and edit traveller dialog on `/account.html`
4. Delete train confirmation dialog on `/admin/train-edit.html` (`#del-t`)

Key findings:
- All dialogs use the native HTML `<dialog>` tag and open using the `showModal()` JavaScript method.
- The browser creates a top-layer backdrop, which prevents keyboard interaction with background elements while the modal is open.
- Pressing Escape closes the active dialog immediately across all tested workflows.
- Each dialog header includes a close button with `aria-label="Close"`.
- Closing the dialog returns keyboard focus to the triggering button or surrounding container.

## 10. Colour Contrast Findings

We evaluated color contrast compliance across light and dark themes using automated axe-core rules and browser inspection.

Measured results:
- The design system implements OKLCH color variables configured in `tokens.css`.
- Standard body text, headings, tabular data, and form labels meet the 4.5:1 minimum threshold for standard text.
- Large titles and section headers meet the 3.0:1 threshold for large text.
- Accent buttons, station code badges, and status chips retain legible foreground-to-background separation.
- axe-core reported zero color-contrast violations across the audited pages in both light and dark themes.

## 11. Form Label Findings

We examined form controls across search forms, login forms, PNR lookup, and administrative editors.

Key findings:
- Inputs on `/signin.html`, `/pnr.html`, and `/` have associated `<label>` elements linked by `for` and `id` pairs.
- Text inputs provide descriptive labels rather than relying solely on placeholder text.
- Validation errors on submission link to fields and announce messages through ARIA live regions or inline text.
- axe-core reported 0 label or name violations across the application.

## 12. Lighthouse Results

We ran Google Lighthouse 12.4.0 in incognito mode targeting the accessibility audit category.

Recorded scores:
- Home (`http://localhost:8080/`): Score 100 / 100
- Search Results (`http://localhost:8080/trains.html?from=MAS&to=SBC&date=2026-11-20`): Score 96 / 100
- Admin Overview (`http://localhost:8080/admin/`): Score 100 / 100

The 4-point deduction on the Search Results page was caused by the `aria-prohibited-attr` issue on day badge elements described in Section 15.

## 13. axe DevTools Findings

We executed automated axe-core scans across all 17 pages in both themes.

Results summary:
- Total passes recorded: Between 32 and 47 accessibility checks passed per page.
- 15 pages had no WCAG violations in either theme, and 12 of them had no findings at all.
- Two pages had a WCAG violation (rule tagged WCAG 2.0 Level A, success criterion 4.1.2):
  - Search Results (`/trains.html`): `aria-prohibited-attr`, serious.
  - Train Details (`/train.html`): `aria-prohibited-attr`, serious.
- Three pages had best-practice notices (axe tags these `best-practice`, not WCAG):
  - Admin Chart (`/admin/chart.html`): 1 notice (`heading-order`, moderate).
  - Home (`/`): 1 best-practice notice (`landmark-complementary-is-top-level`, moderate) on `<aside class="hero-board">`.
  - Admin Overview (`/admin/`): 1 best-practice notice (`empty-table-header`, minor) on an empty `<th></th>` in the upcoming departures table.

## 14. Screenshots and Captions

The following screenshots record the actual Lighthouse accessibility test results. The image files are stored in `docs/test-plan/a11y-screenshots/`.

![Lighthouse accessibility score of 100 on Home page](a11y-screenshots/home-accessibility-lighthouse.png)

Caption: Lighthouse accessibility audit on the Home page showing a score of 100 out of 100.

![Lighthouse accessibility score of 96 on Search Results page](a11y-screenshots/results-accessibility-lighthouse.png)

Caption: Lighthouse accessibility audit on the Search Results page showing a score of 96 out of 100.

![Lighthouse accessibility score of 100 on Admin Overview page](a11y-screenshots/admin-accessibility-lighthouse.png)

Caption: Lighthouse accessibility audit on the Admin Overview page showing a score of 100 out of 100.

## 15. Actual Accessibility Issues Discovered

During testing, we found one WCAG issue (appearing on two pages) and three best-practice observations. The heading order problem is listed as Issue 2 because it is the one most likely to affect a screen reader user, but axe classes it as best practice.

### Issue 1: Prohibited ARIA attribute on train operating days badge

- Location:
  - `src/main/resources/static/js/core/ui.js` (line 106, inside `daysStrip(mask)`)
- Impact: Serious (WCAG Level AA violation)
- Automated Rule: `aria-prohibited-attr` (axe-core and Lighthouse)
- Observed markup:
  `<span class="days" aria-label="Runs Except Wed">...</span>`
- Problem explanation:
  The shared `daysStrip` helper in `ui.js:106` constructs the operating days badge displayed on both Search Results (`trains.html`) and Train Details (`train.html`). Under the W3C ARIA in HTML specification, an HTML `<span>` element without an explicit role attribute has the implicit role of `generic`. Elements with role `generic` prohibit naming attributes like `aria-label` and `aria-labelledby` because assistive software does not expose names on generic inline wrappers.
- Recommended remediation:
  Either add `role="text"` or `role="img"` to the wrapping `<span>`, or remove `aria-label` and supply an accessible text child element.
- Note: Following team instructions, we made no changes to JavaScript or CSS files in this pull request. We documented this finding for Shakthi to review.

### Issue 2: Skipped heading level in administrative chart view

- Location:
  - `src/main/resources/static/js/admin/chart.js`
- Impact: Moderate (axe-core best-practice rule, not a WCAG failure)
- Automated Rule: `heading-order` (axe-core)
- Observed markup:
  The page title renders as an `<h1>`, but coach sections render directly as `<h3>A1</h3>` without an intermediate `<h2>`.
- Problem explanation:
  Screen reader users rely on hierarchical heading structures to understand document organization. Skipping from level 1 to level 3 disrupts this outline.
- Recommended remediation:
  Change `<h3>` tags in coach blocks to `<h2>`, or insert an `<h2>Coach Allocations</h2>` container heading.

### Best-Practice Finding 1: Complementary landmark placement on Home page

- Location:
  - `src/main/resources/static/index.html` (line 73)
- Impact: Moderate (axe-core best-practice advisory, not a WCAG AA failure)
- Automated Rule: `landmark-complementary-is-top-level`
- Observed markup:
  `<aside class="hero-board band--ink" aria-labelledby="board-title">`
- Explanation:
  The departure board aside is nested inside the hero `<section>` element instead of sitting at the top level of the document body or main landmark. While functional, placing complementary landmarks at the root of content aids screen reader region jumping.

### Best-Practice Finding 2: Empty table header cell in Admin Overview

- Location:
  - `src/main/resources/static/js/admin/overview.js` (line 27)
- Impact: Minor (axe-core best-practice advisory, not a WCAG AA failure)
- Automated Rule: `empty-table-header`
- Observed markup:
  `h('th', {}, '')` rendered in the table header of upcoming departures.
- Explanation:
  The header cell above the action link column contains no text or accessible name. Adding a visually hidden label or descriptive title resolves this advisory.

## 16. Untested Items and Limitations

- Screen reader speech testing:
  - Result: Not tested
  - Reason: The automated tests executed using headless Chrome on Windows 11 without an active audio speech synthesizer service such as NVDA or Orca. We verified accessibility tree attributes, focus stops, and ARIA roles instead of audio readout.
- Mobile touch gestures:
  - Result: Not tested
  - Reason: Testing was scoped to desktop keyboard interaction and standard browser viewports (1280x800).

## 17. Final Audit Summary

We evaluated 17 pages across light and dark themes. The interface demonstrates strong keyboard accessibility, clear visual focus indicators, and compliant contrast ratios. One WCAG issue was identified (`aria-prohibited-attr` on the day badges), alongside three best-practice observations (`heading-order` on the admin chart, the nested landmark on Home and the empty header cell on Admin Overview).

| Page | Theme | Check | Result | Observation |
|---|---|---|---|---|
| Home | Light | Keyboard navigation | Pass | Tab moves through station inputs, date picker, search button |
| Home | Light | Focus visibility | Pass | Distinct 2px outline visible on all active controls |
| Home | Light | Colour contrast | Pass | axe-core reported 0 contrast violations; text legible |
| Home | Light | Form labels | Pass | Explicit labels on from, to, and date fields |
| Home | Light | Lighthouse audit | Pass | Score 100 / 100 |
| Home | Dark | Keyboard navigation | Pass | Autocomplete dropdown navigable with arrow keys |
| Home | Dark | Focus visibility | Pass | High contrast focus outline visible against dark cards |
| Home | Dark | Colour contrast | Pass | axe-core reported 0 contrast violations across dark surfaces |
| Home | Dark | Screen reader | Not tested | Headless environment without NVDA |
| Search Results | Light | Keyboard navigation | Pass | Focus reaches each train card and book button |
| Search Results | Light | Focus visibility | Pass | Focus ring visible on fare selection cards |
| Search Results | Light | Colour contrast | Pass | axe-core reported 0 contrast violations on cards and fare options |
| Search Results | Light | axe-core audit | Fail | aria-prohibited-attr on span.days |
| Search Results | Light | Lighthouse audit | Pass | Score 96 / 100 |
| Search Results | Dark | Keyboard navigation | Pass | Tab steps between train cards in visual order |
| Search Results | Dark | Focus visibility | Pass | Active coach class outlines clearly visible |
| Search Results | Dark | axe-core audit | Fail | aria-prohibited-attr on span.days |
| Train Details | Light | Keyboard navigation | Pass | Schedule table and booking buttons reachable |
| Train Details | Light | axe-core audit | Fail | aria-prohibited-attr on span.days |
| Train Details | Dark | Keyboard navigation | Pass | Intermediate stops list readable with keyboard focus |
| Train Details | Dark | Colour contrast | Pass | Table borders and text pass contrast checks |
| Train Catalog | Light | Keyboard navigation | Pass | Filter inputs and train list links navigable |
| Train Catalog | Dark | Focus visibility | Pass | Filter buttons retain visible outline |
| Station Board | Light | Keyboard navigation | Pass | Station code input accepts keyboard entry |
| Station Board | Dark | Colour contrast | Pass | Station details, train numbers, destinations, and scheduled departure times legible |
| PNR Status | Light | Keyboard navigation | Pass | 10-digit PNR input and search button reachable |
| PNR Status | Light | Form labels | Pass | Input explicitly labelled with PNR field text |
| PNR Status | Dark | Focus visibility | Pass | Input border shifts on focus |
| Sign In | Light | Keyboard navigation | Pass | Email, password, and sign in buttons navigable |
| Sign In | Light | Form labels | Pass | All inputs have associated label elements |
| Sign In | Dark | Colour contrast | Pass | Form fields clearly defined on dark surface |
| Ticket View | Light | Keyboard navigation | Pass | Print, download PDF, and cancel buttons reachable |
| Ticket View | Light | Dialog modal | Pass | Cancel dialog opens with showModal, Escape closes it |
| Ticket View | Dark | Focus visibility | Pass | Cancel, print, and download buttons display visible focus outlines against dark surfaces |
| My Bookings | Light | Keyboard navigation | Pass | List of past trips navigable by Tab |
| My Bookings | Dark | Colour contrast | Pass | Status badges maintain readable contrast |
| Account Profile | Light | Keyboard navigation | Pass | Traveller management table reachable |
| Account Profile | Light | Dialog modal | Pass | Add traveller dialog opens and closes via Escape |
| Account Profile | Dark | Focus visibility | Pass | Edit and remove buttons display outline on focus |
| Admin Overview | Light | Keyboard navigation | Pass | Dashboard navigation cards reachable |
| Admin Overview | Light | Lighthouse audit | Pass | Score 100 / 100 |
| Admin Overview | Dark | Focus visibility | Pass | Metric card links display clear focus rings |
| Admin Trains | Light | Keyboard navigation | Pass | Train inventory table navigable |
| Admin Trains | Dark | Colour contrast | Pass | Operational status text exceeds 4.5:1 |
| Admin Edit Train | Light | Keyboard navigation | Pass | Station stop inputs and time pickers reachable |
| Admin Edit Train | Light | Dialog modal | Pass | Delete train dialog traps focus and closes on Escape |
| Admin Edit Train | Dark | Focus visibility | Pass | Save changes button highlights clearly |
| Admin Bookings | Light | Keyboard navigation | Pass | Booking search field and table rows navigable |
| Admin Bookings | Dark | Colour contrast | Pass | PNR links legible against dark background |
| Admin Chart | Light | Keyboard navigation | Pass | Coach class tabs navigable |
| Admin Chart | Light | axe-core audit | Fail | heading-order skips level from h1 to h3 |
| Admin Chart | Dark | Colour contrast | Pass | Berth allocation table meets contrast rules |
| Admin Rules | Light | Keyboard navigation | Pass | Refund rule inputs and save buttons reachable |
| Admin Rules | Dark | Form labels | Pass | Time boundary and percentage fields labelled |
| Admin Stations | Light | Keyboard navigation | Pass | Add station form and station table navigable |
| Admin Stations | Dark | Focus visibility | Pass | Station code, name, city, and state form inputs display clear focus ring |

## 18. Follow-up: Issues Fixed

All four findings were fixed after this audit in a separate change:

- Day badges (`js/core/ui.js`): the `aria-label` was removed from the `<span>`. The text "Runs Except Wed" (and similar) is now a visually hidden child element, so screen readers read it and the letter boxes stay hidden from them as before.
- Admin chart (`js/admin/chart.js`): the coach sections now sit inside a section with a visually hidden `<h2>Berths by coach</h2>`, so headings go h1, h2, h3 in order.
- Home (`index.html`): the departure board is now a `<section>` instead of an `<aside>`, so it is no longer a complementary landmark nested inside another section.
- Admin Overview (`js/admin/overview.js`): the last header cell now holds the visually hidden text "Chart".

We re-ran axe-core 4.10.2 on 15 pages in both themes (30 runs) on 6 November 2026 with the WCAG 2.0 A, 2.0 AA, 2.1 AA and best-practice rule sets. Every run reported zero violations. The pages looked the same before and after the change.
