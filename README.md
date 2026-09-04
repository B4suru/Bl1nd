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

## Salles multijoueur

Les salles sont actuellement stockées en mémoire (elles sont supprimées au redémarrage du backend).

- `POST /api/rooms` crée une salle et renvoie un code à 6 chiffres.
- `POST /api/rooms/{code}/join` permet à un joueur de rejoindre la salle.
- `GET /api/rooms/{code}` consulte les joueurs et les règles.
- `POST /api/rooms/{code}/start` lance la partie côté hôte.

## Déploiement

Le projet contient un `Dockerfile` pour chaque application et un `docker-compose.yml` qui sert le frontend et reverse-proxy `/api` vers le backend.

1. Installez Docker Desktop.
2. Créez une variable d'environnement `YOUTUBE_API_KEY` avec votre clé YouTube.
3. Depuis la racine du projet, lancez `docker compose up --build -d`.
4. Ouvrez `http://localhost` pour vérifier le site.

Pour obtenir un lien partageable, déployez ce dépôt sur un serveur disposant de Docker (par exemple une VM Hetzner, Render ou Railway), ouvrez le port 80/443, puis utilisez un domaine et HTTPS. Les salles étant en mémoire, un redémarrage du conteneur les supprime ; une base de données sera nécessaire pour une conservation durable.
