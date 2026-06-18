package com.gobang.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameState {
    public static final int BOARD_SIZE = 15;
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = 2;

    private String id;
    private int[][] board;
    private int currentPlayer;
    private List<Move> moves;
    private Integer winner; // null=ongoing, 0=draw, 1=black, 2=white
    private String createdAt;
    private String forbiddenMessage;

    public GameState(String id) {
        this.id = id;
        this.board = new int[BOARD_SIZE][BOARD_SIZE];
        this.currentPlayer = BLACK;
        this.moves = new ArrayList<>();
        this.winner = null;
        this.createdAt = java.time.LocalDateTime.now().toString();
        this.forbiddenMessage = null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Move {
        private int row;
        private int col;
        private int player;
        private long timestamp;
    }

    public boolean placeStone(int row, int col) {
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return false;
        if (board[row][col] != EMPTY) return false;
        if (winner != null) return false;

        if (isForbiddenMove(row, col, currentPlayer)) {
            forbiddenMessage = getForbiddenReason(row, col, currentPlayer);
            return false;
        }
        forbiddenMessage = null;

        board[row][col] = currentPlayer;
        moves.add(new Move(row, col, currentPlayer, System.currentTimeMillis()));

        if (checkWin(row, col, currentPlayer)) {
            winner = currentPlayer;
        } else if (moves.size() == BOARD_SIZE * BOARD_SIZE) {
            winner = 0;
        } else {
            currentPlayer = currentPlayer == BLACK ? WHITE : BLACK;
        }
        return true;
    }

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

    private static boolean matchAny(String s, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(s).find()) return true;
        }
        return false;
    }

    private static String extractLine(int[][] b, int row, int col, int dr, int dc, int player) {
        StringBuilder sb = new StringBuilder();
        for (int i = -WINDOW; i <= WINDOW; i++) {
            int r = row + i * dr;
            int c = col + i * dc;
            if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
                sb.append('2');
            } else if (b[r][c] == EMPTY) {
                sb.append('0');
            } else if (b[r][c] == player) {
                sb.append('1');
            } else {
                sb.append('2');
            }
        }
        return sb.toString();
    }

    private int countDirection(int row, int col, int dr, int dc, int player) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    private boolean checkWin(int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1;
            count += countDirection(row, col, dir[0], dir[1], player);
            count += countDirection(row, col, -dir[0], -dir[1], player);
            if (count >= 5) return true;
        }
        return false;
    }

    private static int countStonesInLine(int[][] b, int row, int col, int dr, int dc, int player) {
        int count = 1;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && b[r][c] == player) {
            count++; r += dr; c += dc;
        }
        r = row - dr; c = col - dc;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && b[r][c] == player) {
            count++; r -= dr; c -= dc;
        }
        return count;
    }

    private static boolean hasFiveAt(int[][] b, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            if (countStonesInLine(b, row, col, dir[0], dir[1], player) == 5) return true;
        }
        return false;
    }

    private static boolean hasOverlineAt(int[][] b, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            if (countStonesInLine(b, row, col, dir[0], dir[1], player) >= 6) return true;
        }
        return false;
    }

    private static int countFoursAt(int[][] b, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            String line = extractLine(b, row, col, dir[0], dir[1], player);
            if (matchAny(line, FOUR_PATTERNS) || matchAny(line, LIVE_FOUR_PATTERNS)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isTrueLiveThree(int[][] b, int row, int col, int dr, int dc, int player) {
        for (int i = -5; i <= 5; i++) {
            int r = row + i * dr;
            int c = col + i * dc;
            if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) continue;
            if (b[r][c] != EMPTY) continue;
            b[r][c] = player;
            String lineAfter = extractLine(b, row, col, dr, dc, player);
            boolean foundLiveFour = false;
            if (matchAny(lineAfter, LIVE_FOUR_PATTERNS)) {
                foundLiveFour = true;
            } else if (matchAny(lineAfter, FOUR_PATTERNS)) {
                outer:
                for (int j = -5; j <= 5; j++) {
                    int r2 = row + j * dr;
                    int c2 = col + j * dc;
                    if (r2 < 0 || r2 >= BOARD_SIZE || c2 < 0 || c2 >= BOARD_SIZE) continue;
                    if (b[r2][c2] != EMPTY) continue;
                    b[r2][c2] = player;
                    if (countStonesInLine(b, r2, c2, dr, dc, player) == 5) {
                        b[r2][c2] = EMPTY;
                        continue;
                    }
                    String l2 = extractLine(b, r, c, dr, dc, player);
                    if (matchAny(l2, LIVE_FOUR_PATTERNS)) {
                        foundLiveFour = true;
                    }
                    b[r2][c2] = EMPTY;
                    if (foundLiveFour) break outer;
                }
            }
            b[r][c] = EMPTY;
            if (foundLiveFour) return true;
        }
        return false;
    }

    private static int countLiveThreesAt(int[][] b, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            String line = extractLine(b, row, col, dir[0], dir[1], player);
            if (matchAny(line, LIVE_THREE_PATTERNS)) {
                int[][] bCopy = new int[BOARD_SIZE][BOARD_SIZE];
                for (int r = 0; r < BOARD_SIZE; r++) {
                    System.arraycopy(b[r], 0, bCopy[r], 0, BOARD_SIZE);
                }
                if (isTrueLiveThree(bCopy, row, col, dir[0], dir[1], player)) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isForbiddenMove(int row, int col, int player) {
        if (player != BLACK) return false;
        if (board[row][col] != EMPTY) return false;

        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            System.arraycopy(board[r], 0, boardCopy[r], 0, BOARD_SIZE);
        }
        boardCopy[row][col] = player;

        if (hasFiveAt(boardCopy, row, col, player)) return false;
        if (hasOverlineAt(boardCopy, row, col, player)) return true;

        int fourCount = countFoursAt(boardCopy, row, col, player);
        if (fourCount >= 2) return true;

        int threeCount = countLiveThreesAt(boardCopy, row, col, player);
        if (threeCount >= 2) return true;

        return false;
    }

    public String getForbiddenReason(int row, int col, int player) {
        if (!isForbiddenMove(row, col, player)) return null;

        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            System.arraycopy(board[r], 0, boardCopy[r], 0, BOARD_SIZE);
        }
        boardCopy[row][col] = player;

        if (hasOverlineAt(boardCopy, row, col, player)) return "长连禁手";
        if (countFoursAt(boardCopy, row, col, player) >= 2) return "四四禁手";
        if (countLiveThreesAt(boardCopy, row, col, player) >= 2) return "三三禁手";

        return "禁手";
    }
}
