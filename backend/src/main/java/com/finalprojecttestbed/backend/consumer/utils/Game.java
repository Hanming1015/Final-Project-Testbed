package com.finalprojecttestbed.backend.consumer.utils;

import com.alibaba.fastjson.JSONObject;
import com.finalprojecttestbed.backend.consumer.WebSocketServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread{
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_walls_count;
    private final int[][] g;

    public final static int[] dx = {-1, 0, 1, 0};
    public final static int[] dy = {0, 1, 0, -1};

    private final Player playerA, playerB;

    private Integer nextStepA = 0;  // A down
    private Integer nextStepB = 2;  // B up

    private ReentrantLock lock = new ReentrantLock();
    private String status = "playing";
    private String loser = "";

    public Game(Integer rows, Integer cols, Integer inner_walls_count, String idA, String idB) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
        this.playerA = new Player(idA, rows - 2, 1, new ArrayList<>());
        this.playerB = new Player(idB, 1, cols - 2, new ArrayList<>());
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public int[][] getG() {
        return g;
    }

    private boolean check_connectivity(int sx, int sy, int tx, int ty) {
        if(sx == tx && sy == ty) return true;
        g[sx][sy] = 1;
        for(int i = 0; i < 4; i ++) {
            int x = sx + dx[i], y = sy + dy[i];
            if(x < 0 || x >= this.rows || y < 0 || y >= this.cols) continue;
            if(g[x][y] == 1) continue;
            if(check_connectivity(x, y, tx, ty)) {
                g[sx][sy] = 0;
                return true;
            }
        }
        g[sx][sy] = 0;
        return false;
    }

    private boolean draw() {  // 画地图
        for(int i = 0; i < this.rows; i ++) {
            for(int j = 0; j < this.cols; j ++) {
                g[i][j] = 0;
            }
        }
        for(int r = 0; r < this.rows; r ++){
            g[r][0] = g[r][this.cols - 1] = 1;
        }
        for(int c = 0; c < this.cols; c ++) {
            g[0][c] = g[this.rows - 1][c] = 1;
        }
        Random random = new Random();
        for(int i = 0; i < this.inner_walls_count / 2; i ++) {
            for(int j = 0; j < 1000; j ++) {
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);
                if(g[r][c] == 1 || g[this.rows - r - 1][this.cols - c - 1] == 1) {
                    continue;
                }
                if(r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2){
                    continue;
                }
                g[r][c] = g[this.rows - r - 1][this.cols - c - 1] = 1;
                break;
            }
        }
        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2);
    }

    public void createMap() {
        for(int i = 0; i < 1000; i ++) {
            if(draw()) {
                break;
            }
        }
    }

    private void nextStep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        lock.lock();
        try {
            playerA.getSteps().add(nextStepA);
            playerB.getSteps().add(nextStepB);
        } finally {
            lock.unlock();
        }
    }

    private boolean checkValid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if (g[cell.getX()][cell.getY()] == 1) return false;

        for (int i = 0; i < n - 1; i++) {
            if (cellsA.get(i).getX() == cell.getX() && cellsA.get(i).getY() == cell.getY()) {
                return false;
            }
        }

        for (int i = 0; i < n - 1; i ++ ) {
            if (cellsB.get(i).getX() == cell.getX() && cellsB.get(i).getY() == cell.getY()) {
                return false;
            }
        }
        return true;
    }

    private void judge() {
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean validA = checkValid(cellsA, cellsB);
        boolean validB = checkValid(cellsB, cellsA);
        if (!validA || !validB) {
            status = "finished";

            if (!validA && !validB) {
                loser = "all";
            } else if (!validA) {
                loser = "A";
            } else {
                loser = "B";
            }
        }
    }

    private void sendAllMessage(String message) {
        WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        WebSocketServer.users.get(playerB.getId()).sendMessage(message);
    }

    private void sendMove() {
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            resp.put("a_direction", nextStepA);
            resp.put("b_direction", nextStepB);
            sendAllMessage(resp.toJSONString());
        } finally {
            lock.unlock();
        }
    }

    private void sendResult() {
        JSONObject resp = new JSONObject();
        resp.put("event", "result");
        resp.put("loser", loser);
        sendAllMessage(resp.toJSONString());
    }

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            nextStep();
            judge();
            if (status.equals("playing")) {
                sendMove();
            } else {
                sendResult();
                break;
            }
        }
    }
}
