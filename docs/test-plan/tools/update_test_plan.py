#!/usr/bin/env python3
"""Rebuild the test case list and traceability matrix for the test plan.

Run from the repository root after a fresh `./mvnw clean test`:

    python3 docs/test-plan/tools/update_test_plan.py

It reads:
  - target/surefire-reports/TEST-*.xml   (what ran, how many, pass or fail)
  - src/test/java/**/*.java               (@DisplayName text and test IDs)
  - the REQUIREMENTS table below          (hand-kept mapping from SRS IDs to tests)

and rewrites:
  - docs/test-plan/README.md
  - docs/test-plan/test-plan-railway-reservation-system.docx (selected sections only)

The PDF is not touched. Export it again from Word after running this.
Needs python-docx (pip3 install python-docx).
"""

import copy
import datetime as dt
import re
import sys
import xml.etree.ElementTree as ET
from collections import OrderedDict, defaultdict
from pathlib import Path

try:
    import docx
    from docx.oxml import OxmlElement
    from docx.oxml.ns import qn
    from docx.shared import Inches, Pt
    from docx.text.paragraph import Paragraph
except ImportError:
    sys.exit("python-docx is missing: pip3 install python-docx")

ROOT = Path(__file__).resolve().parents[3]
REPORTS = ROOT / "target" / "surefire-reports"
TEST_SRC = ROOT / "src" / "test" / "java"
README = ROOT / "docs" / "test-plan" / "README.md"
DOCX = ROOT / "docs" / "test-plan" / "test-plan-railway-reservation-system.docx"

ID_RE = re.compile(r"TC-[A-Z]+-\d+")

# ---------------------------------------------------------------------------
# Hand-kept mapping. Edit this when tests or requirements change.
#
# refs: a test ID ("TC-BK-01"), an ID prefix ending in "*" ("TC-SRC-*"),
#       a test class ("SeatLedgerTest") or a single method ("UserLoginControllerTest#locksAccount...").
# status: "Covered", "Partly covered" or "Not covered".
# manual: evidence outside the Maven suite (JMeter, audits).
# note: one plain sentence on what is or is not checked.
# ---------------------------------------------------------------------------
# Not in SRS v1.0. Kept as the last row of the matrix and left out of the SRS counts.
NF_006 = "RRS-NF-006 (added in Sprint 3, not in SRS v1.0)"

REQUIREMENTS = [
    ("RRS-F-001", "User registration", ["UserRegistrationControllerTest",
        "UserLoginControllerTest#registeredUserCanImmediatelyLogIn"], "Covered", None,
     "Sign up, field rules, duplicate email (any case) and BCrypt storage."),
    ("RRS-F-002", "User login", ["UserLoginControllerTest"], "Covered", None,
     "Correct login returns a signed JWT; wrong password and unknown email get the same 401."),
    ("RRS-F-003", "Account lock after 5 failed logins", [
        "UserLoginControllerTest#locksAccountAfterFiveFailedAttemptsAndBlocksCorrectPassword",
        "UserLoginControllerTest#persistsLockoutAcrossCommittedLoginRequests",
        "UserLoginControllerTest#concurrentFailedLoginsAreCountedWithoutLostUpdates",
        "UserLoginControllerTest#successfulLoginResetsFailuresAndExpiredLock"], "Covered", None,
     "The lock is tested, but the API answers \"Invalid credentials.\" instead of the lock message "
     "in the SRS, so the response does not reveal that the account exists."),
    ("RRS-F-004", "Train search by source, destination and date", ["TC-SRC-*"], "Covered", None,
     "Includes intermediate stops, running days, nearby stations and bad input (SAME_STATION, past date, more than 60 days)."),
    ("RRS-F-005", "Seat availability", ["TC-SRC-01", "TC-SRC-06", "TC-SEAT-01", "TC-SEAT-05",
        "TC-SEAT-06", "TC-SEAT-11", "TC-DEM-*"], "Covered", None,
     "Availability per class, the availability calendar, and berths freed at an intermediate station."),
    ("RRS-F-006", "Book ticket with PNR", ["TC-BK-01", "TC-TQ-01", "TC-SEAT-*", "TC-FARE-*"], "Covered", None,
     "Berth allocation, RAC and waiting list, Tatkal window, server-side fares and a 10-digit PNR."),
    ("RRS-F-007", "Passenger details for each seat", ["TC-BK-02"], "Covered", None,
     "Missing or invalid name, age or gender is rejected field by field."),
    ("RRS-F-008", "Payment gateway after booking", ["TC-PAY-01", "TC-PAY-02"], "Partly covered", None,
     "The app uses its own simulated gateway with test cards and UPI IDs, so there is no redirect to an external page to test."),
    ("RRS-F-009", "Confirm only after successful payment", ["TC-PAY-*", "TC-BK-03"], "Covered", None,
     "A declined card keeps the hold for a retry instead of marking the booking FAILED; "
     "berths come back when the 10 minute hold expires or is released."),
    ("RRS-F-010", "E-ticket after payment", ["TC-BK-03"], "Partly covered", None,
     "The test checks the PDF is refused before payment and is a real PDF after it, but does not read the PNR or fare inside the file."),
    ("RRS-F-011", "PNR status check", ["TC-PNR-01"], "Covered", None,
     "A paid PNR shows each traveller's status but no names; unpaid and unknown PNRs return not found."),
    ("RRS-F-012", "Cancel ticket", ["TC-CAN-01", "TC-CAN-03", "TC-CAN-04", "TC-SEC-07"], "Covered", None,
     "Partial cancellation, RAC promotion and automatic refund of waitlisted tickets after charting."),
    ("RRS-F-013", "Refund as per cancellation rules", ["TC-CAN-02", "TC-ADM-05", "CancellationRefundTest"],
     "Covered", None, "Refund tiers and the preview shown before confirming."),
    ("RRS-F-014", "Booking history", ["TC-HIS-01"], "Partly covered", None,
     "The paid booking appears in history, but newest-first ordering is not checked (the test has one booking)."),
    ("RRS-F-015", "Admin add, update, delete trains, routes, fares", ["TC-ADM-01", "TC-ADM-03"],
     "Partly covered", None,
     "Adding a train and blocking delete when it has bookings are tested; editing an existing train "
     "(PUT /api/v1/admin/trains/{number}) has no test."),
    ("RRS-F-016", "Admin bookings report by date and train", ["TC-ADM-04"], "Covered", None,
     "Report filtered by train 12901 and date, plus the chart and its CSV."),
    ("RRS-NF-001", "Search within 3 s for 95% of requests", [], "Partly covered",
     "TC-PERF-01, JMeter run on 29 Oct 2026 (docs/performance/issue17-search-benchmark.md)",
     "p95 was 200 ms and 290 ms over two runs of 100 requests, but that was before the rebuild in PR #68 and has not been repeated since."),
    ("RRS-NF-002", "99% availability during demo", ["TC-OPS-01"], "Partly covered", None,
     "The health endpoint and a burst of parallel health checks are tested; real uptime over the demo period is not measured."),
    ("RRS-NF-003", "New user books within 5 minutes", [], "Not covered", None,
     "No automated test. The user test with 3 classmates (TC-UX-01) has no record in the repository."),
    ("RRS-NF-004", "100 concurrent users", [
        "SystemReliabilityAndLoggingTest#testSystemReliability_HighConcurrencyHealthRequestsDoNotCrashServer"],
     "Partly covered", "TC-PERF-02, JMeter run on 29 Oct 2026 with 100 threads, 0 errors",
     "Same caveat as RRS-NF-001: the JMeter run predates PR #68."),
    ("RRS-NF-005", "Log errors with timestamp and user id", ["TC-OPS-02"], "Covered", None,
     "ERROR level logs with user id and timestamp, daily rotation in logs/, and no stack traces in responses."),
    ("RRS-SR-001", "BCrypt password hashing", ["PasswordProtectionSecurityTest",
        "UserRegistrationControllerTest#registersUserWithBcryptAndDoesNotExposePasswordData"], "Covered", None,
     "Hash format and salt, and the password never appears in responses or logs."),
    ("RRS-SR-002", "HTTPS (TLS 1.2+)", [], "Not covered", None,
     "The app runs on plain HTTP at localhost:8080 and has no TLS settings in application.properties."),
    ("RRS-SR-003", "Server-side input validation (SQL injection, XSS)", ["InputValidationSecurityTest",
        "SafeTextRulesTest", "QueryParameterizationTest",
        "UserLoginControllerTest#rejectsSqlInjectionPayloadInLoginEmail", "TC-OPS-03"], "Covered", None,
     "Payloads on real endpoints, the SafeText rules on their own, bound query parameters and the CSP header."),
    ("RRS-SR-004", "Role based access control", ["TC-ADM-02", "TC-SEC-06", "TC-SEC-07"], "Covered", None,
     "A passenger gets 403 and a guest 401 on admin routes; passengers cannot open each other's bookings."),
    ("RRS-SR-005", "Session timeout after 15 minutes", ["TC-SEC-05"], "Covered", None,
     "The API side (401 Session Expired, token renewal) is tested; the redirect to the login page in the browser is not."),
    (NF_006, "Accessibility (keyboard, focus, contrast, labels)", [], "Covered", None,
     "Checked by hand with Lighthouse, axe and keyboard only, in light and dark themes; there is no automated test."),
]

