package com.finalprojecttestbed.backend.consumer.bot;

import com.finalprojecttestbed.backend.consumer.game.Cell;

import java.util.*;

/**
 * A heuristic bot that simulates human snake-game intuition for training data generation.
 *
 * <h2>Algorithm Overview</h2>
 * Each tick, {@link #computeDirection} scores every candidate direction and returns the
 * highest-scoring one. The score for direction {@code d} is:
 *
 * <pre>
 *   score(d) = appleScore(d) + inertiaScore(d) + spaceScore(d) − oppHeadPenalty(d) + noise(d)
 * </pre>
 *
 * <h3>1. Safety Filter (pre-scoring)</h3>
 * Before scoring, all fatal directions are removed from consideration:
 * <ul>
 *   <li>180-degree reversals — the neck cell is always occupied, so reversing is
 *       equivalent to immediate self-collision.</li>
 *   <li>Out-of-bounds moves.</li>
 *   <li>Wall / boundary cells ({@code g[r][c] == 1}).</li>
 *   <li>Own or opponent snake body cells.</li>
 * </ul>
 * If only one safe direction remains it is returned immediately without scoring.
 * If no safe direction exists the current direction is returned (snake is already doomed).
 *
 * <h3>2. Apple Score — directional attraction</h3>
 * <pre>
 *   appleScore(d) = (manhattan(head, apple) − manhattan(next, apple)) × W_apple
 * </pre>
 * A positive value rewards moving closer; a negative value penalises moving away.
 * When the apple is within 4 cells ({@code distToApple ≤ 4}), {@code W_apple} is
 * raised from {@value #APPLE_WEIGHT} to {@value #APPLE_WEIGHT_NEAR} to model the
 * human tendency to commit more decisively once a target is close.
 *
 * <h3>3. Inertia Score — continuation preference</h3>
 * <pre>
 *   inertiaScore(d) = INERTIA_WEIGHT   if d == currentDir, else 0
 * </pre>
 * Humans rarely change direction without reason. A small bonus ({@value #INERTIA_WEIGHT})
 * for continuing straight prevents the bot from oscillating between equivalent paths and
 * produces smoother, more realistic trajectories in the training data.
 *
 * <h3>4. Space Score — dead-end avoidance via BFS flood fill</h3>
 * <pre>
 *   spaceScore(d) = floodFill(next_cell, depth={@value #FLOOD_DEPTH}) × SPACE_WEIGHT
 * </pre>
 * {@link #floodFill} runs a BFS from the candidate next cell and counts reachable empty
 * cells up to {@value #FLOOD_DEPTH} steps. A larger reachable area signals a safer
 * corridor and discourages moves that lead into dead ends or tight spaces, mimicking the
 * instinctive human preference for "open room".
 *
 * <h3>5. Opponent Head Penalty — head-on collision avoidance</h3>
 * <pre>
 *   oppHeadPenalty(d) = OPP_HEAD_PENALTY           if manhattan(next, oppHead) == 1
 *                     = OPP_HEAD_PENALTY × 0.4      if manhattan(next, oppHead) == 2
 *                     = 0                            otherwise
 * </pre>
 * Moving to a cell directly adjacent to the opponent's head risks a simultaneous
 * head-on collision next tick (both snakes die). A strong penalty ({@value #OPP_HEAD_PENALTY})
 * discourages entering this danger zone, while a partial penalty for distance-2 reflects
 * the reduced — but non-zero — risk of being cornered in the following step.
 * This term is intentionally stronger than {@code APPLE_WEIGHT} so survival takes
 * precedence over apple chasing when both objectives conflict.
 *
 * <h3>6. Stochastic Noise — human imperfection</h3>
 * Two sources of randomness are applied:
 * <ul>
 *   <li><b>Score noise:</b> uniform random value in [−{@value #NOISE}, +{@value #NOISE}]
 *       added to every candidate's score. This prevents deterministic, perfectly optimal
 *       play and introduces the slight unpredictability characteristic of human decision-making.</li>
 *   <li><b>Random mistake:</b> with probability {@value #ERROR_RATE} the bot ignores all
 *       scoring and picks uniformly at random from valid directions, simulating momentary
 *       lapses in attention.</li>
 * </ul>
 */
public class Bot {

    private static final int[] dr = {-1, 0, 1, 0};
    private static final int[] dc = {0, 1, 0, -1};

