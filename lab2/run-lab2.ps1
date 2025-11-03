param()

$ErrorActionPreference = 'Stop'

function Write-Step([string]$message) {
    Write-Host "------------------------------------------------------------"
    Write-Host $message
    Write-Host "------------------------------------------------------------"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir

try {
    Write-Step "[1/4] Cleaning and building lab2 (application + benchmarks)"
    mvn -DskipTests clean package

    Write-Host ""
    Write-Step "[2/4] Running JMH benchmark suite (summary mode)"
    $benchJar = Join-Path (Get-Location) "target/lab2-1.0-SNAPSHOT-benchmarks.jar"
    if (-not (Test-Path $benchJar)) {
        throw "Benchmarks JAR not found. Expected at $benchJar"
    }
    & java -cp $benchJar rj.lab2.benchmarks.ReceiptStatisticsBenchmarkSummary

    Write-Host ""
    Write-Step "[3/4] Locating Python interpreter"
    $python = Get-Command python -ErrorAction SilentlyContinue
    if (-not $python) {
        $python = Get-Command py -ErrorAction SilentlyContinue
    }
    if (-not $python) {
        throw "Python interpreter not found (python or py). Install Python 3 with pandas/matplotlib."
    }
    Write-Host "Using Python:" $python.Source

    Write-Host ""
    Write-Step "[4/4] Building benchmark graphs (saved under target\reports)"
    & $python.Source "report/graph/graph.py" --save --output "target/reports"

    Write-Host ""
    Write-Host "[OK] lab2 benchmarking workflow finished successfully."
    $cwd = (Get-Location).Path
    Write-Host ("CSV data:       {0}\target\reports\lab2-benchmark-summary.csv" -f $cwd)
    Write-Host ("Crossover data: {0}\target\reports\lab2-benchmark-crossover.csv" -f $cwd)
    Write-Host ("Graph images:   {0}\target\reports" -f $cwd)
}
catch {
    Write-Host ""
    Write-Host "[FAIL] lab2 workflow failed. Details:"
    Write-Host $_.Exception.Message
    exit 1
}
finally {
    Pop-Location
}
