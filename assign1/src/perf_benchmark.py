import subprocess
import re
import csv
import argparse
import os

# =============================================================================
# USAGE INSTRUCTIONS
# =============================================================================
#
# PREREQUISITES
# -------------
# 1. Compile the binary with OpenMP:
#       g++ -O2 -fopenmp matrixproduct.cpp -o ../bin/matrixproduct_cpp
#
# 2. Lower perf permissions (required before every session, resets on reboot):
#       sudo sysctl kernel.perf_event_paranoid=0
#
# 3. After benchmarking, restore permissions:
#       sudo sysctl kernel.perf_event_paranoid=4
#
# -----------------------------------------------------------------------------
# PART 1 — Single-core benchmarks
# -----------------------------------------------------------------------------
#
# 1.1 + 1.2  Normal and Line algorithms
#            (Normal: 1024–3072 step 512 | Line: same + 4096–10240 step 2048):
#
#   python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 \
#       --algorithm normal line --detailed --runs 3 \
#       --output perf_results_part1.csv
#
# 1.3  Block algorithm (4096–10240 step 2048, block sizes 128/256/512):
#
#   python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 \
#       --algorithm block --detailed --runs 3 \
#       --output perf_results_part1.csv
#
# All Part 1 experiments at once:
#
#   python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 \
#       --detailed --runs 3 \
#       --output perf_results_part1.csv
#
# -----------------------------------------------------------------------------
# PART 2 — Multi-core benchmarks
# -----------------------------------------------------------------------------
#
# 2.1  All parallel versions of Normal and Line
#      (4 threads, sizes 1024–3072 step 512):
#      + 2.2  Line_Parallel1 thread scaling
#      (size 8192, threads 4/8/12/16/20/24) — runs automatically when 'line' selected:
#
#   python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 2 \
#       --algorithm normal line --runs 3 \
#       --output perf_results_part2.csv
#
# 2.2 only — skip Normal, run only line thread-scaling:
#
#   python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 2 \
#       --algorithm line --runs 3 \
#       --output perf_results_part2.csv
#
# -----------------------------------------------------------------------------
# RUN EVERYTHING IN ONE SHOT (permissions auto-restored on finish or crash)
# -----------------------------------------------------------------------------
#
#   sudo sysctl kernel.perf_event_paranoid=0 && \
#   python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 2 \
#       --detailed --runs 3 \
#       --output perf_results.csv ; \
#   sudo sysctl kernel.perf_event_paranoid=4
#
# NOTE: ';' before the restore ensures permissions reset even on Ctrl+C.
# =============================================================================



# Perf events to measure — covers L1, L2/LLC cache behavior plus general counters
DEFAULT_EVENTS = [
    "L1-dcache-loads",
    "L1-dcache-load-misses",
    "LLC-loads",
    "LLC-load-misses",
    "instructions",
    "cycles",
]

# More specific events mentioned in the assignment (may need kernel support)
DETAILED_EVENTS = [
    "mem_load_retired.l1_miss",
    "mem_load_retired.l2_miss",
]

ALGORITHM_NAMES = {
    (1, 1): "Normal",
    (1, 2): "Normal_Parallel1",
    (1, 3): "Normal_Parallel2",
    (2, 1): "Line",
    (2, 2): "Line_Parallel1",
    (2, 3): "Line_Parallel2",
    (3, 1): "Block",
}


def build_input(option, size, alg_version=1, block_size=None):
    """Build the stdin input string for the matrix product binary."""
    # Menu: choose option -> enter size -> (sub-menu or block size) -> exit
    if option == 3:
        return f"{option}\n{size}\n{block_size}\n0\n"
    else:
        return f"{option}\n{size}\n{alg_version}\n0\n"


