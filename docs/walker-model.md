# The Three-Walker Model — Reading Latency Compensation Under a Spike

A mental model for understanding what the `ahead` metric means and how
client-side prediction masks a latency spike. It tracks three "walkers", each
advancing at the same pace (one cell per **100 ms** — the server tick and the
animation duration are both 100 ms).

> This doc uses the current `ahead` definition: **`ahead = snake0.step − moveCount`
> (= `step − M`)**, where the resting value is **−1** (the render is always one
> step behind the latest confirmed move because of the 100 ms cell animation).

---

## The three walkers

| Walker | Meaning | In code |
|--------|---------|---------|
| **walker1** | What I have **rendered** (the snake on screen) | `snake0.step` |
| **walker2** | The **truth I already know** (latest confirmed move received) | `moveCount` (= `M`) |
| **walker3** | The **server's "now"** (real-time authoritative state) | server tick |

Key relations:

```
ahead       = walker1 − walker2     (= step − M, the SyncMonitor number)
latency     = walker3 − walker2     (how far behind the truth I am)
felt lag    = walker3 − walker1     = latency − ahead
```

The **felt lag** is what the player actually experiences: how far the on-screen
snake is behind the true server state.

---

## Start state — no latency, steady

```
walker3 = 10   (server now)
walker2 = 10   (known)      ← walker2 & walker3 coincide  (latency = 0)
walker1 =  9   (rendered)   ← one step behind  (the 100 ms animation, always in flight)

ahead = 9 − 10 = −1      latency = 0      felt lag = 10 − 9 = 1
```

Note: **even at zero latency the felt lag is 1 step** — the animation's 100 ms can
never be removed. This is why `ahead = −1` is the *healthy resting value*, not a
problem.

---

## The two spike parameters

A spike is injected via `fireSpike({ ms, ticks })` (the **Latency Spike** panel).
The two knobs are independent and control different axes:

| Parameter | Meaning | Axis it controls |
|---|---|---|
| **`ms`** (magnitude) | how **late** each affected move is delivered | **depth** — peak latency in ticks = `ms / 100`. How far walker3 runs ahead of walker2. |
| **`ticks`** (duration) | how **many** consecutive moves are delayed | **width** — how long latency stays near its peak / how many moves bunch in the recovery burst. |

Mnemonic: **magnitude = how deep the hole is, duration = how wide it is.** The
default `200 ms × 2 ticks` is "a 2-step-deep, 2-move-wide" hole.

Throughout this doc we write **`m = ms / 100`** (magnitude in ticks) and **`n = ticks`**
(duration). Delivery is **order-preserving with bunching** (`scheduleDelivery`):
a delayed move pushes the moves behind it, so a finished spike **flushes as a
burst** and the stream catches back up.

---

## Case A — `200 ms × 1 tick`: the spike is fully masked

Only **one** move (move 11) is delayed (`m = 2, n = 1`). Moves 12 and 13 are not
spiked but get pushed behind it, so **moves 11, 12, 13 all arrive together** as a
burst ~200 ms later. The constraint gate is open, K = 2, predictions correct.

| Tick | w3 (now) | w2 (known) | w1 (rendered) | latency | **ahead** | felt lag | On screen |
|:----:|:--------:|:----------:|:-------------:|:-------:|:---------:|:--------:|-----------|
| t0 | 10 | 10 | 9 | 0 | **−1** | 1 | steady |
| t1 | 11 | 10 ❄️ | 10 | 1 | **0** | 1 | w1 catches the frozen w2; move detected as late |
| t2 | 12 | 10 ❄️ | 11 *(pred)* | 2 | **+1** | 1 | w1 **passes w2** onto a prediction; latency peaks at 2 |
| t3 | 13 | 13 ⚡ | 12 *(pred ✓)* | 0 | **−1** | 1 | burst (11,12,13) flushes in; predictions 11 & 12 verify ✓ |
| t4 | 14 | 14 | 13 | 0 | **−1** | 1 | real move 13 was already in the burst → no gap, w1 keeps gliding |
| t5 | 15 | 15 | 14 | 0 | **−1** | 1 | recovered to steady |

❄️ = frozen (delayed move not yet arrived) · ⚡ = burst arrival (bunched moves)

**The whole point — the felt-lag column stays at 1 the entire time.** Latency
surges 0 → 2 and back, but walker1 stays exactly one step behind walker3
throughout: the player's experience is identical to the zero-latency case. **The
spike is invisible.** The K = 2 prediction (steps 11, 12) covers the gap exactly,
and by the time w1 needs step 13 the burst has already delivered it for real — so
there is **no freeze**.

> `ahead` dips to **−1** at t3 not because anything went wrong, but because the
> burst makes **walker2 jump forward** (10 → 13) in one tick while walker1 is mid-
> glide. Read **felt lag**, not `ahead`, to judge the player experience: it is the
> column that stays flat.

---

## Case B — `200 ms × 2 ticks` (the default): glide, then freeze + snap

Now **two** moves (11 and 12) are delayed (`m = 2, n = 2`). Delivery bunches
**moves 12, 13, 14** at ~1400 ms (one tick later than Case A). That extra tick is
exactly enough to **outrun the K = 2 prediction horizon**.

