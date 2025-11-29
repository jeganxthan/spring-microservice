package com.example.demo.model;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Map;

@Entity
@Table(name = "shows")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Show {

    @Id
    @Column(name = "show_title")
    @JsonProperty("show_title")
    private String showTitle;

    @Column(name = "season_number")
    private int seasonNumber;

    @Column(name = "year")
    private String year;

    @ElementCollection
    @CollectionTable(name = "show_seasons", joinColumns = @JoinColumn(name = "show_title"))
    @Column(name = "season")
    @JsonIgnore
    private List<String> seasons;

    @Column(name = "rating")
    private String rating;

    @Column(name = "genre")
    private String genre;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "starring")
    private String starring;

    @Column(name = "creators")
    private String creators;

    @Column(name = "genres")
    private String genres;

    @Column(name = "show_characteristics")
    private String showCharacteristics;

    @Column(name = "audio")
    private String audio;

    @Column(name = "subtitles")
    private String subtitles;

    @Column(name = "show_cast")
    @JsonProperty("cast")
    private String showCast;

    @Column(name = "poster")
    @JsonProperty("Poster")
    private String poster;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "show_title")
    private List<SeasonData> seasons_data;


    public String getShowTitle() {
        return showTitle;
    }

    public void setShowTitle(String showTitle) {
        this.showTitle = showTitle;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public List<String> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<String> seasons) {
        this.seasons = seasons;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStarring() {
        return starring;
    }

    public void setStarring(String starring) {
        this.starring = starring;
    }

    public String getCreators() {
        return creators;
    }

    public void setCreators(String creators) {
        this.creators = creators;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getShowCharacteristics() {
        return showCharacteristics;
    }

    public void setShowCharacteristics(String showCharacteristics) {
        this.showCharacteristics = showCharacteristics;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public String getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(String subtitles) {
        this.subtitles = subtitles;
    }

    public String getShowCast() {
        return showCast;
    }

    public void setShowCast(String showCast) {
        this.showCast = showCast;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public List<SeasonData> getSeasons_data() {
        return seasons_data;
    }

    public void setSeasons_data(List<SeasonData> seasons_data) {
        this.seasons_data = seasons_data;
    }

    @JsonProperty("seasons")
    public void setSeasonsString(String seasonsString) {
        // Ignore or parse if needed
    }

    @JsonProperty("seasons_data")
    public void setSeasonsDataJson(List<Map<String, List<Episode>>> data) {
        this.seasons = new ArrayList<>();
        this.seasons_data = new ArrayList<>();
        if (data != null) {
            for (Map<String, List<Episode>> map : data) {
                for (Map.Entry<String, List<Episode>> entry : map.entrySet()) {
                    this.seasons.add(entry.getKey());
                    
                    SeasonData sd = new SeasonData();
                    sd.setSeasonName(entry.getKey());
                    sd.setEpisodes(entry.getValue());
                    this.seasons_data.add(sd);
                }
            }
        }
    }

}
