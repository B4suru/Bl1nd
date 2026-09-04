package fr.bl1nd.playlist;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(originPatterns = {"http://localhost:4200", "https://*.onrender.com"})
public class PlaylistController {
    private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);
    private final YouTubePlaylistService service;
    public PlaylistController(YouTubePlaylistService service) { this.service = service; }

    @GetMapping("/health")
    public String health() { return "ok"; }

    @PostMapping("/import")
    public ImportedPlaylist importPlaylist(@Valid @RequestBody ImportPlaylistRequest request) {
        logger.info("Demande d'import reçue pour une playlist YouTube.");
        try {
            ImportedPlaylist playlist = service.importPlaylist(request.playlistUrl());
            logger.info("Import terminé : {} morceau(x) récupéré(s).", playlist.tracks().size());
            return playlist;
        } catch (PlaylistImportException exception) {
            logger.warn("Import refusé : {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
