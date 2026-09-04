package fr.bl1nd.room;

import fr.bl1nd.playlist.Track;
import java.util.List;

public record Room(String code, String playlistId, String playlistTitle, List<Track> tracks,
                   int guessDurationSeconds, int revealDurationSeconds, int musicCount,
                   List<String> players, boolean started, String phase, int roundNumber,
                   Track currentTrack, long phaseEndsAt, int excerptStartSeconds,
                   List<RoomPlaylist> playlists) {}
