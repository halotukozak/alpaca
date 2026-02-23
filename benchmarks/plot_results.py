#!/usr/bin/env python3
"""
Benchmark visualization script for Alpaca parser benchmarks.

Generates comparison charts from CSV benchmark results.
Usage: python3 benchmarks/plot_results.py
"""

import csv
import re
import sys
from pathlib import Path

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.ticker import FuncFormatter
except ImportError:
    print("Error: matplotlib is required. Install with: pip install matplotlib")
    sys.exit(1)

OUTPUTS_DIR = Path(__file__).parent / "outputs"
CHARTS_DIR = OUTPUTS_DIR / "charts"

PARSERS = ["alpaca", "fastparse", "sly"]
STYLES = ["iterative", "recursive"]
INPUT_TYPES = ["math", "json"]

PARSER_STYLES = {
    "alpaca":    {"color": "#2196F3", "marker": "o"},
    "fastparse": {"color": "#4CAF50", "marker": "s"},
    "sly":       {"color": "#FF9800", "marker": "^"},
}

STYLE_LINES = {
    "iterative": "-",
    "recursive": "--",
}


# --- Time parsing ---

def parse_time(time_str: str) -> float | None:
    """Parse human-readable time string to seconds. Returns None for errors."""
    if not time_str or "StackOverflowError" in time_str:
        return None
    match = re.match(r"^([\d.]+)\s*(µs|us|ms|s)$", time_str.strip())
    if not match:
        return None
    value = float(match.group(1))
    unit = match.group(2)
    return value * {"µs": 1e-6, "us": 1e-6, "ms": 1e-3, "s": 1.0}[unit]


# --- CSV loading ---

def load_csv(filename: str) -> dict | None:
    """Load a benchmark CSV. Returns {column_name: [(size, seconds|None)]}."""
    filepath = OUTPUTS_DIR / filename
    if not filepath.exists():
        return None

    with filepath.open("r") as f:
        reader = csv.reader(f)
        header = next(reader)
        rows = [row for row in reader if len(row) >= 2]

    time_cols = {
        col.strip(): i
        for i, col in enumerate(header)
        if "Time" in col
    }

    result = {}
    for col_name in time_cols:
        result[col_name] = []

    for row in rows:
        size = int(row[0].strip())
        for col_name, col_idx in time_cols.items():
            val = parse_time(row[col_idx]) if col_idx < len(row) else None
            result[col_name].append((size, val))

    return result


def load_all() -> dict:
    """Returns data[parser][style][input_type] = csv_data."""
    data = {}
    for parser in PARSERS:
        data[parser] = {}
        for style in STYLES:
            data[parser][style] = {}
            for input_type in INPUT_TYPES:
                data[parser][style][input_type] = load_csv(
                    f"{parser}_{style}_{input_type}.csv"
                )
    return data


# --- Extraction helpers ---

def extract(csv_data: dict | None, col: str) -> tuple[list, list]:
    """Extract (sizes, values) filtering out None entries."""
    if not csv_data or col not in csv_data:
        return [], []
    pairs = csv_data[col]
    sizes = [s for s, v in pairs if v is not None]
    values = [v for _, v in pairs if v is not None]
    return sizes, values


def soe_boundary(csv_data: dict | None, col: str):
    """Find last valid point before StackOverflowError. Returns (size, value) or None."""
    if not csv_data or col not in csv_data:
        return None
    pairs = csv_data[col]
    for i, (size, value) in enumerate(pairs):
        if value is None and i > 0 and pairs[i - 1][1] is not None:
            return pairs[i - 1]
    return None


# --- Matplotlib setup ---

def setup_matplotlib():
    for style_name in ["seaborn-v0_8-whitegrid", "seaborn-whitegrid"]:
        try:
            plt.style.use(style_name)
            break
        except OSError:
            continue

    plt.rcParams.update({
        "font.size": 12,
        "axes.titlesize": 14,
        "axes.labelsize": 12,
        "legend.fontsize": 11,
        "figure.dpi": 100,
        "savefig.dpi": 150,
        "savefig.bbox": "tight",
    })


