package fr.bl1nd.room;

import fr.bl1nd.playlist.Track;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddPlaylistRequest(@NotBlank String playerName, @NotBlank String playlistId,
                                 @NotBlank String playlistTitle, @NotEmpty List<Track> tracks) {}
