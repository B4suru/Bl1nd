package fr.bl1nd.room;

import fr.bl1nd.playlist.Track;
import java.util.List;

public record RoomPlaylist(String id, String title, String owner, List<Track> tracks) {}
