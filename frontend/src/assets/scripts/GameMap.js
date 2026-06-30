import { AcGameObject } from "./AcGameObject";
import { Snake } from "./Snake";
import { Wall } from "./Wall";
import { Apple } from "./Apple";

const HISTORY_LIMIT = 6;

// Discrete snapshot of a snake. Taken only while the snake is idle, so pixel
// positions (x,y) coincide with grid positions and the snapshot is exact.
export function snapSnake(snake) {
    return {
        cells:         snake.cells.map(c => ({ r: c.r, c: c.c, x: c.x, y: c.y })),
        step:          snake.step,
        eye_direction: snake.eye_direction
    };
}

export function restoreSnake(snake, snap) {
    snake.cells           = snap.cells.map(c => ({ r: c.r, c: c.c, x: c.x, y: c.y }));
    snake.next_cell       = null;
    snake.status          = "idle";
    snake.direction       = -1;
    snake.direction_queue = [];
    snake.step            = snap.step;
    snake.eye_direction   = snap.eye_direction;
    snake.eat_apple       = false;
}

export class GameMap extends AcGameObject {
    constructor(ctx, parent, store) {
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.store = store;
        this.L = 0; // length of one grid

        this.rows = 25;
        this.cols = 28;

        this.walls = [];

        this.snakes = [
            new Snake({id: 0, color: "#4876EC", r: this.rows - 2, c: 1}, this),
            new Snake({id: 1, color: "#F50057", r: 1, c: this.cols - 2}, this)
        ];

        this.apple = null;

        // Ring buffer of pre-advance snapshots, tagged with the step each advance
        // produced. Used by rollback_to() to undo mispredicted moves.
        this.history = [];

        // Largest backlog that is still animated smoothly; anything beyond it is
        // fast-forwarded (instant_step) so the render snaps to the newest server
        // state. The view sets this per tick: high while predicting (so the
        // predicted lookahead lead animates), low when silent (so a confirmed
        // backlog from a finished latency spike catches up instantly).
        this.maxLag = 3;
    }

    create_walls() {
        const g = this.store.state.playground.gamemap;
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c] === 1) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }
    }

    create_apple() {
        const g = this.store.state.playground.gamemap;
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c] === 2) {
                    this.apple = new Apple(r, c, this);
                    return;
                }
            }
        }
    }

    add_listening_events() {
        this.ctx.canvas.focus();

        this.ctx.canvas.addEventListener("keydown", e => {
            let d = -1;
            if (e.key === "w") d = 0;
            else if (e.key === "d") d = 1;
            else if (e.key === "s") d = 2;
            else if (e.key === "a") d = 3;
            if (d >= 0) {
                this.store.state.playground.socket.send(JSON.stringify({
                    event: "move",
                    direction: d,
                }));
            }
        });
    }

    start() {
        this.create_walls();
        this.create_apple();
        this.add_listening_events();
    }

    update_size() {
        this.L = parseInt(
            Math.min(
                this.parent.clientWidth / this.cols,
                this.parent.clientHeight / this.rows,
            ),
        );
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    check_ready() {
        for (const snake of this.snakes) {
            if (snake.status !== "idle") return false;
            if (snake.direction_queue.length === 0) return false;
        }
        return true;
    }

    next_step() {
        // Snapshot the pre-advance state for potential rollback. Both snakes are
        // idle here (guaranteed by check_ready), so the snapshot is exact.
        const targetStep = this.snakes[0].step + 1;
        this.history.push({ targetStep, snaps: this.snakes.map(snapSnake) });
        if (this.history.length > HISTORY_LIMIT) this.history.shift();

        for (const snake of this.snakes) {
            const move = snake.direction_queue.shift();
            snake.direction = move.dir;
            snake.eat_apple = move.eat;
        }
        for (const snake of this.snakes) snake.next_step();
    }

    // Undo the move that produced `targetStep` (and everything after it): restore
    // both snakes to their pre-`targetStep` state and clear pending moves.
    // Returns true if a matching snapshot was found.
    rollback_to(targetStep) {
        const idx = this.history.findIndex(h => h.targetStep === targetStep);
        if (idx === -1) return false;
        const h = this.history[idx];
        restoreSnake(this.snakes[0], h.snaps[0]);
        restoreSnake(this.snakes[1], h.snaps[1]);
        this.history.length = idx; // drop this snapshot and all later ones
        return true;
    }

    update() {
        this.update_size();
        this.advance();
        this.render();
    }

    // Drain the move queue. When the backlog exceeds MAX_LAG (we have fallen
    // behind the server tick rate, e.g. on an fps dip), fast-forward the oldest
    // confirmed moves with no animation so rendering catches up; always leave a
    // small lead (the prediction lookahead) to animate smoothly.
    advance() {
        // Legitimate backlog = the predicted lookahead lead + 1 in-flight real
        // move. Only fast-forward when genuinely behind that, so a normal
        // prediction burst animates smoothly instead of teleporting. When silent
        // the budget drops (see maxLag) so a spike's flushed backlog snaps.
        const MAX_LAG = this.maxLag;
        while (this.check_ready()) {
            const backlog = Math.min(
                this.snakes[0].direction_queue.length,
                this.snakes[1].direction_queue.length
            );
            if (backlog > MAX_LAG) {
                this.instant_step();
            } else {
                this.next_step();
                break; // one animated step per frame
            }
        }
    }

    instant_step() {
        const targetStep = this.snakes[0].step + 1;
        this.history.push({ targetStep, snaps: this.snakes.map(snapSnake) });
        if (this.history.length > HISTORY_LIMIT) this.history.shift();

        for (const snake of this.snakes) {
            const move = snake.direction_queue.shift();
            snake.apply_step_instant(move.dir, move.eat);
        }
    }

    destroy() {
        for (const snake of this.snakes) snake.destroy();
        for (const wall of this.walls) wall.destroy();
        if (this.apple) this.apple.destroy();
        super.destroy();
    }

    render() {
        const color_even = "#AAD751",
            color_odd = "#A2D149";
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if ((r + c) % 2 === 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(c * this.L, r * this.L, this.L, this.L);
            }
        }
    }
}