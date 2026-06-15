package com.finalprojecttestbed.backend.consumer.game;

import com.alibaba.fastjson.JSONObject;
import com.finalprojecttestbed.backend.consumer.WebSocketServer;
import com.finalprojecttestbed.backend.consumer.bot.Bot;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread{
    private final Integer rows;
    private final Integer cols;
    private final Integer innerWallsCount;
    private final int[][] g;

    public final static int[] dx = {-1, 0, 1, 0};
    public final static int[] dy = {0, 1, 0, -1};

    private final Player playerA, playerB;
    private final boolean botA, botB;
    private final Bot bot;

    private Integer nextStepA = 0;  // A default up
    private Integer nextStepB = 2;  // B default down

    private ReentrantLock lock = new ReentrantLock();
    private String status = "playing";
    private String loser = "";

    private int appleR, appleC;
    private boolean ateAppleA, ateAppleB;

    private final String gameId = UUID.randomUUID().toString().substring(0, 8);
    private final List<String[]> telemetryBuffer; // [playerId, rowData]

    public Game(Integer rows, Integer cols, Integer innerWallsCount, String idA, String idB) {
        this(rows, cols, innerWallsCount, idA, idB, false, false);
    }

    public Game(Integer rows, Integer cols, Integer innerWallsCount,
                String idA, String idB, boolean botA, boolean botB) {
        this.rows = rows;
        this.cols = cols;
        this.innerWallsCount = innerWallsCount;
        this.g = new int[rows][cols];
        this.botA = botA;
        this.botB = botB;
        this.bot = (botA || botB) ? new Bot(rows, cols) : null;
        this.playerA = new Player(idA, rows - 2, 1, new ArrayList<>());
        this.playerB = new Player(idB, 1, cols - 2, new ArrayList<>());
        // Only collect telemetry for bot games to keep training data clean
        this.telemetryBuffer = (botA || botB) ? new ArrayList<>() : null;
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

    private boolean checkConnectivity(int sx, int sy, int tx, int ty) {
        if(sx == tx && sy == ty) return true;
        g[sx][sy] = 1;
        for(int i = 0; i < 4; i ++) {
            int x = sx + dx[i], y = sy + dy[i];
            if(x < 0 || x >= this.rows || y < 0 || y >= this.cols) continue;
            if(g[x][y] == 1) continue;
            if(checkConnectivity(x, y, tx, ty)) {
                g[sx][sy] = 0;
                return true;
            }
        }
        g[sx][sy] = 0;
        return false;
    }

    private boolean draw() {  // draw map
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
        for(int i = 0; i < this.innerWallsCount / 2; i ++) {
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

        if (!checkConnectivity(this.rows - 2, 1, 1, this.cols - 2)) return false;

        // init apple after connectivity check to avoid DFS overwriting g[r][c]=2
        for (int i = 0; i < 1000; i ++) {
            int r = random.nextInt(this.rows);
            int c = random.nextInt(this.cols);
            if (g[r][c] == 1) continue;
            if (r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2) continue;
            g[r][c] = 2;
            appleR = r;
            appleC = c;
            return true;
        }
        return true;
    }

    public void createMap() {
        for(int i = 0; i < 1000; i ++) {
            if(draw()) {
                break;
            }
        }
    }

    private void nextStep() {
        if (!botA || !botB) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (botA) nextStepA = bot.computeDirection(nextStepA, g,
                playerA.getCells(), playerB.getCells(), appleR, appleC);
        if (botB) nextStepB = bot.computeDirection(nextStepB, g,
                playerB.getCells(), playerA.getCells(), appleR, appleC);
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

        for (int i = 0; i < cellsB.size(); i++) {
            if (cellsB.get(i).getX() == cell.getX() && cellsB.get(i).getY() == cell.getY()) {
                return false;
            }
        }
        return true;
    }

    private boolean checkApple(List<Cell> cells) {
        int n = cells.size();
        Cell cell = cells.get(n - 1);

        if (g[cell.getX()][cell.getY()] == 2) return true;
        return false;
    }

    private int[] computeGrid(Player self, Player opponent) {
        List<Cell> selfCells = self.getCells();
        List<Cell> oppCells = opponent.getCells();
        Cell head = selfCells.get(selfCells.size() - 1);
        int hr = head.getX(), hc = head.getY();

        int[] grid = new int[25];
        for (int dr = -2; dr <= 2; dr++) {
            for (int dc = -2; dc <= 2; dc++) {
                int r = hr + dr, c = hc + dc;
                int idx = (dr + 2) * 5 + (dc + 2);
                if (r < 0 || r >= rows || c < 0 || c >= cols || g[r][c] == 1) {
                    grid[idx] = 1;
                }
            }
        }
        for (int i = 0; i < selfCells.size() - 1; i++) {
            Cell cell = selfCells.get(i);
            int dr = cell.getX() - hr, dc = cell.getY() - hc;
            if (dr >= -2 && dr <= 2 && dc >= -2 && dc <= 2)
                grid[(dr + 2) * 5 + (dc + 2)] = 1;
        }
        for (Cell cell : oppCells) {
            int dr = cell.getX() - hr, dc = cell.getY() - hc;
            if (dr >= -2 && dr <= 2 && dc >= -2 && dc <= 2)
                grid[(dr + 2) * 5 + (dc + 2)] = 1;
        }
        grid[12] = 0; // head center is always 0
        return grid;
    }

    private void collectTelemetry(Player self, Player opponent, int direction, boolean ateApple) {
        if (telemetryBuffer == null) return;
        List<Cell> cells = self.getCells();
        Cell head = cells.get(cells.size() - 1);
        int hr = head.getX(), hc = head.getY();

        int[] grid = computeGrid(self, opponent);
        double norm = Math.max(rows, cols);
        double apple_dr = (appleR - hr) / norm;
        double apple_dc = (appleC - hc) / norm;

        StringBuilder sb = new StringBuilder();
        sb.append(gameId).append(',')
          .append(self.getSteps().size()).append(',')
          .append(self.getId()).append(',')
          .append(direction);
        for (int v : grid) sb.append(',').append(v);
        sb.append(String.format(",%.4f,%.4f,%d", apple_dr, apple_dc, ateApple ? 1 : 0));
        telemetryBuffer.add(new String[]{self.getId(), sb.toString()});
    }

    private void spawnApple() {
        Set<Integer> occupied = new HashSet<>();
        for (Cell c : playerA.getCells()) occupied.add(c.getX() * cols + c.getY());
        for (Cell c : playerB.getCells()) occupied.add(c.getX() * cols + c.getY());

        List<int[]> available = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (g[r][c] == 0 && !occupied.contains(r * cols + c)) {
                    available.add(new int[]{r, c});
                }
            }
        }

        if (!available.isEmpty()) {
            int[] pos = available.get(new Random().nextInt(available.size()));
            g[pos[0]][pos[1]] = 2;
            appleR = pos[0];
            appleC = pos[1];
        }
    }

    private void decreaseTail(Player player) {
        List<Cell> cells = player.getCells();
        Cell head = cells.get(cells.size() - 1);
        g[head.getX()][head.getY()] = 0;
        player.decreaseTail(player.getSteps().size());
        if (player == playerA) ateAppleA = true;
        else ateAppleB = true;
        spawnApple();
    }

    private void judge() {
        ateAppleA = false;
        ateAppleB = false;

        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean validA = checkValid(cellsA, cellsB);
        boolean validB = checkValid(cellsB, cellsA);

        boolean appleA = checkApple(cellsA);
        boolean appleB = checkApple(cellsB);

        // game over
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

        // eat apple
        if (appleA) decreaseTail(playerA);
        if (appleB) decreaseTail(playerB);
    }

    private void sendAllMessage(String message) {
        var connA = WebSocketServer.users.get(playerA.getId());
        var connB = WebSocketServer.users.get(playerB.getId());
        if (connA != null) connA.sendMessage(message);
        if (connB != null) connB.sendMessage(message);
    }

    private void sendMove() {
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            resp.put("a_direction", nextStepA);
            resp.put("b_direction", nextStepB);
            resp.put("a_ate_apple", ateAppleA);
            resp.put("b_ate_apple", ateAppleB);
            resp.put("apple_r", appleR);
            resp.put("apple_c", appleC);
            sendAllMessage(resp.toJSONString());
        } finally {
            lock.unlock();
        }
    }

    private void flushTelemetry() {
        if (telemetryBuffer == null || telemetryBuffer.isEmpty()) return;
        try {
            new File("training_data").mkdirs();
            try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                    new FileWriter("training_data/game_" + gameId + ".csv")))) {
                StringBuilder header = new StringBuilder("game_id,tick,player,direction");
                for (int i = 0; i < 25; i++) header.append(",g").append(String.format("%02d", i));
                header.append(",apple_dr,apple_dc,just_ate_apple,result");
                writer.println(header);
                for (String[] entry : telemetryBuffer) {
                    String playerId = entry[0];
                    String rowData  = entry[1];
                    String result;
                    if (loser.equals("all")) {
                        result = "draw";
                    } else if ((loser.equals("A") && playerId.equals(playerA.getId()))
                            || (loser.equals("B") && playerId.equals(playerB.getId()))) {
                        result = "loss";
                    } else {
                        result = "win";
                    }
                    writer.println(rowData + "," + result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        int tickLimit = (botA && botB) ? 1500 : 10000;
        for (int i = 0; i < tickLimit; i++) {
            nextStep();
            judge();
            if (status.equals("playing")) {
                int dirA = playerA.getSteps().get(playerA.getSteps().size() - 1);
                int dirB = playerB.getSteps().get(playerB.getSteps().size() - 1);
                collectTelemetry(playerA, playerB, dirA, ateAppleA);
                collectTelemetry(playerB, playerA, dirB, ateAppleB);
                sendMove();
            } else {
                flushTelemetry();
                sendResult();
                break;
            }
        }
        flushTelemetry(); // safety flush if loop exits without break (10000 tick limit)
    }
}