# Work that is in flight. While the class (or file) is missing, the matrix says "pending".
PENDING = [
    {"what": "TC-SRC-07 onwards (search swap and date edges)", "where": "issue #57, Kishan",
     "test_class": "SearchBoundaryTest", "reqs": ["RRS-F-004"]},
    {"what": "TC-CAN-05 onwards (refund tier edges)", "where": "PR #69, Sujeet",
     "test_class": "CancellationRefundTest", "reqs": ["RRS-F-013"]},
    {"what": "accessibility audit (docs/test-plan/accessibility-audit.md)", "where": "PR #70, Sujeet",
     "file": "docs/test-plan/accessibility-audit.md", "reqs": [NF_006],
     "pending_note": "Sujeet's manual audit (Lighthouse, axe, keyboard only, light and dark themes) is in "
                     "PR #70 and not merged yet; there is no automated test."},
]

SCHEDULE = [
    ("Sprint 1: tests for registration, login, lockout, admin trains, input validation", "19-Oct-2026", "23-Oct-2026"),
    ("Sprint 2: feature tests added with each PR, JMeter search benchmark (29 Oct)", "26-Oct-2026", "30-Oct-2026"),
    ("App rebuilt and test suite rewritten (PR #62 to PR #68)", "02-Nov-2026", "03-Nov-2026"),
    ("Sprint 3: search and refund boundary tests, accessibility audit", "02-Nov-2026", "06-Nov-2026"),
    ("Sprint 3: test plan and traceability matrix update (issue #60)", "04-Nov-2026", "06-Nov-2026"),
]

ROLES = [
    ("Project guide", "Faculty", "Review and approve test plan"),
    ("Team lead", "Shakthi Raja G P", "Reviews and merges PRs; rebuilt the app and wrote RailJourneyIntegrationTest, SeatLedgerTest, FareCalculatorTest and the reliability tests"),
    ("Developer and tester", "Kishan S", "Login and registration tests, lockout; search boundary tests (#57)"),
    ("Developer and tester", "Sujeet V Bire", "Refund boundary tests (#59) and accessibility audit (#58)"),
    ("Developer and tester", "Likhith S", "Password, input validation and query parameterisation tests; session timeout tests (SessionTimeoutSecurityTest, TC-SEC-05); this test plan and matrix (#60)"),
]

BANNED = ["delve", "robust", "seamless", "leverage", "comprehensive", "crucial", "pivotal", "ensure",
          "utilize", "facilitate", "showcase", "streamline", "holistic", "cutting-edge", "landscape",
          "realm", "testament", "tapestry", "elevate", "navigate", "furthermore", "moreover",
          "additionally", "in conclusion", "it is worth noting", "it is important to note",
          "plays a vital role", "in today's world"]


# ---------------------------------------------------------------------------
# Reading the run
# ---------------------------------------------------------------------------
def natural_key(s):
    return [int(t) if t.isdigit() else t for t in re.split(r"(\d+)", s)]


def read_sources():
    """Return {simple class name: {"package", "display", "methods": OrderedDict(method -> display)}}."""
    classes = {}
    for path in sorted(TEST_SRC.rglob("*Test*.java")):
        text = path.read_text(encoding="utf-8")
        cls = path.stem
        pkg = path.parent.relative_to(TEST_SRC).as_posix().replace("com/railway/reservation", "").strip("/") or "(root)"
        info = {"package": pkg, "display": None, "methods": OrderedDict()}
        pending_display, is_test = None, False
        for line in text.splitlines():
            s = line.strip()
            m = re.match(r'@DisplayName\("(.*)"\)', s)
            if m:
                pending_display = m.group(1).replace('\\"', '"')
                continue
            if re.match(r"@(Test|ParameterizedTest|RepeatedTest)\b", s):
                is_test = True
                continue
            if re.search(r"\bclass\s+" + re.escape(cls) + r"\b", s):
                info["display"] = pending_display
                pending_display, is_test = None, False
                continue
            m = re.search(r"\bvoid\s+(\w+)\s*\(", s)
            if m:
                if is_test:
                    info["methods"][m.group(1)] = pending_display
                pending_display, is_test = None, False
        info["bodies"] = {}
        for method in info["methods"]:
            m = re.search(r"\bvoid\s+" + re.escape(method) + r"\s*\(", text)
            start = text.find("{", m.end()) if m else -1
            depth, i = 0, start
            while 0 <= i < len(text):
                depth += {"{": 1, "}": -1}.get(text[i], 0)
                if depth == 0:
                    break
                i += 1
            info["bodies"][method] = text[start:i + 1] if start >= 0 else ""
        if info["methods"]:
            classes[cls] = info
    return classes


