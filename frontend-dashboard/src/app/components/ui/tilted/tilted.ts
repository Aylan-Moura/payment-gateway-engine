import { Component, ElementRef, HostListener, Input } from '@angular/core';
import gsap from 'gsap';

@Component({
  selector: 'app-tilted',
  standalone: true,
  template: `<ng-content></ng-content>`,
  styles: [`:host { display: block; perspective: 1000px; transform-style: preserve-3d; }`]
})
export class TiltedComponent {
  constructor(private el: ElementRef) {}

  @HostListener('mousemove', ['$event'])
  onMouseMove(e: MouseEvent) {
    const rect = this.el.nativeElement.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const rotateX = (y - rect.height / 2) / 20;
    const rotateY = (rect.width / 2 - x) / 20;

    gsap.to(this.el.nativeElement, {
      rotateX,
      rotateY,
      duration: 0.1,
      ease: 'power2.out'
    });
  }

  @HostListener('mouseleave')
  onMouseLeave() {
    gsap.to(this.el.nativeElement, { rotateX: 0, rotateY: 0, duration: 0.5 });
  }
}
