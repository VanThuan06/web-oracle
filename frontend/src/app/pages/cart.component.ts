import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, from, switchMap } from 'rxjs';
import { Cart, EMPTY_CART, OrderSummary } from '../core/models';
import { StoreApiService } from '../core/store-api.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css',
})
export class CartComponent implements OnInit {
  private readonly checkoutKeyStorage = 'newmen.checkout.idempotency.v1';
  private fallbackCheckoutKey: { fingerprint: string; key: string } | null = null;
  private readonly api = inject(StoreApiService);
  private readonly fb = inject(FormBuilder);
  readonly cart = signal<Cart>(EMPTY_CART);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly success = signal<OrderSummary | null>(null);
  readonly step = signal(1);
  readonly paymentMethods = [
    { value: 'COD', label: 'Thanh toán khi nhận hàng', note: 'Thanh toán trực tiếp cho đơn vị giao hàng.' },
    { value: 'BANK_TRANSFER_SAMPLE', label: 'Chuyển khoản · ví dụ', note: 'Bước khởi tạo thanh toán đang ở dạng mẫu, chưa kết nối ngân hàng.' },
  ];
  readonly form = this.fb.group({
    customerName: ['', [Validators.required, Validators.maxLength(100)]],
    customerEmail: ['', [Validators.required, Validators.email]],
    customerPhone: ['', [Validators.required, Validators.pattern(/^[0-9+() .-]{8,18}$/)]],
    recipientName: ['', [Validators.required, Validators.maxLength(100)]],
    deliveryPhone: ['', [Validators.required, Validators.pattern(/^[0-9+() .-]{8,18}$/)]],
    line1: ['', [Validators.required, Validators.maxLength(200)]],
    ward: [''],
    district: ['', Validators.required],
    province: ['', Validators.required],
    paymentMethod: ['COD', Validators.required],
    voucherCode: [''],
    note: [''],
  });

  ngOnInit(): void { this.loadCart(); }

  loadCart(): void {
    this.loading.set(true);
    this.api.cart().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (cart) => this.cart.set(cart ?? EMPTY_CART),
      error: () => { this.cart.set(EMPTY_CART); this.error.set('Không tải được giỏ hàng. Hãy thử tải lại hoặc tiếp tục mua sắm.'); },
    });
  }

  updateQuantity(variantId: number, quantity: number): void {
    if (quantity < 1) { this.remove(variantId); return; }
    this.api.updateCartItem(variantId, quantity).subscribe({ next: (cart) => this.cart.set(cart), error: () => this.error.set('Số lượng yêu cầu vượt quá tồn kho.') });
  }
  remove(variantId: number): void {
    this.api.removeCartItem(variantId).subscribe({ next: (cart) => this.cart.set(cart), error: () => this.error.set('Không thể xóa sản phẩm khỏi giỏ.') });
  }
  clear(): void {
    this.api.clearCart().subscribe({ next: () => this.cart.set(EMPTY_CART), error: () => this.error.set('Không thể làm trống giỏ hàng.') });
  }
  continueToDelivery(): void { this.step.set(2); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  showCart(): void { this.step.set(1); }
  statusLabel(status: string): string {
    return ({ PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chuẩn bị', READY_TO_SHIP: 'Chờ giao hàng', SHIPPING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' } as Record<string, string>)[status] ?? status;
  }

  placeOrder(): void {
    this.error.set('');
    if (this.form.invalid || !this.cart().items.length) { this.form.markAllAsTouched(); this.error.set('Hãy điền đủ thông tin liên hệ và địa chỉ nhận hàng trước khi đặt.'); return; }
    this.submitting.set(true);
    const value = this.form.getRawValue();
    const payload = {
      customerName: value.customerName,
      customerEmail: value.customerEmail,
      customerPhone: value.customerPhone,
      shippingAddress: {
        recipientName: value.recipientName,
        phone: value.deliveryPhone,
        line1: value.line1,
        ward: value.ward,
        district: value.district,
        province: value.province,
      },
      paymentMethod: value.paymentMethod,
      voucherCode: value.voucherCode,
      note: value.note,
    };
    const intent = {
      items: this.cart().items.map(({ variantId, quantity, unitPrice }) => ({ variantId, quantity, unitPrice })),
      payload,
    };
    let fingerprintForRequest = '';
    from(this.checkoutIdempotencyKey(intent)).pipe(
      switchMap(({ key, fingerprint }) => {
        fingerprintForRequest = fingerprint;
        return this.api.csrf().pipe(switchMap(() => this.api.checkout(payload, key)));
      }),
      finalize(() => this.submitting.set(false)),
    ).subscribe({
      next: (order) => {
        this.clearCheckoutIdempotencyKey(fingerprintForRequest);
        this.success.set(order); this.cart.set(EMPTY_CART); this.step.set(3); window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err: { error?: { error?: { message?: string }; message?: string } }) => this.error.set(err.error?.error?.message ?? err.error?.message ?? 'Chưa thể tạo đơn hàng. Kiểm tra địa chỉ và số lượng rồi thử lại.'),
    });
  }

  private async checkoutIdempotencyKey(intent: unknown): Promise<{ key: string; fingerprint: string }> {
    const serialized = JSON.stringify(intent);
    let fingerprint: string;
    if (globalThis.crypto?.subtle) {
      const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(serialized));
      fingerprint = Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
    } else {
      let hash = 2166136261;
      for (let index = 0; index < serialized.length; index++) hash = Math.imul(hash ^ serialized.charCodeAt(index), 16777619);
      fingerprint = `${(hash >>> 0).toString(16)}-${serialized.length}`;
    }

    let saved: { fingerprint: string; key: string } | null = this.fallbackCheckoutKey;
    try {
      const stored = sessionStorage.getItem(this.checkoutKeyStorage);
      if (stored) saved = JSON.parse(stored) as { fingerprint: string; key: string };
    } catch { /* Storage may be disabled; keep the retry key in memory for this page. */ }

    if (!saved || saved.fingerprint !== fingerprint) {
      saved = { fingerprint, key: globalThis.crypto?.randomUUID?.() ?? `newmen-${Date.now()}-${Math.random().toString(16).slice(2)}` };
      this.fallbackCheckoutKey = saved;
      try { sessionStorage.setItem(this.checkoutKeyStorage, JSON.stringify(saved)); } catch { /* In-memory fallback remains available. */ }
    }
    return { key: saved.key, fingerprint };
  }

  private clearCheckoutIdempotencyKey(fingerprint: string): void {
    if (this.fallbackCheckoutKey?.fingerprint === fingerprint) this.fallbackCheckoutKey = null;
    try {
      const stored = sessionStorage.getItem(this.checkoutKeyStorage);
      const saved = stored ? JSON.parse(stored) as { fingerprint?: string } : null;
      if (saved?.fingerprint === fingerprint) sessionStorage.removeItem(this.checkoutKeyStorage);
    } catch { /* There is nothing to clear in inaccessible storage. */ }
  }
}
