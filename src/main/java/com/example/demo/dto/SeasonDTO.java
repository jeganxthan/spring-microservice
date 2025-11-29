// src/main/java/com/example/demo/dto/SeasonDTO.java
package com.example.demo.dto;

import java.util.List;

public class SeasonDTO {
    private String seasonName;
    private int episodeCount;
    private List<EpisodeDTO> episodes; // optional, null unless requested

    public SeasonDTO() {}

    public SeasonDTO(String seasonName, int episodeCount) {
        this.seasonName = seasonName;
        this.episodeCount = episodeCount;
    }

    public SeasonDTO(String seasonName, int episodeCount, List<EpisodeDTO> episodes) {
        this.seasonName = seasonName;
        this.episodeCount = episodeCount;
        this.episodes = episodes;
    }

    public String getSeasonName() { return seasonName; }
    public int getEpisodeCount() { return episodeCount; }
    public List<EpisodeDTO> getEpisodes() { return episodes; }

    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }
    public void setEpisodeCount(int episodeCount) { this.episodeCount = episodeCount; }
    public void setEpisodes(List<EpisodeDTO> episodes) { this.episodes = episodes; }
}
