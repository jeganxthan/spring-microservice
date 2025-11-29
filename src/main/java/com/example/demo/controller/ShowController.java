package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SeasonDTO;
import com.example.demo.dto.ShowMinimalDTO;
import com.example.demo.model.Show;
import com.example.demo.service.ShowService;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    // Get all shows
    @GetMapping("/all")
    public ResponseEntity<List<Show>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }

    @GetMapping("/all/minimal")
    public ResponseEntity<List<ShowMinimalDTO>> getAllShowsMinimal() {
        return ResponseEntity.ok(showService.getAllShowsMinimal());
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<List<Show>> getShowsByTitle(@PathVariable String title) {
        return ResponseEntity.ok(showService.getShowsByTitle(title));
    }

    // Search shows by title
    @GetMapping("/search")
    public ResponseEntity<List<Show>> searchShows(@RequestParam String query) {
        return ResponseEntity.ok(showService.searchShowsByTitle(query));
    }

    @PostMapping("/load")
    public ResponseEntity<String> loadShowsFromJson() {
        try {
            showService.loadShowsFromJson();
            return ResponseEntity.ok("Shows successfully loaded into the database.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error loading shows from JSON: " + e.getMessage());
        }
    }

    @GetMapping("/{seriesName}/all-seasons")
    public ResponseEntity<List<SeasonDTO>> getAllSeasonsForSeries(
            @PathVariable String seriesName,
            @RequestParam(name = "includeEpisodes", required = false, defaultValue = "false") boolean includeEpisodes) {

        List<SeasonDTO> seasons = showService.getAllSeasonsForSeries(seriesName, includeEpisodes);
        return ResponseEntity.ok(seasons);
    }
}
