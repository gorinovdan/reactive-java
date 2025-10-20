import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

def load_data(csv_path=None):
    """
    Загружает данные из CSV или запрашивает ввод вручную.
    """
    if csv_path and os.path.exists(csv_path):
        df = pd.read_csv(csv_path)
        print(f"✅ Загружено {len(df)} строк из '{csv_path}'")
    else:
        print("CSV не найден. Введите данные вручную (пример ниже):")
        print("Dataset Receipts Iterative Stream Custom")
        print("Пример: Simple 5000 45 62 59")
        print("Введите пустую строку для завершения.")

        rows = []
        while True:
            line = input("> ").strip()
            if not line:
                break
            parts = line.split()
            if len(parts) != 5:
                print("Ошибка: нужно 5 значений.")
                continue
            dataset, receipts, iterative, stream, custom = parts
            rows.append({
                "Dataset": dataset,
                "Receipts": int(receipts),
                "Iterative": float(iterative),
                "Stream": float(stream),
                "Custom": float(custom),
            })
        df = pd.DataFrame(rows)

    return df


def plot_benchmarks_bar(df, save=False):
    """
    Строит столбчатые диаграммы для каждого типа набора (Simple / Complex)
    с логарифмической шкалой по оси Y.
    """
    datasets = df["Dataset"].unique()

    for ds in datasets:
        subset = df[df["Dataset"] == ds].sort_values("Receipts")

        x = np.arange(len(subset))
        width = 0.25

        fig, ax = plt.subplots(figsize=(8, 5))
        bars1 = ax.bar(x - width, subset["Iterative"], width, label="Iterative")
        bars2 = ax.bar(x, subset["Stream"], width, label="Stream")
        bars3 = ax.bar(x + width, subset["Custom"], width, label="Custom collector")

        ax.set_xlabel("Receipts count")
        ax.set_ylabel("Time (ms, log scale)")
        ax.set_title(f"Benchmark: {ds} dataset (logarithmic scale, lower is better)")

        ax.set_xticks(x)
        ax.set_xticklabels([f"{int(v):,}".replace(",", " ") for v in subset["Receipts"]])

        ax.set_yscale("log")  # ← вот ключевая строка!
        ax.legend()
        ax.grid(axis="y", linestyle="--", alpha=0.6, which="both")

        plt.tight_layout()

        if save:
            filename = f"{ds.lower()}_bar_log.png"
            plt.savefig(filename, dpi=150)
            print(f"💾 Сохранено: {filename}")

        plt.show()


def main():
    csv_path = input("Введите путь к CSV (или Enter для ручного ввода): ").strip() or None
    df = load_data(csv_path)
    save_choice = input("Сохранить графики в PNG? (y/n): ").strip().lower().startswith("y")
    plot_benchmarks_bar(df, save=save_choice)


if __name__ == "__main__":
    main()
