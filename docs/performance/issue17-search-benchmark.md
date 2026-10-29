# Issue #17 — Search Performance Benchmark Results

**Requirement:** RRS-NF-001 and RRS-NF-004

**Test cases:** TC-PERF-01 and TC-PERF-02

**Date:** 2026-10-29 (Asia/Kolkata)

**Branch:** `feat/perf-search-issue-17`

**Base:** `origin/main` at `75ed8c88effd19888e5dbc10c41fdbf8e74ac478`

## Environment

| Item | Value |
|---|---|
| OS | Windows, local developer workstation |
| Java | Eclipse Temurin 25.0.2 |
| Application | Spring Boot 4.1.1 |
| Database | H2 in-memory (`jdbc:h2:mem:railwaydb`) |
| Dataset | Demo seed from `TrainDataInitializer`: two trains; Chennai–Bengaluru has one train with CC and EC classes |
| Server | Local Tomcat on `localhost:8080` |
| Load tool | Apache JMeter 5.6.3 (official Apache binary, SHA-512 and PGP verified) |
| Warm-up | 5–10 sequential search requests per run |

This is a **local demo benchmark**. It does not model production network latency,
MySQL/HA deployment, TLS termination, a reverse proxy, or production-scale data.
The demo dataset is intentionally small.

## Workload

`perf/issue17-search-load-test.jmx`:

- 100 threads / virtual users
- 1-second ramp-up
- Synchronizing Timer with group size 100, releasing all users together
- one loop and one search request per user
- `GET /api/v1/trains/search?src=Chennai&dst=Bengaluru&date=<future>`
- HTTP 200 response assertion
- future date generated at runtime with `${__timeShift(yyyy-MM-dd,,P2D,,)}`

Each run produced exactly 100 samples. `maxGrpThreads` and `maxAllThreads` in
both JTLs reached **100**, confirming the concurrent-user target.

## Results

### Run 1 — 2026-10-29 17:51:30

JMeter summary:

```text
summary = 100 in 00:00:02 = 66.3/s Avg: 157 Min: 100 Max: 218 Err: 0 (0.00%)
```

JTL-derived metrics:

| Metric | Value |
|---|---:|
| Samples | 100 |
| Request-window duration | 0.225 s |
| Request-window throughput | 444.44 req/s |
| Average latency | 157.74 ms |
| Minimum latency | 100 ms |
| **p95 latency** | **200 ms** |
| **Maximum latency** | **218 ms** |
| Errors | 0 |
| Error rate | 0.000% |
| **HTTP 5xx** | **0** |
| Max concurrent threads | 100 |
| Response codes | 200 × 100 |

### Run 2 — 2026-10-29 17:51:37

JMeter summary:

```text
summary = 100 in 00:00:02 = 60.1/s Avg: 198 Min: 109 Max: 320 Err: 0 (0.00%)
```

JTL-derived metrics:

| Metric | Value |
|---|---:|
| Samples | 100 |
| Request-window duration | 0.321 s |
| Request-window throughput | 311.53 req/s |
| Average latency | 198.59 ms |
| Minimum latency | 109 ms |
| **p95 latency** | **290 ms** |
| **Maximum latency** | **320 ms** |
| Errors | 0 |
| Error rate | 0.000% |
| **HTTP 5xx** | **0** |
| Max concurrent threads | 100 |
| Response codes | 200 × 100 |

Run 2 had a slightly higher average and maximum latency than run 1, but both
runs remain far below the 3-second p95 target. The request-window duration
measures the first sample to the last response; JMeter’s overall summary
includes the 1-second ramp-up and synchronization.

## Acceptance assessment

| Criterion | Result |
|---|---|
| 100 search requests executed | **PASS** — exactly 100 in each run |
| All requests within 10 seconds | **PASS** — request windows 0.225 s and 0.321 s; JMeter overall runs ~2 s |
| 95% of responses within 3.0 s | **PASS** — p95 200 ms and 290 ms |
| 100 concurrent virtual users | **PASS** — max active threads 100 in both runs |
| No errors | **PASS** — 0 errors in both runs |
| No HTTP 5xx | **PASS** — 0 5xx in both runs |
| Document results and logs | **PASS** — this report plus raw JTL evidence |

## Evidence

Raw JMeter logs committed with this change:

- [Run 1 JTL](../../perf/results/issue17-search-run1.jtl)
- [Run 2 JTL](../../perf/results/issue17-search-run2.jtl)

Reproduction commands: [`perf/README.md`](../../perf/README.md).

## Limitations

- Local H2 demo dataset, not production-scale data or a production database.
- Loopback-only execution; no WAN/TLS/proxy latency.
- Single application instance and default Tomcat/Hikari configuration.
- Client-side JMeter timing includes the local JMeter process overhead.
- JMeter 5.6.3 on Java 25 emitted `sun.misc.Unsafe` deprecation warnings from
  XStream but completed successfully.
- The load test is external to Maven/CI; it is not part of the automated suite.
- No production-code optimization was required because all measured criteria
  passed.
