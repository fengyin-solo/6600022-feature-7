package com.gobang.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

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

    private boolean hasFiveAt(int[][] b, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1 + countDirOnBoard(b, row, col, dir[0], dir[1], player)
                           + countDirOnBoard(b, row, col, -dir[0], -dir[1], player);
            if (count == 5) return true;
        }
        return false;
    }

    private int countDirOnBoard(int[][] b, int row, int col, int dr, int dc, int player) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && b[r][c] == player) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    private boolean hasOverlineAt(int[][] b, int row, int col, int player) {
        for (int[] dir : DIRECTIONS) {
            int count = 1 + countDirOnBoard(b, row, col, dir[0], dir[1], player)
                           + countDirOnBoard(b, row, col, -dir[0], -dir[1], player);
            if (count >= 6) return true;
        }
        return false;
    }

    private boolean isLiveThreeAt(int[][] b, int row, int col, int dr, int dc, int player) {
        int fwd = countDirOnBoard(b, row, col, dr, dc, player);
        int bwd = countDirOnBoard(b, row, col, -dr, -dc, player);
        int count = 1 + fwd + bwd;

        if (count != 3) return false;

        int fwdR = row + dr * (fwd + 1);
        int fwdC = col + dc * (fwd + 1);
        int bwdR = row - dr * (bwd + 1);
        int bwdC = col - dc * (bwd + 1);

        boolean fwdEmpty = fwdR >= 0 && fwdR < BOARD_SIZE && fwdC >= 0 && fwdC < BOARD_SIZE && b[fwdR][fwdC] == EMPTY;
        boolean bwdEmpty = bwdR >= 0 && bwdR < BOARD_SIZE && bwdC >= 0 && bwdC < BOARD_SIZE && b[bwdR][bwdC] == EMPTY;

        if (!fwdEmpty || !bwdEmpty) return false;

        int fwd2R = row + dr * (fwd + 2);
        int fwd2C = col + dc * (fwd + 2);
        int bwd2R = row - dr * (bwd + 2);
        int bwd2C = col - dc * (bwd + 2);

        boolean fwd2Empty = fwd2R >= 0 && fwd2R < BOARD_SIZE && fwd2C >= 0 && fwd2C < BOARD_SIZE && b[fwd2R][fwd2C] == EMPTY;
        boolean bwd2Empty = bwd2R >= 0 && bwd2R < BOARD_SIZE && bwd2C >= 0 && bwd2C < BOARD_SIZE && b[bwd2R][bwd2C] == EMPTY;

        if (fwd2Empty && bwd2Empty) return true;

        if (fwd == 0 && bwd == 2) {
            if (bwd2Empty && fwdEmpty) return true;
        }
        if (bwd == 0 && fwd == 2) {
            if (fwd2Empty && bwdEmpty) return true;
        }

        return false;
    }

    private int countLiveThreesAt(int[][] b, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            if (isLiveThreeAt(b, row, col, dir[0], dir[1], player)) {
                count++;
            }
        }
        return count;
    }

    private boolean isFourAt(int[][] b, int row, int col, int dr, int dc, int player) {
        int fwd = countDirOnBoard(b, row, col, dr, dc, player);
        int bwd = countDirOnBoard(b, row, col, -dr, -dc, player);
        int count = 1 + fwd + bwd;

        if (count != 4) return false;

        int fwdR = row + dr * (fwd + 1);
        int fwdC = col + dc * (fwd + 1);
        int bwdR = row - dr * (bwd + 1);
        int bwdC = col - dc * (bwd + 1);

        boolean fwdEmpty = fwdR >= 0 && fwdR < BOARD_SIZE && fwdC >= 0 && fwdC < BOARD_SIZE && b[fwdR][fwdC] == EMPTY;
        boolean bwdEmpty = bwdR >= 0 && bwdR < BOARD_SIZE && bwdC >= 0 && bwdC < BOARD_SIZE && b[bwdR][bwdC] == EMPTY;

        return fwdEmpty || bwdEmpty;
    }

    private int countFoursAt(int[][] b, int row, int col, int player) {
        int count = 0;
        for (int[] dir : DIRECTIONS) {
            if (isFourAt(b, row, col, dir[0], dir[1], player)) {
                count++;
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
