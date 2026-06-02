import { AcGameObject } from "./AcGameObject";
import { Snake } from "./Snake";
import { Wall } from "./Wall";
import { Apple } from "./Apple";

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
            if (snake.direction === -1) return false;
        }
        return true;
    }

    next_step() {
        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    update() {
        this.update_size();
        if (this.check_ready()) {
            this.next_step();
        }
        this.render();
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