package fr.bl1nd.playlist;

import jakarta.validation.constraints.NotBlank;

public record ImportPlaylistRequest(@NotBlank(message = "Le lien de playlist est obligatoire") String playlistUrl) {}
