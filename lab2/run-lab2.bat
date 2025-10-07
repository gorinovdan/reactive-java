@echo off
setlocal ENABLEDELAYEDEXPANSION

rem Navigate to the lab2 module directory
pushd %~dp0

echo ------------------------------------------------------------
echo [1/3] Cleaning and building lab2 (with shaded benchmarks JAR)
echo ------------------------------------------------------------
call mvn -DskipTests clean package
if ERRORLEVEL 1 goto :error

echo.
echo ------------------------------------------------------------
echo [2/3] Running sample statistics report (Main)
echo ------------------------------------------------------------
call mvn -DskipTests exec:java
if ERRORLEVEL 1 goto :error

echo.
echo ------------------------------------------------------------
echo [3/3] Running JMH benchmarks (all configured variants)
echo ------------------------------------------------------------
if not exist "target\lab2-1.0-SNAPSHOT-benchmarks.jar" (
    echo Benchmarks JAR not found. Expected at target\lab2-1.0-SNAPSHOT-benchmarks.jar
    goto :error
)
java -jar "target\lab2-1.0-SNAPSHOT-benchmarks.jar" ReceiptStatisticsBenchmark
if ERRORLEVEL 1 goto :error

echo.
echo [OK] lab2 workflow finished successfully.
goto :end

:error
echo.
echo [FAIL] lab2 workflow failed. Check the logs above for details.
exit /b 1

:end
popd
endlocal
