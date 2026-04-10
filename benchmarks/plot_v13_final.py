#!/usr/bin/env python3
"""
Final v1.3 milestone comparison charts: baseline vs post-lexer vs post-parser.

Reads three JMH result snapshots and generates overlay comparison charts
showing the full optimization journey across Phase 15-17.

Data sources:
  - Phase 15 baseline:    baselines/alpaca_results.json
  - Phase 16 post-lexer:  post-lexer/alpaca_results.json
  - Phase 17 post-parser: outputs/alpaca_results.json

Usage: python3 benchmarks/plot_v13_final.py
"""

import json
import sys
from pathlib import Path

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.ticker import FuncFormatter
    import numpy as np
except ImportError:
    print("Error: matplotlib and numpy are required. Install with: pip install matplotlib numpy")
    sys.exit(1)

SCRIPT_DIR = Path(__file__).parent
BASELINES_DIR = SCRIPT_DIR / "baselines"
POST_LEXER_DIR = SCRIPT_DIR / "post-lexer"
OUTPUTS_DIR = SCRIPT_DIR / "outputs"
CHARTS_DIR = OUTPUTS_DIR / "charts"

SCENARIOS = [
    "iterative_math", "recursive_math",
    "iterative_json", "recursive_json",
    "big_grammar",
]
METHODS = ["lex", "parseOnly", "fullParse"]
METHOD_LABELS = {"lex": "Lex", "parseOnly": "Parse Only", "fullParse": "Full Parse",
                 "pureParseOnly": "Pure Parse Only"}

BASELINE_STYLE = {"color": "#E53935", "marker": "s", "linestyle": "--", "label": "Phase 15 baseline"}
POST_LEXER_STYLE = {"color": "#1E88E5", "marker": "o", "linestyle": "-.", "label": "Phase 16 post-lexer"}
POST_PARSER_STYLE = {"color": "#43A047", "marker": "D", "linestyle": "-", "label": "Phase 17 post-parser"}


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


def plot_three_way(baseline_data, post_lexer_data, post_parser_data, scenario, method):
    """Plot three-phase comparison for a single scenario + method."""
    b_sizes, b_scores, b_errors = extract_series(baseline_data, scenario, method)
    l_sizes, l_scores, l_errors = extract_series(post_lexer_data, scenario, method)
    p_sizes, p_scores, p_errors = extract_series(post_parser_data, scenario, method)

    if not b_sizes and not l_sizes and not p_sizes:
        return False

    fig, ax = plt.subplots(figsize=(10, 6))

    if b_sizes:
        ax.errorbar(b_sizes, b_scores, yerr=b_errors,
                     label=BASELINE_STYLE["label"],
                     color=BASELINE_STYLE["color"],
                     marker=BASELINE_STYLE["marker"],
                     linestyle=BASELINE_STYLE["linestyle"],
                     linewidth=2.5, markersize=8, capsize=3, capthick=1.5)

    if l_sizes:
        ax.errorbar(l_sizes, l_scores, yerr=l_errors,
                     label=POST_LEXER_STYLE["label"],
                     color=POST_LEXER_STYLE["color"],
                     marker=POST_LEXER_STYLE["marker"],
                     linestyle=POST_LEXER_STYLE["linestyle"],
                     linewidth=2.5, markersize=8, capsize=3, capthick=1.5)

    if p_sizes:
        ax.errorbar(p_sizes, p_scores, yerr=p_errors,
                     label=POST_PARSER_STYLE["label"],
                     color=POST_PARSER_STYLE["color"],
                     marker=POST_PARSER_STYLE["marker"],
                     linestyle=POST_PARSER_STYLE["linestyle"],
                     linewidth=2.5, markersize=8, capsize=3, capthick=1.5)

    method_label = METHOD_LABELS.get(method, method)
    scenario_label = scenario.replace("_", " ").title()
    ax.set_xlabel("Input size (tokens)")
    ax.set_ylabel("Time (ms/op)")
    ax.set_title(f"{method_label} -- {scenario_label} -- v1.3 Optimization Progress")
    ax.set_yscale("log")
    ax.yaxis.set_major_formatter(FuncFormatter(time_formatter))
    ax.legend()
    ax.grid(True, alpha=0.3)
    fig.tight_layout()

    filename = f"v13_{method}_{scenario}.png"
    fig.savefig(CHARTS_DIR / filename)
    plt.close(fig)
    print(f"  {filename}")
    return True


