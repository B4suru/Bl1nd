package fr.bl1nd.room;

import fr.bl1nd.playlist.Track;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateRoomRequest(
        String playlistId,
        String playlistTitle,
        List<Track> tracks,
        @NotBlank String hostName,
        @Min(5) int guessDurationSeconds,
        @Min(1) int revealDurationSeconds,
        @Min(0) int musicCount) {}
