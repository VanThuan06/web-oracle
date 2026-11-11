import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { Product, ProductVariant } from '../core/models';
import { StoreApiService } from '../core/store-api.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product.component.html',
  styleUrl: './product.component.css',
})
export class ProductComponent implements OnInit {
  private readonly api = inject(StoreApiService);
  private readonly route = inject(ActivatedRoute);
  readonly product = signal<Product | null>(null);
  readonly related = signal<Product[]>([]);
  readonly selected = signal<ProductVariant | null>(null);
  readonly quantity = signal(1);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');
  readonly imageIndex = signal(0);

  ngOnInit(): void {
    this.route.paramMap.pipe(switchMap((params) => {
      this.loading.set(true);
      this.error.set('');
      this.product.set(null);
      return this.api.product(params.get('slug') ?? '');
    })).subscribe({
      next: (product) => {
        this.product.set(product);
        this.selected.set(product.variants?.find((variant) => variant.stock > 0) ?? product.variants?.[0] ?? null);
        this.loading.set(false);
        this.api.recommendations(product.id).subscribe({ next: (result) => this.related.set(result.data ?? []), error: () => undefined });
      },
      error: () => { this.error.set('Không tìm thấy sản phẩm hoặc dịch vụ đang tạm gián đoạn.'); this.loading.set(false); },
    });
  }

  colors(): string[] { return [...new Set(this.product()?.variants?.map((variant) => variant.color) ?? [])]; }
  sizes(): string[] { return [...new Set(this.product()?.variants?.map((variant) => variant.size) ?? [])]; }
  chooseColor(color: string): void {
    const current = this.selected();
    const options = this.product()?.variants?.filter((variant) => variant.color === color) ?? [];
    this.selected.set(options.find((variant) => variant.size === current?.size && variant.stock > 0) ?? options.find((variant) => variant.stock > 0) ?? options[0] ?? null);
  }
  chooseSize(size: string): void {
    const current = this.selected();
    const option = this.product()?.variants?.find((variant) => variant.size === size && variant.color === current?.color);
    if (option) this.selected.set(option);
  }
  variantFor(color: string, size: string): ProductVariant | undefined { return this.product()?.variants?.find((variant) => variant.color === color && variant.size === size); }
  setQuantity(value: number): void { this.quantity.set(Math.max(1, Math.min(this.selected()?.stock ?? 1, value))); }
  imageUrl(): string | null { return this.product()?.images?.[this.imageIndex()] ?? null; }
  addToCart(): void {
    const variant = this.selected();
    const product = this.product();
    if (!variant || !product) return;
    this.api.addToCart(variant.id, this.quantity()).subscribe({
      next: () => this.notice.set(`Đã thêm ${this.quantity()} × ${product.name} (${variant.color} - ${variant.size}) vào giỏ.`),
      error: () => this.notice.set('Không thể thêm sản phẩm. Vui lòng thử lại.'),
    });
  }

  openSizeGuide(): void {
    this.api.openSizeGuide();
  }
}