| Tick | w3 (now) | w2 (known) | w1 (rendered) | latency | **ahead** | felt lag | On screen |
|:----:|:--------:|:----------:|:-------------:|:-------:|:---------:|:--------:|-----------|
| t0 | 10 | 10 | 9 | 0 | **−1** | 1 | steady |
| t1 | 11 | 10 ❄️ | 10 | 1 | **0** | 1 | w1 catches frozen w2; move detected as late |
| t2 | 12 | 10 ❄️ | 11 *(pred)* | 2 | **+1** | 1 | w1 passes w2 onto a prediction |
| t3 | 13 | 11 | 12 *(pred)* | 2 | **+1** | 1 | move 11 flushes in, verifies step-11 ✓ |
| **t4** | 14 | **14** ⚡ | **12** ❄️ | 0 | **−2** | **2** | **moves 12,13,14 burst in; w1 has run out of predictions and is frozen at 12** |
| t5 | 15 | 15 | 14 | 0 | **−1** | 1 | engine **snaps** 12 → 13 then animates to 14 |
| t6 | 16 | 16 | 15 | 0 | **−1** | 1 | recovered to steady |

**Why the freeze.** With K = 2 the snake predicts only steps 11 and 12 from the
frozen frontier (move 10). It renders step 12 and then **wants step 13 — which was
never predicted, and whose real move has not arrived yet** (it is one tick later in
the burst). So walker1 freezes at 12 for ~half a tick. When the burst lands, the
real moves arrive **all at once**; because `pending` is empty the engine sets
`maxLag = 1` and **snaps** the backlog rather than animating it. The felt-lag
column is therefore **not** flat — it blips to **2** at t4, then the snap pulls it
back.

This is the **graceful-degradation** floor: a spike beyond the horizon costs one
visible freeze + snap, i.e. it degrades to the **no-prediction baseline, never
worse**.

---

## How deep a prediction the no-freeze guarantee needs

Counting from the moment prediction starts to the moment the delayed moves bunch
back in, the snake must self-propel a fixed number of steps. The result:

```
predicted steps needed to avoid a freeze  =  m + n − 1
                                          =  (ms / 100) + ticks − 1
```

So with horizon **K**, a spike is masked with **no freeze** iff:

```
m + n − 1 ≤ K
```

| Spike (`ms × ticks`) | `m + n − 1` | K = 2 result |
|---|:---:|---|
| 200 × 1 | 2 | ✅ fully masked (Case A) |
| 200 × 2 *(default)* | 3 | ❌ one freeze + snap (Case B) |
| 200 × 3 | 4 | ❌ freeze |
| 300 × 2 | 4 | ❌ freeze |

The **+1** is structural: walker1 starts one animation-step behind the frontier
(`felt lag = 1` even at zero latency), so it has that extra step to make up before
its predictions can hold the felt lag flat. Both **magnitude and duration** feed
the requirement — `architecture.md`'s `K = RTT/tick = 2` is the *latency-match*
count, not the *no-freeze* count, which is one higher.

> With the current single-shot trigger (predict K from the frozen frontier, then
> wait for `pending` to empty), `200 ms × 1 tick` is the **only** spike K = 2 masks
> with zero freeze — it is the headline reproducible demo.

---

## The rollback variant (prediction wrong)

If a predicted step turns out wrong — say a step-11 guess:

- When move 11 flushes in, verification fails → `rollback_to(11)` → **walker1 snaps
  back** from the wrong cell and re-renders the real step 11.
- On screen: the snake head briefly moves the wrong way, then is **visibly
  corrected** ~100 ms later. `ahead` drops for that tick, then prediction restarts.

This is the visible cost of a wrong guess — kept rare by only predicting in
constrained (high-confidence) states.

---

## Two preconditions

1. **Constraint gate open** — both snakes must be constrained (a wall / boundary /
   body next to the head). In open space prediction does **not** fire; walker1
   freezes with walker2 and the spike degrades to a brief **freeze + snap** instead
   of a glide.
2. **Detection delay** — prediction only fires once a move is overdue by
   `OVERDUE_MS` (≈ 150 ms, 1.5 ticks). This is what keeps zero latency silent: a
   normal 100 ms inter-arrival never crosses the threshold, only a genuine gap
   does.

---

## Design extension — rolling top-up and the depth limit *(future work)*

The single-shot trigger above is limited by **both** axes (`m + n − 1 ≤ K`). A
**rolling top-up** variant would re-predict one step from **each newly confirmed
frontier** as moves verify (re-anchored on confirmed truth — **not**
autoregressive, so error stays bounded), keeping a rolling K-step lead instead of
gliding to the cap and freezing. Its no-freeze condition collapses to **depth
only**:

```
single-shot:    freeze  ⟺  m + n − 1 > K     (depth and duration both bite)
rolling top-up: freeze  ⟺  m > K             (duration fully absorbed)
```

i.e. rolling top-up masks a spike of **any duration** as long as its **depth ≤ K**
(≤ 200 ms for K = 2). It does **not** beat depth `> K`, because during the initial
gap **no new truth arrives** to re-anchor from — masking depth `d` requires
correctly predicting `d` *unobserved* future steps, which is a **predictability**
limit, not an engineering one. In this game predictability ≈ how constrained the
state is, so going deeper would mean either a deeper single-pass output (a higher
fixed ceiling) or a confidence-gated autoregressive rollout (dynamic depth at the
cost of compounding error). Both are scoped as **future work**; the implementation
wrinkle for rolling top-up itself is a reliable *"recovered → stop topping up"*
signal, which route-B (no measured RTT) cannot derive from inter-arrival timing
alone.

---

## Cross-reference

- The `ahead` metric and the dashboard panels (Prediction Trace, Prediction Stats,
  Sync Monitor) are described in [`architecture.md`](./architecture.md).
- Clock-driven (route-B) prediction fires on **move lateness**, not measured RTT —
  see the prediction trigger conditions in `PlaygroundIndexView.vue` (`tryPredict`).
