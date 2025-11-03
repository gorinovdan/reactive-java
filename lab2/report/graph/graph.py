#!/usr/bin/env python3
"""Visualisation helpers for lab2 JMH benchmarks.

The script expects the CSV artefacts produced by
``rj.lab2.benchmarks.ReceiptStatisticsBenchmarkSummary``:

* ``target/reports/lab2-benchmark-summary.csv`` (required)
* ``target/reports/lab2-benchmark-crossover.csv`` (optional but recommended)

Usage examples::

    # load default CSV files, render charts interactively
    python graph.py

    # export charts to PNG files alongside the CSVs
    python graph.py --save --output target/reports
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

MODULE_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SUMMARY = MODULE_ROOT / "target" / "reports" / "lab2-benchmark-summary.csv"
DEFAULT_CROSSOVER = MODULE_ROOT / "target" / "reports" / "lab2-benchmark-crossover.csv"

PLOT_CONFIG = {
    "ReceiptStatisticsBenchmark": [
        ("sequentialStream", "ReceiptStats: Sequential"),
        ("parallelStream", "ReceiptStats: Parallel"),
        ("parallelStreamWithCustomSpliterator", "ReceiptStats: Spliterator"),
    ],
    "StatisticsMicroBenchmarks": [
        ("totalRevenueCircle", "TotalRevenue: Loop"),
        ("totalRevenueStream", "TotalRevenue: Stream"),
        ("totalRevenueCollector", "TotalRevenue: Collector"),
        ("topItemsCircle", "TopItems: Loop"),
        ("topItemsStream", "TopItems: Stream"),
        ("topItemsCollector", "TopItems: Collector"),
        ("itemAverageSequential", "ItemAverage: Sequential"),
        ("itemAverageParallel", "ItemAverage: Parallel"),
        ("receiptStatisticsSequential", "ReceiptStats (Micro): Sequential"),
        ("receiptStatisticsParallel", "ReceiptStats (Micro): Parallel"),
        ("receiptStatisticsSpliterator", "ReceiptStats (Micro): Spliterator"),
    ],
}


@dataclass
class CrossoverInfo:
    delay_millis: int
    description: str

    @staticmethod
    def from_row(row: pd.Series) -> "CrossoverInfo":
        typ = (row.get("type") or "").strip().lower()
        note = str(row.get("note") or "").strip()
        delay = int(row["delayMillis"])

        if typ == "exact" or typ == "interpolated":
            estimated = row.get("estimatedDatasetSize")
            if pd.notna(estimated):
                approx = int(round(float(estimated)))
                description = f"≈ {approx:,} receipts"
            else:
                description = "within measured range"
        else:
            description = note or "no crossover within measured range"
        return CrossoverInfo(delay_millis=delay, description=description)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--summary",
        type=Path,
        default=DEFAULT_SUMMARY,
        help="Path to lab2-benchmark-summary.csv (default: %(default)s)",
    )
    parser.add_argument(
        "--crossover",
        type=Path,
        default=DEFAULT_CROSSOVER,
        help="Path to lab2-benchmark-crossover.csv (optional, default: %(default)s)",
    )
    parser.add_argument(
        "--save",
        action="store_true",
        help="Save figures to PNG files instead of (or in addition to) showing them interactively.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Directory for exported images (defaults to the summary CSV directory).",
    )
    return parser.parse_args()


def load_summary(path: Path) -> pd.DataFrame:
    if not path.exists():
        raise FileNotFoundError(f"Summary CSV not found: {path}")
    df = pd.read_csv(path)
    expected_columns = {
        "delayMillis",
        "datasetSize",
        "aggregator",
        "meanMillis",
        "errorMillis",
        "unit",
    }
    missing = expected_columns - set(df.columns)
    if missing:
        raise ValueError(f"Summary CSV is missing columns: {', '.join(sorted(missing))}")
    return df


def load_crossover(path: Path) -> dict[int, CrossoverInfo]:
    if not path.exists():
        return {}
    df = pd.read_csv(path)
    if df.empty:
        return {}
    return {
        int(row["delayMillis"]): CrossoverInfo.from_row(row)
        for _, row in df.iterrows()
        if pd.notna(row.get("delayMillis"))
    }


def plot_benchmarks(
    df: pd.DataFrame,
    crossover_info: dict[int, CrossoverInfo],
    save: bool,
    output_dir: Optional[Path],
) -> None:
    df = df.copy()
    df["delayMillis"] = df["delayMillis"].astype(int)
    df["datasetSize"] = df["datasetSize"].astype(int)
    splits = df["aggregator"].apply(lambda name: name.rsplit(".", 1) if isinstance(name, str) else [name, ""])
    df["aggregatorClass"] = splits.apply(lambda parts: parts[0] if parts else "")
    df["aggregatorMethod"] = splits.apply(lambda parts: parts[1] if len(parts) > 1 else (parts[0] or ""))

    # Stable ordering by delay and dataset size.
    grouped_by_delay = df.groupby("delayMillis")

    for delay, delay_subset in sorted(grouped_by_delay, key=lambda item: item[0]):
        for class_name, aggregators in PLOT_CONFIG.items():
            class_subset = delay_subset[delay_subset["aggregatorClass"] == class_name]
            if class_subset.empty:
                continue

            class_subset = class_subset.sort_values("datasetSize")
            pivot_mean = class_subset.pivot(index="datasetSize", columns="aggregatorMethod", values="meanMillis")
            pivot_err = class_subset.pivot(index="datasetSize", columns="aggregatorMethod", values="errorMillis")

            plot_methods = [(method, label) for method, label in aggregators if method in pivot_mean.columns]
            if not plot_methods:
                continue

            x = np.arange(len(pivot_mean.index))
            width = 0.8 / max(1, len(plot_methods))
            colors = plt.cm.get_cmap("viridis", len(plot_methods))

            fig, ax = plt.subplots(figsize=(10, 6))
            for idx, (method, label) in enumerate(plot_methods):
                offset = (idx - (len(plot_methods) - 1) / 2) * width
                means = pivot_mean[method].to_numpy()
                errors = pivot_err.get(method, pd.Series([0.0] * len(means))).to_numpy()
                ax.bar(
                    x + offset,
                    means,
                    width=width,
                    label=label,
                    color=colors(idx),
                    yerr=errors,
                    capsize=6,
                )

            ax.set_title(f"{class_name} (delay = {delay} ms)")
            ax.set_xlabel("Receipts count")
            ax.set_ylabel("Mean time, ms (log scale)")
            ax.set_xticks(x)
            ax.set_xticklabels([f"{value:,}".replace(",", " ") for value in pivot_mean.index])
            ax.set_yscale("log")
            ax.grid(axis="y", linestyle="--", alpha=0.5, which="both")
            ax.legend()

            if delay in crossover_info:
                info = crossover_info[delay]
                ax.text(
                    0.02,
                    0.95,
                    f"Crossover: {info.description}",
                    transform=ax.transAxes,
                    fontsize=10,
                    va="top",
                    ha="left",
                    bbox=dict(boxstyle="round", facecolor="white", alpha=0.6),
                )

            fig.tight_layout()

            if save:
                target_dir = output_dir or Path(DEFAULT_SUMMARY).parent
                target_dir.mkdir(parents=True, exist_ok=True)
                safe_class = class_name.replace(".", "_")
                filename = target_dir / f"lab2-benchmark-{safe_class}-delay-{delay}.png"
                fig.savefig(filename, dpi=180)
                print(f"Saved chart: {filename}")

            plt.show()


def main() -> None:
    args = parse_args()
    try:
        summary_df = load_summary(args.summary)
        crossover_info = load_crossover(args.crossover)
        plot_benchmarks(summary_df, crossover_info, save=args.save, output_dir=args.output)
    except Exception as exc:  # pragma: no cover - used for a concise CLI experience
        print(f"Error: {exc}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
