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

    public GameState(String id) {
        this.id = id;
        this.board = new int[BOARD_SIZE][BOARD_SIZE];
        this.currentPlayer = BLACK;
        this.moves = new ArrayList<>();
        this.winner = null;
        this.createdAt = java.time.LocalDateTime.now().toString();
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

    private boolean checkWin(int row, int col, int player) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;
            count += countDir(row, col, dir[0], dir[1], player);
            count += countDir(row, col, -dir[0], -dir[1], player);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countDir(int row, int col, int dr, int dc, int player) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }
}