def plot_speedup_summary(baseline_data, post_lexer_data, post_parser_data):
    """Generate speedup bar chart at size=2000 for each scenario+method."""
    rows = []
    for scenario in SCENARIOS:
        for method in METHODS:
            b_sizes, b_scores, _ = extract_series(baseline_data, scenario, method)
            l_sizes, l_scores, _ = extract_series(post_lexer_data, scenario, method)
            p_sizes, p_scores, _ = extract_series(post_parser_data, scenario, method)

            b_map = dict(zip(b_sizes, b_scores))
            l_map = dict(zip(l_sizes, l_scores))
            p_map = dict(zip(p_sizes, p_scores))

            if 2000 not in b_map:
                continue

            b_val = b_map[2000]
            l_val = l_map.get(2000)
            p_val = p_map.get(2000)

            lexer_speedup = b_val / l_val if l_val and l_val > 0 else None
            parser_speedup = b_val / p_val if p_val and p_val > 0 else None

            rows.append((scenario, method, lexer_speedup, parser_speedup))

    if not rows:
        print("  No data for speedup summary")
        return

    # Create grouped bar chart
    labels = [f"{s.replace('_', ' ')}\n{METHOD_LABELS.get(m, m)}" for s, m, _, _ in rows]
    lexer_speedups = [r[2] if r[2] else 0 for r in rows]
    parser_speedups = [r[3] if r[3] else 0 for r in rows]

    x = np.arange(len(labels))
    width = 0.35

    fig, ax = plt.subplots(figsize=(16, 8))

    bars1 = ax.bar(x - width/2, lexer_speedups, width, label="Baseline -> Post-Lexer",
                    alpha=0.8, edgecolor="white", linewidth=0.5)
    bars2 = ax.bar(x + width/2, parser_speedups, width, label="Baseline -> Post-Parser",
                    alpha=0.8, edgecolor="white", linewidth=0.5)

    # Color: green for improvement (>1), red for regression (<1)
    for bar, sp in zip(bars1, lexer_speedups):
        bar.set_color("#4CAF50" if sp >= 1.0 else "#E53935")
    for bar, sp in zip(bars2, parser_speedups):
        bar.set_color("#2E7D32" if sp >= 1.0 else "#C62828")

    ax.axhline(y=1.0, color="black", linestyle="--", linewidth=1, alpha=0.5, label="No change")
    ax.set_xlabel("Scenario / Method")
    ax.set_ylabel("Speedup (x)")
    ax.set_title("v1.3 Speedup Summary at size=2000 (higher = faster)")
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=8, ha="center")
    ax.legend()
    ax.grid(True, alpha=0.3, axis="y")
    fig.tight_layout()

    fig.savefig(CHARTS_DIR / "v13_speedup_summary.png")
    plt.close(fig)
    print("  v13_speedup_summary.png")


def plot_dashboard(baseline_data, post_lexer_data, post_parser_data):
    """Create 2x3 dashboard: top row = lex per scenario, bottom row = parseOnly per scenario."""
    scenarios_to_plot = ["iterative_math", "iterative_json", "recursive_math",
                         "recursive_json", "big_grammar"]

    fig, axes = plt.subplots(2, 3, figsize=(20, 12))

    # Top row: lex method
    for idx, scenario in enumerate(scenarios_to_plot[:3]):
        ax = axes[0][idx]
        _plot_subplot(ax, baseline_data, post_lexer_data, post_parser_data,
                      scenario, "lex", f"Lex -- {scenario.replace('_', ' ').title()}")

    # Bottom row: parseOnly method
    for idx, scenario in enumerate(scenarios_to_plot[:3]):
        ax = axes[1][idx]
        _plot_subplot(ax, baseline_data, post_lexer_data, post_parser_data,
                      scenario, "parseOnly", f"Parse Only -- {scenario.replace('_', ' ').title()}")

    # Hide unused subplots if any
    for row in range(2):
        for col in range(3):
            if row == 0 and col >= len(scenarios_to_plot[:3]):
                axes[row][col].set_visible(False)
            if row == 1 and col >= len(scenarios_to_plot[:3]):
                axes[row][col].set_visible(False)

    fig.suptitle("v1.3 Optimization Dashboard: Lex (top) and Parse Only (bottom)",
                 fontsize=16, fontweight="bold")
    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "v13_dashboard.png")
    plt.close(fig)
    print("  v13_dashboard.png")


