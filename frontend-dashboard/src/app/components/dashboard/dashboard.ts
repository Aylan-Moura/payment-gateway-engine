import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricsComponent } from '../metrics/metrics';
import { PaymentFormComponent } from '../payment-form/payment-form';
import { TransactionsComponent } from '../transactions/transactions';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MetricsComponent, PaymentFormComponent, TransactionsComponent],
  template: `
    <div class="space-y-8 max-w-7xl mx-auto">
      <app-metrics [transactions]="transactions()" />

      <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <div class="lg:col-span-5">
          <app-payment-form (transactionCreated)="onTransactionCreated($event)" />
        </div>
        <div class="lg:col-span-7">
          <app-transactions [transactions]="transactions()" />
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  transactions = signal<any[]>([]);

  ngOnInit() {
    try {
      const stored = localStorage.getItem('pgw_transactions');
      if (stored) {
        this.transactions.set(JSON.parse(stored));
      }
    } catch (e) {
      console.error(e);
    }
  }

  onTransactionCreated(tx: any) {
    const updated = [tx, ...this.transactions()];
    this.transactions.set(updated);
    localStorage.setItem('pgw_transactions', JSON.stringify(updated));
  }
}