def read_reports():
    """Return ({class: {method: [runs, failed]}}, totals, timestamp)."""
    files = sorted(REPORTS.glob("TEST-*.xml"))
    if not files:
        sys.exit("No surefire reports. Run ./mvnw clean test first.")
    results = defaultdict(lambda: defaultdict(lambda: [0, 0]))
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    stamps = []
    for f in files:
        suite = ET.parse(f).getroot()
        for k in totals:
            totals[k] += int(suite.get(k, 0))
        if suite.get("timestamp"):
            stamps.append(suite.get("timestamp"))
        # Classes with a class-level @DisplayName get that text in "classname",
        # so take the class from the file name (TEST-com.railway...ClassName.xml).
        cls = f.stem.split(".")[-1]
        for case in suite.iter("testcase"):
            method = case.get("name").split("(")[0].split("[")[0]
            r = results[cls][method]
            r[0] += 1
            if case.find("failure") is not None or case.find("error") is not None:
                r[1] += 1
    stamp = max(stamps) if stamps else dt.datetime.now().isoformat()
    return results, totals, dt.datetime.fromisoformat(stamp[:19])


KEEP_TOGETHER = ["Utf8", "Sha256", "Base64"]   # tokens whose digits belong to the word


def humanize(method):
    """registrationRejectsMaliciousNameWith400 -> registration rejects malicious name with 400."""
    words = method.replace("_", " ")
    for i, tok in enumerate(KEEP_TOGETHER):
        words = words.replace(tok, f"Keep{i}x")
    words = re.sub(r"([a-z])([A-Z0-9])", r"\1 \2", words)
    words = re.sub(r"([0-9])([A-Za-z])", r"\1 \2", words)
    words = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1 \2", words)
    for i, tok in enumerate(KEEP_TOGETHER):
        words = re.sub(rf"Keep\s?{i}\s?x", tok, words)
    return re.sub(r"^test ", "", re.sub(r"\s+", " ", words).lower().strip())


def build_cases(sources, results):
    """One entry per JUnit method that ran."""
    cases = []
    for cls, info in sources.items():
        class_ids = ID_RE.findall(info["display"] or "")
        for method, display in info["methods"].items():
            runs, failed = results.get(cls, {}).get(method, [0, 0])
            if runs == 0:
                continue
            ids = ID_RE.findall(display or "")
            if display and ids:
                desc = re.sub(r"^(TC-[A-Z]+-\d+(\s*/\s*)?)+[:\s]*", "", display)
            else:
                desc = display or humanize(method)
            cases.append({
                "cls": cls, "method": method, "ids": ids or class_ids, "own_id": bool(ids),
                "desc": desc, "runs": runs, "failed": failed, "package": info["package"],
                "body": info["bodies"].get(method, ""),
            })
    return cases


def ref_matches(ref, case):
    if ref.endswith("*"):
        return any(i.startswith(ref[:-1]) for i in case["ids"] if case["own_id"])
    if ref.startswith("TC-"):
        return ref in case["ids"] and case["own_id"]
    if "#" in ref:
        c, m = ref.split("#")
        return case["cls"] == c and case["method"] == m
    return case["cls"] == ref


def reqs_for(case):
    return [r[0] for r in REQUIREMENTS if any(ref_matches(x, case) for x in r[2])]


def pending_open(p, present_classes):
    if "test_class" in p:
        return p["test_class"] not in present_classes
    return not (ROOT / p["file"]).exists()


def coverage(cases, present_classes):
    rows = []
    for rid, name, refs, status, manual, note in REQUIREMENTS:
        hit = [c for c in cases if any(ref_matches(r, c) for r in refs)]
        labels = []
        for c in hit:
            label = " / ".join(c["ids"]) if c["own_id"] else c["cls"]
            if label not in labels:
                labels.append(label)
        labels.sort(key=natural_key)
        failed = sum(c["failed"] for c in hit)
        pend = [p for p in PENDING if rid in p["reqs"] and pending_open(p, present_classes)]
        done = [p for p in PENDING if rid in p["reqs"] and not pending_open(p, present_classes)
                and "file" in p]
        if not hit and not manual and not done and status != "Not covered":
            status = "Pending" if pend else "Not covered"
        note = next((p["pending_note"] for p in pend if "pending_note" in p), note)
        rows.append({
            "id": rid, "name": name, "tests": labels, "runs": sum(c["runs"] for c in hit),
            "failed": failed, "status": status, "manual": manual, "note": note,
            "pending": pend, "docs": [p["file"] for p in done],
        })
    return rows


def compress_ids(labels):
    """TC-FARE-01, TC-FARE-02, ... TC-FARE-08 -> TC-FARE-01 to 08."""
    out, groups = [], OrderedDict()
    for l in labels:
        m = re.fullmatch(r"(TC-[A-Z]+-)(\d+)", l)
        if m:
            groups.setdefault(m.group(1), []).append(m.group(2))
        else:
            out.append(l)
    res = []
    for prefix, nums in groups.items():
        ints = sorted(int(n) for n in nums)
        width = len(nums[0])
        if len(ints) > 2 and ints == list(range(ints[0], ints[-1] + 1)):
            res.append(f"{prefix}{ints[0]:0{width}d} to {ints[-1]:0{width}d}")
        else:
            res.extend(f"{prefix}{n:0{width}d}" for n in ints)
    return res + out


# ---------------------------------------------------------------------------
# README
# ---------------------------------------------------------------------------
def md_escape(s):
    return s.replace("|", "\\|")


