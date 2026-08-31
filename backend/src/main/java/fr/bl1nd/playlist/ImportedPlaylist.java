package fr.bl1nd.playlist;

import java.util.List;

public record ImportedPlaylist(String id, String title, List<Track> tracks) {}