    private static final double APPLE_WEIGHT      = 3.0;
    private static final double APPLE_WEIGHT_NEAR = 5.5;
    private static final double INERTIA_WEIGHT    = 1.2;
    private static final double SPACE_WEIGHT      = 0.08;
    private static final double OPP_HEAD_PENALTY  = 4.0;
    private static final double NOISE             = 0.6;
    private static final double ERROR_RATE        = 0.05;
    private static final int    FLOOD_DEPTH       = 10;

    private final int rows, cols;
    private final Random random = new Random();

    public Bot(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * Computes the next direction for the bot player.
     *
     * @param currentDir current direction (0=up, 1=right, 2=down, 3=left)
     * @param g          authoritative game grid; {@code g[r][c]=1} means wall/boundary,
     *                   {@code g[r][c]=2} means apple (passable)
     * @param ownCells   snake body cells of this player, head at {@code get(size-1)}
     * @param oppCells   snake body cells of the opponent
     * @param appleR     apple row
     * @param appleC     apple column
     * @return chosen direction (0–3)
     */
    public int computeDirection(int currentDir, int[][] g,
                                List<Cell> ownCells, List<Cell> oppCells,
                                int appleR, int appleC) {

        Cell head = ownCells.get(ownCells.size() - 1);
        int hr = head.getX(), hc = head.getY();

        Set<Integer> bodyOccupied = new HashSet<>();
        for (Cell c : ownCells) bodyOccupied.add(c.getX() * cols + c.getY());
        for (Cell c : oppCells) bodyOccupied.add(c.getX() * cols + c.getY());

        List<Integer> validDirs = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            if ((d + 2) % 4 == currentDir) continue;
            int nr = hr + dr[d], nc = hc + dc[d];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
            if (g[nr][nc] == 1) continue;
            if (bodyOccupied.contains(nr * cols + nc)) continue;
            validDirs.add(d);
        }

        if (validDirs.isEmpty()) return currentDir;
        if (validDirs.size() == 1) return validDirs.get(0);

        if (random.nextDouble() < ERROR_RATE) {
            return validDirs.get(random.nextInt(validDirs.size()));
        }

        int distToApple = Math.abs(hr - appleR) + Math.abs(hc - appleC);
        double appleW = distToApple <= 4 ? APPLE_WEIGHT_NEAR : APPLE_WEIGHT;

        Cell oppHead = oppCells.get(oppCells.size() - 1);
        int oppHr = oppHead.getX(), oppHc = oppHead.getY();

        double bestScore = Double.NEGATIVE_INFINITY;
        int bestDir = validDirs.get(0);

        for (int d : validDirs) {
            int nr = hr + dr[d], nc = hc + dc[d];
            double score = 0;

            int distAfter = Math.abs(nr - appleR) + Math.abs(nc - appleC);
            score += (distToApple - distAfter) * appleW;

            if (d == currentDir) score += INERTIA_WEIGHT;

            score += floodFill(nr, nc, g, bodyOccupied) * SPACE_WEIGHT;

            // Opponent head danger: penalise moves that land adjacent to or near the opponent
            // head, where a simultaneous advance next tick would cause a head-on collision.
            int distToOppHead = Math.abs(nr - oppHr) + Math.abs(nc - oppHc);
            if (distToOppHead == 1)      score -= OPP_HEAD_PENALTY;
            else if (distToOppHead == 2) score -= OPP_HEAD_PENALTY * 0.4;

            score += (random.nextDouble() - 0.5) * 2.0 * NOISE;

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        return bestDir;
    }

    /**
     * BFS flood fill from ({@code startR}, {@code startC}) counting reachable empty cells
     * up to {@code FLOOD_DEPTH} steps. Used as a proxy for available manoeuvring space.
     *
     * <p>A cell is considered reachable if it is within map bounds, not a wall
     * ({@code g[r][c] != 1}), and not in {@code occupied}. Apple cells ({@code g[r][c]=2})
     * are treated as passable so the bot correctly routes through them.</p>
     *
     * @return number of distinct reachable cells within the depth limit
     */
    private int floodFill(int startR, int startC, int[][] g, Set<Integer> occupied) {
        Queue<int[]> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(new int[]{startR, startC, 0});
        visited.add(startR * cols + startC);
        int count = 0;

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1], depth = cur[2];
            count++;
            if (depth >= FLOOD_DEPTH) continue;
            for (int d = 0; d < 4; d++) {
                int nr = r + dr[d], nc = c + dc[d];
                int key = nr * cols + nc;
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
                if (g[nr][nc] == 1) continue;
                if (occupied.contains(key)) continue;
                if (visited.contains(key)) continue;
                visited.add(key);
                queue.add(new int[]{nr, nc, depth + 1});
            }
        }
        return count;
    }
}
