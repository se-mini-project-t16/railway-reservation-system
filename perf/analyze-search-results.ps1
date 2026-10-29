param(
    [Parameter(Mandatory = $true)]
    [string]$JtlPath
)

if (-not (Test-Path -LiteralPath $JtlPath)) {
    throw "JTL file not found: $JtlPath"
}

$samples = @(Import-Csv -LiteralPath $JtlPath)
if ($samples.Count -eq 0) {
    throw "No samples found in JTL file: $JtlPath"
}

$elapsed = @($samples | ForEach-Object { [double]$_.elapsed } | Sort-Object)
$roundedCount = [Math]::Ceiling($samples.Count * 0.95)
$p95 = $elapsed[$roundedCount - 1]
$max = ($elapsed | Measure-Object -Maximum).Maximum
$min = ($elapsed | Measure-Object -Minimum).Minimum
$average = ($elapsed | Measure-Object -Average).Average
$start = ($samples | ForEach-Object { [long]$_.timeStamp } | Measure-Object -Minimum).Minimum
$end = ($samples | ForEach-Object { [long]$_.timeStamp + [double]$_.elapsed } | Measure-Object -Maximum).Maximum
$durationSeconds = ($end - $start) / 1000.0
$throughput = if ($durationSeconds -gt 0) { $samples.Count / $durationSeconds } else { 0 }
$errors = @($samples | Where-Object { $_.success -ne 'true' })
$serverErrors = @($samples | Where-Object { $_.responseCode -match '^5' })

[pscustomobject]@{
    jtl = (Resolve-Path -LiteralPath $JtlPath).Path
    samples = $samples.Count
    requestWindowSeconds = [Math]::Round($durationSeconds, 3)
    requestWindowThroughputPerSecond = [Math]::Round($throughput, 2)
    averageMs = [Math]::Round($average, 2)
    minMs = [Math]::Round($min, 2)
    p95Ms = [Math]::Round($p95, 2)
    maxMs = [Math]::Round($max, 2)
    errors = $errors.Count
    errorRatePercent = [Math]::Round(($errors.Count / $samples.Count) * 100, 3)
    http5xx = $serverErrors.Count
    passP95Under3000 = ($p95 -lt 3000)
    passNoErrors = ($errors.Count -eq 0)
    passNo5xx = ($serverErrors.Count -eq 0)
    passWithin10Seconds = ($durationSeconds -lt 10)
} | ConvertTo-Json
