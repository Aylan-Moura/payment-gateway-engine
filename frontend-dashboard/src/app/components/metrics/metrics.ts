import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SpotlightComponent } from '../ui/spotlight/spotlight';
import { TiltedComponent } from '../ui/tilted/tilted';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, SpotlightComponent, TiltedComponent],
  template: `
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      <!-- Card 1 -->
      <app-tilted>
        <app-spotlight>
          <div class="p-5">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-slate-500 uppercase">Volume Total</span>
              <span class="text-[10px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded">Live</span>
            </div>
            <h3 class="text-2xl font-bold text-white font-mono">
              {{ totalVolume() | currency:'BRL':'symbol':'1.2-2' }}
            </h3>
          </div>
        </app-spotlight>
      </app-tilted>

      <!-- Card 2 -->
      <app-tilted>
        <app-spotlight>
          <div class="p-5">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-slate-500 uppercase">Aprovados</span>
              <span class="text-[10px] font-bold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded">Status</span>
            </div>
            <h3 class="text-2xl font-bold text-white font-mono">
              {{ approvedCount() }}
            </h3>
          </div>
        </app-spotlight>
      </app-tilted>

      <!-- Card 3 -->
      <app-tilted>
        <app-spotlight>
          <div class="p-5">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-slate-500 uppercase">Pendentes</span>
              <span class="text-[10px] font-bold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded">Status</span>
            </div>
            <h3 class="text-2xl font-bold text-white font-mono">
              {{ pendingCount() }}
            </h3>
          </div>
        </app-spotlight>
      </app-tilted>

      <!-- Card 4 -->
      <app-tilted>
        <app-spotlight>
          <div class="p-5">
            <div class="flex justify-between items-start mb-4">
              <span class="text-xs font-bold text-slate-500 uppercase">Total Transações</span>
              <span class="text-[10px] font-bold text-cyan-400 bg-cyan-500/10 px-2 py-0.5 rounded">Qtd</span>
            </div>
            <h3 class="text-2xl font-bold text-white font-mono">
              {{ transactions.length }}
            </h3>
          </div>
        </app-spotlight>
      </app-tilted>
    </div>
  `
})
export class MetricsComponent {
  @Input() transactions: any[] = [];

  totalVolume = computed(() => {
    return this.transactions.reduce((acc, tx) => acc + Number(tx.amount || 0), 0);
  });

  approvedCount = computed(() => {
    return this.transactions.filter(tx => tx.status === 'APPROVED').length;
  });

  pendingCount = computed(() => {
    return this.transactions.filter(tx => tx.status === 'PENDING').length;
  });
}
