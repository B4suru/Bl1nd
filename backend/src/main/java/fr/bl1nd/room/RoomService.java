package fr.bl1nd.room;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Room create(CreateRoomRequest request) {
        if (request.guessDurationSeconds() > 60 || request.revealDurationSeconds() > 120) {
            throw new IllegalArgumentException("Les paramètres de la salle sont invalides.");
        }
        String code;
        do {
            code = randomCode();
        } while (rooms.containsKey(code));
        String playlistId = request.playlistId() == null ? "" : request.playlistId();
        String playlistTitle = request.playlistTitle() == null ? "" : request.playlistTitle();
        List<fr.bl1nd.playlist.Track> initialTracks = request.tracks() == null ? new ArrayList<>() : request.tracks();
        List<RoomPlaylist> initialPlaylists = initialTracks.isEmpty() ? new ArrayList<>() :
                new ArrayList<>(List.of(new RoomPlaylist(playlistId, playlistTitle, request.hostName(), initialTracks)));
        Room room = new Room(code, playlistId, playlistTitle, initialTracks,
                request.guessDurationSeconds(), request.revealDurationSeconds(), request.musicCount(),
                new ArrayList<>(List.of(request.hostName())), false, "ready", 0, null, 0, 0,
                initialPlaylists);
        rooms.put(code, room);
        return room;
    }

    public Room join(String code, JoinRoomRequest request) {
        return rooms.compute(code, (key, room) -> {
            if (room == null) throw new IllegalArgumentException("Cette salle n'existe pas.");
            if (room.started()) throw new IllegalStateException("La partie a déjà commencé.");
            ArrayList<String> players = new ArrayList<>(room.players());
            if (!players.contains(request.playerName())) players.add(request.playerName());
            ArrayList<fr.bl1nd.playlist.Track> tracks = new ArrayList<>(room.tracks());
            if (request.tracks() != null) tracks.addAll(request.tracks());
            List<RoomPlaylist> playlists = request.tracks() == null || request.tracks().isEmpty()
                    ? room.playlists()
                    : appendPlaylist(room.playlists(), request.playlistId(), request.playlistTitle(), request.playerName(), request.tracks());
            return new Room(room.code(), room.playlistId(), room.playlistTitle(), tracks,
                    room.guessDurationSeconds(), room.revealDurationSeconds(), room.musicCount(), players,
                    room.started(), room.phase(), room.roundNumber(), room.currentTrack(), room.phaseEndsAt(), room.excerptStartSeconds(),
                    playlists);
        });
    }

    public Room get(String code) {
        Room room = rooms.get(code);
        if (room == null) throw new IllegalArgumentException("Cette salle n'existe pas.");
        return advance(room);
    }

    public Room start(String code) {
        return rooms.compute(code, (key, room) -> {
            if (room == null) throw new IllegalArgumentException("Cette salle n'existe pas.");
            if (room.started()) return room;
            List<fr.bl1nd.playlist.Track> tracks = new ArrayList<>(room.tracks());
            if (tracks.isEmpty()) throw new IllegalArgumentException("Ajoutez au moins une playlist avant de lancer la partie.");
            Collections.shuffle(tracks, random);
            int count = room.musicCount() == 0 ? tracks.size() : room.musicCount();
            if (count > tracks.size()) {
                throw new IllegalArgumentException("Le nombre de musiques demandé dépasse le total des playlists.");
            }
            tracks = new ArrayList<>(tracks.subList(0, count));
            long endsAt = System.currentTimeMillis() + room.guessDurationSeconds() * 1_000L;
            return new Room(room.code(), room.playlistId(), room.playlistTitle(), tracks,
                    room.guessDurationSeconds(), room.revealDurationSeconds(), room.musicCount(), room.players(),
                    true, "playing", 1, tracks.get(0), endsAt, random.nextInt(30) + 10, room.playlists());
        });
    }

    private Room advance(Room room) {
        if (!room.started() || room.phase().equals("completed") || System.currentTimeMillis() < room.phaseEndsAt()) return room;
        if (room.phase().equals("playing")) {
            Room revealed = new Room(room.code(), room.playlistId(), room.playlistTitle(), room.tracks(),
                    room.guessDurationSeconds(), room.revealDurationSeconds(), room.musicCount(), room.players(),
                    true, "revealed", room.roundNumber(), room.currentTrack(),
                    System.currentTimeMillis() + room.revealDurationSeconds() * 1_000L, room.excerptStartSeconds(), room.playlists());
            rooms.put(room.code(), revealed);
            return revealed;
        }
        if (room.roundNumber() >= room.tracks().size()) {
            Room completed = new Room(room.code(), room.playlistId(), room.playlistTitle(), room.tracks(),
                    room.guessDurationSeconds(), room.revealDurationSeconds(), room.musicCount(), room.players(),
                    true, "completed", room.roundNumber(), room.currentTrack(), 0, room.excerptStartSeconds(), room.playlists());
            rooms.put(room.code(), completed);
            return completed;
        }
        int index = room.roundNumber();
        Room next = new Room(room.code(), room.playlistId(), room.playlistTitle(), room.tracks(),
                room.guessDurationSeconds(), room.revealDurationSeconds(), room.musicCount(), room.players(),
                true, "playing", room.roundNumber() + 1, room.tracks().get(index),
                System.currentTimeMillis() + room.guessDurationSeconds() * 1_000L, random.nextInt(30) + 10, room.playlists());
        rooms.put(room.code(), next);
        return next;
    }

    private String randomCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public Room update(String code, UpdateRoomRequest request) {
        return rooms.compute(code, (key, room) -> {
            if (room == null) throw new IllegalArgumentException("Cette salle n'existe pas.");
            if (!room.players().get(0).equals(request.playerName())) throw new IllegalStateException("Seul l'hôte peut modifier les règles.");
            if (room.started()) throw new IllegalStateException("Les règles ne peuvent plus être modifiées.");
            if (request.guessDurationSeconds() > 60 || request.revealDurationSeconds() > 120
                    || (request.musicCount() > 0 && request.musicCount() > room.tracks().size())) {
                throw new IllegalArgumentException("Les paramètres de la salle sont invalides.");
            }

            return new Room(room.code(), room.playlistId(), room.playlistTitle(), room.tracks(),
                    request.guessDurationSeconds(), request.revealDurationSeconds(), request.musicCount(), room.players(),
                    false, room.phase(), room.roundNumber(), room.currentTrack(), room.phaseEndsAt(), room.excerptStartSeconds(), room.playlists());
        });
    }

    public Room addPlaylist(String code, AddPlaylistRequest request) {
        return rooms.compute(code, (key, room) -> {
            if (room == null) throw new IllegalArgumentException("Cette salle n'existe pas.");
            if (room.started()) throw new IllegalStateException("La partie a déjà commencé.");
            ArrayList<fr.bl1nd.playlist.Track> tracks = new ArrayList<>(room.tracks());
            tracks.addAll(request.tracks());
            return new Room(room.code(), room.playlistId(), room.playlistTitle(), tracks,
                    room.guessDurationSeconds(), room.revealDurationSeconds(), room.musicCount(), room.players(),
                    false, room.phase(), room.roundNumber(), room.currentTrack(), room.phaseEndsAt(), room.excerptStartSeconds(),
                    appendPlaylist(room.playlists(), request.playlistId(), request.playlistTitle(), request.playerName(), request.tracks()));
        });
    }

    private List<RoomPlaylist> appendPlaylist(List<RoomPlaylist> playlists, String id, String title, String owner, List<fr.bl1nd.playlist.Track> tracks) {
        ArrayList<RoomPlaylist> result = new ArrayList<>(playlists);
        result.add(new RoomPlaylist(id, title, owner, tracks));
        return result;
    }
}
