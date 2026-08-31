import { Component, OnDestroy, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

interface Track { videoId: string; title: string; channelTitle: string; thumbnailUrl: string; }
interface ImportedPlaylist { id: string; title: string; tracks: Track[]; }

interface YouTubePlayer {
  loadVideoById(videoId: string | { videoId: string; startSeconds: number }): void;
  stopVideo(): void;
}

declare global {
  interface Window { YT?: { Player: new (elementId: string, options: object) => YouTubePlayer }; onYouTubeIframeAPIReady?: () => void; }
}

@Component({ selector: 'app-root', imports: [FormsModule], templateUrl: './app.component.html', styleUrl: './app.component.css' })
export class AppComponent implements OnDestroy {
  playlistUrl = ''; playlist = signal<ImportedPlaylist | null>(null); error = signal(''); loading = signal(false);
  view = signal<'preparation' | 'game'>('preparation');
  phase = signal<'ready' | 'playing' | 'revealed' | 'completed'>('ready');
  currentTrack = signal<Track | null>(null);
  secondsLeft = signal(10);
  roundNumber = signal(0);
  showTracks = signal(false);
  private excerptStartSeconds = 10;
  private roundOrder: Track[] = [];
  private player?: YouTubePlayer;
  private timer?: ReturnType<typeof setInterval>;
  private revealTimer?: ReturnType<typeof setTimeout>;
  constructor(private readonly http: HttpClient) {}
  importPlaylist(): void {
    if (!this.playlistUrl.trim()) return;
    this.loading.set(true); this.error.set(''); this.playlist.set(null);
    this.http.post<ImportedPlaylist>('http://localhost:8080/api/playlists/import', { playlistUrl: this.playlistUrl }).subscribe({
        next: playlist => { this.playlist.set(playlist); this.phase.set('ready'); this.showTracks.set(false); this.loading.set(false); },
      error: response => { this.error.set(response.error?.detail ?? 'Import impossible.'); this.loading.set(false); }
    });
  }

  toggleTracks(): void { this.showTracks.update(value => !value); }

  thumbnailFor(track: Track): string {
    return track.thumbnailUrl || `https://i.ytimg.com/vi/${track.videoId}/hqdefault.jpg`;
  }

  openGameRoom(): void {
    if (!this.playlist()?.tracks.length) {
      this.error.set("Aucune vidéo de cette playlist n'est autorisée par YouTube dans un lecteur intégré.");
      return;
    }
    this.error.set('');
    this.phase.set('ready');
    this.roundNumber.set(0);
    this.roundOrder = [...this.playlist()!.tracks].sort(() => Math.random() - 0.5);
    this.view.set('game');
  }

  backToPreparation(): void {
    this.clearTimers();
    this.player?.stopVideo();
    this.phase.set('ready');
    this.view.set('preparation');
  }

  async startRound(): Promise<void> {
    if (!this.roundOrder.length) { this.error.set('Cette playlist ne contient aucun morceau jouable.'); return; }
    if (this.roundNumber() >= this.roundOrder.length) {
      this.clearTimers();
      this.player?.stopVideo();
      this.phase.set('completed');
      return;
    }
    this.clearTimers();
    this.currentTrack.set(this.roundOrder[this.roundNumber()]);
    this.roundNumber.update(value => value + 1);
    this.phase.set('playing');
    this.secondsLeft.set(10);
    try {
      const player = await this.getPlayer();
      this.excerptStartSeconds = Math.floor(Math.random() * 30) + 10;
      player.loadVideoById({ videoId: this.currentTrack()!.videoId, startSeconds: this.excerptStartSeconds });
      this.timer = setInterval(() => this.secondsLeft.update(seconds => Math.max(0, seconds - 1)), 1_000);
      this.revealTimer = setTimeout(() => this.reveal(), 10_000);
    } catch {
      this.phase.set('ready');
      this.error.set('Le lecteur YouTube n’a pas pu être chargé. Vérifiez votre connexion ou les restrictions du navigateur.');
    }
  }

  reveal(): void {
    this.clearTimers();
    const track = this.currentTrack();
    if (!track) return;
    this.phase.set('revealed');
    this.secondsLeft.set(15);
    this.player?.loadVideoById({ videoId: track.videoId, startSeconds: this.excerptStartSeconds });
    this.timer = setInterval(() => this.secondsLeft.update(seconds => Math.max(0, seconds - 1)), 1_000);
    this.revealTimer = setTimeout(() => void this.startRound(), 15_000);
  }

  ngOnDestroy(): void { this.clearTimers(); this.player?.stopVideo(); }

  private clearTimers(): void {
    if (this.timer) clearInterval(this.timer);
    if (this.revealTimer) clearTimeout(this.revealTimer);
    this.timer = undefined; this.revealTimer = undefined;
  }

  private getPlayer(): Promise<YouTubePlayer> {
    if (this.player) return Promise.resolve(this.player);
    return new Promise((resolve, reject) => {
      const createPlayer = () => {
        try {
          this.player = new window.YT!.Player('youtube-audio-player', {
            height: '1', width: '1',
            playerVars: { autoplay: 1, controls: 0, disablekb: 1, playsinline: 1, rel: 0, origin: window.location.origin },
            events: { onReady: () => resolve(this.player!) }
          });
        } catch { reject(); }
      };
      if (window.YT?.Player) { createPlayer(); return; }
      window.onYouTubeIframeAPIReady = createPlayer;
      const script = document.createElement('script');
      script.src = 'https://www.youtube.com/iframe_api';
      script.onerror = () => reject();
      document.head.appendChild(script);
    });
  }
}