def parse_perf_output(stderr_text, requested_events):
    """Parse perf stat stderr output into a dict of event -> value.

    Handles both plain format (e.g. '1,234  instructions') and hybrid CPU
    format (e.g. '1,234  cpu_atom/instructions/').
    For hybrid CPUs, prefers a counted value from either core type.
    """
    counters = {e: "N/A" for e in requested_events}

    for line in stderr_text.splitlines():
        line = line.strip()
        if not line:
            continue

        # Match: "1,234,567  cpu_atom/event/" or "1,234,567  event-name"
        match = re.match(r"^([\d,]+)\s+(?:\w+/)?([\w.\-]+)/?", line)
        if match:
            value_str = match.group(1).replace(",", "")
            raw_event = match.group(2)
            try:
                value = int(value_str)
            except ValueError:
                value = value_str
            # Map back to the requested event name
            for req in requested_events:
                if raw_event == req or raw_event.rstrip("/") == req:
                    # Only overwrite N/A or if this is a real (non-zero) value
                    if counters[req] == "N/A" or (isinstance(value, int) and value > 0):
                        counters[req] = value
                    break
            continue

        # Handle "<not supported>" or "<not counted>" lines
        ns_match = re.match(r".*<not (?:supported|counted)>\s+(?:\w+/)?([\w.\-]+)/?", line)
        if ns_match:
            raw_event = ns_match.group(1)
            for req in requested_events:
                if raw_event == req or raw_event.rstrip("/") == req:
                    # Don't overwrite a real value with N/A
                    break

    return counters


def parse_time(stdout_text):
    """Extract execution time from the program's stdout."""
    match = re.search(r"Time:\s+([\d.]+)\s+seconds", stdout_text)
    return float(match.group(1)) if match else None


def run_perf_benchmark(binary, size, option, alg_version=1, block_size=None,
                       events=None, threads=None):
    """Run a single perf stat benchmark and return (time, counters)."""
    if events is None:
        events = DEFAULT_EVENTS

    input_data = build_input(option, size, alg_version, block_size)
    events_str = ",".join(events)

    env = os.environ.copy()
    if threads is not None:
        env["OMP_NUM_THREADS"] = str(threads)

    cmd = ["perf", "stat", "-e", events_str, binary]

    try:
        process = subprocess.run(
            cmd,
            input=input_data,
            capture_output=True,
            text=True,
            timeout=600,
            env=env,
        )
    except subprocess.TimeoutExpired:
        print(f"  TIMEOUT for size={size}")
        return None, {}
    except FileNotFoundError as e:
        print(f"  Error: {e}")
        return None, {}

    time_sec = parse_time(process.stdout)
    counters = parse_perf_output(process.stderr, events)

    return time_sec, counters


def compute_gflops(n, time_sec):
    """GFlop/s = 2*n^3 / (time * 10^9)."""
    if time_sec and time_sec > 0:
        return (2.0 * n ** 3) / (time_sec * 1e9)
    return None


