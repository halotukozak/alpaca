#!/usr/bin/env python3
"""
Benchmark visualization script for Alpaca parser benchmarks.

Generates comparison charts from JMH JSON benchmark results.
Reads alpaca_results.json, fastparse_results.json, and sly_results.json
from benchmarks/outputs/ and produces PNG charts in benchmarks/outputs/charts/.

Usage: python3 benchmarks/plot_results.py
"""

import json
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

LIBRARIES = ["alpaca", "fastparse", "sly"]
SCENARIOS = [
    "iterative_math", "recursive_math",
    "iterative_json", "recursive_json",
    "big_grammar",
]
METHODS = ["lex", "parse", "fullParse"]

# Scenarios grouped by style and input type for comparison charts
STYLE_INPUT_COMBOS = [
    ("iterative", "math", "iterative_math"),
    ("recursive", "math", "recursive_math"),
    ("iterative", "json", "iterative_json"),
    ("recursive", "json", "recursive_json"),
]

LIBRARY_STYLES = {
    "alpaca":    {"color": "#2196F3", "marker": "o"},
    "fastparse": {"color": "#4CAF50", "marker": "s"},
    "sly":       {"color": "#FF9800", "marker": "^"},
}

STYLE_LINES = {
    "iterative": "-",
    "recursive": "--",
}


# --- JMH JSON Loading ---

def load_jmh_json(library):
    """Load a JMH JSON result file for a library. Returns list of entries or None."""
    filepath = OUTPUTS_DIR / f"{library}_results.json"
    if not filepath.exists():
        return None
    try:
        with filepath.open("r") as f:
            return json.load(f)
    except (json.JSONDecodeError, IOError) as e:
        print(f"Warning: failed to load {filepath}: {e}")
        return None


def extract_library_name(benchmark_field):
    """Extract library name from JMH benchmark field.

    Examples:
      'AlpacaBenchmark.fullParse' -> 'alpaca'
      'FastparseBenchmark.fullParse' -> 'fastparse'
      'sly.MathParser.fullParse' -> 'sly'
    """
    lower = benchmark_field.lower()
    if "alpaca" in lower:
        return "alpaca"
    elif "fastparse" in lower:
        return "fastparse"
    elif "sly" in lower:
        return "sly"
    return None


def extract_method(benchmark_field):
    """Extract method name from JMH benchmark field.

    Examples:
      'AlpacaBenchmark.fullParse' -> 'fullParse'
      'sly.MathParser.lex' -> 'lex'
    """
    return benchmark_field.rsplit(".", 1)[-1]


def load_all():
    """Load all results and organize into nested structure.

    Returns: data[library][scenario][method] = [(size, score, error), ...] sorted by size
    """
    data = {}
    for lib in LIBRARIES:
        data[lib] = {}

    for lib in LIBRARIES:
        entries = load_jmh_json(lib)
        if entries is None:
            continue

        for entry in entries:
            benchmark = entry.get("benchmark", "")
            lib_name = extract_library_name(benchmark)
            if lib_name is None or lib_name != lib:
                # Try to assign by source file if extraction fails
                lib_name = lib

            method = extract_method(benchmark)
            params = entry.get("params", {})
            scenario = params.get("scenario", "")
            try:
                size = int(params.get("size", "0"))
            except (ValueError, TypeError):
                continue

            primary = entry.get("primaryMetric", {})
            score = primary.get("score")
            error = primary.get("scoreError", 0.0)

            # Skip entries with no valid score (NaN or None)
            if score is None:
                continue
            try:
                if score != score:  # NaN check
                    continue
            except (TypeError, ValueError):
                continue

            if scenario not in data[lib]:
                data[lib][scenario] = {}
            if method not in data[lib][scenario]:
                data[lib][scenario][method] = []
            data[lib][scenario][method].append((size, score, error))

    # Sort each series by size
    for lib in data:
        for scenario in data[lib]:
            for method in data[lib][scenario]:
                data[lib][scenario][method].sort(key=lambda x: x[0])

    return data


# --- Extraction helpers ---

def extract_series(data, lib, scenario, method):
    """Extract (sizes, scores, errors) for a specific combination."""
    try:
        series = data[lib][scenario][method]
        sizes = [s for s, _, _ in series]
        scores = [sc for _, sc, _ in series]
        errors = [e for _, _, e in series]
        return sizes, scores, errors
    except KeyError:
        return [], [], []


def soe_boundary(data, lib, scenario, method):
    """Find last valid point before a gap (StackOverflowError / RecursionError).

    Returns (size, score) or None.
    """
    sizes, scores, _ = extract_series(data, lib, scenario, method)
    if not sizes:
        return None

    # Check if the series ends before the maximum expected size
    expected_sizes = [100, 500, 1000, 2000, 5000, 10000]
    if len(sizes) < len(expected_sizes) and sizes:
        # Series ends early -- last point is the SOE boundary
        return (sizes[-1], scores[-1])

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
    """Format time values in ms for y-axis labels."""
    if value >= 1000:
        return f"{value / 1000:.2g} s"
    if value >= 1:
        return f"{value:.2g} ms"
    if value >= 0.001:
        return f"{value * 1000:.2g} us"
    return f"{value * 1e6:.2g} ns"


# --- Plotting ---

