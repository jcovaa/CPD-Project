import subprocess
import re
import csv
import argparse
import os

PERF_COUNTERS = [
    "cpu-cycles",
    "mem_load_retired.l1_miss",
    "mem_load_retired.l1_hit",
    "mem_load_retired.l2_miss",
    "mem_load_retired.l2_hit",
    "mem_load_retired.l3_miss",
    "mem_load_retired.l3_hit",
]


def run_perf_benchmark(binary_path, size, option, block_size=None, no_submenu=False, debug=False):
    input_sequence = [str(option), str(size)]
    # C++ binary has a sub-menu asking for Normal/Parallel1/Parallel2 (options 1 and 2)
    # Go binary does NOT have this sub-menu — use --no_submenu flag for Go
    if not no_submenu and option in (1, 2):
        input_sequence.append("1")
    if option == 3:
        input_sequence.append(str(block_size))
    # Send multiple 0s as safety net — Go's fmt.Scan on EOF keeps the last op value
    # so the loop may not break on a single "0" if timing is off
    input_sequence.extend(["0", "0", "0"])

    input_data = "\n".join(input_sequence) + "\n"

    if debug:
        print(f"\n--- INPUT SEQUENCE SENT ---")
        print(repr(input_data))
        print("--- END INPUT SEQUENCE ---\n")

    perf_cmd = [
        "perf", "stat",
        "-e", ",".join(PERF_COUNTERS),
        binary_path
    ]

    try:
        process = subprocess.run(
            perf_cmd,
            input=input_data,
            capture_output=True,
            text=True,
            timeout=600,   # 10 min safety timeout — prevents infinite hangs
            check=True
        )

        perf_output = process.stderr

        if debug:
            print("\n--- RAW PERF OUTPUT (stderr) ---")
            print(perf_output)
            print("--- END RAW PERF OUTPUT ---\n")

        results = {}
        for counter in PERF_COUNTERS:
            pattern = rf"([\d,]+)\s+cpu_core/{re.escape(counter)}/"
            match = re.search(pattern, perf_output)
            if match:
                results[counter] = match.group(1).replace(",", "")
            else:
                # Fallback: try without cpu_core prefix (older perf versions)
                pattern_fallback = rf"([\d,]+)\s+{re.escape(counter)}"
                match_fallback = re.search(pattern_fallback, perf_output)
                if match_fallback:
                    results[counter] = match_fallback.group(1).replace(",", "")
                else:
                    results[counter] = "N/A"

        return results

    except subprocess.TimeoutExpired:
        print(f"ERROR: Process timed out. The binary may be waiting for input.")
        print(f"Try running with --debug to inspect the input sequence being sent.")
        return None
    except subprocess.CalledProcessError as e:
        print(f"Error executing perf: {e}")
        print(f"stderr: {e.stderr}")
        return None
    except FileNotFoundError:
        print("Error: 'perf' not found. Make sure it is installed and in your PATH.")
        return None


def main():
    parser = argparse.ArgumentParser(description="Matrix Multiplication Perf Benchmarker")
    parser.add_argument("binary", help="Path to the binary (C++ or Go)")
    parser.add_argument("size", type=int, help="Matrix size (N x N)")
    parser.add_argument(
        "option",
        type=int,
        choices=[1, 2, 3],
        help="Algorithm: 1-Normal, 2-Line, 3-Block",
    )
    parser.add_argument("runs", type=int, help="Number of experiments to run")
    parser.add_argument("--block_size", type=int, help="Block size (for option 3)")
    parser.add_argument("--output", default="perf_results.csv", help="Output CSV file")
    parser.add_argument("--no_submenu", action="store_true",
                        help="Use for Go binary (no version sub-menu for options 1 and 2)")
    parser.add_argument("--debug", action="store_true",
                        help="Print raw perf stderr and input sequence for debugging")

    args = parser.parse_args()

    if args.option == 3 and args.block_size is None:
        parser.error("--block_size is required when option is 3")

    file_exists = os.path.isfile(args.output)

    with open(args.output, "a", newline="") as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(
                ["Binary", "Algorithm", "Size", "BlockSize", "Run_Index"]
                + PERF_COUNTERS
            )

        print(
            f"Starting {args.runs} perf runs for {args.binary} "
            f"(Size: {args.size}, Option: {args.option})..."
        )

        for i in range(args.runs):
            results = run_perf_benchmark(
                args.binary, args.size, args.option, args.block_size,
                args.no_submenu, args.debug
            )

            if results:
                writer.writerow(
                    [
                        args.binary,
                        args.option,
                        args.size,
                        args.block_size if args.option == 3 else "N/A",
                        i + 1,
                    ]
                    + [results[c] for c in PERF_COUNTERS]
                )
                f.flush()
                print(f"[Run {i + 1}/{args.runs}] Counters collected:")
                for counter, value in results.items():
                    print(f"  {counter}: {value}")
            else:
                print(f"[Run {i + 1}/{args.runs}] Failed to collect perf data.")

    print(f"\nDone. Results appended to {args.output}")


if __name__ == "__main__":
    main()
    