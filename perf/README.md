# Issue #17 — Search Performance Benchmark

This directory contains the reproducible JMeter load test for
**RRS-NF-001** (95% of train-search responses within 3 seconds under normal load)
and **RRS-NF-004** (support 100 concurrent users without failure).

The test targets the existing public endpoint:

```text
GET /api/v1/trains/search?src=Chennai&dst=Bengaluru&date=<YYYY-MM-DD>
```

It uses the demo dataset seeded by `TrainDataInitializer` when
`app.seed-demo-data=true`. This is a **local demo benchmark**, not a
production-scale performance claim.

## Prerequisites

- JDK 8 or newer (the repository currently uses Java 25).
- Apache JMeter **5.6.3** extracted outside the repository.
- The application running on `http://localhost:8080`.
- PowerShell 7 for the result-analysis helper.

The verified local JMeter path used for this benchmark is:

```text
C:\Users\KISHAN S\Tools\apache-jmeter-5.6.3
```

JMeter is intentionally **not** a Maven dependency.

## Workload

`issue17-search-load-test.jmx` defines:

- 100 threads (concurrent virtual users)
- 1-second ramp-up
- a Synchronizing Timer with group size 100 so all users are released together
- one loop per thread
- one `GET /api/v1/trains/search` request per thread
- HTTP 200 response assertion
- a future journey date generated at run time with `${__timeShift(yyyy-MM-dd,,P2D,,)}`

The analysis script reports the **request window** from the first sample to the
last response, and the burst throughput within that window. JMeter's own
console summary reports the overall run time including ramp-up and
synchronization; both figures are recorded in the benchmark report.
The p95, maximum latency, error count, and HTTP 5xx count are derived from the
raw `.jtl` file.

## Run the benchmark

Start the application from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

In another PowerShell window, warm up the endpoint:

```powershell
$date = (Get-Date).AddDays(2).ToString('yyyy-MM-dd')
1..10 | ForEach-Object {
    Invoke-WebRequest -UseBasicParsing `
      "http://localhost:8080/api/v1/trains/search?src=Chennai&dst=Bengaluru&date=$date" |
      Out-Null
}
```

Run the load test twice:

```powershell
$jmeter = 'C:\Users\KISHAN S\Tools\apache-jmeter-5.6.3\bin\jmeter.bat'

& $jmeter -n -t perf\issue17-search-load-test.jmx `
  -l perf\results\issue17-search-run1.jtl `
  -j perf\results\jmeter-run1.log `
  -Jjmeter.save.saveservice.output_format=csv `
  -Jjmeter.save.saveservice.print_field_names=true

& $jmeter -n -t perf\issue17-search-load-test.jmx `
  -l perf\results\issue17-search-run2.jtl `
  -j perf\results\jmeter-run2.log `
  -Jjmeter.save.saveservice.output_format=csv `
  -Jjmeter.save.saveservice.print_field_names=true
```

Analyze each raw log:

```powershell
.\perf\analyze-search-results.ps1 -JtlPath perf\results\issue17-search-run1.jtl
.\perf\analyze-search-results.ps1 -JtlPath perf\results\issue17-search-run2.jtl
```

The script reports:

- sample count
- request-window duration and burst throughput
- average, minimum, p95, and maximum latency
- error count and error rate
- HTTP 5xx count
- pass/fail checks for p95 < 3000 ms, zero errors, zero 5xx, and a request window under 10 seconds

## Results

The committed `.jtl` files under `perf/results/` are genuine logs from completed
runs. See [`docs/performance/issue17-search-benchmark.md`](../docs/performance/issue17-search-benchmark.md)
for the environment, measured results, and limitations.
