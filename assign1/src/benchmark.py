import subprocess
import re
import csv
import argparse
import os


def run_matrix_benchmark(binary_path, size, option, block_size=None):
    input_sequence = [str(option), str(size)]
    if option == 3:
        input_sequence.append(str(block_size))
    input_sequence.append("0")

    input_data = "\n".join(input_sequence) + "\n"

    try:
        process = subprocess.run(
            [binary_path], input=input_data, capture_output=True, text=True, check=True
        )

        match = re.search(r"Time:\s+([\d.]+)\s+seconds", process.stdout)
        return match.group(1) if match else None

    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(f"Error executing {binary_path}: {e}")
        return None


def main():
    parser = argparse.ArgumentParser(description="Matrix Multiplication Benchmarker")
    parser.add_argument("binary", help="Path to the binary")
    parser.add_argument("size", type=int, help="Matrix size (N x N)")
    parser.add_argument(
        "option",
        type=int,
        choices=[1, 2, 3],
        help="Algorithm: 1-Normal, 2-Line, 3-Block",
    )
    parser.add_argument("runs", type=int, help="Number of experiments to run")
    parser.add_argument("--block_size", type=int, help="Block size (for option 3)")
    parser.add_argument("--output", default="results.csv", help="Output CSV file")

    args = parser.parse_args()

    file_exists = os.path.isfile(args.output)

    with open(args.output, "a", newline="") as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(
                [
                    "Binary",
                    "Algorithm",
                    "Size",
                    "BlockSize",
                    "Run_Index",
                    "Time_Seconds",
                ]
            )

        print(
            f"Starting {args.runs} runs for {args.binary} (Size: {args.size}, Option: {args.option})..."
        )

        for i in range(args.runs):
            elapsed_time = run_matrix_benchmark(
                args.binary, args.size, args.option, args.block_size
            )

            if elapsed_time:
                writer.writerow(
                    [
                        args.binary,
                        args.option,
                        args.size,
                        args.block_size if args.option == 3 else "N/A",
                        i + 1,
                        elapsed_time,
                    ]
                )
                f.flush()
                print(f"[Run {i + 1}/{args.runs}] Time: {elapsed_time}s")
            else:
                print(f"[Run {i + 1}/{args.runs}] Failed to extract time.")

    print(f"\nDone. Results appended to {args.output}")


if __name__ == "__main__":
    main()