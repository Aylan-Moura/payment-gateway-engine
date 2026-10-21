import { Component, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { SpotlightComponent } from '../ui/spotlight/spotlight';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, FormsModule, SpotlightComponent],
  template: `
    <app-spotlight>
      <div class="p-6 sm:p-8">
        <h2 class="text-lg font-bold text-white mb-4">Nova Cobrança</h2>

        <form (ngSubmit)="onSubmit()" class="space-y-4">
          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Merchant API Key</label>
            <input 
              type="text" 
              readonly 
              [value]="auth.user().apiKey || ''" 
              class="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-slate-400 font-mono text-xs cursor-not-allowed"
            />
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Valor (R$)</label>
            <input 
              type="number" 
              step="0.01" 
              min="0.01" 
              required 
              name="amount" 
              [(ngModel)]="amount" 
              placeholder="150.00"
              class="w-full bg-white/5 border border-white/10 rounded-xl py-3 px-4 text-white placeholder:text-slate-600 font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
            />
          </div>

          <div class="space-y-1">
            <label class="text-xs font-bold text-slate-500 uppercase tracking-wider ml-1">Método</label>
            <select 
              name="method" 
              [(ngModel)]="method"
              class="w-full bg-[#161923] border border-white/10 rounded-xl py-3 px-4 text-white focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
            >
              <option value="PIX">PIX (Instantâneo)</option>
              <option value="CREDIT_CARD">Cartão de Crédito</option>
            </select>
          </div>

          @if (alert()) {
            <div [class]="alert()?.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' : 'bg-red-500/10 border-red-500/20 text-red-400'" class="p-3 rounded-lg border text-sm">
              {{ alert()?.message }}
            </div>
          }

          <button 
            type="submit" 
            [disabled]="loading()"
            class="w-full py-4 mt-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold transition-all disabled:opacity-50"
          >
            {{ loading() ? 'Processando...' : 'Processar Pagamento' }}
          </button>
        </form>
      </div>
    </app-spotlight>
  `
})
export class PaymentFormComponent {
  @Output() transactionCreated = new EventEmitter<any>();

  amount: number | null = null;
  method = 'PIX';
  loading = signal(false);
  alert = signal<{ type: string; message: string } | null>(null);

  constructor(public auth: AuthService) {}

  async onSubmit() {
    if (!this.amount) return;
    this.loading.set(true);
    this.alert.set(null);

    try {
      const payload = await this.auth.request('/api/v1/payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + this.auth.user().token,
          'X-API-Key': this.auth.user().apiKey
        },
        body: JSON.stringify({
          merchantId: this.auth.user().merchantId,
          amount: this.amount,
          paymentMethod: this.method
        })
      });

      const newTx = {
        transactionId: payload.transactionId,
        merchantId: this.auth.user().merchantId,
        amount: this.amount,
        paymentMethod: this.method,
        status: payload.status,
        createdAt: new Date().toISOString()
      };

      this.transactionCreated.emit(newTx);
      this.alert.set({ type: 'success', message: `Cobrança criada com sucesso! Status: ${payload.status}` });
      this.amount = null;
    } catch (err: any) {
      this.alert.set({ type: 'error', message: 'Falha ao processar: ' + err.message });
    } finally {
      this.loading.set(false);
    }
  }
}
