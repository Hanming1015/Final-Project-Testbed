#!/usr/bin/env python3
"""Offline analysis for the latency-compensation evaluation.

Reads the JSON exports produced by the in-app MetricsExport panel
(`metrics-*.json`, each holding four tables: ticks / verifies / overdues /
infers) and computes the thesis metrics per condition (mode x spikeMs x
spikeTicks):

  * Tier 1 - model accuracy: overall, by horizon (t+1/t+2), by snake (A/B)
  * Tier 2 - system effectiveness: felt-lag (mean/p95/max), masked rate,
             visible-rollback rate
  *          coverage: fraction of overdue episodes where the gate opened
  * Tier 3 - feasibility: inference time (mean/max)

Protocol assumed = option (a) UNPAIRED: each game is an independent bot-vs-bot
match under ONE (mode, spike) condition. Aggregation is mean +/- 95% CI across
games (normal approximation).

This is a SKELETON: the numbers are defensible, but several places are marked
`TODO` where you may want to tighten the method once you have real pilot data.

Usage:
    python analyze.py path/to/exports_dir
    python analyze.py path/to/exports_dir --outdir results --grid-ms 20
"""

import argparse
import glob
import json
import os

import numpy as np
import pandas as pd

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    HAVE_MPL = True
except Exception:
    HAVE_MPL = False

TICK_MS = 100.0          # server tick interval (must match the frontend)
COND = ["mode", "spikeMs", "spikeTicks"]   # condition key


# ── loading ──────────────────────────────────────────────────────────────────

def load(path):
    """Load one file or a directory of metrics-*.json into four DataFrames."""
    if os.path.isdir(path):
        files = sorted(glob.glob(os.path.join(path, "metrics-*.json")))
    else:
        files = [path]
    if not files:
        raise SystemExit(f"no metrics-*.json found under {path!r}")

    tables = {"ticks": [], "renders": [], "verifies": [], "overdues": [], "infers": []}
    for i, f in enumerate(files):
        with open(f, "r", encoding="utf-8") as fh:
            blob = json.load(fh)
        # Make game ids unique across files (each export restarts its own counter).
        for name in tables:
            rows = blob.get(name, [])
            for r in rows:
                r["_src"] = os.path.basename(f)
                r["_ugame"] = f"{i}:{r.get('game', 0)}"   # unique game id
            tables[name].extend(rows)

    dfs = {name: pd.DataFrame(rows) for name, rows in tables.items()}
    print(f"loaded {len(files)} file(s): " +
          ", ".join(f"{n}={len(df)}" for n, df in dfs.items()))
    return dfs


# ── aggregation helper ───────────────────────────────────────────────────────

def mean_ci(series):
    """Mean and half-width of a 95% CI (normal approx). NaN-safe."""
    x = pd.Series(series).dropna().to_numpy(dtype=float)
    n = len(x)
    if n == 0:
        return np.nan, np.nan
    if n == 1:
        return float(x[0]), np.nan
    sem = x.std(ddof=1) / np.sqrt(n)
    return float(x.mean()), float(1.96 * sem)


# ── Tier 1: accuracy ─────────────────────────────────────────────────────────

def accuracy(verifies):
    """Per-condition accuracy: overall, by horizon, by snake."""
    if verifies.empty:
        return pd.DataFrame()
    v = verifies.copy()
    g = v.groupby(COND)
    out = g.agg(
        n=("correct", "size"),
        acc_both=("correct", "mean"),
        acc_A=("okA", "mean"),
        acc_B=("okB", "mean"),
    )
    # By horizon (t+1 vs t+2): errors concentrate on t+2.
    for h in sorted(v["horizon"].unique()):
        sub = v[v["horizon"] == h].groupby(COND)["correct"].mean()
        out[f"acc_h{h}"] = sub
    return out.reset_index()


# ── Tier 2: masking / rollback ───────────────────────────────────────────────

def masking(verifies):
    """Masked rate (correct & on-screen) and visible-rollback rate."""
    if verifies.empty:
        return pd.DataFrame()
    v = verifies.copy()
    v["masked"] = v["correct"] & v["onScreen"]
    v["visible_rb"] = (~v["correct"]) & v["onScreen"]
    g = v.groupby(COND)
    return g.agg(
        n=("masked", "size"),
        masked_rate=("masked", "mean"),
        visible_rb_rate=("visible_rb", "mean"),
        onscreen_rate=("onScreen", "mean"),
    ).reset_index()


# ── Tier 1/2: coverage ───────────────────────────────────────────────────────

def coverage(overdues):
    """Fraction of overdue episodes where the constraint gate opened."""
    if overdues.empty:
        return pd.DataFrame()
    g = overdues.groupby(COND)
    return g.agg(
        overdue_episodes=("gateOpen", "size"),
        coverage=("gateOpen", "mean"),
    ).reset_index()