def write_readme(cases, cov, results, totals, when, sources):
    present = set(results)
    per_class = []
    for cls in sorted(present, key=lambda c: (-sum(r[0] for r in results[c].values()), c)):
        runs = sum(r[0] for r in results[cls].values())
        failed = sum(r[1] for r in results[cls].values())
        pkg = sources.get(cls, {}).get("package", "")
        reqs = sorted({r for c in cases if c["cls"] == cls for r in reqs_for(c)}, key=natural_key)
        per_class.append((pkg, cls, runs, failed, reqs))

    ok = totals["failures"] == 0 and totals["errors"] == 0
    line = (f"Tests run: {totals['tests']}, Failures: {totals['failures']}, "
            f"Errors: {totals['errors']}, Skipped: {totals['skipped']}")
    srs = [r for r in cov if r["id"] != NF_006]
    counts = defaultdict(int)
    for r in srs:
        counts[r["status"]] += 1

    L = []
    L += ["# Software Test Plan (STP)", "",
          "**Project:** Railway Reservation System",
          "",
          "This folder holds the test plan, the list of automated test cases and the traceability "
          "matrix from SRS requirements to tests.", "",
          "## Deliverables", "",
          "- Test plan document: [`test-plan-railway-reservation-system.pdf`](./test-plan-railway-reservation-system.pdf) "
          "| [`test-plan-railway-reservation-system.docx`](./test-plan-railway-reservation-system.docx)",
          "- Search performance benchmark (issue #17): [`issue17-search-benchmark.md`](../performance/issue17-search-benchmark.md)",
          "- Script that rebuilds this page and the docx tables: [`tools/update_test_plan.py`](./tools/update_test_plan.py)",
          "",
          "## Latest run", "",
          f"We ran `./mvnw clean test` on {when:%d %B %Y} at {when:%H:%M} on `dev/likhith` "
          f"(merged with `main` after PR #68). Maven reported:", "",
          "```text", line, "```", ""]
    if ok:
        L += [f"All {totals['tests']} tests passed. The number is lower than the 256 this page used to "
              "list because the app was rebuilt in PR #68 and the old test classes "
              "(`BookingControllerTest`, `PaymentServiceTest`, `AdminServiceTest` and others) were "
              "replaced by the classes below.", ""]
    else:
        L += [f"{totals['failures'] + totals['errors']} tests did not pass in this run. "
              "They are marked Fail in the tables below.", ""]

    L += ["## Tests per class", "",
          "| Package | Test class | Tests | Failed | Requirements |",
          "|---|---|---:|---:|---|"]
    for pkg, cls, runs, failed, reqs in per_class:
        L.append(f"| `{pkg}` | `{cls}` | {runs} | {failed} | {', '.join(reqs) or 'App start-up'} |")
    L.append(f"| | **Total** | **{totals['tests']}** | **{totals['failures'] + totals['errors']}** | |")
    if "SafeTextRulesTest" in present:
        st = results["SafeTextRulesTest"]
        L += ["", "Parameterised tests count once per input row, which is why `SafeTextRulesTest` "
              f"shows {sum(r[0] for r in st.values())} tests from {len(st)} methods."]
    L.append("")

    L += ["## Traceability matrix", "",
          f"The SRS has {len(srs)} requirements. In this run {counts['Covered']} were covered by automated "
          f"tests, {counts['Partly covered']} were partly covered and {counts['Not covered']} had no test. "
          f"The last row, {NF_006.split(' ')[0]}, is the accessibility check we added in Sprint 3; it is not "
          "in SRS v1.0, so it is not part of these counts. "
          "Where the rebuilt app behaves differently from the SRS acceptance criteria we wrote that "
          "in the Notes column instead of counting it as a pass.", "",
          "| Requirement | Description | Covering tests | Tests run | Status | Notes |",
          "|---|---|---|---:|---|---|"]
    for r in cov:
        tests = ", ".join(compress_ids(r["tests"]))
        extra = []
        if r["manual"]:
            extra.append(r["manual"])
        extra += [f"`{d}`" for d in r["docs"]]
        extra += [f"{p['what']}: pending ({p['where']})" for p in r["pending"]]
        cell = "; ".join(x for x in [tests] + extra if x) or "None"
        status = r["status"] + (f" ({r['failed']} failing)" if r["failed"] else "")
        L.append(f"| {r['id']} | {md_escape(r['name'])} | {md_escape(cell)} | {r['runs'] or '-'} | "
                 f"{status} | {md_escape(r['note'])} |")
    L.append("")

    gaps = [r for r in cov if r["status"] != "Covered"]
    if gaps:
        L += ["### Gaps", ""]
        for r in gaps:
            L.append(f"- **{r['id']}** ({r['status'].lower()}): {r['note']}")
        L.append("")
    unmapped = sorted({" / ".join(c["ids"]) if c["own_id"] else c["cls"]
                       for c in cases if not reqs_for(c)
                       and c["cls"] != "RailwayReservationApplicationTests"}, key=natural_key)
    if unmapped:
        L += ["### Tests with no SRS requirement", "",
              "These tests check behaviour the rebuilt app added beyond the SRS v1.0 list: "
              + ", ".join(unmapped) + ".", ""]
    open_p = [p for p in PENDING if pending_open(p, present)]
    if open_p:
        L += ["### Work not merged yet", ""]
        for p in open_p:
            L.append(f"- {p['what']}, {p['where']}. Maps to {', '.join(p['reqs'])}.")
        L += ["", "After these merge, run the steps under *Updating this page* again.", ""]

    L += ["## Test cases", "",
          "Every JUnit test that ran, grouped by class. The ID and description come from the "
          "`@DisplayName` on each test. Classes written before we used per-test IDs show the method "
          "name instead.", ""]
    by_cls = OrderedDict()
    for c in sorted(cases, key=lambda c: (c["cls"], natural_key(" ".join(c["ids"]) or "~"), c["method"])):
        by_cls.setdefault(c["cls"], []).append(c)
    for cls, items in sorted(by_cls.items(), key=lambda kv: kv[0]):
        disp = sources[cls]["display"]
        L += [f"### `{cls}`" + (f" ({md_escape(disp)})" if disp else ""), "",
              "| ID | Test | Runs | Result | Requirements |", "|---|---|---:|---|---|"]
        for c in items:
            idl = " / ".join(c["ids"]) if c["own_id"] else (f"({c['ids'][0]})" if c["ids"] else "No ID")
            text = c["desc"] if c["own_id"] else f"`{c['method']}`"
            res = "Pass" if c["failed"] == 0 else f"Fail ({c['failed']} of {c['runs']})"
            L.append(f"| {idl} | {md_escape(text)} | {c['runs']} | {res} | {', '.join(reqs_for(c)) or '-'} |")
        L.append("")

    L += ["## Updating this page", "",
          "```bash",
          "git checkout dev/likhith",
          "git fetch origin",
          "git merge origin/main",
          "export APP_JWT_SECRET=$(openssl rand -base64 32)",
          "./mvnw clean test",
          "python3 docs/test-plan/tools/update_test_plan.py",
          "```", "",
          "The script rewrites this README and the tables in the docx. Export the PDF again from Word "
          "afterwards. New tests are picked up by their ID prefix (for example `TC-CAN-*`); a test with a "
          "new prefix or a new requirement needs a line in `REQUIREMENTS` inside the script.", ""]
    text = "\n".join(L).replace("\n\n\n", "\n\n")
    README.write_text(text, encoding="utf-8")
    return text


