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

> Can a **learned** sequence model (GRU Seq2Seq) mask client-side latency **better
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

**Run every spike condition under all three modes** (paired comparison) so the only
difference is the predictor.

---

## 2. Metrics

### Tier 1 — Model accuracy (intrinsic)

| Metric | Definition | Status |
|---|---|---|
| Per-step direction accuracy | predicted dir == server dir, over all predicted steps | ✅ in PredictionStats |
| **Accuracy by horizon** | t+1 accuracy vs t+2 accuracy *(errors concentrate on t+2)* | ⬜ to add |
| Accuracy by snake | A vs B (explains predictability asymmetry) | ✅ in PredictionStats |
| **Coverage** | fraction of overdue/spike steps where the **constraint gate fired** | ⬜ to add |

> Effective masking ≈ **coverage × accuracy**: high accuracy but low coverage means
> "accurate when it fires, but rarely fires." Always report coverage alongside.

### Tier 2 — System effectiveness (headline)

| Metric | Definition | Status |
|---|---|---|
| **Felt-lag** (mean, **p95**, distribution) | `felt lag = latency − ahead`; report per mode | ⬜ to add (`ahead` logged; combine with injected delay) |
| **Masked rate** | predicted steps that were correct **and** on-screen → latency hidden | ✅ in PredictionStats |
| **Visible-rollback rate** | predicted steps that were wrong **and** on-screen → visible correction (the **cost** side) | ✅ in PredictionStats |
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

Logging these per event lets you derive every metric above. **Add a structured export
(CSV / JSON dump)** — do not rely on reading panels by eye; multi-run aggregation needs
structured data.

- **Per verify:** `mode, spike(m,n), horizon(t+1/t+2), A_pred, A_srv, B_pred, B_srv, correct?, on_screen?`
- **Per tick:** `ahead, injected_latency` → `felt_lag = latency − ahead`
- **Per overdue trigger:** `gate_open?` (for coverage)
- **Per inference:** elapsed ms (already via InferTiming)

---

## 4. Aggregation & rigor

1. **Repeat:** run **N games / N spikes per condition**; report **mean ± confidence
   interval**, never a single game (one-game numbers are anecdotal).
2. **Pair:** identical spike settings across `off` / `rule` / `model`.
3. **Be honest about coverage:** felt-lag improvement only occurs on gate-open spikes —
   either report felt-lag **on gate-open spikes**, or report over all spikes **with the
   coverage stated**, so open-space spikes don't silently dilute the mean.

---

## 5. Headline figure

> **x-axis** = spike size (`m`, or `m + n − 1`) · **y-axis** = **felt-lag** (or
> masked-rate) · **three curves** = `off` / `rule` / `model`.

One figure shows both results at once:

1. the **model > rule > none** gap (the learned-vs-rules answer), and
2. **where each method breaks**, lining up with the analytic boundary
   `m + n − 1 ≤ K` from [`walker-model.md`](./walker-model.md).

---

## 6. Engineering still to add

Most instrumentation already exists (accuracy, A/B, masked %, visible-rollback %,
inference timing). The remaining work:

1. **t+1 / t+2 accuracy split** (tag each pending step with its horizon).
2. **Felt-lag logging** (combine `ahead` with the known injected delay per tick).
3. **Coverage counter** (gate-open vs total overdue triggers).
4. **`rule` predictor mode** (the single `tryPredict` seam — see
   [`architecture.md`](./architecture.md)) + a tri-state `ModelToggle`.
5. **Structured export + multi-run aggregation.**

---

## 7. One-line summary

Vary **mode × m × n**; lead with **felt-lag** (+ visible-rollback as the cost),
backed by **t+1/t+2 accuracy** and **coverage**, with **inference time** for
feasibility; aggregate over many runs and plot **one "three modes vs spike size"
curve**.