# ── Tier 2: felt-lag (timeline reconstruction) ───────────────────────────────

def _fit_cadence(game_ticks):
    """Fit the ideal server-production line t ~= a + b*M from CLEAN ticks.

    Clean = no recent spike (spikeMs==0 or sinceSpike beyond the spike window).
    Returns (a, b). Falls back to the fixed 100 ms cadence if too few points.
    """
    gt = game_ticks
    clean = gt[(gt["spikeMs"] == 0) |
               (gt["sinceSpike"].isna()) |
               (gt["sinceSpike"] > gt["spikeTicks"] + 2)]
    if len(clean) >= 3:
        b, a = np.polyfit(clean["M"].to_numpy(float), clean["t"].to_numpy(float), 1)
        # Guard against a degenerate fit; cadence should be ~TICK_MS.
        if 0.5 * TICK_MS < b < 2.0 * TICK_MS:
            return a, b
    # Fallback: anchor on the first tick, assume exactly TICK_MS per step.
    m0 = gt["M"].min()
    t0 = gt.loc[gt["M"].idxmin(), "t"]
    return t0 - m0 * TICK_MS, TICK_MS


def felt_lag_per_game(ticks, renders, grid_ms=20.0):
    """Reconstruct felt-lag on a fine wall-clock grid, per game.

    w3(t) = server production step  = (t - a) / b   (cadence fit from `ticks`)
    w1(t) = rendered step (held)    = step of the most recent sample <= t
    felt_lag(t) = w3(t) - w1(t)      (walker model: felt lag = w3 - w1)

    w1 is sampled from the `renders` stream (per animation frame) when available:
    that is the ONLY source with samples DURING a spike freeze, where no move is
    delivered. It distinguishes the glide (model) from the freeze (off) — the
    whole point of the system. Falls back to `ticks` (delivery instants only,
    blind to the freeze window) for older exports without a renders table.

    Returns one row per game with mean/p95/max felt-lag.
    """
    if ticks.empty:
        return pd.DataFrame(), False
    has_renders = (renders is not None and not renders.empty)
    r_by_game = dict(tuple(renders.groupby("_ugame"))) if has_renders else {}

    rows = []
    for gid, gt in ticks.groupby("_ugame"):
        gt = gt.sort_values("M")
        if len(gt) < 2:
            continue
        a, b = _fit_cadence(gt)   # cadence (w3) always from server deliveries

        # w1 source: prefer per-frame renders, else fall back to delivery ticks.
        src = r_by_game[gid].sort_values("t") if gid in r_by_game else gt
        t = src["t"].to_numpy(float)
        step = src["step"].to_numpy(float)
        if len(t) < 2:
            continue

        grid = np.arange(t[0], t[-1], grid_ms)
        if grid.size == 0:
            continue
        w3 = (grid - a) / b
        idx = np.searchsorted(t, grid, side="right") - 1
        idx = np.clip(idx, 0, len(step) - 1)
        w1 = step[idx]
        felt = w3 - w1

        # Condition tag for this game (constant under the unpaired protocol;
        # take the fired spike = max, and the modal mode).
        cond = {
            "mode": gt["mode"].mode().iat[0],
            "spikeMs": int(gt["spikeMs"].max()),
            "spikeTicks": int(gt["spikeTicks"].max()),
        }
        rows.append({
            "_ugame": gid, **cond,
            "felt_mean": float(np.mean(felt)),
            "felt_p95": float(np.percentile(felt, 95)),
            "felt_max": float(np.max(felt)),
        })
    return pd.DataFrame(rows), has_renders


def felt_lag_spikes(ticks, renders, gap_factor=1.5):
    """Felt-lag measured DURING freeze windows — the honest headline.

    A whole-game average dilutes the effect: freeze windows are a minority of
    samples, so clean play washes out the spike. Instead isolate each freeze (a
    delivery gap > gap_factor x cadence) and take the PEAK felt-lag inside it =
    how stale the screen got at the worst instant. Aggregate peaks per condition.
    This is where off (freeze) and model (glide) actually separate.

    Returns (per-condition aggregate, per-spike rows).
    """
    if ticks.empty or renders is None or renders.empty:
        return pd.DataFrame(), pd.DataFrame()
    r_by_game = dict(tuple(renders.groupby("_ugame")))
    rows = []
    for gid, gt in ticks.groupby("_ugame"):
        gt = gt.sort_values("M")
        if len(gt) < 3 or gid not in r_by_game:
            continue
        a, b = _fit_cadence(gt)
        gr = r_by_game[gid].sort_values("t")
        rt = gr["t"].to_numpy(float)
        rfelt = (rt - a) / b - gr["step"].to_numpy(float)
        td = gt["t"].to_numpy(float)
        cond = {
            "mode": gt["mode"].mode().iat[0],
            "spikeMs": int(gt["spikeMs"].max()),
            "spikeTicks": int(gt["spikeTicks"].max()),
        }
        for i in range(len(td) - 1):
            gap = td[i + 1] - td[i]
            if gap > gap_factor * b:                      # a freeze
                sel = (rt >= td[i]) & (rt <= td[i + 1])
                if sel.any():
                    rows.append({"_ugame": gid, **cond,
                                 "gap_ms": round(gap, 1),
                                 "peak_felt": float(rfelt[sel].max())})
    per_spike = pd.DataFrame(rows)
    if per_spike.empty:
        return pd.DataFrame(), per_spike
    recs = []
    for cond, grp in per_spike.groupby(COND):
        pm, pci = mean_ci(grp["peak_felt"])
        recs.append({
            **dict(zip(COND, cond)),
            "n_spikes": len(grp),
            "peak_mean": pm, "peak_ci": pci,
            "peak_p95": float(np.percentile(grp["peak_felt"], 95)),
            "peak_max": float(grp["peak_felt"].max()),
        })
    return pd.DataFrame(recs), per_spike


