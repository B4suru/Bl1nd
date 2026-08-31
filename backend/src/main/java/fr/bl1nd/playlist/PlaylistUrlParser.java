package fr.bl1nd.playlist;

import java.net.URI;
import java.net.URISyntaxException;

final class PlaylistUrlParser {
    private PlaylistUrlParser() {}

    static String extractPlaylistId(String rawUrl) {
        try {
            URI uri = new URI(rawUrl.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().replaceFirst("^www\\.", "");
            if (!"youtube.com".equalsIgnoreCase(host) && !"music.youtube.com".equalsIgnoreCase(host)) {
                throw new PlaylistImportException("Le lien doit provenir de YouTube.");
            }
            String query = uri.getRawQuery();
            if (query != null) for (String parameter : query.split("&")) {
                String[] pair = parameter.split("=", 2);
                if (pair.length == 2 && "list".equals(pair[0]) && !pair[1].isBlank()) return pair[1];
            }
        } catch (URISyntaxException exception) {
            throw new PlaylistImportException("Le lien de playlist est invalide.");
        }
        throw new PlaylistImportException("Aucun identifiant de playlist n'a été trouvé dans ce lien.");
    }
}
