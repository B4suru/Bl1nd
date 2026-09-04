package fr.bl1nd.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateRoomRequest(@NotBlank String playerName, @Min(5) int guessDurationSeconds,
                                @Min(1) int revealDurationSeconds, @Min(0) int musicCount) {}
