import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, AUTH_API } from '../../services/auth.service';
import { SpotlightComponent } from '../ui/spotlight/spotlight';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule, SpotlightComponent],
  template: `
    <div class="max-w-md mx-auto">
      <app-spotlight>
        <div class="p-6 sm:p-8">
          <div class="flex flex-col items-center mb-8">
            <h1 class="text-2xl font-bold text-white mb-2">
              {{ isLogin() ? 'Bem-vindo de volta' : 'Crie sua conta' }}
            </h1>
            <p class="text-slate-400 text-sm text-center">
              Acesse sua plataforma NexusPay e gerencie seus pagamentos com segurança.
            </p>
          </div>

          <!-- Switch Tabs -->
          <div class="flex p-1 bg-white/5 rounded-xl mb-8 relative">
            <button 
              (click)="isLogin.set(true)"
              [class.text-white]="isLogin()"
              [class.bg-slate-800\/50]="isLogin()"
              class="w-1/2 py-2 text-sm font-semibold rounded-lg transition-all text-slate-400 hover:text-slate-200"
            >
              Entrar
            </button>
            <button 
              (click)="isLogin.set(false)"
              [class.text-white]="!isLogin()"
              [class.bg-slate-800\/50]="!isLogin()"
              class="w-1/2 py-2 text-sm font-semibold rounded-lg transition-all text-slate-400 hover:text-slate-200"
            >
              Cadastrar
            </button>
          </div>

          <form (ngSubmit)="onSubmit()" class="space-y-4">
            @if (!isLogin()) {
              <div class="space-y-1">
                <label class="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Empresa</label>
                <input 
                  type="text" 
                  name="company" 
                  [(ngModel)]="company" 
                  required 
                  placeholder="NexusCorp Ltda"
                  class="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-white placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
                />
              </div>
            }

            <div class="space-y-1">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">E-mail</label>
              <input 
                type="email" 
                name="email" 
                [(ngModel)]="email" 
                required 
                placeholder="seu@email.com"
                class="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-white placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
              />
            </div>

            <div class="space-y-1">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Senha</label>
              <input 
                type="password" 
                name="password" 
                [(ngModel)]="password" 
                required 
                minlength="8" 
                placeholder="••••••••"
                class="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-white placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
              />
            </div>

            @if (error()) {
              <div class="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
                {{ error() }}
              </div>
            }

            <button 
              type="submit" 
              [disabled]="loading()"
              class="w-full py-4 mt-6 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold transition-all disabled:opacity-50"
            >
              {{ loading() ? 'Aguarde...' : (isLogin() ? 'Entrar na conta' : 'Criar minha conta') }}
            </button>
          </form>
        </div>
      </app-spotlight>
    </div>
  `
})
export class AuthComponent {
  isLogin = signal(true);
  loading = signal(false);
  error = signal('');

  email = '';
  password = '';
  company = '';

  constructor(private auth: AuthService) {}

  async onSubmit() {
    this.loading.set(true);
    this.error.set('');

    try {
      const path = this.isLogin() ? AUTH_API.login : AUTH_API.register;
      const body = this.isLogin() 
        ? { email: this.email.trim(), password: this.password }
        : { companyName: this.company.trim(), email: this.email.trim(), password: this.password };

      const data = await this.auth.request(path, {
        method: 'POST',
        body: JSON.stringify(body)
      });

      this.auth.setSession(data);
    } catch (err: any) {
      this.error.set(err.message || 'Ocorreu um erro');
    } finally {
      this.loading.set(false);
    }
  }
}
