import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface AuthData {
  token: string | null;
  merchantId: string | null;
  apiKey: string | null;
  email: string | null;
}

export const AUTH_API = {
  register: '/api/v1/auth/register',
  login: '/api/v1/auth/login',
} as const;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly LS_KEYS = {
    TOKEN: 'pgw_token',
    REFRESH: 'pgw_refresh',
    MERCHANT_ID: 'pgw_merchant_id',
    API_KEY: 'pgw_api_key',
    EMAIL: 'pgw_email'
  };

  private authState = signal<AuthData>({
    token: localStorage.getItem(this.LS_KEYS.TOKEN),
    merchantId: localStorage.getItem(this.LS_KEYS.MERCHANT_ID),
    apiKey: localStorage.getItem(this.LS_KEYS.API_KEY),
    email: localStorage.getItem(this.LS_KEYS.EMAIL)
  });

  isAuthenticated = computed(() => !!this.authState().token);
  user = computed(() => this.authState());

  constructor(private http: HttpClient) {}

  async request(path: string, options: any = {}) {
    // Em produção o dashboard é servido pelo nginx, que faz proxy de /api/ para o gateway.
    // Permite sobrescrever em runtime via window.__PGW_API_BASE_URL__ se necessário.
    const baseUrl = (window as any).__PGW_API_BASE_URL__ || '';
    const url = `${baseUrl}${path}`;
    
    // Default headers
    options.headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };
    
    try {
      const resp = await fetch(url, options);
      if (!resp.ok) {
        const body = await resp.json().catch(() => ({}));
        throw new Error(body.message || body.error || `HTTP ${resp.status}`);
      }
      return await resp.json();
    } catch (err: any) {
      throw err;
    }
  }

  setSession(data: any) {
    const email = this.getEmailFromJwt(data.token);
    localStorage.setItem(this.LS_KEYS.TOKEN, data.token);
    localStorage.setItem(this.LS_KEYS.REFRESH, data.refreshToken);
    localStorage.setItem(this.LS_KEYS.MERCHANT_ID, data.merchantId);
    localStorage.setItem(this.LS_KEYS.API_KEY, data.apiKey);
    localStorage.setItem(this.LS_KEYS.EMAIL, email);

    this.authState.set({
      token: data.token,
      merchantId: data.merchantId,
      apiKey: data.apiKey,
      email: email
    });
  }

  logout() {
    Object.values(this.LS_KEYS).forEach(k => localStorage.removeItem(k));
    this.authState.set({ token: null, merchantId: null, apiKey: null, email: null });
  }

  private getEmailFromJwt(token: string): string {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64)).sub || 'merchant';
    } catch {
      return 'merchant';
    }
  }
}