def felt_lag(ticks, renders, grid_ms=20.0):
    """Whole-game felt-lag (context only; the freeze-window metric is the headline)."""
    per_game, has_renders = felt_lag_per_game(ticks, renders, grid_ms)
    if not has_renders:
        print("  [warn] no `renders` table — felt-lag falls back to delivery "
              "instants and is BLIND to the freeze window (see docs).")
    if per_game.empty:
        return pd.DataFrame(), per_game
    recs = []
    for cond, grp in per_game.groupby(COND):
        m_mean, m_ci = mean_ci(grp["felt_mean"])
        p_mean, _ = mean_ci(grp["felt_p95"])
        recs.append({
            **dict(zip(COND, cond)),
            "games": len(grp),
            "felt_mean": m_mean, "felt_mean_ci": m_ci,
            "felt_p95": p_mean,
            "felt_max": grp["felt_max"].max(),
        })
    return pd.DataFrame(recs), per_game


# ── Tier 3: inference timing ─────────────────────────────────────────────────

def infer_timing(infers):
    if infers.empty:
        return pd.DataFrame()
    g = infers.groupby(COND)
    return g.agg(
        n=("ms", "size"),
        infer_mean_ms=("ms", "mean"),
        infer_max_ms=("ms", "max"),
    ).reset_index()


# ── headline figure ──────────────────────────────────────────────────────────

def plot_headline(freeze_agg, outdir):
    """Freeze-peak felt-lag vs spike magnitude, one curve per mode (headline)."""
    if not HAVE_MPL or freeze_agg.empty:
        return
    fig, ax = plt.subplots(figsize=(6, 4))
    for mode, grp in freeze_agg.groupby("mode"):
        grp = grp.sort_values("spikeMs")
        ax.errorbar(grp["spikeMs"], grp["peak_mean"], yerr=grp["peak_ci"],
                    marker="o", capsize=3, label=mode)
    ax.set_xlabel("spike magnitude (ms)")
    ax.set_ylabel("freeze-peak felt-lag (ticks)")
    ax.set_title("Freeze-peak felt-lag vs spike size, by predictor mode")
    ax.legend(title="mode")
    ax.grid(True, alpha=0.3)
    fig.tight_layout()
    path = os.path.join(outdir, "headline_feltlag.png")
    fig.savefig(path, dpi=150)
    print(f"wrote {path}")


# ── main ─────────────────────────────────────────────────────────────────────

def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("path", help="metrics-*.json file or a directory of them")
    ap.add_argument("--outdir", default="results", help="output directory")
    ap.add_argument("--grid-ms", type=float, default=20.0,
                    help="felt-lag reconstruction grid step (ms)")
    args = ap.parse_args()
    os.makedirs(args.outdir, exist_ok=True)

    dfs = load(args.path)

    acc = accuracy(dfs["verifies"])
    msk = masking(dfs["verifies"])
    cov = coverage(dfs["overdues"])
    fl_agg, fl_game = felt_lag(dfs["ticks"], dfs["renders"], args.grid_ms)
    fs_agg, fs_spikes = felt_lag_spikes(dfs["ticks"], dfs["renders"])
    inf = infer_timing(dfs["infers"])

    def show(name, df):
        print(f"\n=== {name} ===")
        print("(none)" if df.empty else df.to_string(index=False))
        if not df.empty:
            df.to_csv(os.path.join(args.outdir, f"{name}.csv"), index=False)

    show("accuracy", acc)
    show("masking", msk)
    show("coverage", cov)
    show("feltlag_freeze", fs_agg)        # headline: felt-lag during freezes
    show("feltlag_per_spike", fs_spikes)
    show("feltlag_wholegame", fl_agg)     # context: diluted by clean play
    show("infer_timing", inf)

    plot_headline(fs_agg, args.outdir)
    print(f"\nresults written to {args.outdir}/")


if __name__ == "__main__":
    main()
