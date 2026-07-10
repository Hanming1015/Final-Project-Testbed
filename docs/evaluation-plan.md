# Evaluation Plan — System Benchmarking & Evaluation

How to quantify whether the learned latency-compensation system **works**, and
whether it works **better than a non-learned rule baseline**. This is the blueprint
for the Benchmarking & Evaluation chapter.

> The thesis claim is a **systems** claim ("a learned model compensates latency"),
> not just a model-accuracy claim. So the evaluation has two tiers: **model
> accuracy** (intrinsic) and **system effectiveness** (the headline), plus a
> **feasibility** tier. The single most persuasive result is **felt-lag reduction**
> across three predictor modes.

---

## 0. Research question under test

> Can a **learned** sequence model (a GRU direct multi-step predictor) mask client-side latency **better
> than hand-written rule-based dead-reckoning**, and how does that advantage depend
> on the latency spike?

Answering it **requires** the rule baseline — without it, the evaluation only shows
"prediction beats no-prediction", which is the weaker (near-trivial) claim. In Snake,
"repeat the last direction" is a *strong* baseline (snakes mostly go straight), so the
learned model's value lives almost entirely **at turns** — exactly the constrained
states where the gate fires.

---

## 1. Independent variables (what you vary)

| Dimension | Example values | Purpose |
|---|---|---|
| **Predictor mode** | `off` / `rule` / `model` | Three-way comparison: no-prediction vs dead-reckoning vs GRU |
| **Spike magnitude `m`** | 100 / 200 / 300 ms | Probe the **depth** axis |
| **Spike duration `n`** | 1 / 2 / 3 ticks | Probe the **width** axis (validates `m + n − 1`) |

- `off` = `ModelToggle` off (no prediction; freeze + snap baseline).
- `rule` = dead-reckoning: repeat the last confirmed direction for both snakes
  (`pa = [shadowA.dir, shadowA.dir]`, same for B), routed through the **same** gate,
  simulation, verify and rollback as the model — only the direction source differs.
- `model` = the GRU.

**Run every spike condition under all three modes** (condition-matched across modes;
independent games per condition — unpaired, aggregated with mean ± CI, see §4) so the only
systematic difference is the predictor.

---

## 2. Metrics

### Tier 1 — Model accuracy (intrinsic)

| Metric | Definition | Status |
|---|---|---|
| Per-step direction accuracy | predicted dir == server dir, over all predicted steps | ✅ in PredictionStats + `verifies` log |
| **Accuracy by horizon** | t+1 accuracy vs t+2 accuracy *(errors concentrate on t+2)* | ✅ logged (`verifies.horizon`) — split offline |
| Accuracy by snake | A vs B (explains predictability asymmetry) | ✅ PredictionStats + `verifies` (`okA`/`okB`) |
| **Coverage** | fraction of overdue/spike steps where the **constraint gate fired** | ✅ logged (`overdues.gateOpen`) |

> Effective masking ≈ **coverage × accuracy**: high accuracy but low coverage means
> "accurate when it fires, but rarely fires." Always report coverage alongside.

### Tier 2 — System effectiveness (headline)

| Metric | Definition | Status |
|---|---|---|
| **Felt-lag — freeze-peak** (headline) | `felt lag = latency − ahead`, measured at the **worst instant of each freeze window** (delivery gap > 1.5× cadence), aggregated per mode. This is where `off` (freeze) and `model` (glide) separate; a whole-game mean **dilutes** the effect and is kept only as context. | ✅ reconstructed offline from `renders` (per-frame w1) + `ticks` cadence — see `analysis/analyze.py` `felt_lag_spikes()` |
| **Masked rate** | predicted steps that were correct **and** on-screen → latency hidden | ✅ PredictionStats + `verifies` (`correct && onScreen`) |
| **Visible-rollback rate** | predicted steps that were wrong **and** on-screen → visible correction (the **cost** side) | ✅ PredictionStats + `verifies` (`!correct && onScreen`) |
| Freeze rate / duration *(optional)* | how often / how long the snake stalls (the `rule`/`off` failure mode) | ⬜ optional |

> Never report felt-lag **alone** — pair it with visible-rollback so both the benefit
> and the cost are on the table. In the testbed the spike is **injected**, so the true
> latency is known and felt-lag is exactly computable.

### Tier 3 — Feasibility / cost (supports "lightweight, client-deployable")

| Metric | Definition | Status |
|---|---|---|
| Inference time per tick (mean / **max** vs 100 ms budget) | proves real-time client-side deployability | ✅ InferTiming |
| Model size (params / ONNX file size) | static, one number | ⬜ record once |

---

## 3. Raw data to log

**Implemented** — `MetricsLog.js` records raw fields per event; the `MetricsExport`
panel dumps CSV / JSON in the browser (do not read panels by eye). Every row is stamped
with the experiment context (`game, mode, spikeMs, spikeTicks, sinceSpike, t`) so a
single CSV can be **filtered per condition** offline. Five tables:

- **`verifies`** (per confirmed prediction): `step, horizon(t+1/t+2), aPred, aSrv, bPred, bSrv, okA, okB, correct, onScreen`
- **`ticks`** (per delivered move): `step, M, ahead` + delivery time `t` → anchors the server cadence
- **`renders`** (per animation frame, throttled ~33 ms): `step, M` → the rendered walker w1(t). The **only** source that samples **during** a spike freeze, where no move is delivered — needed to reconstruct freeze-peak felt-lag (`off` freeze vs `model` glide)
- **`overdues`** (per overdue episode): `gateOpen` → coverage
- **`infers`** (per model inference): `ms` (feasibility; `rule`/`off` log none)

> **One condition per game, not one spike.** With **auto-spike** on (§4), many spikes of
> the **same** `(m,n)` fire within a game (each re-arms `sinceSpike`) — that is intended and
> gives many freeze samples per game. What must stay constant within a game is the
> **condition** (`mode`, `m`, `n`); don't change the mode or spike settings mid-game.

---

## 4. Aggregation & rigor

1. **Repeat (unpaired):** each game is an **independent** bot-vs-bot match under **one**
   condition (`mode × m × n`); run several games per condition and report **mean ± 95% CI**
   across games / freeze-spikes, never a single game (one-game numbers are anecdotal). The
   conditions are **matched across modes** (same `m, n` for `off` / `rule` / `model`), but the
   games themselves are not paired — aggregation is across independent games.
2. **Two collection sub-campaigns:**
   - **Auto-spike ON** → measures the **masking magnitude** (freeze-peak felt-lag, accuracy,
     masked/rollback). Auto-spike fires whenever both snakes are constrained, so it forces
     gate-open freeze windows and yields many comparable samples fast. Fires identically in all
     three modes, so `off` / `rule` / `model` face the same provocations.
   - **Auto-spike OFF (free play)** → measures **natural coverage** (`overdues.gateOpen`): how
     often the gate opens on its own. Auto-spike would bias this, so coverage must come from
     free-play games.
3. **Report two numbers, not one.** Effective benefit ≈ **masking magnitude × natural
   coverage**: the model only masks when the gate opens, so a large freeze-peak reduction under
   auto-spike must be reported **alongside** the natural coverage, or open-space spikes silently
   dilute the story. Never quote felt-lag improvement without the coverage next to it.
4. **Collection hygiene:** one condition per game (§3); **Reset** the metrics log between
   conditions and export one JSON per condition, so files don't overlap. Drop all JSONs in one
   folder — `analysis/analyze.py` tags every row with its condition and aggregates.

---

## 5. Headline figure

> **x-axis** = spike size (`m`, or `m + n − 1`) · **y-axis** = **freeze-peak felt-lag**
> (headline; or masked-rate) · **three curves** = `off` / `rule` / `model`.
> Produced by `analysis/analyze.py` → `headline_feltlag.png`.

One figure shows both results at once:

1. the **model > rule > none** gap (the learned-vs-rules answer), and
2. **where each method breaks**, lining up with the analytic boundary
   `m + n − 1 ≤ K` from [`walker-model.md`](./walker-model.md).

---

## 6. Engineering status

Instrumentation done:

1. ✅ **t+1 / t+2 accuracy split** — each pending step tagged with `horizon`; logged in `verifies`.
2. ✅ **Felt-lag logging + freeze-peak reconstruction** — `ticks` anchors the server cadence,
   `renders` samples the rendered walker per frame (incl. during the freeze); freeze-peak
   felt-lag derived offline (`analyze.py` `felt_lag_spikes()`).
3. ✅ **Coverage counter** — one `overdues` row per overdue episode with `gateOpen`.
4. ✅ **`rule` predictor mode** — dead-reckoning at the single `tryPredict` seam
   (`pa = Array(LOOKAHEAD).fill(shadowA.dir)`) + tri-state `ModelToggle` (off/rule/model).
5. ✅ **Auto-spike** — `SpikeInjector` Auto mode fires when both snakes are constrained, to
   collect gate-open freeze samples efficiently (same in all modes).
6. ✅ **Structured export** — `MetricsLog.js` + `MetricsExport` panel (Verifies / Renders CSV, JSON).
7. ✅ **Offline analysis pipeline** — `analysis/analyze.py`: per-condition accuracy, masking,
   coverage, freeze-peak felt-lag (+ headline plot), inference timing (mean ± 95% CI).

Remaining:

- 🔄 **Data collection** — run the `mode × m × n` sweep (several games per condition; auto-spike
  ON for magnitude, OFF for natural coverage), export per condition, aggregate with `analyze.py`.
- ⬜ **Model size** — record params / ONNX file size once (Tier 3).

---

## 7. One-line summary

Vary **mode × m × n**; lead with **freeze-peak felt-lag** (+ visible-rollback as the
cost), backed by **t+1/t+2 accuracy** and **coverage** (reported as magnitude × coverage),
with **inference time** for feasibility; aggregate over many independent games and plot
**one "three modes vs spike size" curve**.