def plot_parse_comparison(baseline_data, post_lexer_data, post_parser_data):
    """Focused parse-specific comparison: parseOnly and fullParse side by side."""
    parse_methods = ["parseOnly", "fullParse"]
    scenarios_to_plot = [s for s in SCENARIOS if s != "big_grammar"]  # big_grammar may not have parse data

    n_scenarios = len(scenarios_to_plot)
    n_methods = len(parse_methods)

    fig, axes = plt.subplots(n_methods, n_scenarios, figsize=(6 * n_scenarios, 5 * n_methods))
    if n_methods == 1:
        axes = [axes]

    for row, method in enumerate(parse_methods):
        for col, scenario in enumerate(scenarios_to_plot):
            ax = axes[row][col] if n_scenarios > 1 else axes[row]
            _plot_subplot(ax, baseline_data, post_lexer_data, post_parser_data,
                          scenario, method,
                          f"{METHOD_LABELS.get(method, method)} -- {scenario.replace('_', ' ').title()}")

    fig.suptitle("v1.3 Parser Optimization Impact: Parse Methods Only",
                 fontsize=16, fontweight="bold")
    fig.tight_layout()
    fig.savefig(CHARTS_DIR / "v13_parse_comparison.png")
    plt.close(fig)
    print("  v13_parse_comparison.png")


def _plot_subplot(ax, baseline_data, post_lexer_data, post_parser_data,
                  scenario, method, title):
    """Helper to plot a three-phase comparison on a subplot axis."""
    b_sizes, b_scores, b_errors = extract_series(baseline_data, scenario, method)
    l_sizes, l_scores, l_errors = extract_series(post_lexer_data, scenario, method)
    p_sizes, p_scores, p_errors = extract_series(post_parser_data, scenario, method)

    if b_sizes:
        ax.errorbar(b_sizes, b_scores, yerr=b_errors,
                     label=BASELINE_STYLE["label"],
                     color=BASELINE_STYLE["color"],
                     marker=BASELINE_STYLE["marker"],
                     linestyle=BASELINE_STYLE["linestyle"],
                     linewidth=2, markersize=6, capsize=2, capthick=1)

    if l_sizes:
        ax.errorbar(l_sizes, l_scores, yerr=l_errors,
                     label=POST_LEXER_STYLE["label"],
                     color=POST_LEXER_STYLE["color"],
                     marker=POST_LEXER_STYLE["marker"],
                     linestyle=POST_LEXER_STYLE["linestyle"],
                     linewidth=2, markersize=6, capsize=2, capthick=1)

    if p_sizes:
        ax.errorbar(p_sizes, p_scores, yerr=p_errors,
                     label=POST_PARSER_STYLE["label"],
                     color=POST_PARSER_STYLE["color"],
                     marker=POST_PARSER_STYLE["marker"],
                     linestyle=POST_PARSER_STYLE["linestyle"],
                     linewidth=2, markersize=6, capsize=2, capthick=1)

    ax.set_xlabel("Input size")
    ax.set_ylabel("Time (ms/op)")
    ax.set_title(title, fontsize=11)
    if b_scores or l_scores or p_scores:
        ax.set_yscale("log")
        ax.yaxis.set_major_formatter(FuncFormatter(time_formatter))
    ax.legend(fontsize=8)
    ax.grid(True, alpha=0.3)


