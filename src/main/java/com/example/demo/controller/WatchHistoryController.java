package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.WatchHistory;
import com.example.demo.service.WatchHistoryService;

@RestController
@RequestMapping("/watch-history")
public class WatchHistoryController {

    @Autowired
    private WatchHistoryService watchHistoryService;


    // Save watch progress
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/save-progress")
    public void saveWatchProgress(@RequestParam String userId, 
    @RequestParam String showId, 
    @RequestParam String episodeId, 
    @RequestParam String timestamp) {
        watchHistoryService.saveWatchProgress(userId, showId, episodeId, timestamp);
    }
    
    // Get the watch history of a user
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/history")
    public List<WatchHistory> getWatchHistory(@RequestParam String userId) {
        return watchHistoryService.getWatchHistory(userId); // Return the user's watch history
    }
}
