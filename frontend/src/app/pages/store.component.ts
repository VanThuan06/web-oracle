import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Category, Product, Promotion } from '../core/models';
import { StoreApiService } from '../core/store-api.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './store.component.html',
  styleUrl: './store.component.css',
})
export class StoreComponent implements OnInit {
  private readonly api = inject(StoreApiService);
  private readonly route = inject(ActivatedRoute);
  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly promotions = signal<Promotion[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly notice = signal('');
  readonly query = signal('');
  readonly categoryId = signal('');
  readonly color = signal('');
  readonly sizeFilter = signal('');
  readonly sort = signal('newest');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalItems = signal(0);
  readonly size = 12;
  readonly colors = ['Trắng', 'Đen', 'Xanh navy', 'Be', 'Rêu'];
  readonly sizes = ['S', 'M', 'L', 'XL', 'XXL'];

  ngOnInit(): void {
    this.api.categories().subscribe({ next: (result) => this.categories.set(result.data), error: () => undefined });
    const initialSort = this.route.snapshot.queryParamMap.get('sort');
    if (initialSort && ['newest', 'bestseller', 'price-asc', 'price-desc'].includes(initialSort)) this.sort.set(initialSort);
    this.api.promotions().subscribe({ next: (result) => this.promotions.set(result.data ?? []), error: () => undefined });
    this.route.queryParamMap.subscribe((params) => {
      const requestedSort = params.get('sort');
      if (requestedSort && ['newest', 'bestseller', 'price-asc', 'price-desc'].includes(requestedSort) && requestedSort !== this.sort()) { this.sort.set(requestedSort); this.loadProducts(); }
    });
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.catalog({ query: this.query().trim(), categoryId: this.categoryId(), color: this.color(), clothingSize: this.sizeFilter(), sort: this.sort(), page: this.page(), pageSize: this.size })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.products.set(result.data ?? []);
          this.totalPages.set(result.totalPages ?? 0);
          this.totalItems.set(result.totalItems ?? result.data?.length ?? 0);
        },
        error: () => this.error.set('Chưa kết nối được cửa hàng. Hãy khởi động Spring Boot với profile dev rồi tải lại trang.'),
      });
  }

  search(): void { this.page.set(0); this.loadProducts(); }
  selectCategory(id: string): void { this.categoryId.set(id); this.page.set(0); this.loadProducts(); }
  clearFilters(): void { this.categoryId.set(''); this.color.set(''); this.sizeFilter.set(''); this.query.set(''); this.page.set(0); this.loadProducts(); }
  selectColor(value: string): void { this.color.set(value); this.page.set(0); this.loadProducts(); }
  selectSize(value: string): void { this.sizeFilter.set(value); this.page.set(0); this.loadProducts(); }
  selectSort(value: string): void { this.sort.set(value); this.page.set(0); this.loadProducts(); }
  changePage(offset: number): void { this.page.set(Math.max(0, Math.min(this.totalPages() - 1, this.page() + offset))); this.loadProducts(); }

  addQuick(product: Product): void {
    const variant = product.variants?.find((item) => item.stock > 0);
    if (!variant) { this.notice.set('Sản phẩm hiện chưa có biến thể sẵn hàng.'); return; }
    this.api.addToCart(variant.id).subscribe({
      next: () => this.notice.set(`Đã thêm ${product.name} · ${variant.color} / ${variant.size} vào giỏ.`),
      error: () => this.notice.set('Không thể thêm sản phẩm. Vui lòng thử lại.'),
    });
  }

  image(product: Product): string | null { return product.images?.[0] || null; }
  variantColors(product: Product): string { return product.variants?.slice(0, 3).map((variant) => variant.color).join(' · ') ?? ''; }
  trackProduct(_index: number, product: Product): number { return product.id; }

  openQuickView(product: Product, event?: MouseEvent): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.api.openQuickView(product);
  }

  openSizeGuide(): void {
    this.api.openSizeGuide();
  }

  openAiModal(): void {
    this.api.openAiRecommend();
  }
}
