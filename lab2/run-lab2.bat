@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Navigate to the lab2 module directory
pushd %~dp0

echo ------------------------------------------------------------
echo [1/4] Cleaning and building lab2 (application + benchmarks)
echo ------------------------------------------------------------
call mvn -DskipTests clean package
if ERRORLEVEL 1 goto :error

echo.
echo ------------------------------------------------------------
echo [2/4] Running JMH benchmark suite (summary mode)
echo ------------------------------------------------------------
if not exist "target\lab2-1.0-SNAPSHOT-benchmarks.jar" (
    echo Benchmarks JAR not found. Expected at target\lab2-1.0-SNAPSHOT-benchmarks.jar
    goto :error
)
java -cp "target\lab2-1.0-SNAPSHOT-benchmarks.jar" rj.lab2.benchmarks.ReceiptStatisticsBenchmarkSummary
if ERRORLEVEL 1 goto :error

echo.
echo ------------------------------------------------------------
echo [3/4] Looking for Python interpreter
echo ------------------------------------------------------------
set "PYTHON_EXE="
for %%P in (python.exe py.exe) do (
    if not defined PYTHON_EXE (
        for /f "delims=" %%I in ('where %%P 2^>nul') do (
            set "PYTHON_EXE=%%I"
            goto :python_found
        )
    )
)
:python_found
if not defined PYTHON_EXE (
    echo Python interpreter not found (python.exe or py.exe). Install Python 3 with pandas/matplotlib.
    goto :error
)
echo Using Python: %PYTHON_EXE%

echo.
echo ------------------------------------------------------------
echo [4/4] Building benchmark graphs (saved under target\reports)
echo ------------------------------------------------------------
"%PYTHON_EXE%" report\graph\graph.py --save --output target\reports
if ERRORLEVEL 1 goto :error

echo.
echo [OK] lab2 benchmarking workflow finished successfully.
echo CSV data:       %cd%\target\reports\lab2-benchmark-summary.csv
echo Crossover data: %cd%\target\reports\lab2-benchmark-crossover.csv
echo Graph images:   %cd%\target\reports
goto :end

:error
echo.
echo [FAIL] lab2 workflow failed. Check the logs above for details.
exit /b 1

:end
popd
endlocal
