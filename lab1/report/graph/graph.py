import subprocess
import json
import pandas as pd
import matplotlib.pyplot as plt


def run_java_benchmark():
    """
    Запускает Java-бенчмарк и читает benchmark_results.json.
    """
    # print("▶ Запуск Java-бенчмарка...")
    # subprocess.run(["java", "-cp", "/Users/Shared/Alexander/Education/reactive-java/lab1/target/classes", "/Users/Shared/Alexander/Education/reactive-java/lab1/src/main/java/rj/lab1/AggregationBenchmark.java"], check=True)
    # print("✅ Java-бенчмарк завершён.")

    with open("benchmark_results.json", "r", encoding="utf-8") as f:
        data = json.load(f)
    return data


def plot_aggregator_performance(data):
    """
    Строит графики по результатам benchmark_results.json.
    """
    # Преобразуем словарь в формат DataFrame
    metrics = ["TotalRevenue.circleAggregate", "TotalRevenue.streamAggregate", "TotalRevenue.customCollectorAggregate",
               "TopItemsByQuantity.circle", "TopItemsByQuantity.stream", "TopItemsByQuantity.collector",
               "ItemAverageReceipt.circle", "ItemAverageReceipt.stream", "ItemAverageReceipt.collector",
               "ReceiptStatistics.circle", "ReceiptStatistics.stream", "ReceiptStatistics.collector"]

    df = pd.DataFrame({
        "Metric": metrics,
        "5000": [data[m][0] for m in metrics],
        "25000": [data[m][1] for m in metrics],
        "250000": [data[m][2] for m in metrics]
    })

    # Группы метрик
    groups = {
        "TotalRevenue": ["TotalRevenue.circleAggregate", "TotalRevenue.streamAggregate", "TotalRevenue.customCollectorAggregate"],
        "TopItemsByQuantity": ["TopItemsByQuantity.circle", "TopItemsByQuantity.stream", "TopItemsByQuantity.collector"],
        "ItemAverageReceipt": ["ItemAverageReceipt.circle", "ItemAverageReceipt.stream", "ItemAverageReceipt.collector"],
        "ReceiptStatistics": ["ReceiptStatistics.circle", "ReceiptStatistics.stream", "ReceiptStatistics.collector"],
    }

    # Цвета реализаций
    implementations = {
        "circle": "#4e79a7",
        "stream": "#f28e2b",
        "customCollector": "#e15759",
    }

    bar_width = 0.25

    # Построение графиков
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    axes = axes.flatten()

    for idx, (group_name, group_metrics) in enumerate(groups.items()):
        sub_df = df[df["Metric"].isin(group_metrics)]
        sub_df["Implementation"] = sub_df["Metric"].apply(
            lambda x: "circle" if "circle" in x
            else "stream" if "stream" in x
            else "customCollector"
        )

        x = range(3)  # 3 размера: 5000, 25000, 250000

        for i, impl in enumerate(["circle", "stream", "customCollector"]):
            impl_vals = sub_df[sub_df["Implementation"] == impl][["5000", "25000", "250000"]].values.flatten()
            axes[idx].bar([xj + (i - 1) * bar_width for xj in x],
                          impl_vals,
                          width=bar_width,
                          color=implementations[impl],
                          label=impl)

        axes[idx].set_xticks(x)
        axes[idx].set_xticklabels(["5,000", "25,000", "250,000"])
        axes[idx].set_yscale("log")
        axes[idx].set_title(group_name)
        axes[idx].set_ylabel("Execution time (ms, log scale)")
        axes[idx].legend(title="Implementation")

    plt.suptitle("Aggregator Performance by Metric and Implementation (log scale)",
                 fontsize=14, fontweight="bold")
    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.show()


if __name__ == "__main__":
    data = run_java_benchmark()
    plot_aggregator_performance(data)