# ---------------------------------------------------------------------------
# DOCX
# ---------------------------------------------------------------------------
def set_par_text(p, text):
    runs = p.runs
    if not runs:
        p.add_run(text)
        return
    runs[0].text = text
    for r in runs[1:]:
        r._r.getparent().remove(r._r)


def set_cell(cell, text):
    ps = cell.paragraphs
    set_par_text(ps[0], text)
    for p in ps[1:]:
        p._p.getparent().remove(p._p)


def rebuild_table(table, header, rows):
    if header:
        for cell, h in zip(table.rows[0].cells, header):
            set_cell(cell, h)
    template = copy.deepcopy(table.rows[1]._tr)
    for row in list(table.rows)[1:]:
        row._tr.getparent().remove(row._tr)
    for values in rows:
        tr = copy.deepcopy(template)
        table._tbl.append(tr)
        new_row = table.rows[-1]
        for cell, v in zip(new_row.cells, values):
            set_cell(cell, v)


def find_par(doc, start):
    for i, p in enumerate(doc.paragraphs):
        if p.text.strip().startswith(start):
            return i, p
    raise SystemExit(f"Could not find paragraph starting with {start!r} in the docx")


def replace_list(doc, after_start, before_start, items):
    """Replace the non-empty paragraphs between two markers with clones of the first one."""
    i, _ = find_par(doc, after_start)
    j, _ = find_par(doc, before_start)
    old = [p for p in doc.paragraphs[i + 1:j] if p.text.strip()]
    from docx.text.paragraph import Paragraph
    anchor = old[-1]._p
    for text in items:
        new = copy.deepcopy(old[0]._p)
        anchor.addnext(new)
        anchor = new
        set_par_text(Paragraph(new, old[0]._parent), text)
    for p in old:
        p._p.getparent().remove(p._p)


