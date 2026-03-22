import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import os

# ── Config ────────────────────────────────────────────────────────────────────
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT_CSV = os.path.join(BASE_DIR, "..", "csvs", "results_perf.csv")
OUTPUT_DIR = BASE_DIR

PERF_COUNTERS = [
    "cpu-cycles",
    "mem_load_retired.l1_miss",
    "mem_load_retired.l1_hit",
    "mem_load_retired.l2_miss",
    "mem_load_retired.l2_hit",
    "mem_load_retired.l3_miss",
    "mem_load_retired.l3_hit",
]

COUNTER_LABELS = {
    "cpu-cycles": "CPU Cycles",
    "mem_load_retired.l1_miss": "L1 Misses",
    "mem_load_retired.l1_hit": "L1 Hits",
    "mem_load_retired.l2_miss": "L2 Misses",
    "mem_load_retired.l2_hit": "L2 Hits",
    "mem_load_retired.l3_miss": "L3 Misses",
    "mem_load_retired.l3_hit": "L3 Hits",
}

ALG_NAMES = {
    1: "Alg1 - Column (ijk)",
    2: "Alg2 - Line (ikj)",
    3: "Alg3 - Block",
}

COLORS = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd"]
MARKERS = ["o", "s", "^", "D", "v"]

os.makedirs(OUTPUT_DIR, exist_ok=True)


# ── Human-readable axis formatter ─────────────────────────────────────────────
def human_format(num, pos):
    if num >= 1e12:
        return f"{num / 1e12:.1f}T"
    if num >= 1e9:
        return f"{num / 1e9:.1f}B"
    if num >= 1e6:
        return f"{num / 1e6:.1f}M"
    if num >= 1e3:
        return f"{num / 1e3:.1f}K"
    return str(int(num))


formatter = ticker.FuncFormatter(human_format)

# ── Load & prepare data ───────────────────────────────────────────────────────
df = pd.read_csv(INPUT_CSV)

for col in PERF_COUNTERS:
    if col in df.columns:
        df[col] = pd.to_numeric(df[col], errors="coerce")

df["BlockSize"] = df["BlockSize"].fillna("N/A").astype(str)

# Average across runs
df_avg = (
    df.groupby(["Algorithm", "Size", "BlockSize"])[PERF_COUNTERS].mean().reset_index()
)


# ── Helper: series label ──────────────────────────────────────────────────────
def series_label(alg, block_size):
    base = ALG_NAMES.get(alg, f"Alg{alg}")
    if alg == 3 and block_size != "N/A":
        base += f" (block={block_size})"
    return base


# ── Plot 1: Each counter vs Size — all algorithms on one chart ────────────────
print("Generating per-counter plots...")
for counter in PERF_COUNTERS:
    if counter not in df_avg.columns:
        continue

    fig, ax = plt.subplots(figsize=(10, 6))
    color_idx = 0

    for (alg, block_size), group in df_avg.groupby(["Algorithm", "BlockSize"]):
        group = group.sort_values("Size")
        label = series_label(alg, block_size)
        ax.plot(
            group["Size"],
            group[counter],
            marker=MARKERS[color_idx % len(MARKERS)],
            color=COLORS[color_idx % len(COLORS)],
            label=label,
            linewidth=2,
            markersize=6,
        )
        color_idx += 1

    ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
    ax.set_ylabel(COUNTER_LABELS[counter], fontsize=12)
    ax.set_title(f"{COUNTER_LABELS[counter]} vs Matrix Size", fontsize=14)
    ax.yaxis.set_major_formatter(formatter)
    ax.legend(fontsize=9)
    ax.grid(True, linestyle="--", alpha=0.5)

    fname = os.path.join(OUTPUT_DIR, f"{counter.replace('.', '_')}_vs_size.png")
    plt.tight_layout()
    plt.savefig(fname, dpi=150)
    plt.close()
    print(f"  Saved: {fname}")

# ── Plot 2: Cache miss rate (%) vs Size ───────────────────────────────────────
print("\nGenerating cache miss rate plots...")

levels = [
    ("l1", "mem_load_retired.l1_miss", "mem_load_retired.l1_hit", "L1"),
    ("l2", "mem_load_retired.l2_miss", "mem_load_retired.l2_hit", "L2"),
    ("l3", "mem_load_retired.l3_miss", "mem_load_retired.l3_hit", "L3"),
]

for level_key, miss_col, hit_col, level_name in levels:
    if miss_col not in df_avg.columns or hit_col not in df_avg.columns:
        continue

    fig, ax = plt.subplots(figsize=(10, 6))
    color_idx = 0

    for (alg, block_size), group in df_avg.groupby(["Algorithm", "BlockSize"]):
        group = group.sort_values("Size").copy()
        total = group[miss_col] + group[hit_col]
        miss_rate = (group[miss_col] / total) * 100

        label = series_label(alg, block_size)
        ax.plot(
            group["Size"],
            miss_rate,
            marker=MARKERS[color_idx % len(MARKERS)],
            color=COLORS[color_idx % len(COLORS)],
            label=label,
            linewidth=2,
            markersize=6,
        )
        color_idx += 1

    ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
    ax.set_ylabel(f"{level_name} Miss Rate (%)", fontsize=12)
    ax.set_title(f"{level_name} Cache Miss Rate vs Matrix Size", fontsize=14)
    ax.yaxis.set_major_formatter(ticker.FuncFormatter(lambda x, _: f"{x:.1f}%"))
    ax.legend(fontsize=9)
    ax.grid(True, linestyle="--", alpha=0.5)

    fname = os.path.join(OUTPUT_DIR, f"{level_key}_miss_rate_vs_size.png")
    plt.tight_layout()
    plt.savefig(fname, dpi=150)
    plt.close()
    print(f"  Saved: {fname}")

# ── Plot 3: L1/L2/L3 misses grouped bar — one chart per algorithm ─────────────
print("\nGenerating cache miss comparison bar charts...")

miss_cols = [
    "mem_load_retired.l1_miss",
    "mem_load_retired.l2_miss",
    "mem_load_retired.l3_miss",
]
miss_names = ["L1 Misses", "L2 Misses", "L3 Misses"]

for (alg, block_size), group in df_avg.groupby(["Algorithm", "BlockSize"]):
    if not all(c in group.columns for c in miss_cols):
        continue

    group = group.sort_values("Size")
    sizes = group["Size"].astype(str).tolist()
    x = np.arange(len(sizes))
    width = 0.25

    fig, ax = plt.subplots(figsize=(12, 6))
    for i, (col, name) in enumerate(zip(miss_cols, miss_names)):
        ax.bar(
            x + i * width, group[col], width, label=name, color=COLORS[i], alpha=0.85
        )

    label = series_label(alg, block_size)
    ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
    ax.set_ylabel("Miss Count", fontsize=12)
    ax.set_title(f"Cache Misses per Level — {label}", fontsize=13)
    ax.set_xticks(x + width)
    ax.set_xticklabels(sizes)
    ax.yaxis.set_major_formatter(formatter)
    ax.legend(fontsize=10)
    ax.grid(True, axis="y", linestyle="--", alpha=0.5)

    safe = (
        label.replace(" ", "_")
        .replace("/", "_")
        .replace("(", "")
        .replace(")", "")
        .replace("=", "")
    )
    fname = os.path.join(OUTPUT_DIR, f"cache_misses_bar_{safe}.png")
    plt.tight_layout()
    plt.savefig(fname, dpi=150)
    plt.close()
    print(f"  Saved: {fname}")

print(f"\nAll graphs saved to ./{OUTPUT_DIR}/")
