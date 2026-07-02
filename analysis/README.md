# Offline analysis

Turns the in-app metrics exports into the thesis numbers and the headline figure.
See [`../docs/evaluation-plan.md`](../docs/evaluation-plan.md) for the metric
definitions and the experiment design.

## Setup

```bash
cd analysis
python -m venv .venv && . .venv/Scripts/activate   # Windows PowerShell: .venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## Collecting data — protocol (a): unpaired

Each game is one independent bot-vs-bot match under **one** condition
(mode x spike). Per game:

1. Set the **Predictor Mode** toggle (off / rule / model).
2. Start a match; once it is playing, **Fire Spike** once with the target
   `(magnitude ms, duration ticks)`.
3. Let the game finish.

Repeat to collect **N games per condition** (e.g. N=10). Sweep
`mode in {off, rule, model}` x `m in {100, 200, 300}` x `n in {1, 2, 3}`.
Click **JSON** in the Metrics Log panel to download `metrics-*.json` (holds all
four tables). Drop every export into one folder.

> One spike per game — `sinceSpike` tracks only the most recent spike, so don't
> mix conditions within a game.

## Running

```bash
python analyze.py path/to/exports_dir --outdir results
```

Outputs per-condition tables (also written as CSVs under `results/`):

| Table | Metric |
|---|---|
| `accuracy` | overall / per-horizon (t+1,t+2) / per-snake (A,B) accuracy |
| `masking` | masked rate, visible-rollback rate, on-screen rate |
| `coverage` | fraction of overdue episodes where the gate opened |
| `feltlag` | felt-lag mean +/- 95% CI, p95, max, per condition |
| `feltlag_per_game` | per-game felt-lag (the CI is taken across these) |
| `infer_timing` | inference mean / max ms (feasibility vs 100 ms budget) |

Plus `headline_feltlag.png` — felt-lag vs spike magnitude, one curve per mode.

## Felt-lag reconstruction (how it works)

felt-lag is rebuilt on a fine wall-clock grid, not just at delivery instants,
because during a freeze **no tick is logged** yet the server truth keeps
advancing — sampling only at deliveries would miss the peak (and understate p95).

- Fit the server cadence `t ~= a + b*M` from clean (non-spike) ticks per game.
- `w3(t) = (t - a) / b` (server production step), `w1(t)` = rendered `step` held
  from the most recent tick, `felt_lag = w3 - w1`.

**Caveats / TODO** (tighten once you see pilot data):
- Assumes ~steady server cadence; long games may drift (the per-game fit
  absorbs most of it).
- `w1` is held piecewise-constant between deliveries — a proxy for the rendered
  step; refine if you log per-frame render state.
- Baseline (zero-spike) felt-lag should sit near the resting animation lag;
  if it doesn't, check the cadence fit before trusting spike numbers.