def _plot_series(ax, data, lib, scenario, method, label, color, marker, linestyle="-"):
    """Plot a single series with error bars and SOE marker."""
    sizes, scores, errors = extract_series(data, lib, scenario, method)
    if not sizes:
        return

    ax.errorbar(sizes, scores, yerr=errors, label=label, color=color, marker=marker,
                linestyle=linestyle, linewidth=2.5, markersize=8, capsize=3, capthick=1.5)

    soe = soe_boundary(data, lib, scenario, method)
    if soe:
        ax.plot(soe[0], soe[1], marker="x", color="red",
                markersize=12, markeredgewidth=3, zorder=10)
        ax.annotate("SOE", xy=soe, xytext=(10, 10),
                     textcoords="offset points", fontsize=8,
                     color="red", fontweight="bold")


def _finish(ax, title, filename):
    """Finalize a chart with labels, legend, and save to file."""
    ax.set_xlabel("Input size")
    ax.set_ylabel("Time (ms/op)")
    ax.set_title(title)
    ax.set_yscale("log")
    ax.yaxis.set_major_formatter(FuncFormatter(time_formatter))
    handles, labels = ax.get_legend_handles_labels()
    if handles:
        ax.legend()
    ax.grid(True, alpha=0.3)
    ax.get_figure().tight_layout()
    ax.get_figure().savefig(CHARTS_DIR / filename)
    plt.close(ax.get_figure())
    print(f"  {filename}")


def plot_method_comparison(data, method, method_label, libraries, scenario, chart_prefix):
    """Compare libraries on a single method for a given scenario."""
    fig, ax = plt.subplots(figsize=(10, 6))
    for lib in libraries:
        s = LIBRARY_STYLES[lib]
        _plot_series(ax, data, lib, scenario, method,
                     lib.capitalize(), s["color"], s["marker"])
    _finish(ax, f"{method_label} -- {scenario.replace('_', ' ').title()}",
            f"{chart_prefix}_{scenario}.png")


def plot_iter_vs_rec(data, lib, method, method_label, input_type):
    """Compare iterative vs recursive for one library + method."""
    fig, ax = plt.subplots(figsize=(10, 6))
    color = LIBRARY_STYLES[lib]["color"]
    marker = LIBRARY_STYLES[lib]["marker"]

    for style, linestyle in STYLE_LINES.items():
        scenario = f"{style}_{input_type}"
        _plot_series(ax, data, lib, scenario, method,
                     style.capitalize(), color, marker, linestyle)

    _finish(ax,
            f"{lib.capitalize()} -- {method_label} -- {input_type.upper()} (Iterative vs Recursive)",
            f"iter_vs_rec_{lib}_{method_label.lower().replace(' ', '_')}_{input_type}.png")


# --- Main ---

def main():
    setup_matplotlib()
    CHARTS_DIR.mkdir(parents=True, exist_ok=True)
    data = load_all()

    chart_count = 0

    # --- 3-library Full Parse comparisons (most important, prefixed with 00_) ---
    print("Full Parse Time comparison (all libraries):")
    for style, input_type, scenario in STYLE_INPUT_COMBOS:
        fig, ax = plt.subplots(figsize=(10, 6))
        for lib in LIBRARIES:
            s = LIBRARY_STYLES[lib]
            _plot_series(ax, data, lib, scenario, "fullParse",
                         lib.capitalize(), s["color"], s["marker"])
        _finish(ax, f"Full Parse Time -- {style.capitalize()} {input_type.upper()}",
                f"00_full_parse_{style}_{input_type}.png")
        chart_count += 1

    # Big-grammar full parse (also important, 00_ prefix)
    fig, ax = plt.subplots(figsize=(10, 6))
    for lib in LIBRARIES:
        s = LIBRARY_STYLES[lib]
        _plot_series(ax, data, lib, "big_grammar", "fullParse",
                     lib.capitalize(), s["color"], s["marker"])
    _finish(ax, "Full Parse Time -- Big Grammar",
            "00_full_parse_big_grammar.png")
    chart_count += 1

    # --- Lex Time comparison (Alpaca + SLY) ---
    print("Lex Time comparison (Alpaca + SLY):")
    lex_libs = ["alpaca", "sly"]
    for style, input_type, scenario in STYLE_INPUT_COMBOS:
        plot_method_comparison(data, "lex", "Lex Time", lex_libs, scenario, "lex_time")
        chart_count += 1

    # Big-grammar lex time
    plot_method_comparison(data, "lex", "Lex Time", lex_libs, "big_grammar", "lex_time")
    chart_count += 1

    # --- Parse Time comparison (Alpaca + SLY) ---
    print("Parse Time comparison (Alpaca + SLY):")
    parse_libs = ["alpaca", "sly"]
    for style, input_type, scenario in STYLE_INPUT_COMBOS:
        plot_method_comparison(data, "parse", "Parse Time", parse_libs, scenario, "parse_time")
        chart_count += 1

    # Big-grammar parse time
    plot_method_comparison(data, "parse", "Parse Time", parse_libs, "big_grammar", "parse_time")
    chart_count += 1

    # --- Iterative vs Recursive per library ---
    print("Iterative vs Recursive:")
    metrics = [("fullParse", "Full Parse"), ("lex", "Lex Time"), ("parse", "Parse Time")]
    for lib in ["alpaca", "sly"]:
        for method, label in metrics:
            for input_type in ["math", "json"]:
                plot_iter_vs_rec(data, lib, method, label, input_type)
                chart_count += 1

    # Fastparse only has fullParse
    for input_type in ["math", "json"]:
        plot_iter_vs_rec(data, "fastparse", "fullParse", "Full Parse", input_type)
        chart_count += 1

    actual_count = len(list(CHARTS_DIR.glob("*.png")))
    print(f"\nDone! {actual_count} charts saved to {CHARTS_DIR}")


if __name__ == "__main__":
    main()
