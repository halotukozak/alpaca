#!/usr/bin/env python3
"""
Before/After comparison chart generator for Alpaca lexer optimization benchmarks.

Overlays Phase 15 baseline results (baselines/alpaca_results.json) with
post-optimization results (outputs/alpaca_results.json) to visualize
performance improvements from OffsetCharSequence, Matcher reuse, ListBuffer,
and LazyReader offset tracking optimizations.

Usage: python3 benchmarks/plot_comparison.py
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

SCRIPT_DIR = Path(__file__).parent
BASELINES_DIR = SCRIPT_DIR / "baselines"
OUTPUTS_DIR = SCRIPT_DIR / "outputs"
CHARTS_DIR = OUTPUTS_DIR / "charts"

SCENARIOS = [
    "iterative_math", "recursive_math",
    "iterative_json", "recursive_json",
    "big_grammar",
]
METHODS = ["lex", "parseOnly", "fullParse"]
METHOD_LABELS = {"lex": "Lex", "parseOnly": "Parse Only", "fullParse": "Full Parse"}

BASELINE_STYLE = {"color": "#E53935", "marker": "s", "linestyle": "--", "label_suffix": "(baseline)"}
OPTIMIZED_STYLE = {"color": "#1E88E5", "marker": "o", "linestyle": "-", "label_suffix": "(optimized)"}


def load_jmh_json(filepath):
    """Load a JMH JSON result file. Returns list of entries or None."""
    if not filepath.exists():
        print(f"Warning: {filepath} not found")
        return None
    try:
        with filepath.open("r") as f:
            return json.load(f)
    except (json.JSONDecodeError, IOError) as e:
        print(f"Warning: failed to load {filepath}: {e}")
        return None


def organize_results(entries):
    """Organize JMH entries into data[scenario][method] = [(size, score, error), ...]"""
    data = {}
    if entries is None:
        return data

    for entry in entries:
        benchmark = entry.get("benchmark", "")
        method = benchmark.rsplit(".", 1)[-1]
        params = entry.get("params", {})
        scenario = params.get("scenario", "")
        try:
            size = int(params.get("size", "0"))
        except (ValueError, TypeError):
            continue

        primary = entry.get("primaryMetric", {})
        score = primary.get("score")
        error = primary.get("scoreError", 0.0)

        if score is None:
            continue
        try:
            if score != score:  # NaN check
                continue
        except (TypeError, ValueError):
            continue

        if scenario not in data:
            data[scenario] = {}
        if method not in data[scenario]:
            data[scenario][method] = []
        data[scenario][method].append((size, score, error))

    # Sort each series by size
    for scenario in data:
        for method in data[scenario]:
            data[scenario][method].sort(key=lambda x: x[0])

    return data


def extract_series(data, scenario, method):
    """Extract (sizes, scores, errors) for a specific combination."""
    try:
        series = data[scenario][method]
        sizes = [s for s, _, _ in series]
        scores = [sc for _, sc, _ in series]
        errors = [e for _, _, e in series]
        return sizes, scores, errors
    except KeyError:
        return [], [], []


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


def plot_comparison(baseline_data, optimized_data, scenario, method):
    """Plot before/after comparison for a single scenario + method."""
    b_sizes, b_scores, b_errors = extract_series(baseline_data, scenario, method)
    o_sizes, o_scores, o_errors = extract_series(optimized_data, scenario, method)

    if not b_sizes and not o_sizes:
        return False

    fig, ax = plt.subplots(figsize=(10, 6))

    if b_sizes:
        ax.errorbar(b_sizes, b_scores, yerr=b_errors,
                     label=f"Baseline {BASELINE_STYLE['label_suffix']}",
                     color=BASELINE_STYLE["color"],
                     marker=BASELINE_STYLE["marker"],
                     linestyle=BASELINE_STYLE["linestyle"],
                     linewidth=2.5, markersize=8, capsize=3, capthick=1.5)

    if o_sizes:
        ax.errorbar(o_sizes, o_scores, yerr=o_errors,
                     label=f"Optimized {OPTIMIZED_STYLE['label_suffix']}",
                     color=OPTIMIZED_STYLE["color"],
                     marker=OPTIMIZED_STYLE["marker"],
                     linestyle=OPTIMIZED_STYLE["linestyle"],
                     linewidth=2.5, markersize=8, capsize=3, capthick=1.5)

    method_label = METHOD_LABELS.get(method, method)
    scenario_label = scenario.replace("_", " ").title()
    ax.set_xlabel("Input size (tokens)")
    ax.set_ylabel("Time (ms/op)")
    ax.set_title(f"{method_label} -- {scenario_label} -- Baseline vs Optimized")
    ax.set_yscale("log")
    ax.yaxis.set_major_formatter(FuncFormatter(time_formatter))
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()

    filename = f"cmp_{method}_{scenario}.png"
    fig.savefig(CHARTS_DIR / filename)
    plt.close(fig)
    print(f"  {filename}")
    return True


def plot_speedup_table(baseline_data, optimized_data):
    """Generate a summary table chart showing speedup ratios."""
    rows = []
    for scenario in SCENARIOS:
        for method in METHODS:
            b_sizes, b_scores, _ = extract_series(baseline_data, scenario, method)
            o_sizes, o_scores, _ = extract_series(optimized_data, scenario, method)
            if not b_sizes or not o_sizes:
                continue

            # Match on common sizes
            b_map = dict(zip(b_sizes, b_scores))
            o_map = dict(zip(o_sizes, o_scores))
            common = sorted(set(b_map.keys()) & set(o_map.keys()))

            for size in common:
                speedup = b_map[size] / o_map[size] if o_map[size] > 0 else float('inf')
                rows.append((scenario, method, size, b_map[size], o_map[size], speedup))

    if not rows:
        print("  No common data points for speedup table")
        return

    # Print text summary
    print("\n  Speedup Summary (baseline_time / optimized_time):")
    print(f"  {'Scenario':<20} {'Method':<12} {'Size':>6} {'Baseline':>12} {'Optimized':>12} {'Speedup':>10}")
    print("  " + "-" * 74)
    for scenario, method, size, b_score, o_score, speedup in rows:
        direction = "FASTER" if speedup > 1.0 else "SLOWER" if speedup < 1.0 else "SAME"
        print(f"  {scenario:<20} {method:<12} {size:>6} {b_score:>12.4f} {o_score:>12.4f} {speedup:>8.2f}x {direction}")

    # Also create a summary chart grouped by method
    fig, axes = plt.subplots(1, len(METHODS), figsize=(6 * len(METHODS), 8), sharey=True)
    if len(METHODS) == 1:
        axes = [axes]

    for ax, method in zip(axes, METHODS):
        method_rows = [(s, sz, sp) for s, m, sz, _, _, sp in rows if m == method]
        if not method_rows:
            ax.set_visible(False)
            continue

        scenarios_in_method = sorted(set(s for s, _, _ in method_rows))
        sizes_in_method = sorted(set(sz for _, sz, _ in method_rows))
        bar_width = 0.8 / len(sizes_in_method) if sizes_in_method else 0.8

        for i, size in enumerate(sizes_in_method):
            speedups = []
            labels = []
            for scenario in scenarios_in_method:
                matching = [sp for s, sz, sp in method_rows if s == scenario and sz == size]
                if matching:
                    speedups.append(matching[0])
                    labels.append(scenario.replace("_", "\n"))
                else:
                    speedups.append(0)
                    labels.append(scenario.replace("_", "\n"))

            x_positions = range(len(labels))
            offset = (i - len(sizes_in_method) / 2 + 0.5) * bar_width
            bars = ax.bar([x + offset for x in x_positions], speedups,
                         bar_width, label=f"size={size}", alpha=0.8)

            # Color bars: green if faster, red if slower
            for bar, sp in zip(bars, speedups):
                bar.set_color("#4CAF50" if sp >= 1.0 else "#E53935")
                bar.set_alpha(0.7 + 0.3 * (i / max(len(sizes_in_method) - 1, 1)))

        ax.axhline(y=1.0, color="black", linestyle="--", linewidth=1, alpha=0.5)
        ax.set_xlabel("Scenario")
        ax.set_ylabel("Speedup (x)" if ax == axes[0] else "")
        ax.set_title(METHOD_LABELS.get(method, method))
        ax.set_xticks(range(len(scenarios_in_method)))
        ax.set_xticklabels([s.replace("_", "\n") for s in scenarios_in_method], fontsize=9)
        ax.legend(fontsize=8)
        ax.grid(True, alpha=0.3, axis="y")

    fig.suptitle("Speedup: Baseline vs Optimized (>1.0 = faster)", fontsize=16, fontweight="bold")
    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "cmp_speedup_summary.png")
    plt.close(fig)
    print("  cmp_speedup_summary.png")


def plot_scaling_comparison(baseline_data, optimized_data):
    """Plot scaling behavior comparison -- shows whether O(n^2) became O(n)."""
    fig, axes = plt.subplots(2, 3, figsize=(18, 12))

    scenarios_to_plot = ["iterative_json", "recursive_json", "iterative_math",
                         "recursive_math", "big_grammar"]

    for idx, scenario in enumerate(scenarios_to_plot):
        row = idx // 3
        col = idx % 3
        ax = axes[row][col]

        b_sizes, b_scores, _ = extract_series(baseline_data, scenario, "lex")
        o_sizes, o_scores, _ = extract_series(optimized_data, scenario, "lex")

        if b_sizes:
            ax.plot(b_sizes, b_scores,
                    color=BASELINE_STYLE["color"], marker=BASELINE_STYLE["marker"],
                    linestyle=BASELINE_STYLE["linestyle"], linewidth=2, markersize=6,
                    label="Baseline")
        if o_sizes:
            ax.plot(o_sizes, o_scores,
                    color=OPTIMIZED_STYLE["color"], marker=OPTIMIZED_STYLE["marker"],
                    linestyle=OPTIMIZED_STYLE["linestyle"], linewidth=2, markersize=6,
                    label="Optimized")

        # Add O(n) reference line if we have data
        ref_sizes = o_sizes if o_sizes else b_sizes
        ref_scores = o_scores if o_scores else b_scores
        if len(ref_sizes) >= 2:
            # Normalize: O(n) line through the first data point
            base_size = ref_sizes[0]
            base_score = ref_scores[0]
            on_line = [base_score * (s / base_size) for s in ref_sizes]
            ax.plot(ref_sizes, on_line, color="gray", linestyle=":", linewidth=1.5,
                    alpha=0.5, label="O(n) reference")

        scenario_label = scenario.replace("_", " ").title()
        ax.set_title(f"Lex -- {scenario_label}")
        ax.set_xlabel("Input size")
        ax.set_ylabel("Time (ms/op)")
        ax.legend(fontsize=9)
        ax.grid(True, alpha=0.3)

    # Hide the 6th subplot
    axes[1][2].set_visible(False)

    fig.suptitle("Lex Scaling Behavior: Baseline vs Optimized (with O(n) reference)",
                 fontsize=16, fontweight="bold")
    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "cmp_scaling_lex.png")
    plt.close(fig)
    print("  cmp_scaling_lex.png")


def main():
    setup_matplotlib()
    CHARTS_DIR.mkdir(parents=True, exist_ok=True)

    # Load baseline and post-optimization results
    baseline_entries = load_jmh_json(BASELINES_DIR / "alpaca_results.json")
    optimized_entries = load_jmh_json(OUTPUTS_DIR / "alpaca_results.json")

    if baseline_entries is None:
        print("ERROR: No baseline results found at baselines/alpaca_results.json")
        sys.exit(1)
    if optimized_entries is None:
        print("ERROR: No optimized results found at outputs/alpaca_results.json")
        sys.exit(1)

    baseline_data = organize_results(baseline_entries)
    optimized_data = organize_results(optimized_entries)

    print(f"Loaded {len(baseline_entries)} baseline entries, {len(optimized_entries)} optimized entries")

    chart_count = 0

    # --- Per-scenario per-method comparison charts ---
    print("\nBefore/After comparison charts:")
    for scenario in SCENARIOS:
        for method in METHODS:
            if plot_comparison(baseline_data, optimized_data, scenario, method):
                chart_count += 1

    # --- Speedup summary ---
    print("\nSpeedup summary:")
    plot_speedup_table(baseline_data, optimized_data)
    chart_count += 1

    # --- Scaling behavior ---
    print("\nScaling behavior:")
    plot_scaling_comparison(baseline_data, optimized_data)
    chart_count += 1

    cmp_charts = len(list(CHARTS_DIR.glob("cmp_*.png")))
    print(f"\nDone! {cmp_charts} comparison charts saved to {CHARTS_DIR}")


if __name__ == "__main__":
    main()
