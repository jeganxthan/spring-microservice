package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String showId;
    private String episodeId;
    private String timestamp;  // To store where the user left off
    private LocalDateTime dateWatched;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getShowId() {
        return showId;
    }
    public void setShowId(String showId) {
        this.showId = showId;
    }
    public String getEpisodeId() {
        return episodeId;
    }
    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public LocalDateTime getDateWatched() {
        return dateWatched;
    }
    public void setDateWatched(LocalDateTime dateWatched) {
        this.dateWatched = dateWatched;
    }
}