def print_speedup_table(baseline_data, post_lexer_data, post_parser_data):
    """Print a text summary table of speedup ratios at key sizes."""
    key_sizes = [100, 1000, 2000]

    print("\n" + "=" * 110)
    print("v1.3 SPEEDUP SUMMARY TABLE")
    print("=" * 110)
    header = (f"{'Scenario':<20} {'Method':<12} {'Size':>6} "
              f"{'Baseline':>12} {'Post-Lexer':>12} {'Post-Parser':>12} "
              f"{'Lex Speedup':>12} {'Full Speedup':>12}")
    print(header)
    print("-" * 110)

    for scenario in SCENARIOS:
        for method in METHODS:
            b_sizes, b_scores, _ = extract_series(baseline_data, scenario, method)
            l_sizes, l_scores, _ = extract_series(post_lexer_data, scenario, method)
            p_sizes, p_scores, _ = extract_series(post_parser_data, scenario, method)

            b_map = dict(zip(b_sizes, b_scores))
            l_map = dict(zip(l_sizes, l_scores))
            p_map = dict(zip(p_sizes, p_scores))

            for size in key_sizes:
                b_val = b_map.get(size)
                l_val = l_map.get(size)
                p_val = p_map.get(size)

                if b_val is None:
                    continue

                b_str = f"{b_val:>12.4f}"
                l_str = f"{l_val:>12.4f}" if l_val is not None else f"{'N/A':>12}"
                p_str = f"{p_val:>12.4f}" if p_val is not None else f"{'N/A':>12}"
                lex_sp = f"{b_val / l_val:.2f}x" if l_val and l_val > 0 else "N/A"
                par_sp = f"{b_val / p_val:.2f}x" if p_val and p_val > 0 else "N/A"

                print(f"{scenario:<20} {method:<12} {size:>6} "
                      f"{b_str} {l_str} {p_str} "
                      f"{lex_sp:>12} {par_sp:>12}")

    print("=" * 110)


def main():
    setup_matplotlib()
    CHARTS_DIR.mkdir(parents=True, exist_ok=True)

    # Load all three snapshots
    baseline_entries = load_jmh_json(BASELINES_DIR / "alpaca_results.json")
    post_lexer_entries = load_jmh_json(POST_LEXER_DIR / "alpaca_results.json")
    post_parser_entries = load_jmh_json(OUTPUTS_DIR / "alpaca_results.json")

    if baseline_entries is None:
        print("ERROR: No baseline results found at baselines/alpaca_results.json")
        sys.exit(1)
    if post_lexer_entries is None:
        print("ERROR: No post-lexer results found at post-lexer/alpaca_results.json")
        sys.exit(1)
    if post_parser_entries is None:
        print("ERROR: No post-parser results found at outputs/alpaca_results.json")
        sys.exit(1)

    baseline_data = organize_results(baseline_entries)
    post_lexer_data = organize_results(post_lexer_entries)
    post_parser_data = organize_results(post_parser_entries)

    print(f"Loaded: {len(baseline_entries)} baseline, "
          f"{len(post_lexer_entries)} post-lexer, "
          f"{len(post_parser_entries)} post-parser entries")

    chart_count = 0

    # --- Per-scenario per-method 3-way overlay charts ---
    print("\nThree-phase comparison charts:")
    for scenario in SCENARIOS:
        for method in METHODS:
            if plot_three_way(baseline_data, post_lexer_data, post_parser_data,
                              scenario, method):
                chart_count += 1

    # --- Speedup summary bar chart ---
    print("\nSpeedup summary chart:")
    plot_speedup_summary(baseline_data, post_lexer_data, post_parser_data)
    chart_count += 1

    # --- Dashboard ---
    print("\nDashboard chart:")
    plot_dashboard(baseline_data, post_lexer_data, post_parser_data)
    chart_count += 1

    # --- Parse-specific comparison ---
    print("\nParse-specific comparison:")
    plot_parse_comparison(baseline_data, post_lexer_data, post_parser_data)
    chart_count += 1

    # --- Text summary ---
    print_speedup_table(baseline_data, post_lexer_data, post_parser_data)

    v13_charts = len(list(CHARTS_DIR.glob("v13_*.png")))
    print(f"\nDone! {v13_charts} v1.3 comparison charts saved to {CHARTS_DIR}")


if __name__ == "__main__":
    main()
