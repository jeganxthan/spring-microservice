package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.EpisodeDTO;
import com.example.demo.dto.SeasonDTO;
import com.example.demo.dto.ShowMinimalDTO;
import com.example.demo.model.Show;
import com.example.demo.repository.ShowRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

@Service
public class ShowService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ShowRepository showRepository;  // Inject the repository to save shows in the database

    private static final String ALL_SHOWS_KEY = "all_shows";
    private static final String SHOW_SEASON_KEY_PREFIX = "season_";

    // Fetch all shows (cached globally in Redis)
    public List<Show> getAllShows() {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // 1️⃣ First try Redis cache
        List<Show> cached = (List<Show>) ops.get(ALL_SHOWS_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2️⃣ Fetch from DATABASE always
        List<Show> dbShows = showRepository.findAll();

        // 3️⃣ Cache DB results for next request
        ops.set(ALL_SHOWS_KEY, dbShows);

        return dbShows;
    }

    public List<ShowMinimalDTO> getAllShowsMinimal() {
        List<Show> dbShows = showRepository.findAll();

        return dbShows.stream()
                .map(show -> new ShowMinimalDTO(
                show.getShowTitle(),
                show.getPoster()
        ))
                .toList();
    }

    // Load shows from the breaking_bad.json file and persist them to the database
    // Cache the list of shows in Redis
    private void cacheAllShows(List<Show> shows) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(ALL_SHOWS_KEY, shows);  // Cache the list of shows
        System.out.println("All shows cached in Redis.");
    }

    // Load shows from the BreakingBad.json file and persist them to the database
    // Cache the list of shows in Redis
    @Transactional
    public void loadShowsFromJson() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        ObjectMapper mapper = new ObjectMapper();

        List<Show> allParsedShows = new ArrayList<>();
        int filesProcessed = 0;

        try {
            Resource[] resources = resolver.getResources("classpath:/static/*.json");
            if (resources == null || resources.length == 0) {
                System.out.println("No JSON files found under classpath:/static/");
                return;
            }

            CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, Show.class);

            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                filesProcessed++;
                try (InputStream is = resource.getInputStream()) {
                    // Try to parse as array of Show objects
                    try {
                        List<Show> shows = mapper.readValue(is, listType);
                        if (shows != null) {
                            for (Show show : shows) {
                                if (show == null || show.getShowTitle() == null) {
                                    System.out.println("Skipping show with null title from file " + resource.getFilename());
                                    continue;
                                }
                                // save if not exists
                                Show existingShow = showRepository.findByShowTitle(show.getShowTitle());
                                if (existingShow == null) {
                                    showRepository.save(show);
                                    System.out.println("Saved show: " + show.getShowTitle());
                                    System.out.println("Skipping duplicate show: " + show.getShowTitle());
                                }
                            }
                            allParsedShows.addAll(shows.stream().filter(s -> s != null && s.getShowTitle() != null).collect(Collectors.toList()));
                            System.out.println("Processed file (array): " + resource.getFilename() + " => parsed " + shows.size() + " shows");
                            continue; // done with this resource
                        }
                    } catch (JsonMappingException arrayEx) {
                        // fall through to try single-object parsing
                        // (some files might have a single JSON object representing a Show)
                    }

                    // Re-open stream for single-object attempt (resource.getInputStream() was consumed)
                    try (InputStream is2 = resource.getInputStream()) {
                        // Try to parse as single Show object
                        try {
                            Show single = mapper.readValue(is2, Show.class);
                            if (single != null && single.getShowTitle() != null) {
                                Show existingShow = showRepository.findByShowTitle(single.getShowTitle());
                                if (existingShow == null) {
                                    showRepository.save(single);
                                    System.out.println("Saved show: " + single.getShowTitle());
                                    System.out.println("Skipping duplicate show: " + single.getShowTitle());
                                }
                                allParsedShows.add(single);
                                System.out.println("Processed file (single): " + resource.getFilename());
                            } else {
                                System.out.println("Parsed single object from " + resource.getFilename() + " but it had null title — skipped.");
                            }
                        } catch (JsonMappingException singleEx) {
                            // File is not in expected Show/Show[] shape; attempt to inspect root for an array node named e.g. "shows"
                            try (InputStream is3 = resource.getInputStream()) {
                                JsonNode root = mapper.readTree(is3);
                                if (root != null) {
                                    // attempt to find an array node in the JSON (common keys like "shows" or first array child)
                                    JsonNode arrayNode = null;
                                    if (root.isArray()) {
                                        arrayNode = root;
                                    } else if (root.has("shows") && root.get("shows").isArray()) {
                                        arrayNode = root.get("shows");
                                    } else {
                                        // pick the first array child if present
                                        for (JsonNode child : root) {
                                            if (child.isArray()) {
                                                arrayNode = child;
                                                break;
                                            }
                                        }
                                    }

                                    if (arrayNode != null && arrayNode.isArray()) {
                                        List<Show> shows = mapper.readerFor(listType).readValue(arrayNode);
                                        for (Show show : shows) {
                                            if (show == null || show.getShowTitle() == null) {
                                                System.out.println("Skipping show with null title from file " + resource.getFilename());
                                                continue;
                                            }
                                            Show existingShow = showRepository.findByShowTitle(show.getShowTitle());
                                            if (existingShow == null) {
                                                showRepository.save(show);
                                                System.out.println("Saved show: " + show.getShowTitle());
                                                System.out.println("Skipping duplicate show: " + show.getShowTitle());
                                            }
                                        }
                                        allParsedShows.addAll(shows.stream().filter(s -> s != null && s.getShowTitle() != null).collect(Collectors.toList()));
                                        System.out.println("Processed file (nested array): " + resource.getFilename() + " => parsed " + shows.size() + " shows");
                                    } else {
                                        System.out.println("File " + resource.getFilename() + " does not contain an array or object shape that can be parsed to Show(s). Skipping.");
                                    }
                                }
                            }
                        }
                    }

                } catch (IOException ex) {
                    System.err.println("Failed to read/parse file " + resource.getFilename() + ": " + ex.getMessage());
                }
            }

            // After processing all files, cache them once (deduplicate before caching if needed)
            // Optionally deduplicate combined list by title
            Map<String, Show> uniqueByTitle = allParsedShows.stream()
                    .filter(s -> s != null && s.getShowTitle() != null)
                    .collect(Collectors.toMap(
                            s -> s.getShowTitle().toLowerCase(),
                            s -> s,
                            (existing, replacement) -> existing // keep first encountered
                    ));
            List<Show> toCache = new ArrayList<>(uniqueByTitle.values());

            if (!toCache.isEmpty()) {
                cacheAllShows(toCache);
            } else {
                System.out.println("No shows parsed to cache.");
            }

            System.out.println("Completed processing " + filesProcessed + " JSON files from classpath:/static/");

        } catch (IOException e) {
            System.err.println("Error scanning classpath for JSON files: " + e.getMessage());
        }
    }

    // Search shows by title (cached search results)
    public List<Show> searchShowsByTitle(String query) {
        String searchCacheKey = "search_" + query.toLowerCase();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<Show> cachedResults = (List<Show>) ops.get(searchCacheKey);

        if (cachedResults != null) {
            return cachedResults;  // Return cached search results
        } else {
            List<Show> allShows = getAllShows(); // Get all shows from cache or DB
            List<Show> results = allShows.stream()
                    .filter(s -> s.getShowTitle().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());

            // Cache the search results for future queries
            ops.set(searchCacheKey, results);
            return results;
        }
    }

    // Get shows by title (cached by title)
    public List<Show> getShowsByTitle(String title) {
        String titleCacheKey = "show_title_" + title.toLowerCase();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<Show> cachedShows = (List<Show>) ops.get(titleCacheKey);

        if (cachedShows != null) {
            return cachedShows;
        } else {
            List<Show> allShows = getAllShows();
            List<Show> filteredShows = allShows.stream()
                    .filter(show -> show.getShowTitle().toLowerCase().contains(title.toLowerCase()))
                    .collect(Collectors.toList());

            ops.set(titleCacheKey, filteredShows);
            return filteredShows;
        }
    }

    public List<SeasonDTO> getAllSeasonsForSeries(String seriesName, boolean includeEpisodes) {
        // 1) fetch seasons from show_seasons
        String seasonSql = "SELECT season FROM show_seasons WHERE show_title = ? ORDER BY season";
        List<String> seasons = jdbcTemplate.query(seasonSql, new Object[]{seriesName}, (rs, rowNum) -> rs.getString("season"));

        List<SeasonDTO> result = new ArrayList<>();

        // 2) for each season, count episodes and optionally fetch episodes
        String countSql = "SELECT COUNT(*) FROM episodes WHERE show_title = ? AND season_name = ?";
        String episodesSql = "SELECT episode_index, episode_title, episode_description, episode_duration, episode_image_url, episode_filename, episode_url "
                + "FROM episodes WHERE show_title = ? AND season_name = ? ORDER BY episode_index";

        for (String seasonName : seasons) {
            Integer episodeCount = jdbcTemplate.queryForObject(countSql, new Object[]{seriesName, seasonName}, Integer.class);

            if (!includeEpisodes) {
                result.add(new SeasonDTO(seasonName, episodeCount == null ? 0 : episodeCount));
            } else {
                List<EpisodeDTO> eps = jdbcTemplate.query(episodesSql, new Object[]{seriesName, seasonName}, (rs, rowNum) -> toEpisodeDTO(rs));
                result.add(new SeasonDTO(seasonName, episodeCount == null ? 0 : episodeCount, eps));
            }
        }

        return result;
    }

    private EpisodeDTO toEpisodeDTO(ResultSet rs) throws SQLException {
        Integer idx = null;
        try {
            idx = rs.getObject("episode_index") == null ? null : rs.getInt("episode_index");
        } catch (Exception ignored) {
        }
        String title = safeGet(rs, "episode_title");
        String desc = safeGet(rs, "episode_description");
        String dur = safeGet(rs, "episode_duration");
        String img = safeGet(rs, "episode_image_url");
        String fn = safeGet(rs, "episode_filename");
        String url = safeGet(rs, "episode_url");
        return new EpisodeDTO(idx, title, desc, dur, img, fn, url);
    }

    private String safeGet(ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (Exception e) {
            return null;
        }
    }
}
