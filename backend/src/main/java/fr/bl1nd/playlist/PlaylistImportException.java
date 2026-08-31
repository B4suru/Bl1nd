package fr.bl1nd.playlist;

public class PlaylistImportException extends RuntimeException {
    public PlaylistImportException(String message) { super(message); }
    public PlaylistImportException(String message, Throwable cause) { super(message, cause); }
}
