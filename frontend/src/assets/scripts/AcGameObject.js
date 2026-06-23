const AC_GAME_OBJECTS = [];

export class AcGameObject {
    constructor() {
        AC_GAME_OBJECTS.push(this);
        this.has_called_start = false; // whether start() has been called
        this.timedelta = 0; // time difference between current frame and previous frame
    }

    start() { // execute once

    }

    update() { // execute each frame except the first one

    }

    on_destroy() { // execute before deleting the object
        
    }

    destroy() { // delete the object
        this.on_destroy();

        for (let i in AC_GAME_OBJECTS) {
            const obj = AC_GAME_OBJECTS[i];
            if (obj === this) {
                AC_GAME_OBJECTS.splice(i, 1);
                break;
            }
        }
    }
}

let last_timestamp; // timestamp of the previous frame
let raf_id;         // id of the currently scheduled frame

const step = timestamp => {
    for (let obj of AC_GAME_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            obj.timedelta = timestamp - last_timestamp;
            obj.update();
        }
    }
    last_timestamp = timestamp;
    raf_id = requestAnimationFrame(step);
}

raf_id = requestAnimationFrame(step);

// Hot-reload guard (dev only): cancel this module's render loop before a hot
// update starts a new one, otherwise overlapping rAF loops accumulate and tank
// fps. No effect in production builds (module.hot is undefined).
if (module.hot) {
    module.hot.dispose(() => cancelAnimationFrame(raf_id));
}