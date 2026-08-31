package fr.bl1nd.playlist;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "http://localhost:4200")
public class PlaylistController {
    private final YouTubePlaylistService service;
    public PlaylistController(YouTubePlaylistService service) { this.service = service; }

    @PostMapping("/import")
    public ImportedPlaylist importPlaylist(@Valid @RequestBody ImportPlaylistRequest request) {
        try { return service.importPlaylist(request.playlistUrl()); }
        catch (PlaylistImportException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception); }
    }
}
