import pandas as pd
import matplotlib.pyplot as plt
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))

INPUT_DIR = os.path.join(REPO_ROOT, "csvs")
OUTPUT_DIR = SCRIPT_DIR
os.makedirs(OUTPUT_DIR, exist_ok=True)

COLORS = {"V1": "#d62728", "V2": "#1f77b4", "Ideal": "#7f7f7f", "Speedup": "#2ca02c"}
MARKERS = {"V1": "o", "V2": "s", "Scale": "D"}


def load_and_group(filename):
    path = os.path.join(INPUT_DIR, filename)
    if not os.path.exists(path):
        return None
    df = pd.read_csv(path)
    return (
        df.groupby(["Algorithm", "Size", "Threads"])[["Time_Sec", "GFlops"]]
        .mean()
        .reset_index()
    )


v1_data = load_and_group("results_parallel_version1.csv")
v2_data = load_and_group("results_parallel_version2.csv")
scale_data = load_and_group("results_parallel.csv")

if v1_data is not None and v2_data is not None:
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))

    ax1.plot(
        v1_data["Size"],
        v1_data["Time_Sec"],
        color=COLORS["V1"],
        marker=MARKERS["V1"],
        label="V1: Column (ijk)",
    )
    ax1.plot(
        v2_data["Size"],
        v2_data["Time_Sec"],
        color=COLORS["V2"],
        marker=MARKERS["V2"],
        label="V2: Line (ikj)",
    )
    ax1.set_title("Execution Time (Seconds)", fontweight="bold")
    ax1.set_xlabel("Matrix Size (N)")
    ax1.set_ylabel("Time (s)")
    ax1.legend()
    ax1.grid(True, linestyle="--", alpha=0.6)

    ax2.plot(
        v1_data["Size"],
        v1_data["GFlops"],
        color=COLORS["V1"],
        marker=MARKERS["V1"],
        label="V1: Column (ijk)",
    )
    ax2.plot(
        v2_data["Size"],
        v2_data["GFlops"],
        color=COLORS["V2"],
        marker=MARKERS["V2"],
        label="V2: Line (ikj)",
    )
    ax2.set_title("Performance (GFlop/s)", fontweight="bold")
    ax2.set_xlabel("Matrix Size (N)")
    ax2.set_ylabel("GFlop/s")
    ax2.legend()
    ax2.grid(True, linestyle="--", alpha=0.6)

    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_DIR, "versions_comparison.png"))

if scale_data is not None:
    df_8192 = scale_data[scale_data["Size"] == 8192].sort_values("Threads")

    t4_ref = df_8192[df_8192["Threads"] == 4]["Time_Sec"].values[0]
    df_8192["Speedup"] = t4_ref / df_8192["Time_Sec"]
    df_8192["Efficiency"] = df_8192["Speedup"] / (df_8192["Threads"] / 4)

    fig, (ax3, ax4) = plt.subplots(1, 2, figsize=(16, 6))

    ax3.plot(
        [4, 24], [1, 6], color=COLORS["Ideal"], linestyle="--", label="Ideal Scaling"
    )
    ax3.plot(
        df_8192["Threads"],
        df_8192["Speedup"],
        color=COLORS["Speedup"],
        marker=MARKERS["Scale"],
        label="Measured Speedup",
    )
    ax3.set_title("Relative Speedup (Baseline: 4 Threads)", fontweight="bold")
    ax3.set_xlabel("Number of Threads")
    ax3.set_ylabel("Speedup ($T_4/T_n$)")
    ax3.set_xticks([4, 8, 12, 16, 20, 24])
    ax3.legend()
    ax3.grid(True, linestyle="--", alpha=0.6)

    ax4.plot(
        df_8192["Threads"],
        df_8192["Efficiency"] * 100,
        color="#9b59b6",
        marker="h",
        label="Efficiency %",
    )
    ax4.axhline(y=100, color="r", linestyle=":", alpha=0.5, label="Max Efficiency")
    ax4.set_title("Parallel Efficiency %", fontweight="bold")
    ax4.set_xlabel("Number of Threads")
    ax4.set_ylabel("Efficiency (%)")
    ax4.set_ylim(0, 110)
    ax4.set_xticks([4, 8, 12, 16, 20, 24])
    ax4.legend()
    ax4.grid(True, linestyle="--", alpha=0.6)

    plt.tight_layout()
    plt.savefig(os.path.join(OUTPUT_DIR, "scalability.png"))

print(f"\nAll graphs saved to ./{OUTPUT_DIR}/")
