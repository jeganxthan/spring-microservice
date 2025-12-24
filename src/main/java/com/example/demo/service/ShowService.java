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
@Transactional(readOnly = true)
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
        Show show = showRepository.findByShowTitle(seriesName);
        if (show == null || show.getSeasons_data() == null) {
            return new ArrayList<>();
        }

        return show.getSeasons_data().stream()
                .map(seasonData -> {
                    int episodeCount = seasonData.getEpisodes() != null ? seasonData.getEpisodes().size() : 0;
                    if (includeEpisodes) {
                        List<EpisodeDTO> episodeDTOs = seasonData.getEpisodes() != null
                                ? seasonData.getEpisodes().stream()
                                        .map(this::toEpisodeDTO)
                                        .collect(Collectors.toList())
                                : new ArrayList<>();
                        return new SeasonDTO(seasonData.getSeasonName(), episodeCount, episodeDTOs);
                    } else {
                        return new SeasonDTO(seasonData.getSeasonName(), episodeCount);
                    }
                })
                .collect(Collectors.toList());
    }

    private EpisodeDTO toEpisodeDTO(com.example.demo.model.Episode episode) {
        if (episode == null) return null;
        // Assuming Episode entity doesn't have episodeIndex, passing null or 0. 
        // If needed, we might need to add an index field to the Episode entity or infer it.
        // For now, using null as it was in the JDBC row mapper if column was missing.
        Integer idx = null; 
        return new EpisodeDTO(
            idx,
            episode.getTitle(),
            episode.getDescription(),
            episode.getDuration(),
            episode.getImageUrl(),
            episode.getFilename(),
            episode.getUrl()
        );
    }
}
