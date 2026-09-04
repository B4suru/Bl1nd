package fr.bl1nd.room;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:4200")
public class RoomController {
    private final RoomService service;
    public RoomController(RoomService service) { this.service = service; }

    @PostMapping
    public Room create(@Valid @RequestBody CreateRoomRequest request) { return execute(() -> service.create(request)); }
    @PostMapping("/{code}/join")
    public Room join(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) { return execute(() -> service.join(code, request)); }
    @GetMapping("/{code}")
    public Room get(@PathVariable String code) { return execute(() -> service.get(code)); }
    @PostMapping("/{code}/start")
    public Room start(@PathVariable String code) { return execute(() -> service.start(code)); }
    @PutMapping("/{code}/rules")
    public Room updateRules(@PathVariable String code, @Valid @RequestBody UpdateRoomRequest request) { return execute(() -> service.update(code, request)); }
    @PostMapping("/{code}/playlists")
    public Room addPlaylist(@PathVariable String code, @Valid @RequestBody AddPlaylistRequest request) { return execute(() -> service.addPlaylist(code, request)); }

    private Room execute(java.util.function.Supplier<Room> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
