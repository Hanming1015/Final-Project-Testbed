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

> **Interpreter note.** Needs a CPython with `numpy`/`pandas`/`matplotlib` wheels
> available — a stock python.org build (3.10–3.12) works. An MSYS2 `python` (no
> `pip`) or a too-new interpreter without prebuilt wheels will fail. On this
> machine the known-good one is
> `C:\Users\19831\AppData\Local\Programs\Python\Python312\python.exe`.

## Collecting data — protocol (a): unpaired

Each game is one independent bot-vs-bot match under **one** condition
(`mode × m × n`). The statistical unit is the **freeze-spike**, not the game, and
with **auto-spike** one game yields many freeze-spikes — so a few games per
condition is plenty.

**Per condition** (not per game):

1. Set the **Predictor Mode** toggle (`off` / `rule` / `model`).
2. Set the spike `(magnitude ms, duration ticks)` in the **Latency Spike** panel.
3. Choose the sub-campaign:
   - **Auto-spike ON** → measures **masking magnitude** (freeze-peak felt-lag,
     accuracy, masked/rollback). Auto fires whenever both snakes are constrained,
     so it forces gate-open freezes and collects samples fast. Fires the same in
     all three modes.
   - **Auto-spike OFF** (free play, don't fire) → measures **natural coverage**
     (`overdues.gateOpen`). Auto-spike would bias this, so coverage comes from
     these runs only.
4. Play **several games back-to-back** under this fixed condition (the logger
   auto-bumps the game id each match — no need to Reset between games).
5. Click **JSON** in the Metrics Log panel to download `metrics-*.json` (holds all
   five tables), then **Reset** before switching to the next condition.

Sweep `mode ∈ {off, rule, model}` × `m ∈ {100, 200, 300}` × `n ∈ {1, 2, 3}`.
Drop every export into one folder; `analyze.py` tags each row with its condition
and aggregates across files.

> **One condition per game.** With auto-spike, many spikes of the *same* `(m,n)`
> fire per game — intended. Just don't change the mode or spike settings mid-game,
> and Reset + export one JSON per condition so files don't overlap.

## Running

```bash
python analyze.py path/to/exports_dir --outdir results
```

Outputs per-condition tables (also written as CSVs under `results/`):

| Table | Metric |
|---|---|
| `accuracy` | overall / per-horizon (t+1, t+2) / per-snake (A, B) accuracy |
| `masking` | masked rate, visible-rollback rate, on-screen rate |
| `coverage` | fraction of overdue episodes where the gate opened |
| `feltlag_freeze` | **headline** — freeze-peak felt-lag (mean ± 95% CI, p95, max) per condition |
| `feltlag_per_spike` | one row per freeze window (`gap_ms`, `peak_felt`) — the raw units the CI is over |
| `feltlag_wholegame` | whole-game felt-lag (context only; diluted by clean play) |
| `infer_timing` | inference mean / max ms (feasibility vs 100 ms budget) |

Plus `headline_feltlag.png` — **freeze-peak felt-lag** vs spike magnitude, one
curve per mode.

## Felt-lag reconstruction (how it works)

A whole-game average **dilutes** the effect: freeze windows are a minority of
samples, so clean play washes the spike out (and `off` ≈ `model` on the mean).
The headline instead isolates each **freeze** and takes the **peak** felt-lag
inside it — where `off` (freeze) and `model` (glide) actually separate.

- Fit the server cadence `t ≈ a + b·M` from clean (non-spike) ticks per game →
  `w3(t) = (t − a) / b` is the server production step.
- `w1(t)` = the rendered `step`, sampled from the **`renders`** table (per
  animation frame, throttled ~33 ms). This is the key: during a freeze **no move
  is delivered**, so `ticks` has no samples there — only the per-frame `renders`
  stream captures the glide-vs-freeze behaviour in the exact window that matters.
- `felt_lag(t) = w3(t) − w1(t)`. A **freeze** = a delivery gap > 1.5× the fitted
  cadence; within each such window take `peak_felt = max(felt_lag)`, then
  aggregate peaks per condition (`felt_lag_spikes()`).

**Caveats:**
- Assumes ~steady server cadence; long games may drift (the per-game fit absorbs
  most of it — check that zero-spike baseline felt-lag sits near the resting
  animation lag before trusting spike numbers).
- Auto-spike runs measure masking **magnitude**, not coverage — always report the
  freeze-peak reduction **alongside** the natural coverage from the free-play runs
  (effective benefit ≈ magnitude × coverage).
