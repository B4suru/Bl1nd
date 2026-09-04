package fr.bl1nd.room;

import jakarta.validation.constraints.NotBlank;
import fr.bl1nd.playlist.Track;
import java.util.List;

public record JoinRoomRequest(@NotBlank String playerName, @NotBlank String playlistId,
                              @NotBlank String playlistTitle, List<Track> tracks) {}
