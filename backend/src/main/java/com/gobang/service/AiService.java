package com.gobang.service;

import com.gobang.model.GameState;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiService {
    private static final int BOARD_SIZE = GameState.BOARD_SIZE;
    private static final int EMPTY = GameState.EMPTY;
    private static final int BLACK = GameState.BLACK;
    private static final int WHITE = GameState.WHITE;

    private static final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    private static final int FIVE = 1000000;
    private static final int LIVE_FOUR = 100000;
    private static final int DEAD_FOUR = 10000;
    private static final int LIVE_THREE = 10000;
    private static final int DEAD_THREE = 1000;
    private static final int LIVE_TWO = 1000;
    private static final int DEAD_TWO = 100;
    private static final int LIVE_ONE = 100;
    private static final int DEAD_ONE = 10;

    public int[] getBestMove(int[][] board, int aiPlayer, int depth) {
        List<int[]> candidates = getCandidateMoves(board, aiPlayer);
        if (candidates.isEmpty()) return null;

        int[] bestMove = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int[] move : candidates) {
            board[move[0]][move[1]] = aiPlayer;
            if (checkWinAt(board, move[0], move[1], aiPlayer)) {
                board[move[0]][move[1]] = EMPTY;
                return move;
            }
            int score = minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiPlayer);
            board[move[0]][move[1]] = EMPTY;
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int minimax(int[][] board, int depth, int alpha, int beta, boolean isMaximizing, int aiPlayer) {
        int humanPlayer = aiPlayer == BLACK ? WHITE : BLACK;

        if (depth == 0) return evaluateBoard(board, aiPlayer);

        int currentPlayer = isMaximizing ? aiPlayer : humanPlayer;
        List<int[]> candidates = getCandidateMoves(board, currentPlayer);
        if (candidates.isEmpty()) return evaluateBoard(board, aiPlayer);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] move : candidates) {
                board[move[0]][move[1]] = aiPlayer;
                if (checkWinAt(board, move[0], move[1], aiPlayer)) {
                    board[move[0]][move[1]] = EMPTY;
                    return FIVE * (depth + 1);
                }
                int eval = minimax(board, depth - 1, alpha, beta, false, aiPlayer);
                board[move[0]][move[1]] = EMPTY;
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int[] move : candidates) {
                board[move[0]][move[1]] = humanPlayer;
                if (checkWinAt(board, move[0], move[1], humanPlayer)) {
                    board[move[0]][move[1]] = EMPTY;
                    return -FIVE * (depth + 1);
                }
                int eval = minimax(board, depth - 1, alpha, beta, true, aiPlayer);
                board[move[0]][move[1]] = EMPTY;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private int evaluateBoard(int[][] board, int aiPlayer) {
        int aiScore = 0;
        int humanScore = 0;
        int humanPlayer = aiPlayer == BLACK ? WHITE : BLACK;

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] == aiPlayer) {
                    for (int[] dir : DIRECTIONS) {
                        aiScore += evaluateLine(board, r, c, dir[0], dir[1], aiPlayer);
                    }
                } else if (board[r][c] == humanPlayer) {
                    for (int[] dir : DIRECTIONS) {
                        humanScore += evaluateLine(board, r, c, dir[0], dir[1], humanPlayer);
                    }
                }
            }
        }
        return aiScore - (int)(humanScore * 1.1);
    }

    private int evaluateLine(int[][] board, int row, int col, int dr, int dc, int player) {
        int fwd = countDirection(board, row, col, dr, dc, player);
        int bwd = countDirection(board, row, col, -dr, -dc, player);
        int count = 1 + fwd + bwd;

        if (count >= 5) return FIVE;

        boolean fwdBlocked = !inBounds(row + dr * (fwd + 1), col + dc * (fwd + 1)) || board[row + dr * (fwd + 1)][col + dc * (fwd + 1)] != EMPTY;
        boolean bwdBlocked = !inBounds(row - dr * (bwd + 1), col - dc * (bwd + 1)) || board[row - dr * (bwd + 1)][col - dc * (bwd + 1)] != EMPTY;

        int openEnds = (fwdBlocked ? 0 : 1) + (bwdBlocked ? 0 : 1);
        if (openEnds == 0) return 0;

        if (count == 4) return openEnds == 2 ? LIVE_FOUR : DEAD_FOUR;
        if (count == 3) return openEnds == 2 ? LIVE_THREE : DEAD_THREE;
        if (count == 2) return openEnds == 2 ? LIVE_TWO : DEAD_TWO;
        return openEnds == 2 ? LIVE_ONE : DEAD_ONE;
    }

    private int countDirection(int[][] board, int row, int col, int dr, int dc, int player) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (inBounds(r, c) && board[r][c] == player) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
    }

    private boolean checkWinAt(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1 + countDirection(board, row, col, dir[0], dir[1], player) + countDirection(board, row, col, -dir[0], -dir[1], player);
            if (count >= 5) return true;
        }
        return false;
    }

    private boolean hasFiveAt(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1 + countDirection(board, row, col, dir[0], dir[1], player) + countDirection(board, row, col, -dir[0], -dir[1], player);
            if (count == 5) return true;
        }
        return false;
    }

    private boolean hasOverlineAt(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1 + countDirection(board, row, col, dir[0], dir[1], player) + countDirection(board, row, col, -dir[0], -dir[1], player);
            if (count >= 6) return true;
        }
        return false;
    }

    private boolean isLiveThreeAt(int[][] board, int row, int col, int dr, int dc, int player) {
        int fwd = countDirection(board, row, col, dr, dc, player);
        int bwd = countDirection(board, row, col, -dr, -dc, player);
        int count = 1 + fwd + bwd;

        if (count != 3) return false;

        int fwdR = row + dr * (fwd + 1);
        int fwdC = col + dc * (fwd + 1);
        int bwdR = row - dr * (bwd + 1);
        int bwdC = col - dc * (bwd + 1);

        boolean fwdEmpty = inBounds(fwdR, fwdC) && board[fwdR][fwdC] == EMPTY;
        boolean bwdEmpty = inBounds(bwdR, bwdC) && board[bwdR][bwdC] == EMPTY;

        if (!fwdEmpty || !bwdEmpty) return false;

        int fwd2R = row + dr * (fwd + 2);
        int fwd2C = col + dc * (fwd + 2);
        int bwd2R = row - dr * (bwd + 2);
        int bwd2C = col - dc * (bwd + 2);

        boolean fwd2Empty = inBounds(fwd2R, fwd2C) && board[fwd2R][fwd2C] == EMPTY;
        boolean bwd2Empty = inBounds(bwd2R, bwd2C) && board[bwd2R][bwd2C] == EMPTY;

        if (fwd2Empty && bwd2Empty) return true;

        if (fwd == 0 && bwd == 2) {
            if (bwd2Empty && fwdEmpty) return true;
        }
        if (bwd == 0 && fwd == 2) {
            if (fwd2Empty && bwdEmpty) return true;
        }

        return false;
    }

    private int countLiveThreesAt(int[][] board, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            if (isLiveThreeAt(board, row, col, dir[0], dir[1], player)) {
                count++;
            }
        }
        return count;
    }

    private boolean isFourAt(int[][] board, int row, int col, int dr, int dc, int player) {
        int fwd = countDirection(board, row, col, dr, dc, player);
        int bwd = countDirection(board, row, col, -dr, -dc, player);
        int count = 1 + fwd + bwd;

        if (count != 4) return false;

        int fwdR = row + dr * (fwd + 1);
        int fwdC = col + dc * (fwd + 1);
        int bwdR = row - dr * (bwd + 1);
        int bwdC = col - dc * (bwd + 1);

        boolean fwdEmpty = inBounds(fwdR, fwdC) && board[fwdR][fwdC] == EMPTY;
        boolean bwdEmpty = inBounds(bwdR, bwdC) && board[bwdR][bwdC] == EMPTY;

        return fwdEmpty || bwdEmpty;
    }

    private int countFoursAt(int[][] board, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            if (isFourAt(board, row, col, dir[0], dir[1], player)) {
                count++;
            }
        }
        return count;
    }

    private boolean isForbiddenMove(int[][] board, int row, int col, int player) {
        if (player != BLACK) return false;
        if (board[row][col] != EMPTY) return false;

        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            System.arraycopy(board[r], 0, boardCopy[r], 0, BOARD_SIZE);
        }
        boardCopy[row][col] = player;

        if (hasFiveAt(boardCopy, row, col, player)) return false;
        if (hasOverlineAt(boardCopy, row, col, player)) return true;
        if (countFoursAt(boardCopy, row, col, player) >= 2) return true;
        if (countLiveThreesAt(boardCopy, row, col, player) >= 2) return true;

        return false;
    }

    private List<int[]> getCandidateMoves(int[][] board, int player) {
        List<int[]> candidates = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c] != EMPTY) {
                    for (int dr = -2; dr <= 2; dr++) {
                        for (int dc = -2; dc <= 2; dc++) {
                            int nr = r + dr, nc = c + dc;
                            String key = nr + "," + nc;
                            if (inBounds(nr, nc) && board[nr][nc] == EMPTY && !visited.contains(key)) {
                                if (player == BLACK && isForbiddenMove(board, nr, nc, player)) continue;
                                visited.add(key);
                                candidates.add(new int[]{nr, nc});
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty() && board[7][7] == EMPTY) {
            if (!(player == BLACK && isForbiddenMove(board, 7, 7, player))) {
                candidates.add(new int[]{7, 7});
            }
        }
        return candidates;
    }
}
