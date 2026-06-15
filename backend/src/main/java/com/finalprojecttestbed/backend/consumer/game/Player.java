package com.finalprojecttestbed.backend.consumer.game;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class Player {
    private String id;
    private Integer sx;
    private Integer sy;
    private List<Integer> steps;
    private List<Integer> appleEatenSteps;

    public Player(String id, Integer sx, Integer sy, List<Integer> steps) {
        this.id = id;
        this.sx = sx;
        this.sy = sy;
        this.steps = steps;
        this.appleEatenSteps = new ArrayList<>();
    }

    private boolean checkTailIncreasing(int step) {
        if (step <= 10) return true;
        return step % 3 == 1;
    }

    public void decreaseTail(int step) {
        appleEatenSteps.add(step);
        appleEatenSteps.add(step);
    }

    public List<Cell> getCells() {
        List<Cell> res = new ArrayList<>();
        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        int x = sx, y = sy;
        int step = 0;
        res.add(new Cell(x, y));
        for (int d : steps) {
            x += dx[d];
            y += dy[d];
            res.add(new Cell(x, y));
            if (!checkTailIncreasing(++step)) {
                res.remove(0);
            }
            int freq = Collections.frequency(appleEatenSteps, step);
            for (int i = 0; i < freq && res.size() > 1; i++) {
                res.remove(0);
            }
        }
        return res;
    }

    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        for (int d : steps) {
            res.append(d);
        }
        return res.toString();
    }
}
