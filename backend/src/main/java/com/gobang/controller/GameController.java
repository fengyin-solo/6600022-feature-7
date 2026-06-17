package com.gobang.controller;

import com.gobang.model.GameState;
import com.gobang.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private AiService aiService;

    private final Map<String, GameState> games = new ConcurrentHashMap<>();

    @PostMapping("/new")
    public ResponseEntity<GameState> newGame() {
        String id = UUID.randomUUID().toString();
        GameState game = new GameState(id);
        games.put(id, game);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameState> getGame(@PathVariable String id) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/move")
    public ResponseEntity<?> makeMove(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        if (game.getWinner() != null) return ResponseEntity.badRequest().body("Game is over");

        int row = body.getOrDefault("row", -1);
        int col = body.getOrDefault("col", -1);

        if (!game.placeStone(row, col)) {
            return ResponseEntity.badRequest().body("Invalid move");
        }
        return ResponseEntity.ok(game);
    }

    @PostMapping("/{id}/ai-move")
    public ResponseEntity<?> aiMove(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        GameState game = games.get(id);
        if (game == null) return ResponseEntity.notFound().build();
        if (game.getWinner() != null) return ResponseEntity.badRequest().body("Game is over");

        int aiPlayer = body.getOrDefault("player", GameState.WHITE);
        int depth = body.getOrDefault("depth", 3);

        int[][] boardCopy = new int[GameState.BOARD_SIZE][GameState.BOARD_SIZE];
        for (int r = 0; r < GameState.BOARD_SIZE; r++) {
            System.arraycopy(game.getBoard()[r], 0, boardCopy[r], 0, GameState.BOARD_SIZE);
        }

        int[] move = aiService.getBestMove(boardCopy, aiPlayer, depth);
        if (move == null) return ResponseEntity.badRequest().body("No valid move");

        game.placeStone(move[0], move[1]);
        return ResponseEntity.ok(game);
    }

    @GetMapping("/records")
    public ResponseEntity<List<GameState>> getRecords() {
        List<GameState> finished = games.values().stream()
                .filter(g -> g.getWinner() != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(finished);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable String id) {
        games.remove(id);
        return ResponseEntity.ok().build();
    }
}