# Expected Result column for section 10, written from what each test asserts.
# Key: the test ID (as shown in the TC ID column) or, for classes without per-test IDs, the class name.
# A test that is not listed here gets a line built from its asserts, and the script says so.
EXPECTED = {
    "TC-ACC-01": "Profile update gives 200 with the new phone; a wrong current password gives 400 "
                 "WRONG_PASSWORD and the right one 204; another passenger cannot see the saved traveller "
                 "and gets 404 deleting it.",
    "TC-ADM-01": "201 for new train 12999, which then shows up in a BBB to DDD search; a route with times "
                 "going backwards gives 400 TIME_ORDER; reusing 12999 gives 409.",
    "TC-ADM-02": "GET /api/v1/admin/overview gives 401 to a guest, 403 to a passenger and 200 to the admin.",
    "TC-ADM-03": "Deleting 12901 with a paid booking gives 409 TRAIN_HAS_BOOKINGS; setting it inactive gives "
                 "204 and it no longer appears in search.",
    "TC-ADM-04": "The report for 12901 on the travel date has total 1 and the booked PNR; the SL chart shows 1 "
                 "traveller; the chart CSV contains the PNR and \"Traveller A\".",
    "TC-ADM-05": "After the admin sets the top rule to 10% (204), the cancellation preview deducts 10% of the "
                 "per-traveller fare.",
    "TC-BK-01": "2 SL travellers get CNF, a 10-digit PNR, status PAYMENT_PENDING, the caller's email, a total "
                "of 2 x the per-traveller fare, and payment due at 09:10.",
    "TC-BK-02": "400 with field errors for passengers[0].name (\"<script>\"), passengers[0].age (3) and mobile "
                "(\"12345\"); 7 travellers in one booking also gives 400.",
    "TC-BK-03 / TC-HIS-01": "The ticket PDF gives 409 before payment; after payment 200, type application/pdf, "
                            "file starts with %PDF; upcoming history lists exactly that PNR.",
    "TC-CAN-01": "Cancelling 1 of 2 travellers gives PARTIALLY_CANCELLED and a refund of fare minus Rs 120; "
                 "repeating it gives 409 ALREADY_CANCELLED; cancelling the other gives CANCELLED.",
    "TC-CAN-02": "Preview deduction is Rs 120 three days out, the larger of 25% and Rs 120 at 20 h, the larger "
                 "of 50% and Rs 120 at 6 h, and the refund is 0 at 2 h.",
    "TC-CAN-03": "With 58 berths sold the next traveller is RAC 1; after one confirmed traveller cancels, that "
                 "traveller is CNF and the booking status still reads RAC 1.",
    "TC-CAN-04": "With berths and RAC full the next traveller is WL; at 04:30 (chart at 04:00) the job cancels "
                 "1 booking: status CANCELLED, refund of fare minus Rs 60.",
    "TC-DEM-01": "Two runs for the same departure give the same pre-sold berths and the same external waiting list.",
    "TC-DEM-02": "More berths are pre-sold for tomorrow's departure than for one seven weeks away.",
    "TC-DEM-03": "Simulated Tatkal sales start at or after the first Tatkal berth and stay within coach capacity.",
    "TC-DEM-04": "With the simulator switched off, no berths are pre-sold.",
    "TC-FARE-01": "358 km Sleeper on a superfast: base 186, reservation 20, superfast 30, GST 0, total 236.",
    "TC-FARE-02": "GST is 5% of the fare before tax (rounded) and the total is fare before tax plus GST.",
    "TC-FARE-03": "A 50 km Sleeper journey has the same base fare as a 200 km one.",
    "TC-FARE-04": "The second 1000 km costs less than the first 1000 km.",
    "TC-FARE-05": "Superfast charge is 0 for a 500 km Sleeper journey on a non-superfast train.",
    "TC-FARE-06": "A short Tatkal journey pays the Sleeper Tatkal minimum, a long one the maximum; a General "
                  "quota ticket pays no Tatkal charge.",
    "TC-FARE-07": "A premium Chair Car fare includes the catering charge and a higher base than a plain train, "
                  "which has no catering.",
    "TC-FARE-08": "The total for 3 travellers is exactly 3 x the single-traveller total.",
    "TC-OPS-01": "Health gives 200 with status UP, targetAvailability 99.0%, OPTIMAL, database and memory UP; "
                 "30 parallel health calls all return 200; with the database down it reports DOWN and DEGRADED.",
    "TC-OPS-02": "Uncaught and database errors give 500 with a generic message and no stack trace, class name or "
                 "SQL text; errors are logged at ERROR with the user id; a correlation ID header is generated or "
                 "echoed; logs/ exists.",
    "TC-OPS-03": "Unknown /api/v1 path gives 401 to a guest and 404 JSON (NOT_FOUND) when signed in; an unknown "
                 "page gives the 404 HTML page; /index.html has a CSP with default-src 'self'.",
    "TC-PAY-01": "First payment returns SUCCESS and the booking is BOOKED; paying again returns the same txnRef "
                 "and the booking still has 1 payment.",
    "TC-PAY-02": "Card 4000 0000 0000 0002 returns FAILED, booking stays PAYMENT_PENDING, shown as \"Visa ending "
                 "0002\"; 4111 1111 1111 1112 gives 400 INVALID_CARD; 4111 1111 1111 1111 succeeds and the full "
                 "number is not in the response.",
    "TC-PAY-03": "After 11 minutes the expiry job releases 1 hold, SL availability rises by 2, and paying gives "
                 "409 PAYMENT_CLOSED.",
    "TC-PAY-04": "Abandon gives 200 with status PAYMENT_FAILED and SL availability rises by 1 straight away.",
    "TC-PNR-01": "An unpaid PNR gives 404; after payment 200 with status CNF and no name, email or mobile in the "
                 "response; an unknown PNR gives 404 and \"123\" gives 400.",
    "TC-SEAT-01": "Availability is AVAILABLE with 58 general berths; the coach has 14 Tatkal berths.",
    "TC-SEAT-02": "A traveller asking for side upper gets CNF on a SIDE_UPPER berth in coach S1.",
    "TC-SEAT-03": "A 66 year old with no preference gets a LOWER berth.",
    "TC-SEAT-04": "Each member of a group of 4 is within 8 berths of the first one.",
    "TC-SEAT-05": "With all 58 berths sold from stop 1 to 2, stop 1 to 3 is not AVAILABLE, stop 2 to 4 shows 58, "
                  "and a booking from stop 2 is CNF.",
    "TC-SEAT-06": "After 58 berths the next traveller is RAC 1; after 14 RAC the next is WL 1; with the waiting "
                  "list full availability is REGRET and booking is refused.",
    "TC-SEAT-07": "Booking 3 travellers with 2 berths left and 'book only if confirmed' set fails with "
                  "NOT_ALL_CONFIRMED.",
    "TC-SEAT-08": "Once the chart is prepared and 58 berths are sold, availability is REGRET.",
    "TC-SEAT-09": "When a confirmed traveller cancels: RAC 1 becomes CNF, WL 1 becomes RAC 14, WL 2 becomes WL 1, "
                  "and the cancelled traveller is CAN.",
    "TC-SEAT-10": "With 2 RAC from other channels, our RAC traveller is number 3; after one berth frees up, "
                  "external RAC drops to 1 and ours becomes number 2.",
    "TC-SEAT-11": "With all 58 general berths sold, a Tatkal traveller still gets CNF on a berth from the Tatkal pool.",
    "TC-SEC-05": "A token idle for over 15 minutes gives 401 \"Session Expired\" with no renewal; an active request "
                 "gets a renewed token (later expiry, same email, role and userId, 15 minutes within 1 s); a "
                 "missing or tampered token gives a plain 401 with no renewal.",
    "TC-SEC-06": "A guest gets 401 for booking, paying, listing bookings and downloading a ticket.",
    "TC-SEC-07": "Another passenger gets 404 viewing, paying, downloading or cancelling the booking and sees an "
                 "empty list; the admin gets 200 on the same PNR.",
    "TC-SRC-01": "AAA to DDD gives 200 with 2 trains; the first is 12901 leaving 08:00, arriving on day 2, first "
                 "class AVAILABLE with a fare above 0.",
    "TC-SRC-02": "BBB to CCC returns only 12901 with distance 250 km; CCC to BBB returns nothing.",
    "TC-SRC-03": "On Saturday AAA to DDD returns only 12901 (12902 runs on weekdays).",
    "TC-SRC-04": "400 with SAME_STATION (AAA to AAA), DATE_IN_PAST (yesterday), DATE_TOO_FAR (today plus 61 days) "
                 "and UNKNOWN_STATION (ZZZ).",
    "TC-SRC-05": "AAB to DDD finds nothing; with nearby=true it finds trains from the other station in that city.",
    "TC-SRC-06": "Train 12901 has 4 stops; the SL availability calendar has 6 days; AAA's board has 2 departures; "
                 "train 99999 gives 404.",
    "TC-TQ-01": "A Tatkal SL booking at 09:00 the day before gives 409 TATKAL_NOT_OPEN; at 11:00 it gives 201 with "
                "quota TQ and a Tatkal charge; 5 travellers gives 400 TOO_MANY_TRAVELLERS.",
    "InputValidationSecurityTest": "4 malicious registration names give 400; O'Brien and hyphenated names give 201; "
                                   "SQL injection in search source and XSS in destination give 400; a script tag in a "
                                   "train name or SQL in a station gives 400; normal train names give 201.",
    "PasswordProtectionSecurityTest": "The stored password is a BCrypt hash; the same password hashed twice differs; "
                                      "register and login responses never contain the password or hash; the "
                                      "password never appears in the logs.",
    "QueryParameterizationTest": "No production code calls raw SQL APIs, and every custom @Query uses named parameters.",
    "RailwayReservationApplicationTests": "The Spring application context starts.",
    "SafeTextRulesTest": "16 SQL injection and XSS payloads are rejected; 14 normal values (O'Brien, Anne-Marie, "
                         "KSR Bengaluru (SBC), 12627) pass; null counts as safe; requireSafe fails without echoing "
                         "the payload.",
    "UserLoginControllerTest": "Correct login gives 200 and a signed JWT with userId and role; wrong password and "
                               "unknown email both give 401 \"Invalid credentials.\"; after 5 failures the right "
                               "password is refused too, also across requests and parallel attempts; login after "
                               "the lock expires resets the count; SQL in the email gives 400; a new user can log "
                               "in at once.",
    "UserRegistrationControllerTest": "Valid sign up gives 201 with a BCrypt hash and no password in the response; "
                                      "missing or invalid fields and malformed JSON give 400; a duplicate email in "
                                      "any letter case gives 409; a password over the BCrypt 72 byte limit gives 400.",
}

AUTO_EXPECTED = []

HTTP_STATUS = {"isOk": 200, "isCreated": 201, "isNoContent": 204, "isBadRequest": 400,
               "isUnauthorized": 401, "isForbidden": 403, "isNotFound": 404, "isConflict": 409,
               "isInternalServerError": 500}


def auto_expected(items):
    """Fallback for tests not in EXPECTED: list the HTTP statuses, error codes and numbers they assert."""
    statuses, codes, values = [], [], []
    for c in items:
        body = c.get("body", "")
        for name in re.findall(r"status\(\)\.(\w+)\(\)", body):
            if name in HTTP_STATUS and str(HTTP_STATUS[name]) not in statuses:
                statuses.append(str(HTTP_STATUS[name]))
        for code in re.findall(r'(?:value|isEqualTo)\("([A-Z][A-Z0-9_]{2,})"\)', body):
            if code not in codes:
                codes.append(code)
        for n in re.findall(r"isEqualTo\((\d+)\)", body):
            if n not in values:
                values.append(n)
    parts = []
    if statuses:
        parts.append("HTTP " + ", ".join(statuses))
    if codes:
        parts.append("codes " + ", ".join(codes))
    if values:
        parts.append("values " + ", ".join(values))
    if not parts:
        return "; ".join(OrderedDict.fromkeys(c["desc"] for c in items))
    return "Asserts " + "; ".join(parts) + "."


