import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SpotlightComponent } from '../ui/spotlight/spotlight';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, SpotlightComponent],
  template: `
    <app-spotlight>
      <div class="p-6 sm:p-8">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-bold text-white">Histórico de Transações</h2>
          <span class="text-xs font-mono px-3 py-1 rounded-full bg-white/5 border border-white/10 text-slate-400">
            {{ transactions.length }} registros
          </span>
        </div>

        @if (transactions.length === 0) {
          <div class="py-12 border border-dashed border-white/10 rounded-xl text-center">
            <p class="text-slate-400 text-sm">Nenhuma transação registrada ainda.</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
              <thead>
                <tr class="border-b border-white/10 text-slate-500 uppercase text-[10px] font-bold">
                  <th class="py-3 px-4">ID Transação</th>
                  <th class="py-3 px-4">Valor</th>
                  <th class="py-3 px-4">Método</th>
                  <th class="py-3 px-4">Status</th>
                  <th class="py-3 px-4">Data</th>
                  <th class="py-3 px-4 text-right">Comprovante</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-white/5">
                @for (tx of transactions; track tx.transactionId) {
                  <tr class="hover:bg-white/[0.02]">
                    <td class="py-3.5 px-4 font-mono text-xs text-indigo-300">{{ tx.transactionId }}</td>
                    <td class="py-3.5 px-4 font-mono font-bold text-white">{{ tx.amount | currency:'BRL':'symbol':'1.2-2' }}</td>
                    <td class="py-3.5 px-4 text-slate-300">
                      <span class="px-2 py-1 rounded bg-white/5 text-xs">{{ tx.paymentMethod }}</span>
                    </td>
                    <td class="py-3.5 px-4">
                      <span 
                        [class]="tx.status === 'APPROVED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'"
                        class="px-2.5 py-1 rounded-full text-xs font-semibold"
                      >
                        {{ tx.status }}
                      </span>
                    </td>
                    <td class="py-3.5 px-4 text-slate-400 text-xs font-mono">{{ tx.createdAt | date:'short' }}</td>
                    <td class="py-3.5 px-4 text-right">
                      <button (click)="downloadReceipt(tx)" class="text-xs text-indigo-400 hover:underline">
                        PDF
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </app-spotlight>
  `
})
export class TransactionsComponent {
  @Input() transactions: any[] = [];

  downloadReceipt(tx: any) {
    window.open(`/api/v1/receipts/${tx.transactionId}`, '_blank');
  }
}
