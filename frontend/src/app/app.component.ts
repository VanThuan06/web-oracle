import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Product, ProductVariant } from './core/models';
import { StoreApiService } from './core/store-api.service';

@Component({
  selector: 'nm-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, CurrencyPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  readonly api = inject(StoreApiService);
  private readonly router = inject(Router);

  readonly cartCount = signal(0);
  readonly user = signal<{ fullName: string; role: string } | null>(null);
  readonly mobileOpen = signal(false);
  readonly isAdminRoute = signal(false);

  // Global modals exposed from API service
  readonly showAiModal = this.api.showAiModal;
  readonly showSizeGuideModal = this.api.showSizeGuideModal;
  readonly showContactModal = this.api.showContactModal;
  readonly quickViewProduct = this.api.quickViewProduct;

  // AI Recommendation State
  readonly aiAge = signal<'18-24' | '25-35' | '36-50' | '50+'>('25-35');
  readonly aiElement = signal<'Kim' | 'Moc' | 'Thuy' | 'Hoa' | 'Tho'>('Moc');
  readonly aiOccasion = signal<'office' | 'casual' | 'dating' | 'active'>('office');
  readonly aiHeight = signal(172);
  readonly aiWeight = signal(65);
  readonly aiLoading = signal(false);
  readonly aiDone = signal(false);
  readonly aiAdvice = signal('');
  readonly aiRecommendedProducts = signal<Array<Product & { matchScore: number; matchReason: string }>>([]);

  // Size Guide & Calculator State
  readonly sizeHeight = signal(172);
  readonly sizeWeight = signal(65);
  readonly calculatedSize = computed(() => {
    const h = this.sizeHeight();
    const w = this.sizeWeight();
    const bmi = +(w / ((h / 100) * (h / 100))).toFixed(1);
    let size = 'L';
    let fit = 'Vừa vặn tiêu chuẩn (Regular Fit)';
    let desc = 'Phom dáng cân đối, tôn chiều cao và che khuyết điểm bụng nhẹ.';

    if (h <= 164 || w <= 54) {
      size = 'S';
      fit = 'Thon gọn (Slim/Standard Fit)';
      desc = 'Phù hợp vóc dáng thanh mảnh, chiều dài áo vừa vặn thắt lưng.';
    } else if (h <= 170 && w <= 64) {
      size = 'M';
      fit = 'Vừa vặn chuẩn Việt (Regular Fit)';
      desc = 'Phù hợp vóc dáng trung bình, cử động vai và nách thoải mái.';
    } else if (h <= 176 && w <= 74) {
      size = 'L';
      fit = 'Cân đối thanh lịch (Regular Fit)';
      desc = 'Vừa vặn hoàn hảo cho công sở lẫn dạo phố, vai áo đứng phom.';
    } else if (h <= 182 && w <= 83) {
      size = 'XL';
      fit = 'Thoải mái (Comfort Fit)';
      desc = 'Rộng rãi phần ngực và eo, tạo cảm giác tự tin khi hoạt động.';
    } else {
      size = 'XXL';
      fit = 'Rộng rãi tối ưu (Loose/Relaxed Fit)';
      desc = 'Thiết kế thông minh che bụng, độ co giãn và thông thoáng tốt.';
    }

    return { size, fit, desc, bmi };
  });

  // Contact Form State
  readonly contactName = signal('');
  readonly contactPhone = signal('');
  readonly contactEmail = signal('');
  readonly contactCity = signal('Hà Nội');
  readonly contactContent = signal('');
  readonly contactBusy = signal(false);
  readonly contactNotice = signal('');
  readonly contactError = signal('');

  // Quick View State
  readonly qvSelectedColor = signal('');
  readonly qvSelectedSize = signal('');
  readonly qvQuantity = signal(1);
  readonly qvActiveImageIndex = signal(0);
  readonly qvToast = signal('');

  readonly qvColors = computed(() => {
    const p = this.quickViewProduct();
    if (!p || !p.variants) return [];
    const set = new Set<string>();
    p.variants.forEach((v) => { if (v.color) set.add(v.color); });
    return Array.from(set);
  });

  readonly qvSizes = computed(() => {
    const p = this.quickViewProduct();
    const color = this.qvSelectedColor();
    if (!p || !p.variants) return [];
    return p.variants
      .filter((v) => !color || v.color === color)
      .map((v) => ({ size: v.size, stock: v.stock, id: v.id }));
  });

  readonly qvCurrentVariant = computed<ProductVariant | null>(() => {
    const p = this.quickViewProduct();
    if (!p || !p.variants) return null;
    const color = this.qvSelectedColor();
    const size = this.qvSelectedSize();
    return p.variants.find((v) => (!color || v.color === color) && (!size || v.size === size)) ?? p.variants[0] ?? null;
  });

  ngOnInit(): void {
    this.isAdminRoute.set(this.router.url.startsWith('/admin'));
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.isAdminRoute.set(e.urlAfterRedirects.startsWith('/admin'));
      });

    this.refreshSession();
    this.refreshCart();
    this.api.cartChanged.subscribe(() => this.refreshCart());
    this.api.sessionChanged.subscribe(() => this.refreshSession());
  }

  refreshSession(): void {
    this.api.me().subscribe({
      next: (user) => this.user.set(user),
      error: () => this.user.set(null),
    });
  }

  refreshCart(): void {
    this.api.cart().subscribe({
      next: (cart) => this.cartCount.set(cart.items.reduce((sum, item) => sum + item.quantity, 0)),
      error: () => this.cartCount.set(0),
    });
  }

  closeMenu(): void { this.mobileOpen.set(false); }

  signOut(): void {
    this.api.csrf().subscribe({
      next: () => this.api.logout().subscribe({ next: () => { this.user.set(null); this.refreshCart(); }, error: () => this.user.set(null) }),
      error: () => this.api.logout().subscribe({ next: () => this.user.set(null), error: () => this.user.set(null) }),
    });
  }

  // --- AI RECOMMENDATION LOGIC ---
  openAiModal(): void {
    this.closeMenu();
    this.api.openAiRecommend();
    if (!this.aiDone()) {
      this.runAiRecommendation();
    }
  }

  openAiRecommend(): void {
    this.openAiModal();
  }

  closeAiModal(): void {
    this.api.closeAiRecommend();
  }

  runAiRecommendation(): void {
    this.aiLoading.set(true);
    this.api.catalog({ pageSize: 30 }).subscribe({
      next: (res) => {
        const all = res.data ?? [];
        const el = this.aiElement();
        const age = this.aiAge();
        const occ = this.aiOccasion();
        const h = this.aiHeight();
        const w = this.aiWeight();

        // Element colors mapping
        const colorMap: Record<string, string[]> = {
          Kim: ['Trắng', 'Be', 'Ghi'],
          Moc: ['Rêu', 'Đen', 'Xanh navy'],
          Thuy: ['Đen', 'Xanh navy', 'Trắng'],
          Hoa: ['Cam', 'Đỏ', 'Be'],
          Tho: ['Be', 'Nâu', 'Vàng'],
        };
        const preferredColors = colorMap[el] ?? ['Trắng', 'Đen'];

        const scored = all.map((p, idx) => {
          let score = 80;
          const nameLower = p.name.toLowerCase();
          const pColors = p.variants?.map((v) => v.color) || [];

          // Color match (+10)
          const hasPrefColor = pColors.some((c) => preferredColors.some((pc) => c.toLowerCase().includes(pc.toLowerCase())));
          if (hasPrefColor) score += 10;

          // Occasion match (+6)
          if (occ === 'office' && (nameLower.includes('sơ mi') || nameLower.includes('oxford') || nameLower.includes('polo'))) score += 6;
          if (occ === 'dating' && (nameLower.includes('blazer') || nameLower.includes('sơ mi') || nameLower.includes('dệt kim'))) score += 7;
          if (occ === 'casual' && (nameLower.includes('polo') || nameLower.includes('thun') || nameLower.includes('cotton'))) score += 6;

          // Age match (+4)
          if (age === '25-35' && (nameLower.includes('polo') || nameLower.includes('sơ mi'))) score += 3;

          score = Math.min(98, score - (idx % 3));

          let reason = `Hợp mệnh ${el} với gam màu tương sinh, tôn dáng ${h}cm/${w}kg cho hoàn cảnh ${occ === 'office' ? 'công sở' : occ === 'dating' ? 'hẹn hò & tiệc' : 'dạo phố'}.`;
          if (hasPrefColor) {
            reason = `Tương sinh bản mệnh ${el} (ưu tiên ${preferredColors.join(', ')}). Form dáng ôm vai đứng dáng chuẩn NewMen.`;
          }

          return { ...p, matchScore: score, matchReason: reason };
        });

        scored.sort((a, b) => b.matchScore - a.matchScore);
        const topP = scored.slice(0, 4);

        const adviceText = `Theo bản mệnh ${el}, bạn tương sinh mạnh mẽ với các gam màu ${preferredColors.join(', ')}. ` +
          `Với thể hình ${h}cm / ${w}kg (${this.calculatedSize().fit}), dáng áo vừa vặn sẽ giúp tôn tỉ lệ cơ thể và toát lên vẻ nam tính, đĩnh đạc.`;

        this.aiAdvice.set(adviceText);
        this.aiRecommendedProducts.set(topP);
        this.aiLoading.set(false);
        this.aiDone.set(true);
      },
      error: () => {
        this.aiLoading.set(false);
      },
    });
  }

  // --- SIZE GUIDE LOGIC ---
  openSizeGuide(): void {
    this.closeMenu();
    this.api.openSizeGuide();
  }

  closeSizeGuide(): void {
    this.api.closeSizeGuide();
  }

  // --- CONTACT LOGIC ---
  openContact(): void {
    this.closeMenu();
    this.api.openContact();
    this.contactNotice.set('');
    this.contactError.set('');
  }

  closeContact(): void {
    this.api.closeContact();
  }

  submitContactForm(): void {
    if (!this.contactName().trim() || !this.contactPhone().trim()) {
      this.contactError.set('Vui lòng nhập Họ tên và Số điện thoại liên hệ.');
      return;
    }
    this.contactBusy.set(true);
    this.contactNotice.set('');
    this.contactError.set('');

    const payload = {
      name: this.contactName().trim(),
      phone: this.contactPhone().trim(),
      email: this.contactEmail().trim() || undefined,
      province: this.contactCity().trim(),
      content: this.contactContent().trim() || 'Khách hàng yêu cầu tư vấn sản phẩm NewMen Store.',
    };

    this.api.submitContact(payload).subscribe({
      next: (res) => {
        this.contactBusy.set(false);
        this.contactNotice.set(res?.message || 'Đã gửi lời nhắn thành công! Đội ngũ NewMen sẽ liên hệ với bạn trong ít phút.');
        this.contactName.set('');
        this.contactPhone.set('');
        this.contactContent.set('');
      },
      error: () => {
        this.contactBusy.set(false);
        this.contactError.set('Chưa thể gửi yêu cầu lúc này. Vui lòng thử lại hoặc gọi Hotline 1900 2026.');
      },
    });
  }

  // --- QUICK VIEW LOGIC ---
  openQuickView(product: Product): void {
    this.qvActiveImageIndex.set(0);
    this.qvQuantity.set(1);
    this.qvToast.set('');
    if (product.variants?.length) {
      this.qvSelectedColor.set(product.variants[0].color || '');
      this.qvSelectedSize.set(product.variants[0].size || '');
    } else {
      this.qvSelectedColor.set('');
      this.qvSelectedSize.set('');
    }
    this.api.openQuickView(product);
  }

  closeQuickView(): void {
    this.api.closeQuickView();
  }

  selectQvColor(color: string): void {
    this.qvSelectedColor.set(color);
    const p = this.quickViewProduct();
    const match = p?.variants?.find((v) => v.color === color && v.stock > 0) ?? p?.variants?.find((v) => v.color === color);
    if (match) {
      this.qvSelectedSize.set(match.size);
    }
  }

  selectQvSize(size: string): void {
    this.qvSelectedSize.set(size);
  }

  changeQvQuantity(delta: number): void {
    const cur = this.qvQuantity();
    const next = Math.max(1, cur + delta);
    const variant = this.qvCurrentVariant();
    if (variant && variant.stock > 0 && next > variant.stock) return;
    this.qvQuantity.set(next);
  }

  addQvToCart(buyNow = false): void {
    const variant = this.qvCurrentVariant();
    const p = this.quickViewProduct();
    if (!variant) {
      this.qvToast.set('Vui lòng chọn biến thể màu sắc và kích thước.');
      return;
    }
    if (variant.stock <= 0) {
      this.qvToast.set('Biến thể đã tạm hết hàng trong kho.');
      return;
    }

    const qty = this.qvQuantity();
    this.api.addToCart(variant.id, qty).subscribe({
      next: () => {
        this.qvToast.set(`Đã thêm ${qty} × ${p?.name || 'sản phẩm'} (${variant.color} - ${variant.size}) vào giỏ!`);
        if (buyNow) {
          this.closeQuickView();
          this.router.navigate(['/cart']);
        }
      },
      error: () => {
        this.qvToast.set('Không thể thêm vào giỏ hàng. Vui lòng thử lại.');
      },
    });
  }
}