def run_part1(binary, output, events, runs, algorithms=None):
    """Part 1: Single-core perf analysis for Normal, Line, and Block algorithms.
    
    Args:
        algorithms: List of algorithm names to run. If None, runs all.
                   Valid: 'normal', 'line', 'block'
    """
    print("=" * 60)
    print("PART 1: Single-core perf benchmarks")
    print("=" * 60)

    # Default to all algorithms if none specified
    if algorithms is None:
        algorithms = ['normal', 'line', 'block']
    algorithms = [a.lower() for a in algorithms]

    configs = []

    # Assignment Part 1.1 & 1.2: Normal and Line, sizes 1024 to 3072 step 512
    # (both C++ and the other language — here we benchmark the C++ binary)
    sizes_small = list(range(1024, 3073, 512))  # [1024, 1536, 2048, 2560, 3072]
    
    if 'normal' in algorithms:
        for size in sizes_small:
            configs.append((1, 1, size, None))  # Normal (col-by-col)
    
    if 'line' in algorithms:
        for size in sizes_small:
            configs.append((2, 1, size, None))  # Line (row-by-row)

    # Assignment Part 1.2 extended: Line only, sizes 4096 to 10240 step 2048 (C++ only)
    sizes_large = list(range(4096, 10241, 2048))  # [4096, 6144, 8192, 10240]
    
    if 'line' in algorithms:
        for size in sizes_large:
            configs.append((2, 1, size, None))  # Line

    # Assignment Part 1.3: Block multiplication, sizes 4096 to 10240 step 2048,
    # block sizes 128, 256, 512
    if 'block' in algorithms:
        block_sizes = [128, 256, 512]
        for size in sizes_large:
            for bs in block_sizes:
                configs.append((3, 1, size, bs))  # Block

    file_exists = os.path.isfile(output)
    with open(output, "a", newline="") as f:
        writer = csv.writer(f)
        if not file_exists:
            header = ["Algorithm", "Size", "BlockSize", "Threads", "Run",
                       "Time_Seconds", "GFlops"] + events
            writer.writerow(header)

        for option, alg, size, bs in configs:
            alg_name = ALGORITHM_NAMES.get((option, alg), f"op{option}_alg{alg}")
            bs_label = bs if bs else "N/A"
            print(f"\n>> {alg_name} size={size} block={bs_label}")

            for run_i in range(1, runs + 1):
                time_sec, counters = run_perf_benchmark(
                    binary, size, option, alg, bs, events
                )
                gflops = compute_gflops(size, time_sec)

                counter_values = [counters.get(e, "N/A") for e in events]
                row = [alg_name, size, bs_label, 1, run_i,
                       f"{time_sec:.3f}" if time_sec else "N/A",
                       f"{gflops:.4f}" if gflops else "N/A"] + counter_values
                writer.writerow(row)
                f.flush()

                time_str = f"{time_sec:.3f}s" if time_sec else "N/A"
                gf_str = f"{gflops:.4f}" if gflops else "N/A"
                print(f"  [Run {run_i}/{runs}] Time={time_str} GFlop/s={gf_str}")

    print(f"\nPart 1 results appended to {output}")


def run_part2(binary, output, events, runs, algorithms=None):
    """Part 2: Multi-core perf analysis with varying threads and sizes.
    
    Args:
        algorithms: List of algorithm names to run. If None, runs all.
                   Valid: 'normal', 'line'
    """
    print("=" * 60)
    print("PART 2: Multi-core perf benchmarks")
    print("=" * 60)

    # Default to all algorithms if none specified
    if algorithms is None:
        algorithms = ['normal', 'line']
    algorithms = [a.lower() for a in algorithms]

    file_exists = os.path.isfile(output)
    with open(output, "a", newline="") as f:
        writer = csv.writer(f)
        if not file_exists:
            header = ["Algorithm", "Size", "BlockSize", "Threads", "Run",
                       "Time_Seconds", "GFlops"] + events
            writer.writerow(header)

        # --- Phase A: 4 threads, sizes 1024 to 3072 step 512 ---
        # Assignment Part 2.1: parallel versions of Normal and Line
        print("\n--- Phase A: 4 threads, sizes 1024-3072 step 512 ---")
        thread_count = 4
        sizes_a = list(range(1024, 3073, 512))  # [1024, 1536, 2048, 2560, 3072]
        
        all_parallel_algos = [
            (1, 2, "Normal_Parallel1", "normal"),   # #pragma omp parallel for (outer loop)
            (1, 3, "Normal_Parallel2", "normal"),   # #pragma omp parallel + #pragma omp for (inner)
            (2, 2, "Line_Parallel1", "line"),       # #pragma omp parallel for (outer loop)
            (2, 3, "Line_Parallel2", "line"),       # #pragma omp parallel + #pragma omp for (inner)
        ]
        
        parallel_algos = [(o, a, n) for o, a, n, t in all_parallel_algos if t in algorithms]

        for option, alg, alg_name in parallel_algos:
            for size in sizes_a:
                print(f"\n>> {alg_name} size={size} threads={thread_count}")
                for run_i in range(1, runs + 1):
                    time_sec, counters = run_perf_benchmark(
                        binary, size, option, alg, None, events,
                        threads=thread_count
                    )
                    gflops = compute_gflops(size, time_sec)

                    counter_values = [counters.get(e, "N/A") for e in events]
                    row = [alg_name, size, "N/A", thread_count, run_i,
                           f"{time_sec:.3f}" if time_sec else "N/A",
                           f"{gflops:.4f}" if gflops else "N/A"] + counter_values
                    writer.writerow(row)
                    f.flush()

                    time_str = f"{time_sec:.3f}s" if time_sec else "N/A"
                    gf_str = f"{gflops:.4f}" if gflops else "N/A"
                    print(f"  [Run {run_i}/{runs}] Time={time_str} GFlop/s={gf_str}")

        # --- Phase B: Assignment Part 2.2 — ONLY Line_Parallel1 (Version 2),
        # size=8192, threads = 4,8,12,16,20,24 ---
        if 'line' in algorithms:
            print("\n--- Phase B: Line_Parallel1 only, size=8192, threads 4-24 ---")
            size_b = 8192
            thread_counts = [4, 8, 12, 16, 20, 24]
            phase_b_algos = [
                (2, 2, "Line_Parallel1"),   # the only algo required by assignment Part 2.2
            ]

            for option, alg, alg_name in phase_b_algos:
                for tc in thread_counts:
                    print(f"\n>> {alg_name} size={size_b} threads={tc}")
                    for run_i in range(1, runs + 1):
                        time_sec, counters = run_perf_benchmark(
                            binary, size_b, option, alg, None, events,
                            threads=tc
                        )
                        gflops = compute_gflops(size_b, time_sec)

                        counter_values = [counters.get(e, "N/A") for e in events]
                        row = [alg_name, size_b, "N/A", tc, run_i,
                               f"{time_sec:.3f}" if time_sec else "N/A",
                               f"{gflops:.4f}" if gflops else "N/A"] + counter_values
                        writer.writerow(row)
                        f.flush()

                        time_str = f"{time_sec:.3f}s" if time_sec else "N/A"
                        gf_str = f"{gflops:.4f}" if gflops else "N/A"
                        print(f"  [Run {run_i}/{runs}] Time={time_str} GFlop/s={gf_str}")
        else:
            print("\n--- Phase B: Skipped (Line algorithm not selected) ---")

    print(f"\nPart 2 results appended to {output}")


