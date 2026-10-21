import { Component, ElementRef, HostListener, Input } from '@angular/core';
import gsap from 'gsap';

@Component({
  selector: 'app-magnet',
  standalone: true,
  template: `<ng-content></ng-content>`,
  styles: [`:host { display: inline-block; }`]
})
export class MagnetComponent {
  @Input() strength = 0.5;

  constructor(private el: ElementRef) {}

  @HostListener('mousemove', ['$event'])
  onMouseMove(e: MouseEvent) {
    const rect = this.el.nativeElement.getBoundingClientRect();
    const x = (e.clientX - (rect.left + rect.width / 2)) * this.strength;
    const y = (e.clientY - (rect.top + rect.height / 2)) * this.strength;
    gsap.to(this.el.nativeElement, { x, y, duration: 0.3, ease: 'power2.out' });
  }

  @HostListener('mouseleave')
  onMouseLeave() {
    gsap.to(this.el.nativeElement, { x: 0, y: 0, duration: 0.5, ease: 'elastic.out(1, 0.5)' });
  }
}
