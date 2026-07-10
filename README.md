# Edge-AI Latency Compensation Testbed

A research testbed for studying and mitigating the effects of network latency in real-time multiplayer web applications, using a two-player snake game as the experimental platform. A **GRU direct multi-step predictor** (a recurrent sequence encoder with a fixed K-step output head) is trained on bot self-play, exported to **ONNX**, and run **in the browser** to predict player trajectories and mask perceived latency.

## Research Background

In highly interactive web systems (e.g. real-time multiplayer games, high-frequency collaboration tools), network latency causes severe visual jitter and frequent server-side state rollbacks, significantly degrading user experience. The traditional approach — **Dead Reckoning** — predicts future positions from object inertia and works well for simple linear motion, but fails under complex, non-linear game logic or sudden state changes.

This project investigates a **learned recurrent sequence model** — a GRU encoder with a **direct multi-step** output head (all K future steps produced in a single forward pass, rather than an autoregressive decoder) — deployed as a context-aware, lightweight Edge-AI model running directly in the user's browser (Edge Computing), to predict player trajectories and mask perceived network latency without transmitting sensitive data to the server. The central research question:

> **Can a learned sequence model (a GRU direct multi-step predictor) mask client-side latency better than hand-written rule-based dead-reckoning, and how does that advantage depend on the latency spike?**

## Research Objectives

1. **Testbed Implementation** — Build a real-time two-player snake environment (25×28 grid, 40 obstacles, apple rewards) as a data generator and visual demonstration platform. ✅
2. **Context-Aware Trigger Mechanism** — A **clock-driven** trigger ("route-B"): the client predicts only when a confirmed server move is **overdue**, and only through a **constraint gate** (fires when both snakes are boxed in, where prediction is both useful and reliable). ✅
3. **Lightweight Sequence Model** — Train a GRU model on egocentric trajectory features, exported to ONNX for in-browser deployment via `onnxruntime-web` (WebAssembly). A single forward pass emits **K = 2** future steps via two output heads (t+1, t+2); **each snake is predicted by its own egocentric inference** (two predictor instances), not by one model outputting both. ✅
4. **Benchmarking & Evaluation** — Compare three predictor modes (`off` / `rule` / `model`) across a latency-spike sweep, on **felt-lag reduction** (headline), **prediction accuracy** (by horizon / snake), **masked vs visible-rollback rate**, **coverage**, and **inference time**. ✅ instrumented; data collection in progress.

See [`docs/evaluation-plan.md`](docs/evaluation-plan.md) for the full evaluation design and [`analysis/`](analysis/) for the offline analysis pipeline.

## Architecture

The testbed adopts a **strictly decoupled, server-authoritative client-server architecture** — this separation is what *creates* the latency gap the system compensates.

```
┌─────────────────────────────────────────────────────────┐
│                     Browser (Client)                     │
│                                                          │
│  Vue 3 UI  ──►  Game Rendering (queue engine + history)  │
│                       │                                  │
│         [ONNX Runtime Web — GRU predictor A / B]         │
│           predict up to K=2 steps, verify, rollback      │
│                       │                                  │
│           WebSocket Client (JWT Auth) + ping-pong        │
└───────────────────────┼──────────────────────────────────┘
                        │  WebSocket  (ws://host:3000)
┌───────────────────────┼──────────────────────────────────┐
│              Spring Boot Backend (Java)                  │
│                                                          │
│   WebSocket Server  ──►  Matchmaking / Bot Games         │
│                       │                                  │
│              Game Thread (100 ms authoritative tick)     │
│                       │                                  │
│          Spring Security + JWT  ──►  MySQL               │
└──────────────────────────────────────────────────────────┘
```

- The **server** is the single authoritative game state manager; all collision detection and game logic run here on a 100 ms tick.
- The **client** renders confirmed server state and runs the Edge-AI model to locally predict and smooth positions between server updates, verifying every prediction against the next real move and rolling back on mismatch.
- Real-time communication is handled over **WebSocket**, enabling low-latency state synchronisation and allowing artificial latency **spike** injection for experiments.

