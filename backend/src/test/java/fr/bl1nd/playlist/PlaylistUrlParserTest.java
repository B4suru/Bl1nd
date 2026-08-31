package fr.bl1nd.playlist;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlaylistUrlParserTest {
    @Test void extractsPlaylistId() { assertEquals("PLabc_123", PlaylistUrlParser.extractPlaylistId("https://www.youtube.com/playlist?list=PLabc_123")); }
    @Test void rejectsNonYoutubeUrl() { assertThrows(PlaylistImportException.class, () -> PlaylistUrlParser.extractPlaylistId("https://example.com/?list=PLabc")); }
}
