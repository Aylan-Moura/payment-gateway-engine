import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-decrypted',
  standalone: true,
  template: `<span>{{displayText}}</span>`
})
export class DecryptedComponent implements OnInit {
  @Input() text = '';
  displayText = '';
  private chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@#$%^&*()_+";

  ngOnInit() {
    let iteration = 0;
    const interval = setInterval(() => {
      this.displayText = this.text
        .split('')
        .map((letter, index) => index < iteration ? letter : this.chars[Math.floor(Math.random() * this.chars.length)])
        .join('');
      
      if (iteration >= this.text.length) clearInterval(interval);
      iteration += 1 / 10;
    }, 50);
  }
}