For the full prediction/reconciliation loop, the dashboard instrumentation, and the three-walker `felt-lag` model, see [`docs/architecture.md`](docs/architecture.md) and [`docs/walker-model.md`](docs/walker-model.md).

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 3.x |
| ORM | MyBatis-Plus |
| Database | MySQL |
| Authentication | Spring Security + JWT (JJWT) |
| Real-time Communication | Spring WebSocket |
| Frontend Framework | Vue.js 3 + Vue Router + Vuex |
| UI | Bootstrap 5 + jQuery |
| HTTP Client | Axios |
| ML Training | PyTorch (GRU encoder + direct multi-step head) |
| ML Inference | ONNX Runtime Web (`onnxruntime-web`, WebAssembly) |
| Offline Analysis | Python (pandas / numpy / matplotlib) |

## Game Rules

- The map is a **25 × 28 grid** with **40** randomly placed inner obstacles.
- Two snakes start from opposite ends of the map and move once per 100 ms server tick.
- A snake **loses** if it collides with the boundary, an obstacle, or the other snake's body.
- The snake's length **grows automatically** over time (increasing collision risk); eating an **apple** reduces its length.
- Modes: **Player vs Bot** and **Bot vs Bot** (spectate). Evaluation is run **bot-vs-bot** — both snakes are driven by the server-side bot, giving reproducible, fully-observable conditions.

## Project Structure

```
Final-Project-Testbed/
├── backend/                        # Spring Boot application
│   └── src/main/java/.../
│       ├── config/                 # CORS, Security, WebSocket config
│       ├── consumer/
│       │   ├── WebSocketServer.java   # WebSocket endpoint & matchmaking
│       │   └── game/
│       │       ├── Game.java          # Game loop thread (authoritative state)
│       │       ├── Player.java / Cell.java
│       │       └── ...                # bot logic, map generation
│       ├── controller/             # REST API controllers
│       ├── service/                # Business logic (login, register, info)
│       ├── mapper/                 # MyBatis-Plus mappers
│       └── pojo/                   # Data models
├── frontend/                       # Vue.js application
│   ├── public/models/model.onnx    # exported GRU model (loaded at runtime)
│   └── src/
│       ├── assets/scripts/
│       │   ├── AcGameObject.js     # Base game object (game loop)
│       │   ├── GameMap.js          # Canvas rendering & wall generation
│       │   ├── Snake.js            # Client-side snake (queue engine + rollback)
│       │   ├── SnakePredictor.js   # ONNX inference + egocentric features
│       │   └── MetricsLog.js       # Structured evaluation logger (CSV/JSON)
│       ├── components/
│       │   ├── GameMap.vue / PlayGround.vue
│       │   ├── MatchGround.vue / ResultBoard.vue
│       │   ├── SpikeInjector.vue   # INPUT  — inject latency spike (+ auto-spike)
│       │   ├── ModelToggle.vue     # INPUT  — cycle off / rule / model
│       │   ├── MetricsExport.vue   # OUTPUT — export metrics (CSV / JSON)
│       │   ├── PredictionStats.vue # OUTPUT — live accuracy / masked / rollback
│       │   ├── PredictionTrace.vue # OUTPUT — per-event predict / verify log
│       │   └── SyncMonitor.vue     # OUTPUT — client↔server sync (`ahead`)
│       ├── views/playground/       # Playground page (prediction/reconciliation loop)
│       ├── store/                  # Vuex state (user auth, playground)
│       └── router/                 # Vue Router routes
├── docs/                           # Design docs (architecture, walker model, eval plan)
├── analysis/                       # Offline analysis (analyze.py + README)
└── Project Plan.pdf
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 16+ & npm
- MySQL 8.x
- Python 3.10+ (offline analysis only)

### Backend Setup

```bash
# 1. Create the database
mysql -u root -p -e "CREATE DATABASE testbed CHARACTER SET utf8mb4;"

