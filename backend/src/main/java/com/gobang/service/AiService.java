package com.gobang.service;

import com.gobang.model.GameState;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiService {
    private static final int BOARD_SIZE = GameState.BOARD_SIZE;
    private static final int EMPTY = GameState.EMPTY;
    private static final int BLACK = GameState.BLACK;
    private static final int WHITE = GameState.WHITE;

    private static final int[][] DIRECTIONS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
    private static final int WINDOW = 6;

    private static final Pattern[] LIVE_FOUR_PATTERNS = {
        Pattern.compile("011110"),
    };

    private static final Pattern[] FOUR_PATTERNS = {
        Pattern.compile("11110"), Pattern.compile("01111"),
        Pattern.compile("11011"), Pattern.compile("10111"), Pattern.compile("11101"),
        Pattern.compile("100111"), Pattern.compile("111001"),
        Pattern.compile("101011"), Pattern.compile("110101"),
        Pattern.compile("101101"),
    };

    private static final Pattern[] LIVE_THREE_PATTERNS = {
        Pattern.compile("0011100"), Pattern.compile("001110"), Pattern.compile("011100"),
        Pattern.compile("0101100"), Pattern.compile("0011010"),
        Pattern.compile("0110100"), Pattern.compile("0010110"),
        Pattern.compile("0100110"), Pattern.compile("0110010"),
    };

    private static final int FIVE = 1000000;
    private static final int LIVE_FOUR = 100000;
    private static final int DEAD_FOUR = 10000;
    private static final int LIVE_THREE = 10000;
    private static final int DEAD_THREE = 1000;
    private static final int LIVE_TWO = 1000;
    private static final int DEAD_TWO = 100;
    private static final int LIVE_ONE = 100;
    private static final int DEAD_ONE = 10;

    private static boolean matchAny(String s, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(s).find()) return true;
        }
        return false;
    }

    private static String extractLine(int[][] board, int row, int col, int dr, int dc, int player) {
        StringBuilder sb = new StringBuilder();
        for (int i = -WINDOW; i <= WINDOW; i++) {
            int r = row + i * dr;
            int c = col + i * dc;
            if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                sb.append('2');
            } else if (board[r][c] == EMPTY) {
                sb.append('0');
            } else if (board[r][c] == player) {
                sb.append('1');
            } else {
                sb.append('2');
            }
        }
        return sb.toString();
    }

    private static int countStonesInLine(int[][] board, int row, int col, int dr, int dc, int player) {
        int count = 1;
        int r = row + dr, c = col + dc;
        while (inBounds(r, c) && board[r][c] == player) {
            count++; r += dr; c += dc;
        }
        r = row - dr; c = col - dc;
        while (inBounds(r, c) && board[r][c] == player) {
            count++; r -= dr; c -= dc;
        }
        return count;
    }

    private static boolean hasFiveAt(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            if (countStonesInLine(board, row, col, dir[0], dir[1], player) == 5) return true;
        }
        return false;
    }

    private static boolean hasOverlineAt(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            if (countStonesInLine(board, row, col, dir[0], dir[1], player) >= 6) return true;
        }
        return false;
    }

    private static int countFoursAt(int[][] board, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            String line = extractLine(board, row, col, dir[0], dir[1], player);
            if (matchAny(line, FOUR_PATTERNS) || matchAny(line, LIVE_FOUR_PATTERNS)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isTrueLiveThree(int[][] board, int row, int col, int dr, int dc, int player) {
        for (int i = -5; i <= 5; i++) {
            int r = row + i * dr;
            int c = col + i * dc;
            if (!inBounds(r, c)) continue;
            if (board[r][c] != EMPTY) continue;
            board[r][c] = player;
            String lineAfter = extractLine(board, row, col, dr, dc, player);
            boolean foundLiveFour = false;
            if (matchAny(lineAfter, LIVE_FOUR_PATTERNS)) {
                foundLiveFour = true;
            } else if (matchAny(lineAfter, FOUR_PATTERNS)) {
                outer:
                for (int j = -5; j <= 5; j++) {
                    int r2 = row + j * dr;
                    int c2 = col + j * dc;
                    if (!inBounds(r2, c2)) continue;
                    if (board[r2][c2] != EMPTY) continue;
                    board[r2][c2] = player;
                    if (countStonesInLine(board, r2, c2, dr, dc, player) == 5) {
                        board[r2][c2] = EMPTY;
                        continue;
                    }
                    String l2 = extractLine(board, r, c, dr, dc, player);
                    if (matchAny(l2, LIVE_FOUR_PATTERNS)) {
                        foundLiveFour = true;
                    }
                    board[r2][c2] = EMPTY;
                    if (foundLiveFour) break outer;
                }
            }
            board[r][c] = EMPTY;
            if (foundLiveFour) return true;
        }
        return false;
    }

    private static int countLiveThreesAt(int[][] board, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            String line = extractLine(board, row, col, dir[0], dir[1], player);
            if (matchAny(line, LIVE_THREE_PATTERNS)) {
                int[][] bCopy = new int[BOARD_SIZE][BOARD_SIZE];
                for (int r = 0; r < BOARD_SIZE; r++) {
                    System.arraycopy(board[r], 0, bCopy[r], 0, BOARD_SIZE);
                }
                if (isTrueLiveThree(bCopy, row, col, dir[0], dir[1], player)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isForbiddenMove(int[][] board, int row, int col, int player) {
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

    private int evaluateLine(int[][] board, int row, int col, int dr, int dc, int player) {
        int count = 1 + countDirection(board, row, col, dr, dc, player) + countDirection(board, row, col, -dr, -dc, player);
        if (count >= 5) return FIVE;

        int fwd = countDirection(board, row, col, dr, dc, player);
        int bwd = countDirection(board, row, col, -dr, -dc, player);
        boolean fwdBlocked = !inBounds(row + dr * (fwd + 1), col + dc * (fwd + 1)) || board[row + dr * (fwd + 1)][col + dc * (fwd + 1)] != EMPTY;
        boolean bwdBlocked = !inBounds(row - dr * (bwd + 1), col - dc * (bwd + 1)) || board[row - dr * (bwd + 1)][col - dc * (bwd + 1)] != EMPTY;

        int openEnds = (fwdBlocked ? 0 : 1) + (bwdBlocked ? 0 : 1);
        if (openEnds == 0) return 0;

        if (count == 4) return openEnds == 2 ? LIVE_FOUR : DEAD_FOUR;
        if (count == 3) return openEnds == 2 ? LIVE_THREE : DEAD_THREE;
        if (count == 2) return openEnds == 2 ? LIVE_TWO : DEAD_TWO;
        return openEnds == 2 ? LIVE_ONE : DEAD_ONE;
    }

    private static boolean inBounds(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
    }

    private boolean checkWinAt(int[][] board, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1 + countDirection(board, row, col, dir[0], dir[1], player) + countDirection(board, row, col, -dir[0], -dir[1], player);
            if (count >= 5) return true;
        }
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
