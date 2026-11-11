import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { OrderDetail, OrderSummary } from '../core/models';
import { Address, StoreApiService } from '../core/store-api.service';

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './account.component.html',
  styleUrl: './account.component.css',
})
export class AccountComponent implements OnInit {
  private readonly api = inject(StoreApiService);
  private readonly fb = inject(FormBuilder);
  readonly user = signal<{ fullName: string; email: string; role: string } | null>(null);
  readonly orders = signal<OrderSummary[]>([]);
  readonly selectedOrder = signal<OrderDetail | null>(null);
  readonly addresses = signal<Address[]>([]);
  readonly editingAddress = signal<Address | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly registerMode = signal(false);
  readonly tab = signal<'orders' | 'profile' | 'addresses'>('orders');
  readonly error = signal('');
  readonly notice = signal('');
  readonly profileForm = this.fb.group({ fullName: ['', [Validators.required, Validators.maxLength(100)]], phone: ['', Validators.maxLength(20)] });
  readonly addressForm = this.fb.group({
    recipientName: ['', Validators.required], phone: ['', [Validators.required, Validators.pattern(/^[0-9+() .-]{8,18}$/)]],
    line1: ['', Validators.required], ward: [''], district: ['', Validators.required], province: ['', Validators.required],
  });
  readonly reviewForm = this.fb.group({ rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]], content: ['', [Validators.required, Validators.maxLength(500)]] });
  readonly reviewProductId = signal<number | null>(null);
  readonly returnRequestOrderId = signal<number | null>(null);
  readonly returnSubmittingOrderId = signal<number | null>(null);
  readonly returnRequestedOrderIds = signal<number[]>([]);
  readonly returnForm = this.fb.group({ reason: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(500)]] });
  readonly authForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.pattern(/^[0-9+() .-]{8,18}$/)],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void { this.loadAccount(); }

  loadAccount(): void {
    this.loading.set(true);
    this.api.me().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (user) => {
        if (user?.fullName) user.fullName = user.fullName.normalize('NFC');
        this.user.set(user);
        this.loadPrivateData();
      },
      error: () => this.user.set(null),
    });
  }

  private loadPrivateData(): void {
    this.api.profile().subscribe({ next: (profile) => this.profileForm.patchValue(profile), error: () => undefined });
    this.api.myOrders().subscribe({ next: (result) => this.orders.set(result.data ?? []), error: () => undefined });
    this.api.addresses().subscribe({ next: (result) => this.addresses.set(result.data ?? []), error: () => undefined });
  }

  submitAuth(): void {
    this.error.set(''); this.notice.set('');
    const invalid = this.registerMode() ? this.authForm.invalid : this.authForm.controls.email.invalid || this.authForm.controls.password.invalid;
    if (invalid) { this.authForm.markAllAsTouched(); this.error.set('Hãy kiểm tra email, mật khẩu và các trường bắt buộc.'); return; }
    const value = this.authForm.getRawValue();
    this.busy.set(true);
    this.api.csrf().pipe(switchMap(() => this.registerMode()
      ? this.api.register(value.fullName ?? '', value.email ?? '', value.password ?? '', value.phone ?? undefined)
      : this.api.login(value.email ?? '', value.password ?? '')),
      switchMap(() => this.api.me()), finalize(() => this.busy.set(false)))
      .subscribe({
        next: (user) => {
          if (user?.fullName) user.fullName = user.fullName.normalize('NFC');
          this.user.set(user);
          this.authForm.reset();
          this.loadPrivateData();
          this.notice.set(this.registerMode() ? 'Tài khoản đã được tạo.' : 'Chào mừng bạn quay lại.');
        },
        error: (err: { error?: { error?: { message?: string }; message?: string } }) => this.error.set(err.error?.error?.message ?? err.error?.message ?? 'Không thể xác thực. Kiểm tra thông tin rồi thử lại.'),
      });
  }

  signOut(): void {
    this.api.csrf().pipe(switchMap(() => this.api.logout())).subscribe({
      next: () => { this.user.set(null); this.orders.set([]); this.addresses.set([]); this.notice.set('Bạn đã đăng xuất.'); },
      error: () => { this.user.set(null); this.notice.set('Bạn đã đăng xuất.'); },
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) { this.profileForm.markAllAsTouched(); return; }
    this.busy.set(true); this.error.set('');
    this.api.csrf().pipe(switchMap(() => this.api.updateProfile(this.profileForm.getRawValue())), finalize(() => this.busy.set(false))).subscribe({
      next: (profile) => { this.profileForm.patchValue(profile); this.notice.set('Hồ sơ đã được cập nhật.'); },
      error: () => this.error.set('Không thể lưu hồ sơ. Vui lòng thử lại.'),
    });
  }

  addAddress(): void {
    if (this.addressForm.invalid) { this.addressForm.markAllAsTouched(); return; }
    const editing = this.editingAddress();
    const request = editing ? this.api.updateAddress(editing.id, this.addressForm.getRawValue()) : this.api.addAddress(this.addressForm.getRawValue());
    this.api.csrf().pipe(switchMap(() => request)).subscribe({
      next: () => { this.addressForm.reset(); this.editingAddress.set(null); this.api.addresses().subscribe({ next: (result) => this.addresses.set(result.data ?? []) }); this.notice.set(editing ? 'Địa chỉ đã được cập nhật.' : 'Địa chỉ đã được thêm.'); },
      error: () => this.error.set('Không thể thêm địa chỉ. Vui lòng kiểm tra các trường bắt buộc.'),
    });
  }
  editAddress(address: Address): void { this.editingAddress.set(address); this.addressForm.patchValue(address); }
  cancelAddressEdit(): void { this.editingAddress.set(null); this.addressForm.reset(); }
  deleteAddress(id: number): void {
    this.api.csrf().pipe(switchMap(() => this.api.deleteAddress(id))).subscribe({
      next: () => this.addresses.update((items) => items.filter((item) => item.id !== id)),
      error: () => this.error.set('Không thể xóa địa chỉ này.'),
    });
  }
  toggleOrderDetails(order: OrderSummary): void {
    if (this.selectedOrder()?.id === order.id) { this.selectedOrder.set(null); return; }
    this.selectedOrder.set(null);
    this.api.order(order.id).subscribe({ next: (detail) => this.selectedOrder.set(detail), error: () => this.error.set('Không tải được tiến trình đơn hàng.') });
  }
  openReview(productId: number): void { this.reviewProductId.set(this.reviewProductId() === productId ? null : productId); this.reviewForm.reset({ rating: 5, content: '' }); }
  submitReview(orderId: number): void {
    const productId = this.reviewProductId();
    if (!productId || this.reviewForm.invalid) { this.reviewForm.markAllAsTouched(); return; }
    this.api.csrf().pipe(switchMap(() => this.api.review(orderId, { productId, ...this.reviewForm.getRawValue() }))).subscribe({
      next: () => { this.reviewProductId.set(null); this.notice.set('Cảm ơn bạn đã chia sẻ đánh giá.'); },
      error: () => this.error.set('Không thể gửi đánh giá cho sản phẩm này.'),
    });
  }
  toggleReturnForm(orderId: number): void {
    this.returnRequestOrderId.set(this.returnRequestOrderId() === orderId ? null : orderId);
    this.returnForm.reset();
  }
  submitReturnRequest(orderId: number): void {
    if (this.returnForm.invalid || this.returnSubmittingOrderId() !== null) { this.returnForm.markAllAsTouched(); return; }
    this.returnSubmittingOrderId.set(orderId);
    this.api.requestReturn(orderId, this.returnForm.controls.reason.value ?? '').pipe(finalize(() => this.returnSubmittingOrderId.set(null))).subscribe({
      next: () => {
        this.returnRequestedOrderIds.update((ids) => ids.includes(orderId) ? ids : [...ids, orderId]);
        this.returnRequestOrderId.set(null);
        this.returnForm.reset();
        this.notice.set('Đã gửi yêu cầu đổi trả. Nhân viên NewMen sẽ liên hệ với bạn.');
      },
      error: () => this.error.set('Không thể gửi yêu cầu đổi trả cho đơn hàng này.'),
    });
  }

  statusLabel(status: string): string {
    return ({ PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang chuẩn bị', READY_TO_SHIP: 'Chờ giao hàng', SHIPPING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' } as Record<string, string>)[status] ?? status;
  }
}
