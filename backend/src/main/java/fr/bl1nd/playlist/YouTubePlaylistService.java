package fr.bl1nd.playlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class YouTubePlaylistService {
    private static final String API_URL = "https://www.googleapis.com/youtube/v3/playlistItems";
    private static final Logger logger = LoggerFactory.getLogger(YouTubePlaylistService.class);
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;

    public YouTubePlaylistService(ObjectMapper mapper, @Value("${youtube.api-key}") String apiKey) {
        this.mapper = mapper;
        this.apiKey = apiKey;
    }

    public ImportedPlaylist importPlaylist(String playlistUrl) {
        if (apiKey.isBlank()) {
            logger.error("Import impossible : aucune clé YouTube configurée.");
            throw new PlaylistImportException("La clé YouTube n'est pas configurée. Définissez YOUTUBE_API_KEY.");
        }
        String playlistId = PlaylistUrlParser.extractPlaylistId(playlistUrl);
        logger.info("Début de l'import de la playlist {}.", playlistId);
        List<Track> tracks = new ArrayList<>();
        String token = null;
        String title = "Playlist YouTube";
        do {
            JsonNode page = fetchPage(playlistId, token);
            for (JsonNode item : page.path("items")) {
                JsonNode snippet = item.path("snippet");
                String videoId = snippet.path("resourceId").path("videoId").asText();
                String videoTitle = snippet.path("title").asText();
                if (!videoId.isBlank() && !"Private video".equalsIgnoreCase(videoTitle) && !"Deleted video".equalsIgnoreCase(videoTitle)) {
                    JsonNode thumbnails = snippet.path("thumbnails");
                    String thumbnail = thumbnails.path("medium").path("url").asText("");
                    if (thumbnail.isBlank()) thumbnail = thumbnails.path("high").path("url").asText("");
                    if (thumbnail.isBlank()) thumbnail = thumbnails.path("default").path("url").asText("");
                    tracks.add(new Track(videoId, videoTitle, snippet.path("videoOwnerChannelTitle").asText(), thumbnail));
                }
                if ("Playlist YouTube".equals(title)) title = snippet.path("playlistTitle").asText(title);
            }
            token = page.path("nextPageToken").asText(null);
        } while (token != null);
        logger.info("Playlist {} importée : {} morceau(x) jouable(s).", playlistId, tracks.size());
        return new ImportedPlaylist(playlistId, title, tracks);
    }

    private JsonNode fetchPage(String playlistId, String token) {
        UriComponentsBuilder url = UriComponentsBuilder.fromHttpUrl(API_URL).queryParam("part", "snippet").queryParam("playlistId", playlistId).queryParam("maxResults", 50).queryParam("key", apiKey);
        if (token != null) url.queryParam("pageToken", token);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.toUriString())).timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Réponse YouTube pour la playlist {} : HTTP {}.", playlistId, response.statusCode());
            if (response.statusCode() != 200) {
                logger.error("Erreur YouTube pour la playlist {} : {}.", playlistId, youtubeError(response.body()));
                throw new PlaylistImportException("YouTube n'a pas pu charger cette playlist (code " + response.statusCode() + ").");
            }
            return mapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PlaylistImportException("L'import de la playlist a été interrompu.", exception);
        } catch (PlaylistImportException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PlaylistImportException("Impossible de contacter YouTube.", exception);
        }
    }

    private String youtubeError(String responseBody) {
        try {
            JsonNode error = mapper.readTree(responseBody).path("error");
            String reason = error.path("errors").path(0).path("reason").asText("");
            String message = error.path("message").asText("");
            if (!reason.isBlank() && !message.isBlank()) return reason + " - " + message;
            return !message.isBlank() ? message : "Réponse d'erreur sans détail.";
        } catch (Exception exception) {
            return "Réponse d'erreur illisible.";
        }
    }
}
