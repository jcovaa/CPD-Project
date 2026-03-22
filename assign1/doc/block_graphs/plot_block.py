import pandas as pd
import matplotlib.pyplot as plt
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))

INPUT_DIR = os.path.join(REPO_ROOT, "csvs")
OUTPUT_DIR = SCRIPT_DIR
if not os.path.exists(OUTPUT_DIR):
    os.makedirs(OUTPUT_DIR)

df_block = pd.read_csv(os.path.join(INPUT_DIR, "results_block.csv"))
df_block['GFlops'] = (2 * df_block['Size']**3) / (df_block['Time_Seconds'] * 1e9)
df_avg = df_block.groupby(['Size', 'BlockSize'])['GFlops'].mean().reset_index()

plt.figure(figsize=(10, 6))
colors = ['#1f77b4', '#ff7f0e', '#2ca02c']
markers = ['o', 's', '^']

for i, bs in enumerate(df_avg['BlockSize'].unique()):
    subset = df_avg[df_avg['BlockSize'] == bs]
    plt.plot(subset['Size'], subset['GFlops'], 
             label=f'BlockSize {bs}', color=colors[i], marker=markers[i], linewidth=2)

plt.title("Effect of Block Size on Performance", fontweight='bold')
plt.xlabel("Matrix Size (N)")
plt.ylabel("Performance (GFlop/s)")
plt.legend(title="Block Size", frameon=True)
plt.grid(True, linestyle="--", alpha=0.7)
plt.savefig(f"{OUTPUT_DIR}/block_analysis.png")

print(f"Block analysis graph saved to ./{OUTPUT_DIR}")