def time_formatter(value, _pos):
    if value >= 1:
        return f"{value:.2g} s"
    if value >= 1e-3:
        return f"{value * 1e3:.2g} ms"
    if value >= 1e-6:
        return f"{value * 1e6:.2g} \u00b5s"
    return f"{value * 1e9:.2g} ns"


# --- Plotting ---

def _plot_series(ax, csv_data, col, label, color, marker, linestyle="-"):
    sizes, values = extract(csv_data, col)
    if not sizes:
        return
    ax.plot(sizes, values, label=label, color=color, marker=marker,
            linestyle=linestyle, linewidth=2.5, markersize=8)

    soe = soe_boundary(csv_data, col)
    if soe:
        ax.plot(soe[0], soe[1], marker="x", color="red",
                markersize=12, markeredgewidth=3, zorder=10)
        ax.annotate("SOE", xy=soe, xytext=(10, 10),
                     textcoords="offset points", fontsize=8,
                     color="red", fontweight="bold")


def _finish(ax, title, filename):
    ax.set_xlabel("Input size")
    ax.set_ylabel("Time")
    ax.set_title(title)
    ax.set_yscale("log")
    ax.yaxis.set_major_formatter(FuncFormatter(time_formatter))
    ax.legend()
    ax.grid(True, alpha=0.3)
    ax.get_figure().tight_layout()
    ax.get_figure().savefig(CHARTS_DIR / filename)
    plt.close(ax.get_figure())
    print(f"  {filename}")


def plot_comparison(data, col, parsers, style, input_type, prefix):
    """Compare parsers on a single metric."""
    fig, ax = plt.subplots(figsize=(10, 6))
    for parser in parsers:
        s = PARSER_STYLES[parser]
        _plot_series(ax, data[parser][style][input_type], col,
                     parser.capitalize(), s["color"], s["marker"])
    _finish(ax, f"{col} \u2014 {style.capitalize()} {input_type.upper()}",
            f"{prefix}_{style}_{input_type}.png")


def plot_iter_vs_rec(data, parser, col, metric_label, input_type):
    """Compare iterative vs recursive for one parser+metric."""
    fig, ax = plt.subplots(figsize=(10, 6))
    color = PARSER_STYLES[parser]["color"]
    marker = PARSER_STYLES[parser]["marker"]
    for style in STYLES:
        _plot_series(ax, data[parser][style][input_type], col,
                     style.capitalize(), color, marker, STYLE_LINES[style])
    _finish(ax,
            f"{parser.capitalize()} \u2014 {col} \u2014 {input_type.upper()} (Iterative vs Recursive)",
            f"iter_vs_rec_{parser}_{metric_label}_{input_type}.png")


# --- Main ---

def main():
    setup_matplotlib()
    CHARTS_DIR.mkdir(parents=True, exist_ok=True)
    data = load_all()

    print("Full Parse Time comparison:")
    for style in STYLES:
        for input_type in INPUT_TYPES:
            plot_comparison(data, "Full Parse Time", PARSERS, style, input_type, "full_parse")

    print("Lex Time comparison:")
    for style in STYLES:
        for input_type in INPUT_TYPES:
            plot_comparison(data, "Lex Time", ["alpaca", "sly"], style, input_type, "lex_time")

    print("Parse Time comparison:")
    for style in STYLES:
        for input_type in INPUT_TYPES:
            plot_comparison(data, "Parse Time", ["alpaca", "sly"], style, input_type, "parse_time")

    print("Iterative vs Recursive:")
    metrics = [("Full Parse Time", "full_parse"), ("Lex Time", "lex_time"), ("Parse Time", "parse_time")]
    for parser in ["alpaca", "sly"]:
        for col, label in metrics:
            for input_type in INPUT_TYPES:
                plot_iter_vs_rec(data, parser, col, label, input_type)
    for input_type in INPUT_TYPES:
        plot_iter_vs_rec(data, "fastparse", "Full Parse Time", "full_parse", input_type)

    count = len(list(CHARTS_DIR.glob("*.png")))
    print(f"\nDone! {count} charts saved to {CHARTS_DIR}")


if __name__ == "__main__":
    main()
