import { Component, OnDestroy, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface Track { videoId: string; title: string; channelTitle: string; thumbnailUrl: string; }
interface ImportedPlaylist { id: string; title: string; tracks: Track[]; }
interface RoomPlaylist { id: string; title: string; owner: string; tracks: Track[]; }
interface Room { code: string; playlistId: string; playlistTitle: string; tracks: Track[]; playlists: RoomPlaylist[]; guessDurationSeconds: number; revealDurationSeconds: number; musicCount: number; players: string[]; started: boolean; phase: 'ready' | 'playing' | 'revealed' | 'completed'; roundNumber: number; currentTrack: Track | null; phaseEndsAt: number; excerptStartSeconds: number; }
interface YouTubePlayer { loadVideoById(videoId: string | { videoId: string; startSeconds: number }): void; playVideo(): void; setVolume(volume: number): void; stopVideo(): void; }
declare global { interface Window { YT?: { Player: new (elementId: string, options: object) => YouTubePlayer }; onYouTubeIframeAPIReady?: () => void; __BL1ND_API_URL__?: string; } }

@Component({ selector: 'app-root', imports: [FormsModule], templateUrl: './app.component.html', styleUrl: './app.component.css' })
export class AppComponent implements OnDestroy {
  private readonly apiBaseUrl = window.__BL1ND_API_URL__ || (window.location.hostname === 'localhost' ? 'http://localhost:8080' : '');
  playlistUrl = ''; playlist = signal<ImportedPlaylist | null>(null); room = signal<Room | null>(null);
  error = signal(''); loading = signal(false); view = signal<'home' | 'room' | 'game'>('home');
  phase = signal<'ready' | 'playing' | 'revealed' | 'completed'>('ready'); currentTrack = signal<Track | null>(null);
  secondsLeft = signal(15); roundNumber = signal(0); showTracks = signal(false); showGlobalTracks = signal(false);
  volume = 70;
  guessDurationSeconds = 15; revealDurationSeconds = 20; musicCount = 0;
  playerName = ''; joinCode = ''; isHost = false; multiplayer = false;
  private player?: YouTubePlayer; private roomPoll?: ReturnType<typeof setInterval>; private syncedRound = 0; private syncedPhase = '';
  private soloTimer?: ReturnType<typeof setInterval>; private soloEnd?: ReturnType<typeof setTimeout>;

  constructor(private readonly http: HttpClient) {}
  chooseMode(multiplayer: boolean): void { this.multiplayer = multiplayer; this.error.set(''); this.view.set('room'); }
  importPlaylist(): void {
    if (!this.playlistUrl.trim()) return;
    this.loading.set(true); this.error.set('');
    this.http.post<ImportedPlaylist>(`${this.apiBaseUrl}/api/playlists/import`, { playlistUrl: this.playlistUrl }).subscribe({
      next: playlist => {
        this.playlist.set(playlist);
        if (!this.room()) this.revealDurationSeconds = this.guessDurationSeconds + 5;
        this.loading.set(false);
      },
      error: response => { this.error.set(response.error?.detail ?? 'Import impossible.'); this.loading.set(false); }
    });
  }
  thumbnailFor(track: Track): string { return track.thumbnailUrl || `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg`; }
  toggleTracks(): void { this.showTracks.update(value => !value); }
  toggleGlobalTracks(): void { this.showGlobalTracks.update(value => !value); }
  setVolume(): void { this.player?.setVolume(this.volume); }
  createRoom(): void {
    const playlist = this.playlist();
    if (this.multiplayer && !this.playerName.trim()) { this.error.set('Indiquez votre nom pour créer une salle.'); return; }
    this.http.post<Room>(`${this.apiBaseUrl}/api/rooms`, { playlistId: playlist?.id ?? '', playlistTitle: playlist?.title ?? '', tracks: playlist?.tracks ?? [], hostName: this.playerName.trim(), guessDurationSeconds: this.guessDurationSeconds, revealDurationSeconds: this.revealDurationSeconds, musicCount: this.musicCount }).subscribe({
      next: room => { this.isHost = true; this.room.set(room); this.view.set('room'); this.startPolling(); },
      error: response => this.error.set(response.error?.message ?? 'Impossible de créer la salle.')
    });
  }
  joinRoom(): void {
    const playlist = this.playlist();
    if (!this.joinCode.trim() || !this.playerName.trim()) { this.error.set('Indiquez votre nom et le code.'); return; }
    this.http.post<Room>(`${this.apiBaseUrl}/api/rooms/${this.joinCode.trim()}/join`, { playerName: this.playerName.trim(), playlistId: playlist?.id ?? 'pending', playlistTitle: playlist?.title ?? 'Playlist à ajouter', tracks: playlist?.tracks ?? [] }).subscribe({
      next: room => { this.isHost = false; this.room.set(room); this.view.set('room'); this.startPolling(); },
      error: response => this.error.set(response.error?.message ?? 'Impossible de rejoindre la salle.')
    });
  }
  addPlaylistToRoom(): void {
    const room = this.room(), playlist = this.playlist();
    if (!room || !playlist) { this.error.set('Importez une playlist à ajouter à la salle.'); return; }
    this.http.post<Room>(`${this.apiBaseUrl}/api/rooms/${room.code}/playlists`, { playerName: this.playerName.trim(), playlistId: playlist.id, playlistTitle: playlist.title, tracks: playlist.tracks }).subscribe({
      next: updated => { this.room.set(updated); this.playlist.set(null); this.playlistUrl = ''; },
      error: response => this.error.set(response.error?.message ?? 'Impossible d’ajouter la playlist.')
    });
  }
  updateRules(): void {
    const room = this.room(); if (!room || !this.isHost) return;
    this.http.put<Room>(`${this.apiBaseUrl}/api/rooms/${room.code}/rules`, { playerName: this.playerName.trim(), guessDurationSeconds: this.guessDurationSeconds, revealDurationSeconds: this.revealDurationSeconds, musicCount: this.musicCount }).subscribe({
      next: updated => this.room.set(updated), error: response => this.error.set(response.error?.message ?? 'Règles invalides.')
    });
  }
  startGame(): void {
    if (!this.multiplayer) {
      const playlist = this.playlist();
      if (!playlist) { this.error.set('Ajoutez une playlist avant de lancer la partie.'); return; }
      const tracks = [...playlist.tracks].sort(() => Math.random() - 0.5);
      const selected = this.musicCount ? tracks.slice(0, this.musicCount) : tracks;
      this.room.set({ code: 'SOLO', playlistId: playlist.id, playlistTitle: playlist.title, tracks: selected, playlists: [{ id: playlist.id, title: playlist.title, owner: 'Vous', tracks: playlist.tracks }], guessDurationSeconds: this.guessDurationSeconds, revealDurationSeconds: this.revealDurationSeconds, musicCount: this.musicCount, players: ['Vous'], started: true, phase: 'playing', roundNumber: 1, currentTrack: selected[0], phaseEndsAt: Date.now() + this.guessDurationSeconds * 1000, excerptStartSeconds: 10 });
      this.view.set('game'); this.startSoloRound(); return;
    }
    const room = this.room(); if (!room) return;
    if (!this.multiplayer) { this.view.set('game'); return; }
    this.http.post<Room>(`${this.apiBaseUrl}/api/rooms/${room.code}/start`, {}).subscribe({
      next: updated => { this.room.set(updated); this.syncRoom(updated); }, error: response => this.error.set(response.error?.message ?? 'Impossible de lancer la partie.')
    });
  }
  private startSoloRound(): void {
    const room = this.room(); if (!room || !room.currentTrack) return;
    this.phase.set('playing'); this.currentTrack.set(room.currentTrack); this.secondsLeft.set(this.guessDurationSeconds);
    void this.play(room.currentTrack, 10);
    this.soloTimer = setInterval(() => this.secondsLeft.update(value => Math.max(0, value - 1)), 1000);
    this.soloEnd = setTimeout(() => this.revealSolo(), this.guessDurationSeconds * 1000);
  }
  private revealSolo(): void {
    const room = this.room(); if (!room) return;
    if (this.soloTimer) clearInterval(this.soloTimer);
    this.phase.set('revealed'); this.secondsLeft.set(this.revealDurationSeconds);
    this.soloTimer = setInterval(() => this.secondsLeft.update(value => Math.max(0, value - 1)), 1000);
    this.soloEnd = setTimeout(() => {
      if (this.soloTimer) clearInterval(this.soloTimer);
      const next = room.roundNumber;
      if (next >= room.tracks.length) { this.phase.set('completed'); return; }
      const updated = { ...room, roundNumber: next + 1, currentTrack: room.tracks[next] };
      this.room.set(updated); this.startSoloRound();
    }, this.revealDurationSeconds * 1000);
  }
  private startPolling(): void {
    if (this.roomPoll) clearInterval(this.roomPoll);
    this.roomPoll = setInterval(() => { const room = this.room(); if (!room) return; this.http.get<Room>(`${this.apiBaseUrl}/api/rooms/${room.code}`).subscribe({ next: updated => { this.room.set(updated); this.syncRoom(updated); }, error: () => this.error.set('La connexion à la salle a été perdue.') }); }, 1000);
  }
  private syncRoom(room: Room): void {
    if (!this.isHost) {
      this.guessDurationSeconds = room.guessDurationSeconds;
      this.revealDurationSeconds = room.revealDurationSeconds;
      this.musicCount = room.musicCount;
    }
    this.phase.set(room.phase); this.roundNumber.set(room.roundNumber); this.currentTrack.set(room.currentTrack);
    this.secondsLeft.set(room.phase === 'completed' ? 0 : Math.max(0, Math.ceil((room.phaseEndsAt - Date.now()) / 1000)));
    if (room.phase === this.syncedPhase && room.roundNumber === this.syncedRound) return;
    this.syncedPhase = room.phase; this.syncedRound = room.roundNumber;
    if (room.phase !== 'ready') this.view.set('game');
    if (room.currentTrack && (room.phase === 'playing' || room.phase === 'revealed')) {
      setTimeout(() => void this.play(room.currentTrack!, room.excerptStartSeconds), 0);
    }
    if (room.phase === 'completed') this.player?.stopVideo();
  }
  private async play(track: Track, startSeconds: number): Promise<void> {
    try {
      const player = await this.getPlayer();
      player.loadVideoById({ videoId: track.videoId, startSeconds });
      player.setVolume(this.volume);
      player.playVideo();
    } catch {
      this.error.set('Le lecteur YouTube n’a pas pu être chargé.');
    }
  }
  private playerReady?: Promise<YouTubePlayer>;
  private getPlayer(): Promise<YouTubePlayer> {
    if (this.player) return Promise.resolve(this.player);
    if (this.playerReady) return this.playerReady;
    this.playerReady = new Promise((resolve, reject) => { const create = () => { try { this.player = new window.YT!.Player('youtube-audio-player', { height: '1', width: '1', playerVars: { autoplay: 1, controls: 0, disablekb: 1, playsinline: 1, rel: 0, origin: window.location.origin }, events: { onReady: () => { this.player!.setVolume(this.volume); resolve(this.player!); } } }); } catch { reject(); } };
      if (window.YT?.Player) create(); else { window.onYouTubeIframeAPIReady = create; const script = document.createElement('script'); script.src = 'https://www.youtube.com/iframe_api'; script.onerror = () => reject(); document.head.appendChild(script); }
    });
    return this.playerReady;
  }
  leaveRoom(): void { if (this.roomPoll) clearInterval(this.roomPoll); this.room.set(null); this.playlist.set(null); this.view.set('home'); this.player?.stopVideo(); }
  ngOnDestroy(): void { if (this.roomPoll) clearInterval(this.roomPoll); if (this.soloTimer) clearInterval(this.soloTimer); if (this.soloEnd) clearTimeout(this.soloEnd); this.player?.stopVideo(); }
}
