package com.finalprojecttestbed.backend.controller.game;

import com.finalprojecttestbed.backend.consumer.game.Game;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/data")
public class DataGenerationController {

    /**
     * Starts N bot-vs-bot games in background threads.
     * Each game writes one CSV file to training_data/.
     *
     * Usage: GET /api/data/generate?games=5
     */
    @GetMapping("/generate")
    public String generate(@RequestParam(defaultValue = "5") int games) {
        if (games < 1 || games > 1000) return "Parameter 'games' must be between 1 and 100.";
        for (int i = 0; i < games; i++) {
            String idA = "bot_a_" + UUID.randomUUID().toString().substring(0, 6);
            String idB = "bot_b_" + UUID.randomUUID().toString().substring(0, 6);
            Game game = new Game(25, 28, 40, idA, idB, true, true);
            game.createMap();
            game.start();
        }
        return "Started " + games + " bot games. CSV files will appear in training_data/.";
    }
}
