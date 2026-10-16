# DocGuard Quality Checks Hook
# Runs Maven compile, test, and package validation

param(
    [string]$Phase = "sessionStart"
)

$ErrorActionPreference = "Stop"

function Write-Status {
    param([string]$Message)
    Write-Host "Quality Check: $Message" -ForegroundColor Cyan
}

try {
    switch ($Phase) {
        "sessionStart" {
            Write-Status "Compiling DocGuard project..."
            mvn -q -DskipTests compile
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Compilation failed" -ForegroundColor Red
                exit 1
            }
            Write-Status "Compilation successful"
        }

        "sessionEnd" {
            Write-Status "Running unit tests..."
            mvn -q test
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Tests failed" -ForegroundColor Red
                exit 1
            }

            Write-Status "Building package..."
            mvn -q package
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Package build failed" -ForegroundColor Red
                exit 1
            }

            Write-Status "Validating CLI..."
            java -jar target/docguard-0.1.0-SNAPSHOT.jar --help | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Host "CLI validation failed" -ForegroundColor Red
                exit 1
            }

            Write-Status "All quality checks passed"
        }

        default {
            Write-Host "Unknown phase: $Phase" -ForegroundColor Yellow
            exit 0
        }
    }

    exit 0
}
catch {
    Write-Host "Quality check error: $_" -ForegroundColor Red
    exit 1
}
