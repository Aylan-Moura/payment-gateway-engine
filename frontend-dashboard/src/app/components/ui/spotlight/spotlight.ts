import { Component, ElementRef, HostListener, Input } from '@angular/core';

@Component({
  selector: 'app-spotlight',
  standalone: true,
  template: `
    <div class="relative overflow-hidden rounded-2xl border border-white/5 bg-[#12141c]">
      <div 
        class="pointer-events-none absolute -inset-px transition duration-300"
        [style.opacity]="opacity"
        [style.background]="'radial-gradient(600px circle at ' + pos.x + 'px ' + pos.y + 'px, rgba(99, 102, 241, 0.15), transparent 40%)'"
      ></div>
      <div class="relative z-10"><ng-content></ng-content></div>
    </div>
  `
})
export class SpotlightComponent {
  pos = { x: 0, y: 0 };
  opacity = 0;

  constructor(private el: ElementRef) {}

  @HostListener('mousemove', ['$event'])
  onMouseMove(e: MouseEvent) {
    const rect = this.el.nativeElement.getBoundingClientRect();
    this.pos = { x: e.clientX - rect.left, y: e.clientY - rect.top };
    this.opacity = 1;
  }

  @HostListener('mouseleave')
  onMouseLeave() {
    this.opacity = 0;
  }
}
