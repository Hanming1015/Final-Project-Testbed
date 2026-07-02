// Structured metrics logger for the latency-compensation evaluation.
//
// Philosophy: record RAW fields per event, faithfully. Derived metrics
// (felt-lag, coverage, per-horizon accuracy, masked/rollback rates) are
// computed OFFLINE from the exported tables — the testbed never aggregates by
// eye. Every row is stamped with the experiment context (game id, predictor
// mode, and the most recent spike) so a CSV can be filtered per condition.
//
// Tables:
//   ticks    — one row per delivered server move (anchors the server cadence)
//   renders  — one row per animation frame, throttled (rendered step over time;
//              the ONLY source that samples DURING a spike freeze, where no move
//              is delivered — needed to reconstruct felt-lag across the freeze)
//   verifies — one row per confirmed prediction (accuracy / masked / rollback)
//   overdues — one row per overdue episode (coverage: did the gate open?)
//   infers   — one row per model inference (feasibility: elapsed ms)
//
// Direction values are kept as raw indices (0=up,1=right,2=down,3=left).

const DIRC = ['up', 'right', 'down', 'left'];

export class MetricsLog {
    constructor() {
        this.reset();
    }

    reset() {
        this.ticks    = [];
        this.renders  = [];
        this.verifies = [];
        this.overdues = [];
        this.infers   = [];
        this._game    = 0;        // game counter (bumped each match)
        this._spike   = null;     // { ms, ticks, firedAtMove } — most recent spike
    }

    // New game: bump the id and forget the previous spike.
    newGame() {
        this._game++;
        this._spike = null;
    }

    // Record that a spike was armed at the given confirmed-move count, so later
    // events can carry `sinceSpike` (moves elapsed since it fired).
    markSpike(ms, ticks, atMove) {
        this._spike = { ms, ticks, firedAtMove: atMove };
    }

    // Common context stamped on every row. `M` = confirmed move count at the
    // event (null when not applicable, e.g. inference timing).
    _ctx(mode, M) {
        const s = this._spike;
        return {
            game:       this._game,
            t:          +performance.now().toFixed(1),
            mode,
            spikeMs:    s ? s.ms : 0,
            spikeTicks: s ? s.ticks : 0,
            sinceSpike: (s && M != null) ? M - s.firedAtMove : null
        };
    }

    // Per delivered server move. `step` = rendered step, `M` = confirmed move
    // count, `ahead` = step − M (felt-lag = injected latency − ahead, offline).
    logTick({ mode, step, M, ahead }) {
        this.ticks.push({ ...this._ctx(mode, M), step, M, ahead });
    }

    // Per animation frame (throttled). `step` = rendered step at this instant,
    // `M` = confirmed move count. This is w1(t); combined with the server cadence
    // fitted from `ticks`, felt-lag is reconstructed continuously — including the
    // freeze window, where `ticks` has no samples.
    logRender({ mode, step, M }) {
        this.renders.push({ ...this._ctx(mode, M), step, M });
    }

    // Per confirmed prediction. `horizon` = 1 for t+1, 2 for t+2.
    logVerify({ mode, step, horizon, aPred, aSrv, bPred, bSrv, okA, okB, onScreen }) {
        this.verifies.push({
            ...this._ctx(mode, step),
            step, horizon,
            aPred, aPredDir: DIRC[aPred], aSrv, aSrvDir: DIRC[aSrv],
            bPred, bPredDir: DIRC[bPred], bSrv, bSrvDir: DIRC[bSrv],
            okA, okB, correct: okA && okB, onScreen
        });
    }

    // Per overdue episode (a late move). `gateOpen` = both snakes constrained →
    // coverage = fraction of overdue episodes with gateOpen true.
    logOverdue({ mode, M, gateOpen }) {
        this.overdues.push({ ...this._ctx(mode, M), M, gateOpen });
    }

    // Per model inference — total sequential A+B elapsed ms (feasibility).
    logInfer({ mode, ms }) {
        this.infers.push({ ...this._ctx(mode, null), ms: +ms.toFixed(2) });
    }

    counts() {
        return {
            games:    this._game,
            ticks:    this.ticks.length,
            renders:  this.renders.length,
            verifies: this.verifies.length,
            overdues: this.overdues.length,
            infers:   this.infers.length
        };
    }

    // ── Export ────────────────────────────────────────────────────────────────

    _rowsToCsv(rows) {
        if (rows.length === 0) return '';
        const cols = Object.keys(rows[0]);
        const esc  = v => (v == null ? '' : String(v));
        const head = cols.join(',');
        const body = rows.map(r => cols.map(c => esc(r[c])).join(',')).join('\n');
        return `${head}\n${body}\n`;
    }

    toCsv(table) {
        const rows = this[table];
        if (!Array.isArray(rows)) throw new Error(`unknown table: ${table}`);
        return this._rowsToCsv(rows);
    }

    toJson() {
        return JSON.stringify({
            exportedAt: new Date().toISOString(),
            counts:     this.counts(),
            ticks:      this.ticks,
            renders:    this.renders,
            verifies:   this.verifies,
            overdues:   this.overdues,
            infers:     this.infers
        }, null, 2);
    }
}