def main():
    parser = argparse.ArgumentParser(
        description="Perf-based benchmark for matrix multiplication",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run Part 1 (single-core perf counters):
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1

  # Run Part 2 (multi-core analysis):
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 2
Run ONLY the Block algorithm in Part 1:
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 --algorithm block

  # Run only Normal and Line algorithms in Part 1:
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 --algorithm normal line

  # Run only Line algorithm in Part 2 (multi-core):
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 2 --algorithm line

  # Custom events and more runs:
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 --runs 3 \\
      --events instructions cycles cache-misses

  # Use detailed Intel events:
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 --detailed

  # Block algorithm only with detailed events:
  python3 perf_benchmark.py ../bin/matrixproduct_cpp --part 1 --algorithm block --detailed
        """,
    )
    parser.add_argument("binary", help="Path to the matrix product binary")
    parser.add_argument(
        "--part", nargs="+", type=int, choices=[1, 2], default=[1, 2],
        help="Which part(s) to run (default: both)"
    )
    parser.add_argument(
        "--algorithm", "--algo", nargs="+", 
        choices=['normal', 'line', 'block'],
        help="Filter by algorithm(s). Part 1: normal, line, block. Part 2: normal, line"
    )
    parser.add_argument("--runs", type=int, default=1, help="Runs per config (default: 1)")
    parser.add_argument("--output", default="perf_results.csv", help="Output CSV")
    parser.add_argument(
        "--events", nargs="+", default=None,
        help="Custom perf events (overrides defaults)"
    )
    parser.add_argument(
        "--detailed", action="store_true",
        help="Add mem_load_retired.l1_miss and mem_load_retired.l2_miss events"
    )

    args = parser.parse_args()

    events = args.events if args.events else list(DEFAULT_EVENTS)
    if args.detailed:
        events.extend(DETAILED_EVENTS)

    if 1 in args.part:
        run_part1(args.binary, args.output, events, args.runs, args.algorithm)
    if 2 in args.part:
        run_part2(args.binary, args.output, events, args.runs, args.algorithm)


if __name__ == "__main__":
    main()