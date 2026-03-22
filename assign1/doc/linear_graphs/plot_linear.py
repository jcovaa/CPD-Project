import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import os

# ── Config ────────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))

INPUT_CSV = os.path.join(REPO_ROOT, "doc", "results_linear.csv")
OUTPUT_DIR = os.path.join(REPO_ROOT, "linear_graphs")

ALG_NAMES = {
    1: "Alg1 - Column (ijk)",
    2: "Alg2 - Line (ikj)",
}

# One color/marker per series (language + algorithm combo)
SERIES_STYLE = {
    ("cpp", 1): {"color": "#1f77b4", "marker": "o", "label": "C++ - Alg1 Column (ijk)"},
    ("cpp", 2): {"color": "#ff7f0e", "marker": "s", "label": "C++ - Alg2 Line (ikj)"},
    ("go",  1): {"color": "#2ca02c", "marker": "^", "label": "Go  - Alg1 Column (ijk)"},
    ("go",  2): {"color": "#d62728", "marker": "D", "label": "Go  - Alg2 Line (ikj)"},
}

os.makedirs(OUTPUT_DIR, exist_ok=True)

if not os.path.exists(INPUT_CSV):
    raise FileNotFoundError(f"Input CSV not found: {INPUT_CSV}")

# ── Load & prepare ────────────────────────────────────────────────────────────
df = pd.read_csv(INPUT_CSV)

df["Language"] = df["Binary"].apply(lambda x: "cpp" if "cpp" in x else "go")
df["Time_Seconds"] = pd.to_numeric(df["Time_Seconds"], errors="coerce")

# Average across runs
df_avg = (
    df.groupby(["Language", "Algorithm", "Size"])["Time_Seconds"]
    .mean()
    .reset_index()
)

# Compute GFlops: 2*N^3 / (T * 1e9)
df_avg["GFlops"] = (2 * df_avg["Size"] ** 3) / (df_avg["Time_Seconds"] * 1e9)

# ── Plot 1: Execution Time vs Size ────────────────────────────────────────────
print("Generating execution time plot...")
fig, ax = plt.subplots(figsize=(10, 6))

for (lang, alg), group in df_avg.groupby(["Language", "Algorithm"]):
    style = SERIES_STYLE[(lang, alg)]
    group = group.sort_values("Size")
    ax.plot(group["Size"], group["Time_Seconds"],
            color=style["color"], marker=style["marker"],
            label=style["label"], linewidth=2, markersize=6)

ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
ax.set_ylabel("Execution Time (seconds)", fontsize=12)
ax.set_title("Execution Time vs Matrix Size", fontsize=14)
ax.legend(fontsize=10)
ax.grid(True, linestyle="--", alpha=0.5)
plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "execution_time_vs_size.png"), dpi=150)
plt.close()
print("  Saved: execution_time_vs_size.png")

# ── Plot 2: GFlops vs Size (main assignment graph) ────────────────────────────
print("Generating GFlops plot...")
fig, ax = plt.subplots(figsize=(10, 6))

for (lang, alg), group in df_avg.groupby(["Language", "Algorithm"]):
    style = SERIES_STYLE[(lang, alg)]
    group = group.sort_values("Size")
    ax.plot(group["Size"], group["GFlops"],
            color=style["color"], marker=style["marker"],
            label=style["label"], linewidth=2, markersize=6)

ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
ax.set_ylabel("Performance (GFlop/s)", fontsize=12)
ax.set_title("Performance (GFlop/s) vs Matrix Size", fontsize=14)
ax.legend(fontsize=10)
ax.grid(True, linestyle="--", alpha=0.5)
plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "gflops_vs_size.png"), dpi=150)
plt.close()
print("  Saved: gflops_vs_size.png")

# ── Plot 3: GFlops per language separately (easier to compare alg1 vs alg2) ──
print("Generating per-language GFlops plots...")
for lang in ["cpp", "go"]:
    fig, ax = plt.subplots(figsize=(10, 6))
    for alg in [1, 2]:
        group = df_avg[(df_avg["Language"] == lang) & (df_avg["Algorithm"] == alg)].sort_values("Size")
        style = SERIES_STYLE[(lang, alg)]
        ax.plot(group["Size"], group["GFlops"],
                color=style["color"], marker=style["marker"],
                label=ALG_NAMES[alg], linewidth=2, markersize=6)

    lang_name = "C++" if lang == "cpp" else "Go"
    ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
    ax.set_ylabel("Performance (GFlop/s)", fontsize=12)
    ax.set_title(f"{lang_name} — Performance (GFlop/s) vs Matrix Size", fontsize=14)
    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.5)
    plt.tight_layout()
    fname = f"gflops_vs_size_{lang}.png"
    plt.savefig(os.path.join(OUTPUT_DIR, fname), dpi=150)
    plt.close()
    print(f"  Saved: {fname}")

# ── Plot 4: Speedup of Alg2 over Alg1 per language ───────────────────────────
print("Generating speedup plot (Alg2 vs Alg1)...")
fig, ax = plt.subplots(figsize=(10, 6))

for lang, color, marker in [("cpp", "#1f77b4", "o"), ("go", "#2ca02c", "^")]:
    lang_data = df_avg[df_avg["Language"] == lang]
    alg1 = lang_data[lang_data["Algorithm"] == 1].set_index("Size")["Time_Seconds"]
    alg2 = lang_data[lang_data["Algorithm"] == 2].set_index("Size")["Time_Seconds"]
    speedup = alg1 / alg2
    speedup = speedup.sort_index()
    lang_name = "C++" if lang == "cpp" else "Go"
    ax.plot(speedup.index, speedup.values,
            color=color, marker=marker,
            label=f"{lang_name} Speedup (Alg1 / Alg2)", linewidth=2, markersize=6)

ax.axhline(y=1, color="gray", linestyle="--", linewidth=1, label="Speedup = 1 (no gain)")
ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
ax.set_ylabel("Speedup (T_alg1 / T_alg2)", fontsize=12)
ax.set_title("Speedup of Line (ikj) over Column (ijk)", fontsize=14)
ax.legend(fontsize=10)
ax.grid(True, linestyle="--", alpha=0.5)
plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "speedup_alg2_over_alg1.png"), dpi=150)
plt.close()
print("  Saved: speedup_alg2_over_alg1.png")

# ── Plot 5: C++ vs Go comparison per algorithm ────────────────────────────────
print("Generating C++ vs Go comparison plots...")
for alg in [1, 2]:
    fig, ax = plt.subplots(figsize=(10, 6))
    for lang, color, marker in [("cpp", "#1f77b4", "o"), ("go", "#2ca02c", "^")]:
        group = df_avg[(df_avg["Language"] == lang) & (df_avg["Algorithm"] == alg)].sort_values("Size")
        lang_name = "C++" if lang == "cpp" else "Go"
        ax.plot(group["Size"], group["GFlops"],
                color=color, marker=marker,
                label=lang_name, linewidth=2, markersize=6)

    ax.set_xlabel("Matrix Size (N×N)", fontsize=12)
    ax.set_ylabel("Performance (GFlop/s)", fontsize=12)
    ax.set_title(f"C++ vs Go — {ALG_NAMES[alg]}", fontsize=14)
    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.5)
    plt.tight_layout()
    fname = f"cpp_vs_go_alg{alg}.png"
    plt.savefig(os.path.join(OUTPUT_DIR, fname), dpi=150)
    plt.close()
    print(f"  Saved: {fname}")

print(f"\nAll graphs saved to ./{OUTPUT_DIR}/")