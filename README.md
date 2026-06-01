# Edge-AI Latency Compensation Testbed

A research testbed for studying and mitigating the effects of network latency in real-time multiplayer web applications, using a two-player snake game as the experimental platform.

## Research Background

In highly interactive web systems (e.g. real-time multiplayer games, high-frequency collaboration tools), network latency causes severe visual jitter and frequent server-side state rollbacks, significantly degrading user experience. The traditional approach — **Dead Reckoning** — predicts future positions from object inertia and works well for simple linear motion, but fails under complex, non-linear game logic or sudden state changes.

This project investigates a **Sequence-to-Sequence (Seq2Seq) Machine Learning** architecture deployed as a context-aware, lightweight Edge-AI model running directly in the user's browser (Edge Computing), to predict player trajectories and mask perceived network latency without transmitting sensitive data to the server.

## Research Objectives

1. **Testbed Implementation** — Build a real-time two-player snake environment (13×14 grid, obstacles, apple rewards) as a data generator and visual demonstration platform.
2. **Context-Aware Hybrid Trigger Mechanism** — Design a heuristic trigger based on spatial constraints (e.g. proximity to apples or obstacles) to invoke the AI model on-demand.
3. **Seq2Seq Lightweight Model** — Train a GRU/LSTM model optimised for time-series trajectory prediction, export to ONNX format for in-browser deployment via WebAssembly.
4. **Benchmarking & Evaluation** — Compare the proposed architecture against baseline Dead Reckoning across three metrics: Spatial Accuracy (MAE), Visual Smoothness (rollback frequency & magnitude), and Computational Efficiency (client-side ONNX inference latency).

## Architecture

The testbed adopts a **strictly decoupled client-server architecture**:

```
┌─────────────────────────────────────────────────────────┐
│                     Browser (Client)                     │
│                                                         │
│  Vue.js UI  ──►  Game Rendering (Canvas)                │
│                       │                                 │
│              [ONNX Runtime / Edge-AI Model]             │
│                       │                                 │
│           WebSocket Client (JWT Auth)                   │
└───────────────────────┼─────────────────────────────────┘
                        │  WebSocket  (ws://host:3000)
┌───────────────────────┼─────────────────────────────────┐
│              Spring Boot Backend (Java)                  │
│                                                         │
│   WebSocket Server  ──►  Matchmaking Pool               │
│                       │                                 │
│                  Game Thread (Authoritative State)      │
│                       │                                 │
│          Spring Security + JWT  ──►  MySQL              │
└─────────────────────────────────────────────────────────┘
```

- The **server** is the single authoritative game state manager; all collision detection and game logic run here.
- The **client** renders state received from the server and will host the Edge-AI model to locally predict and smooth player positions between server updates.
- Real-time communication is handled over **WebSocket**, enabling low-latency state synchronisation and allowing artificial network latency injection for experiments.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 3.x |
| ORM | MyBatis-Plus |
| Database | MySQL |
| Authentication | Spring Security + JWT (JJWT) |
| Real-time Communication | Spring WebSocket (`@ServerEndpoint`) |
| Frontend Framework | Vue.js 3 + Vue Router + Vuex |
| UI | Bootstrap 5 + jQuery |
| HTTP Client | Axios |
| ML Training | PyTorch (GRU / LSTM) |
| ML Inference (planned) | ONNX Runtime Web (WebAssembly) |

## Game Rules

- The map is a **13 × 14 grid** with randomly placed obstacles.
- Two players each control a snake, starting from opposite ends of the map.
- Players move in real time by pressing direction keys.
- A player **loses** if their snake collides with the boundary, an obstacle, or the other player's body.
- The snake's length **grows automatically** over time (increasing collision risk).
- **Apple** rewards are placed on the map; eating an apple reduces the snake's length.

## Project Structure

```
Final-Project-Testbed/
├── backend/                        # Spring Boot application
│   └── src/main/java/.../
│       ├── config/                 # CORS, Security, WebSocket config
│       ├── consumer/
│       │   ├── WebSocketServer.java   # WebSocket endpoint & matchmaking
│       │   └── utils/
│       │       ├── Game.java          # Game loop thread (authoritative state)
│       │       ├── Player.java
│       │       └── Cell.java
│       ├── controller/             # REST API controllers
│       ├── service/                # Business logic (login, register, info)
│       ├── mapper/                 # MyBatis-Plus mappers
│       └── pojo/                   # Data models
├── frontend/                       # Vue.js application
│   └── src/
│       ├── assets/scripts/
│       │   ├── AcGameObject.js     # Base game object (game loop)
│       │   ├── GameMap.js          # Canvas rendering & wall generation
│       │   ├── Snake.js            # Client-side snake rendering
│       │   └── Cell.js / Wall.js
│       ├── components/
│       │   ├── GameMap.vue
│       │   ├── PlayGround.vue
│       │   ├── MatchGround.vue
│       │   └── ResultBoard.vue
│       ├── views/                  # Page-level views
│       ├── store/                  # Vuex state (user auth, playground)
│       └── router/                 # Vue Router routes
└── Project Plan.pdf
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 16+ & npm
- MySQL 8.x

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

Open `http://localhost:8080` in your browser. Register an account and start a match — two browser tabs (or two users) can connect simultaneously to test the multiplayer flow.

## WebSocket Protocol

The WebSocket endpoint is `ws://localhost:3000/websocket/{jwt_token}`.

| Direction | Event | Payload |
|---|---|---|
| Client → Server | `startMatching` / `stopMatching` | `{"event": "startMatching"}` |
| Server → Client | `matchSuccess` | player info, map seed |
| Client → Server | `move` | `{"event": "move", "direction": 0-3}` |
| Server → Client | `move` | authoritative positions for both players |
| Server → Client | `result` | `{"event": "result", "loser": 0/1}` |

## Research Roadmap

| Phase | Task | Status |
|---|---|---|
| 1 | Project Initiation & Literature Review | Done |
| 2 | Testbed Implementation (game + WebSocket + auth) | Done |
| 3 | Telemetry Data Collection (synthetic zero-latency traces) | Planned |
| 4 | GRU Model Training & Tuning (PyTorch) | Planned |
| 5 | ONNX Model Export & Web Integration | Planned |
| 6 | Context-Aware Trigger Implementation | Planned |
| 7 | Reconciliation & Conflict Logic | Planned |
| 8 | System Benchmarking & Evaluation (MAE, rollback rate, inference latency) | Planned |
| 9 | Dissertation Writing & Finalisation | Planned |

## Evaluation Metrics

| Metric | Description |
|---|---|
| **Spatial Accuracy (MAE)** | Mean Absolute Error between AI-predicted and ground-truth positions |
| **Visual Smoothness** | Frequency and magnitude of catastrophic state rollbacks |
| **Computational Efficiency** | Client-side ONNX runtime inference latency (ms) |

## Ethical & Privacy Notes

- All training data is **synthetically generated** from the testbed (coordinates, vectors, distances) — no human subjects or real user demographic data is involved.
- All AI inference runs **locally in the browser**; no trajectory data is transmitted to the server, satisfying privacy-by-design principles and GDPR compliance.
- The model relies solely on physical/spatial constraints, making it inherently free from demographic algorithmic bias.
