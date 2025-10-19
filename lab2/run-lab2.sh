#!/bin/sh
set -e  # завершать при ошибке команды

# Перейти в каталог скрипта (аналог pushd %~dp0)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

jenv local 23

echo "------------------------------------------------------------"
echo "[1/3] Cleaning and building lab2 (with shaded benchmarks JAR)"
echo "------------------------------------------------------------"
mvn -DskipTests clean package || { echo "[FAIL] Build failed"; exit 1; }

echo
echo "------------------------------------------------------------"
echo "[2/3] Running sample statistics report (Main)"
echo "------------------------------------------------------------"
mvn -DskipTests exec:java || { echo "[FAIL] Sample run failed"; exit 1; }

echo
echo "------------------------------------------------------------"
echo "[3/3] Running JMH benchmarks (all configured variants)"
echo "------------------------------------------------------------"
if [ ! -f "target/lab2-1.0-SNAPSHOT-benchmarks.jar" ]; then
    echo "Benchmarks JAR not found. Expected at target/lab2-1.0-SNAPSHOT-benchmarks.jar"
    echo "[FAIL] Missing JAR"
    exit 1
fi

java -jar "target/lab2-1.0-SNAPSHOT-benchmarks.jar" ReceiptStatisticsBenchmark || {
    echo "[FAIL] Benchmarks run failed"
    exit 1
}

echo
echo "[OK] lab2 workflow finished successfully."