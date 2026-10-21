import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { MagnetComponent } from './components/ui/magnet/magnet';
import { DecryptedComponent } from './components/ui/decrypted/decrypted';
import { AuthComponent } from './components/auth/auth';
import { DashboardComponent } from './components/dashboard/dashboard';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, MagnetComponent, DecryptedComponent, AuthComponent, DashboardComponent],
  template: `
    <div class="relative min-h-screen text-slate-100 bg-[#090a0f] overflow-x-hidden font-sans">
      <!-- Dynamic Background Grid -->
      <div class="fixed inset-0 -z-10 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:40px_40px] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_0%,#000_70%,transparent_100%)]"></div>
      <div class="fixed top-0 left-0 right-0 h-[500px] bg-gradient-to-b from-indigo-500/10 via-transparent to-transparent -z-10"></div>

      <!-- Top Navbar -->
      <nav class="fixed top-0 left-0 right-0 z-50 glass-panel border-b border-white/5 px-6 h-16 flex items-center justify-between">
        <app-magnet [strength]="0.2">
          <div class="flex items-center gap-2 cursor-pointer">
            <div class="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 3v18"/></svg>
            </div>
            <app-decrypted text="NexusPay" class="font-bold text-lg text-white" />
          </div>
        </app-magnet>

        @if (auth.isAuthenticated()) {
          <div class="flex items-center gap-4">
            <div class="hidden sm:flex flex-col items-end">
              <span class="text-sm font-medium text-slate-200">{{ auth.user().email }}</span>
              <span class="text-[10px] text-slate-500 uppercase tracking-widest font-bold">Lojista Conectado</span>
            </div>
            <app-magnet [strength]="0.3">
              <button (click)="auth.logout()" class="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-white/5" title="Sair">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
              </button>
            </app-magnet>
          </div>
        }
      </nav>

      <!-- Main Content -->
      <main class="container mx-auto px-4 pt-24 pb-12">
        @if (auth.isAuthenticated()) {
          <app-dashboard />
        } @else {
          <app-auth />
        }
      </main>
    </div>
  `
})
export class AppComponent {
  constructor(public auth: AuthService) {}
}
export { AppComponent as App };
