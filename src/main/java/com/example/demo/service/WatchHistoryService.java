package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.model.WatchHistory;
import com.example.demo.repository.WatchHistoryRepository;

@Service
public class WatchHistoryService {

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    // Save user's watch progress
    public void saveWatchProgress(String userId, String showId, String episodeId, String timestamp) {
        WatchHistory watchHistory = new WatchHistory(); // Create WatchHistory, not WatchHistoryService
        watchHistory.setUserId(userId);
        watchHistory.setShowId(showId);
        watchHistory.setEpisodeId(episodeId);
        watchHistory.setTimestamp(timestamp);
        watchHistory.setDateWatched(LocalDateTime.now()); // Store the current date and time as the date watched
        watchHistoryRepository.save(watchHistory); // Save the WatchHistory entity
    }

    // Get watch history for a specific user
    @Cacheable(value = "watchHistory", key = "#userId")
    public List<WatchHistory> getWatchHistory(String userId) {
        return watchHistoryRepository.findByUserId(userId); // Fetch the watch history
    }
}