def type_of(reqs):
    if reqs and all(r.startswith("RRS-SR") for r in reqs):
        return "Security"
    if reqs and all(r.startswith("RRS-NF") for r in reqs):
        return "Non-functional"
    if any(r.startswith("RRS-SR") for r in reqs):
        return "Functional / Security"
    return "Functional"


def docx_case_rows(cases):
    groups = OrderedDict()
    for c in cases:
        key = (" / ".join(c["ids"]), c["cls"]) if c["own_id"] else ("", c["cls"])
        groups.setdefault(key, []).append(c)

    def sort_key(kv):
        (ids, cls), _ = kv
        return (0, natural_key(ids)) if ids else (1, natural_key(cls))

    rows = []
    for (ids, cls), items in sorted(groups.items(), key=sort_key):
        runs = sum(c["runs"] for c in items)
        failed = sum(c["failed"] for c in items)
        reqs = sorted({r for c in items for r in reqs_for(c)}, key=natural_key)
        if ids:
            descs = list(OrderedDict.fromkeys(c["desc"] for c in items))
            name = "; ".join(descs)
            tcid = ids
        else:
            class_ids = items[0]["ids"]
            tcid = class_ids[0] if class_ids else "No ID"
            name = f"{cls}: " + "; ".join(humanize(c["method"]) for c in items)
        status = "Pass" if failed == 0 else f"Fail ({failed} of {runs})"
        key = ids if ids else cls
        expected = EXPECTED.get(key)
        if expected is None:
            expected = auto_expected(items)
            AUTO_EXPECTED.append(key)
        rows.append([tcid, name, ", ".join(reqs) or "-", cls, str(runs), expected,
                     type_of(reqs), status])
    return rows


def set_widths(table, inches):
    """Fixed layout with the given column widths in inches."""
    table.autofit = False
    tblW = table._tbl.tblPr.find(qn("w:tblW"))
    if tblW is not None:
        tblW.set(qn("w:w"), str(round(sum(inches) * 1440)))
        tblW.set(qn("w:type"), "dxa")
    for gc, w in zip(table._tbl.tblGrid.findall(qn("w:gridCol")), inches):
        gc.set(qn("w:w"), str(round(w * 1440)))
    for row in table.rows:
        for cell, w in zip(row.cells, inches):
            cell.width = Inches(w)


def set_table_font(table, size):
    for row in table.rows:
        for cell in row.cells:
            for p in cell.paragraphs:
                for r in p.runs:
                    r.font.size = Pt(size)


def repeat_header(table):
    trPr = table.rows[0]._tr.get_or_add_trPr()
    h = trPr.find(qn("w:tblHeader"))
    if h is None:
        h = OxmlElement("w:tblHeader")
        trPr.append(h)
    h.set(qn("w:val"), "true")


def _empty_par_beside(elem, before):
    sib = elem.getprevious() if before else elem.getnext()
    if sib is not None and sib.tag == qn("w:p") and not "".join(sib.itertext()).strip():
        return sib
    new = OxmlElement("w:p")
    (elem.addprevious if before else elem.addnext)(new)
    return new


def _attach_sectpr(p, sectpr):
    pPr = p.find(qn("w:pPr"))
    if pPr is None:
        pPr = OxmlElement("w:pPr")
        p.insert(0, pPr)
    pPr.append(sectpr)


def landscape_section(doc, first_elem, last_elem):
    """Put first_elem..last_elem in their own landscape section; the rest stays portrait."""
    body = doc.element.body
    for sp in list(body.iter(qn("w:sectPr"))):          # drop breaks from earlier runs
        if sp.getparent().tag == qn("w:pPr"):
            sp.getparent().remove(sp)
    final = body.find(qn("w:sectPr"))
    _attach_sectpr(_empty_par_beside(first_elem, before=True), copy.deepcopy(final))
    land = copy.deepcopy(final)
    size = land.find(qn("w:pgSz"))
    w, h = size.get(qn("w:w")), size.get(qn("w:h"))
    size.set(qn("w:w"), h)
    size.set(qn("w:h"), w)
    size.set(qn("w:orient"), "landscape")
    margins = land.find(qn("w:pgMar"))
    margins.set(qn("w:left"), "720")                      # 0.5 in sides: 11 - 1 = 10 in for the table
    margins.set(qn("w:right"), "720")
    num = land.find(qn("w:pgNumType"))
    if num is not None:
        land.remove(num)                                  # keep page numbers running on
    _attach_sectpr(_empty_par_beside(last_elem, before=False), land)


def unfloat_below(doc, table, heading):
    """Make a table inline (text wrapping none) and place it straight after its heading."""
    floating = table._tbl.tblPr.find(qn("w:tblpPr"))
    if floating is not None:
        table._tbl.tblPr.remove(floating)
    _, h = find_par(doc, heading)
    if h._p.getnext() is not table._tbl:
        h._p.addnext(table._tbl)                 # lxml moves the element
    _empty_par_beside(table._tbl, before=False)  # a blank line before the next heading


def paragraphs_after_table(doc, table, stop_heading, texts):
    """Replace the text paragraphs between a table and the next heading with one paragraph per text."""
    _, src = find_par(doc, "Purpose:")
    elem = table._tbl.getnext()
    while elem is not None and elem.tag == qn("w:p"):
        txt = "".join(elem.itertext()).strip()
        if txt.startswith(stop_heading):
            break
        nxt = elem.getnext()
        if txt:
            elem.getparent().remove(elem)
        elem = nxt
    anchor = table._tbl
    for text in texts:
        new = copy.deepcopy(src._p)
        anchor.addnext(new)
        anchor = new
        set_par_text(Paragraph(new, table._parent), text)