# 2. Configure credentials
#    Edit backend/src/main/resources/application.properties
#    spring.datasource.url / username / password

# 3. Build & run
cd backend
mvn spring-boot:run
# Server starts on port 3000
```

### Frontend Setup

```bash
cd frontend
npm install
npm run serve
# Dev server starts on port 8080
```

### Access

Open `http://localhost:8080` in your browser. Register an account, then start a **Bot vs Bot** match to watch the prediction layer compensate an injected latency spike, or **Player vs Bot** to play manually.

## WebSocket Protocol

The WebSocket endpoint is `ws://localhost:3000/websocket/{jwt_token}`.

| Direction | Event | Notes |
|---|---|---|
| Client → Server | `start-matching` / `stop-matching` | join / leave the human matchmaking pool |
| Client → Server | `start-bot-game` | Player vs Bot |
| Client → Server | `start-watch-game` | Bot vs Bot (spectate) — used for evaluation |
| Client → Server | `move` | `{"event": "move", "direction": 0-3}` (human play) |
| Client → Server | `ping` | latency probe (server replies `pong`) |
| Server → Client | `matching-success` | opponent info + map seed |
| Server → Client | `move` | authoritative directions + apple + ate flags for **both** snakes |
| Server → Client | `result` | `{"event": "result", "loser": "A" \| "B" \| "all"}` |

## Evaluation

Three predictor modes are compared under a latency-**spike** sweep (magnitude `m` × duration `n` ticks):

- **`off`** — no prediction (freeze + snap baseline).
- **`rule`** — dead-reckoning: repeat the last confirmed direction (through the same gate / verify / rollback).
- **`model`** — the GRU.

| Tier | Metric | Definition |
|---|---|---|
| 1 — Accuracy | Direction accuracy, **by horizon** (t+1 / t+2) & by snake | predicted dir == server dir |
| 1 — Accuracy | **Coverage** | fraction of overdue episodes where the constraint gate fired |
| 2 — Effectiveness | **Felt-lag** (freeze-peak — headline) | `felt lag = latency − ahead`, measured at the worst instant of each freeze window |
| 2 — Effectiveness | **Masked rate** / **Visible-rollback rate** | correct-&-on-screen (benefit) vs wrong-&-on-screen (cost) |
| 3 — Feasibility | **Inference time** (mean / max vs 100 ms budget) | client-side ONNX latency |

Metrics are logged raw in-browser (`MetricsLog.js` → `MetricsExport` panel → CSV/JSON) and aggregated **offline** (`analysis/analyze.py`, mean ± 95% CI across independent games). The headline figure plots freeze-peak felt-lag vs spike size with one curve per mode. See [`docs/evaluation-plan.md`](docs/evaluation-plan.md) and [`analysis/README.md`](analysis/README.md).

## Research Roadmap

| Phase | Task | Status |
|---|---|---|
| 1 | Project Initiation & Literature Review | ✅ Done |
| 2 | Testbed Implementation (game + WebSocket + auth) | ✅ Done |
| 3 | Telemetry Data Collection (bot self-play traces) | ✅ Done |
| 4 | GRU Model Training & Tuning (PyTorch) | ✅ Done |
| 5 | ONNX Model Export & Web Integration | ✅ Done |
| 6 | Context-Aware Trigger (constraint gate + clock-driven route-B) | ✅ Done |
| 7 | Reconciliation & Rollback Logic | ✅ Done |
| 8 | System Benchmarking & Evaluation | 🔄 Instrumented; data collection in progress |
| 9 | Dissertation Writing & Finalisation | 🔄 In progress |

## Ethical & Privacy Notes

- All training and evaluation data is **synthetically generated** from bot self-play in the testbed (coordinates, vectors, distances) — no human subjects or real user demographic data is involved. Human-trace collection is acknowledged as future work.
- All AI inference runs **locally in the browser**; no trajectory data is transmitted to the server, satisfying privacy-by-design principles and GDPR compliance.
- The model relies solely on physical/spatial constraints, making it inherently free from demographic algorithmic bias.
