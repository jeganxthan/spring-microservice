// src/main/java/com/example/demo/repository/EpisodeJdbcRepository.java
package com.example.demo.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.EpisodeDTO;

@Repository
public class EpisodeJdbcRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int countEpisodesForSeason(String showTitle, String seasonName) {
        String sql = "SELECT COUNT(*) FROM episodes WHERE show_title = ? AND season_name = ?";
        Integer cnt = jdbcTemplate.queryForObject(sql, new Object[]{showTitle, seasonName}, Integer.class);
        return cnt == null ? 0 : cnt;
    }

    public List<EpisodeDTO> findEpisodesForSeason(String showTitle, String seasonName) {
        String sql = "SELECT episode_index, episode_title, episode_description, episode_duration, episode_image_url, episode_filename, episode_url "
                   + "FROM episodes WHERE show_title = ? AND season_name = ? ORDER BY episode_index";
        return jdbcTemplate.query(sql, new Object[]{showTitle, seasonName}, (rs, rn) -> mapRow(rs));
    }

    private EpisodeDTO mapRow(ResultSet rs) throws SQLException {
        Integer idx = null;
        try { idx = rs.getObject("episode_index") == null ? null : rs.getInt("episode_index"); } catch (Exception ignored) {}
        String title = safeGet(rs, "episode_title");
        String desc = safeGet(rs, "episode_description");
        String dur = safeGet(rs, "episode_duration");
        String img = safeGet(rs, "episode_image_url");
        String fn = safeGet(rs, "episode_filename");
        String url = safeGet(rs, "episode_url");
        return new EpisodeDTO(idx, title, desc, dur, img, fn, url);
    }

    private String safeGet(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }
}