def update_docx(cases, cov, totals, when, present):
    doc = docx.Document(str(DOCX))

    # Cover block
    _, cover = find_par(doc, "Project: Railway Reservation System")
    t = cover.text
    t = re.sub(r"Version: [^\n]*", "Version: 2.0", t)
    t = re.sub(r"Date: [^\n]*", f"Date: {when:%d-%m-%Y}", t)
    t = re.sub(r"Status: [^\n]*", "Status: Updated for Sprint 3 (tests from the rebuilt app)", t)
    set_par_text(cover, t)

    # Section 3: every SRS requirement
    replace_list(doc, "The following features", "4. Features Not to be Tested",
                 [f"{rid} - {name}" for rid, name, *_ in REQUIREMENTS if rid != NF_006])

    # Section 4: payments are simulated in the rebuilt app
    for p in doc.paragraphs:
        if p.text.startswith("Real payment gateway") or p.text.startswith("Payments through"):
            set_par_text(p, "Payments through a real bank or gateway. The app has its own simulated "
                            "gateway with test cards and UPI IDs.")

    # Section 6: environment
    env = {
        "Hardware:": "Hardware: MacBook Pro, Intel Core i7-1068NG7 (x86_64), 32 GB RAM, macOS. "
                     "The JMeter benchmark on 29 Oct 2026 ran on a Windows workstation.",
        "Software:": "Software: Java 25 (Microsoft OpenJDK 25.0.4.1), Spring Boot 4.1.1, H2 in-memory "
                     "database, embedded Tomcat. MySQL is supported but was not used for testing.",
        "Tools:": "Tools: JUnit 5, Mockito, AssertJ, Spring MockMvc, Maven Surefire, Apache JMeter 5.6.3, "
                  "Git and GitHub Actions (runs the tests on every PR).",
        "Test Data:": "Test Data: tests use RailFixtures (stations AAA, AAB, BBB, CCC, DDD; train 12901 daily "
                      "and 12902 on weekdays; clock fixed at Monday 9 Nov 2026 09:00). The running app is "
                      "seeded by RailDataSeeder with 98 stations and 66 trains.",
    }
    for p in doc.paragraphs:
        for k, v in env.items():
            if p.text.startswith(k):
                set_par_text(p, v)

    tables = doc.tables
    # Section 7: schedule, Section 9: roles
    rebuild_table(tables[0], None, SCHEDULE)
    rebuild_table(tables[1], None, ROLES)

    # Section 10: test cases
    rows = docx_case_rows(cases)
    _, intro = find_par(doc, "A total of")
    set_par_text(intro, (
        f"A total of {len(rows)} test cases are listed below, made up of {totals['tests']} JUnit tests. "
        f"We ran them with ./mvnw clean test on {when:%d %B %Y}: {totals['tests']} run, "
        f"{totals['failures']} failures, {totals['errors']} errors, {totals['skipped']} skipped. "
        "Rows with an ID come from the @DisplayName on each test; older classes without per-test IDs "
        "are listed as one row with their method names. The JMeter tests (TC-PERF-01, TC-PERF-02) run "
        "outside Maven and are reported in section 15."))
    rebuild_table(tables[2], ["TC ID", "Test Case", "Req ID(s)", "Test class", "JUnit tests",
                              "Expected Result", "Type", "Status"], rows)
    set_widths(tables[2], [0.9, 2.0, 0.9, 1.4, 0.5, 3.0, 0.8, 0.5])
    set_table_font(tables[2], 10)
    repeat_header(tables[2])
    landscape_section(doc, find_par(doc, "10. Test Cases")[1]._p, tables[2]._tbl)

    # Section 15: traceability
    trows = []
    for r in cov:
        parts = []
        if r["tests"]:
            parts.append(", ".join(compress_ids(r["tests"])))
        if r["manual"]:
            parts.append(r["manual"])
        parts += r["docs"]
        parts += [f"{p['what']} pending, {p['where']}" for p in r["pending"]]
        status = r["status"] + (f", {r['failed']} failing" if r["failed"] else "")
        trows.append([r["id"], r["name"], f"{status}. " + ("; ".join(parts) if parts else "No test.")])
    rebuild_table(tables[4], ["Req ID", "Requirement", "Status and covering tests"], trows)
    # Asked for 1.0 / 2.2 / 3.3 in; scaled to the 6.0 in text width (1.25 in side margins).
    set_widths(tables[4], [0.92, 2.03, 3.05])
    repeat_header(tables[4])

    # Section 11: the Risks table floated (tblpPr) and showed above its own heading.
    risks = next(t for t in doc.tables if t.rows[0].cells[0].text.strip() == "Risk")
    unfloat_below(doc, risks, "11. Risks and Mitigation")

    srs = [r for r in cov if r["id"] != NF_006]
    counts = defaultdict(int)
    for r in srs:
        counts[r["status"]] += 1
    gaps = [r for r in cov if r["status"] != "Covered"]
    texts = [f"Of the {len(srs)} SRS requirements, {counts['Covered']} were covered, "
             f"{counts['Partly covered']} partly covered and {counts['Not covered']} not covered in the "
             f"run on {when:%d %B %Y}. The gaps were:"]
    texts += [f"{r['id'].split(' ')[0]} ({r['status'].lower()}): {r['note']}" for r in gaps]
    paragraphs_after_table(doc, tables[4], "16. Approvals", texts)

    doc.save(str(DOCX))
    return doc


def docx_text(doc):
    parts = [p.text for p in doc.paragraphs]
    for t in doc.tables:
        for row in t.rows:
            parts += [c.text for c in row.cells]
    return "\n".join(parts)


def style_check(name, text):
    problems = []
    for ch, label in (("\u2014", "em dash"), ("\u2013", "en dash")):
        if ch in text:
            problems.append(f"{label} found {text.count(ch)} time(s)")
    low = text.lower()
    for w in BANNED:
        n = len(re.findall(r"\b" + re.escape(w) + r"\b", low))
        if n:
            problems.append(f"'{w}' found {n} time(s)")
    for p in problems:
        print(f"  style warning in {name}: {p}")
    return not problems


def main():
    sources = read_sources()
    results, totals, when = read_reports()
    cases = build_cases(sources, results)
    present = set(results)
    cov = coverage(cases, present)

    readme_text = write_readme(cases, cov, results, totals, when, sources)
    doc = update_docx(cases, cov, totals, when, present)

    print(f"Run of {when:%Y-%m-%d %H:%M}: Tests run: {totals['tests']}, Failures: {totals['failures']}, "
          f"Errors: {totals['errors']}, Skipped: {totals['skipped']}")
    print(f"{len(present)} test classes, {len(cases)} test methods")
    for st in ("Covered", "Partly covered", "Not covered", "Pending"):
        print(f"  {st}: {', '.join(r['id'] for r in cov if r['status'] == st) or '-'}")
    open_p = [p for p in PENDING if pending_open(p, present)]
    print("Still pending: " + ("; ".join(f"{p['what']} ({p['where']})" for p in open_p) or "nothing"))
    if AUTO_EXPECTED:
        print("Expected Result built from asserts (check the wording, or add it to EXPECTED): "
              + ", ".join(AUTO_EXPECTED))
    ok = style_check("README.md", readme_text) & style_check("docx", docx_text(doc))
    print("Style check: " + ("no em dashes or listed words" if ok else "see warnings above"))
    print("Now export the PDF from Word: File > Save As > PDF.")


if __name__ == "__main__":
    main()
