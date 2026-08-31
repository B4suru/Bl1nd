# Bl1nd

Application de blind test multijoueur, construite avec un backend Java/Spring Boot et un frontend Angular.

## Première étape : importer une playlist YouTube

L'API récupère les titres et liens des vidéos d'une playlist YouTube via YouTube Data API v3.

1. Créez une clé API dans Google Cloud et activez **YouTube Data API v3**.
2. Définissez la variable d'environnement `YOUTUBE_API_KEY`.
3. Lancez le backend avec `cd backend; mvn spring-boot:run`.
4. Lancez le frontend avec `cd frontend; npm install; npm start`.

L'interface est disponible sur `http://localhost:4200`.

## API

`POST http://localhost:8080/api/playlists/import`

```json
{ "playlistUrl": "https://www.youtube.com/playlist?list=PL..." }
```
