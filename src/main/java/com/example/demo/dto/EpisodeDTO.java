// src/main/java/com/example/demo/dto/EpisodeDTO.java
package com.example.demo.dto;

public class EpisodeDTO {

    private Integer episodeIndex;
    private String title;
    private String description;
    private String duration;
    private String imageUrl;
    private String filename;
    private String url;

    public EpisodeDTO() {
    }

    public EpisodeDTO(Integer episodeIndex, String title, String description,
            String duration, String imageUrl, String filename, String url) {
        this.episodeIndex = episodeIndex;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.url = url;
    }

    public Integer getEpisodeIndex() {
        return episodeIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDuration() {
        return duration;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

    public void setEpisodeIndex(Integer episodeIndex) {
        this.episodeIndex = episodeIndex;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
