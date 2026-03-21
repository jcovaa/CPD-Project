import subprocess
import re
import csv
import argparse
import os


def run_parallel_benchmark(binary_path, size, option, sub_option, threads):
    # Sequence: 1. Main Op -> 2. Size -> 3. Sub-alg -> 4. Threads -> 5. Exit (0)
    input_sequence = [
        str(option),  # 1-Normal or 2-Line
        str(size),  # Matrix dimension (N x N)
        str(sub_option),  # 2 (Parallel 1) or 3 (Parallel 2)
        str(threads),  # Number of OpenMP threads
        "0",  # Exit the menu in the C++ program
    ]

    input_data = "\n".join(input_sequence) + "\n"

    try:
        process = subprocess.run(
            [binary_path], input=input_data, capture_output=True, text=True, check=True
        )

        # Search for the time output in the program's stdout
        match = re.search(r"Time:\s+([\d.]+)\s+seconds", process.stdout)
        if match:
            time_val = float(match.group(1))
            # GFlops = (2 * N^3) / (Time * 10^9)
            gflops = (2 * (size**3)) / (time_val * 1e9) if time_val > 0 else 0
            return time_val, gflops
        return None, None

    except subprocess.CalledProcessError as e:
        print(f"\nError executing {binary_path}: {e}")
        return None, None


def main():
    parser = argparse.ArgumentParser(
        description="Parallel Matrix Benchmarker with Multiple Runs"
    )
    parser.add_argument("binary", help="Path to the compiled C++ binary")
    parser.add_argument("--size", type=int, required=True, help="Matrix size (N)")
    parser.add_argument(
        "--op", type=int, choices=[1, 2], required=True, help="1-Normal, 2-Line"
    )
    parser.add_argument(
        "--sub",
        type=int,
        choices=[2, 3, 4, 5],
        required=True,
        help="2-Parallel 1, 3-Parallel 2",
    )
    parser.add_argument(
        "--threads",
        type=int,
        nargs="+",
        required=True,
        help="List of thread counts (e.g., 4 8 12 16)",
    )
    parser.add_argument(
        "--runs", type=int, default=1, help="Number of experiments per configuration"
    )
    parser.add_argument(
        "--output", default="results_parallel.csv", help="Output CSV file path"
    )

    args = parser.parse_args()
    file_exists = os.path.isfile(args.output)

    with open(args.output, "a", newline="") as f:
        writer = csv.writer(f)
        if not file_exists:
            # Added Run_Index to the header for better data analysis
            writer.writerow(
                [
                    "Algorithm",
                    "Size",
                    "SubOption",
                    "Threads",
                    "Run_Index",
                    "Time_Sec",
                    "GFlops",
                ]
            )

        print(
            f"Starting benchmark: Size={args.size}, Op={args.op}, Sub={args.sub}, Runs={args.runs}"
        )

        for t in args.threads:
            print(f"\nTesting {t} threads:")
            for i in range(args.runs):
                print(f"  [Run {i + 1}/{args.runs}]...", end=" ", flush=True)

                t_sec, gflops = run_parallel_benchmark(
                    args.binary, args.size, args.op, args.sub, t
                )

                if t_sec is not None:
                    writer.writerow(
                        [args.op, args.size, args.sub, t, i + 1, t_sec, gflops]
                    )
                    f.flush()
                    print(f"Time: {t_sec:.3f}s | GFlops: {gflops:.2f}")
                else:
                    print("FAILED to extract data.")

    print(f"\nDone. Results saved to {args.output}")


if __name__ == "__main__":
    main()
