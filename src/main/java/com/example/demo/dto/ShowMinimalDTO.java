package com.example.demo.dto;

public class ShowMinimalDTO {
    private String showTitle;
    private String poster;

    public ShowMinimalDTO(String showTitle, String poster) {
        this.showTitle = showTitle;
        this.poster = poster;
    }

    public String getShowTitle() {
        return showTitle;
    }

    public String getPoster() {
        return poster;
    }
}
