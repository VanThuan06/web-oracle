import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subject, map, switchMap, tap } from 'rxjs';
import {
  AttributeItem,
  BannerItem,
  Cart,
  Category,
  ContactItem,
  Customer,
  CustomerDetail,
  DashboardStats,
  DiscountItem,
  ExchangeItem,
  FaultProduct,
  GeneratedVariant,
  OrderDetail,
  OrderSummary,
  PageResult,
  Product,
  Promotion,
  ReportSummary,
  ReviewAdminItem,
  RoleItem,
  VoucherItem,
} from './models';

const API = '/api/v1';

@Injectable({ providedIn: 'root' })
export class StoreApiService {
  private readonly http = inject(HttpClient);
  private readonly cartSubject = new Subject<void>();
  private readonly sessionSubject = new Subject<void>();
  readonly cartChanged = this.cartSubject.asObservable();
  readonly sessionChanged = this.sessionSubject.asObservable();

  // Global Client Modals State
  readonly showAiModal = signal(false);
  readonly showSizeGuideModal = signal(false);
  readonly showContactModal = signal(false);
  readonly quickViewProduct = signal<Product | null>(null);

  openAiRecommend(): void { this.showAiModal.set(true); }
  closeAiRecommend(): void { this.showAiModal.set(false); }

  openSizeGuide(): void { this.showSizeGuideModal.set(true); }
  closeSizeGuide(): void { this.showSizeGuideModal.set(false); }

  openContact(): void { this.showContactModal.set(true); }
  closeContact(): void { this.showContactModal.set(false); }

  openQuickView(p: Product): void { this.quickViewProduct.set(p); }
  closeQuickView(): void { this.quickViewProduct.set(null); }

  catalog(params: Record<string, string | number | undefined>): Observable<PageResult<Product>> {
    let query = new HttpParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== '') query = query.set(key, value);
    }
    return this.http.get<PageResult<Product>>(`${API}/catalog/products`, { params: query, withCredentials: true });
  }

  reseed(): Observable<{ success: boolean; message: string }> {
    return this.http.get<{ success: boolean; message: string }>(`${API}/catalog/reseed`, { withCredentials: true });
  }

  product(slug: string): Observable<Product> {
    return this.http.get<Product>(`${API}/catalog/products/${encodeURIComponent(slug)}`, { withCredentials: true });
  }

  categories(): Observable<{ data: Category[] }> {
    return this.http.get<{ data: Category[] }>(`${API}/catalog/categories`, { withCredentials: true });
  }

  recommendations(productId?: number): Observable<{ data: Product[] }> {
    let params = new HttpParams().set('limit', '8');
    if (productId !== undefined) params = params.set('productId', productId);
    return this.http.get<{ data: Product[] }>(`${API}/catalog/recommendations`, { params, withCredentials: true });
  }

  promotions(): Observable<{ data: Promotion[] }> {
    return this.http.get<{ data: Promotion[] }>(`${API}/promotions/active`, { withCredentials: true });
  }

  csrf(): Observable<unknown> { return this.http.get(`${API}/auth/csrf`, { withCredentials: true }); }
  me(): Observable<{ id: number; fullName: string; email: string; role: string }> {
    return this.http.get<{ id: number; fullName: string; email: string; role: string }>(`${API}/auth/me`, { withCredentials: true }).pipe(
      map(user => user ? { ...user, fullName: user.fullName ? user.fullName.normalize('NFC') : '' } : user)
    );
  }
  login(email: string, password: string): Observable<unknown> {
    return this.csrf().pipe(
      switchMap(() => this.http.post(`${API}/auth/login`, { email, password }, { withCredentials: true })),
      switchMap((authResp) => this.csrf().pipe(map(() => authResp))),
      tap(() => this.sessionSubject.next())
    );
  }
  register(fullName: string, email: string, password: string, phone?: string): Observable<unknown> {
    return this.csrf().pipe(
      switchMap(() => this.http.post(`${API}/auth/register`, { fullName, email, password, phone }, { withCredentials: true })),
      switchMap((authResp) => this.csrf().pipe(map(() => authResp))),
      tap(() => this.sessionSubject.next())
    );
  }
  logout(): Observable<unknown> { return this.http.post(`${API}/auth/logout`, {}, { withCredentials: true }).pipe(tap(() => this.sessionSubject.next())); }

  cart(): Observable<Cart> { return this.http.get<Cart>(`${API}/cart`, { withCredentials: true }); }
  addToCart(variantId: number, quantity = 1): Observable<Cart> {
    return this.csrf().pipe(
      switchMap(() => this.http.post<Cart>(`${API}/cart/items`, { variantId, quantity }, { withCredentials: true })),
      tap(() => this.cartSubject.next()),
    );
  }
  updateCartItem(variantId: number, quantity: number): Observable<Cart> {
    return this.csrf().pipe(
      switchMap(() => this.http.patch<Cart>(`${API}/cart/items/${variantId}`, { quantity }, { withCredentials: true })),
      tap(() => this.cartSubject.next()),
    );
  }
  removeCartItem(variantId: number): Observable<Cart> {
    return this.csrf().pipe(
      switchMap(() => this.http.delete<Cart>(`${API}/cart/items/${variantId}`, { withCredentials: true })),
      tap(() => this.cartSubject.next()),
    );
  }
  clearCart(): Observable<unknown> {
    return this.csrf().pipe(
      switchMap(() => this.http.delete(`${API}/cart/items`, { withCredentials: true })),
      tap(() => this.cartSubject.next()),
    );
  }
  checkout(payload: unknown, idempotencyKey: string): Observable<OrderSummary> {
    return this.http.post<OrderSummary>(`${API}/checkout`, payload, {
      headers: { 'Idempotency-Key': idempotencyKey }, withCredentials: true,
    }).pipe(tap(() => this.cartSubject.next()));
  }
  myOrders(): Observable<PageResult<OrderSummary>> {
    return this.http.get<PageResult<OrderSummary>>(`${API}/orders/mine`, { withCredentials: true });
  }
  order(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${API}/orders/${id}`, { withCredentials: true });
  }
  adminOrderDetail(id: number): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${API}/admin/orders/${id}`, { withCredentials: true });
  }
  profile(): Observable<{ fullName: string; email: string; phone?: string }> {
    return this.http.get<{ fullName: string; email: string; phone?: string }>(`${API}/profile`, { withCredentials: true });
  }
  updateProfile(payload: unknown): Observable<{ fullName: string; email: string; phone?: string }> {
    return this.http.patch<{ fullName: string; email: string; phone?: string }>(`${API}/profile`, payload, { withCredentials: true });
  }
  addresses(): Observable<{ data: Address[] }> {
    return this.http.get<{ data: Address[] }>(`${API}/profile/addresses`, { withCredentials: true });
  }
  addAddress(payload: unknown): Observable<Address> {
    return this.http.post<Address>(`${API}/profile/addresses`, payload, { withCredentials: true });
  }
  updateAddress(id: number, payload: unknown): Observable<Address> {
    return this.http.patch<Address>(`${API}/profile/addresses/${id}`, payload, { withCredentials: true });
  }
  deleteAddress(id: number): Observable<unknown> {
    return this.http.delete(`${API}/profile/addresses/${id}`, { withCredentials: true });
  }
  review(orderId: number, payload: unknown): Observable<unknown> {
    return this.http.post(`${API}/orders/${orderId}/reviews`, payload, { withCredentials: true });
  }
  requestReturn(orderId: number, reason: string): Observable<unknown> {
    return this.csrf().pipe(switchMap(() => this.http.post(`${API}/orders/${orderId}/returns`, { reason }, { withCredentials: true })));
  }

  adminSummary(from: string, to: string): Observable<ReportSummary> {
    return this.http.get<ReportSummary>(`${API}/admin/reports/summary`, { params: { from, to }, withCredentials: true });
  }
  adminTopProducts(from: string, to: string): Observable<PageResult<{ productName: string; quantitySold: number; revenue: number }>> {
    return this.http.get<PageResult<{ productName: string; quantitySold: number; revenue: number }>>(`${API}/admin/reports/top-products`, { params: { from, to, page: 0, pageSize: 5 }, withCredentials: true });
  }
  adminProducts(page = 0): Observable<PageResult<Product>> {
    return this.http.get<PageResult<Product>>(`${API}/admin/products`, { params: { page, pageSize: 20 }, withCredentials: true });
  }
  createCategory(payload: unknown): Observable<Category> {
    return this.http.post<Category>(`${API}/admin/categories`, payload, { withCredentials: true });
  }
  updateCategory(id: number, payload: unknown): Observable<Category> {
    return this.http.patch<Category>(`${API}/admin/categories/${id}`, payload, { withCredentials: true });
  }
  deleteCategory(id: number): Observable<unknown> {
    return this.http.delete(`${API}/admin/categories/${id}`, { withCredentials: true });
  }
  createProduct(product: unknown): Observable<Product> {
    return this.http.post<Product>(`${API}/admin/products`, product, { withCredentials: true });
  }
  updateProduct(id: number, payload: unknown): Observable<Product> {
    return this.http.patch<Product>(`${API}/admin/products/${id}`, payload, { withCredentials: true });
  }
  deleteProduct(id: number): Observable<unknown> {
    return this.http.delete(`${API}/admin/products/${id}`, { withCredentials: true });
  }
  addVariant(productId: number, variant: unknown): Observable<unknown> {
    return this.http.post(`${API}/admin/products/${productId}/variants`, variant, { withCredentials: true });
  }
  updateVariant(id: number, payload: unknown): Observable<unknown> {
    return this.http.patch(`${API}/admin/variants/${id}`, payload, { withCredentials: true });
  }
  deleteVariant(id: number): Observable<unknown> {
    return this.http.delete(`${API}/admin/variants/${id}`, { withCredentials: true });
  }
  uploadProductImage(productId: number, file: File): Observable<unknown> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post(`${API}/admin/products/${productId}/images`, data, { withCredentials: true });
  }
  adminOrders(page = 0): Observable<PageResult<OrderSummary>> {
    return this.http.get<PageResult<OrderSummary>>(`${API}/admin/orders`, { params: { page, pageSize: 20 }, withCredentials: true });
  }
  updateOrderStatus(orderId: number, status: string): Observable<OrderSummary> {
    return this.http.patch<OrderSummary>(`${API}/admin/orders/${orderId}/status`, { status }, { withCredentials: true });
  }
  customers(page = 0): Observable<PageResult<Customer>> {
    return this.http.get<PageResult<Customer>>(`${API}/admin/customers`, { params: { page, pageSize: 20 }, withCredentials: true });
  }
  adminPromotions(): Observable<{ data: Promotion[] }> {
    return this.http.get<{ data: Promotion[] }>(`${API}/admin/promotions`, { withCredentials: true });
  }
  createPromotion(payload: unknown): Observable<Promotion> {
    return this.http.post<Promotion>(`${API}/admin/promotions`, payload, { withCredentials: true });
  }
  posCheckout(payload: unknown): Observable<OrderSummary> {
    return this.http.post<OrderSummary>(`${API}/admin/pos/orders`, payload, { withCredentials: true });
  }
  posCarts(): Observable<{ data: PosCart[] }> {
    return this.http.get<{ data: PosCart[] }>(`${API}/admin/pos/carts`, { withCredentials: true });
  }
  createPosCart(): Observable<PosCart> {
    return this.http.post<PosCart>(`${API}/admin/pos/carts`, {}, { withCredentials: true });
  }
  addPosItem(cartId: number, variantId: number, quantity: number): Observable<PosCart> {
    return this.http.post<PosCart>(`${API}/admin/pos/carts/${cartId}/items`, { variantId, quantity }, { withCredentials: true });
  }
  updatePosItem(cartId: number, variantId: number, quantity: number): Observable<PosCart> {
    return this.http.patch<PosCart>(`${API}/admin/pos/carts/${cartId}/items/${variantId}`, { quantity }, { withCredentials: true });
  }
  deletePosItem(cartId: number, variantId: number): Observable<PosCart> {
    return this.http.delete<PosCart>(`${API}/admin/pos/carts/${cartId}/items/${variantId}`, { withCredentials: true });
  }
  deletePosCart(cartId: number): Observable<unknown> {
    return this.http.delete(`${API}/admin/pos/carts/${cartId}`, { withCredentials: true });
  }
  adminReturns(): Observable<{ data: AdminReturn[] }> {
    return this.http.get<{ data: AdminReturn[] }>(`${API}/admin/returns`, { withCredentials: true });
  }
  updateReturnStatus(returnId: number, status: string): Observable<unknown> {
    return this.http.patch(`${API}/admin/returns/${returnId}/status`, { status }, { withCredentials: true });
  }
  staff(): Observable<{ data: Array<{ id: number; fullName: string; email: string; role: string; phone?: string }> }> {
    return this.http.get<{ data: Array<{ id: number; fullName: string; email: string; role: string; phone?: string }> }>(`${API}/admin/staff`, { withCredentials: true });
  }
  createStaff(payload: unknown): Observable<unknown> {
    return this.http.post(`${API}/admin/staff`, payload, { withCredentials: true });
  }
  updateStaff(staffId: number, payload: unknown): Observable<unknown> {
    return this.http.patch(`${API}/admin/staff/${staffId}`, payload, { withCredentials: true });
  }
  invoice(orderId: number): Observable<Blob> {
    return this.http.get(`${API}/admin/orders/${orderId}/invoice`, { responseType: 'blob', withCredentials: true });
  }
  exportSummary(from: string, to: string): Observable<Blob> {
    return this.http.get(`${API}/admin/reports/summary/export`, { params: { from, to }, responseType: 'blob', withCredentials: true });
  }

  // Attributes
  getAttributes(type: string): Observable<{ data: AttributeItem[] }> {
    return this.http.get<{ data: AttributeItem[] }>(`${API}/admin/attributes/${type}`, { withCredentials: true });
  }
  createAttribute(type: string, payload: { name: string; code?: string; extra?: string; status?: boolean }): Observable<AttributeItem> {
    return this.http.post<AttributeItem>(`${API}/admin/attributes/${type}`, payload, { withCredentials: true });
  }
  updateAttribute(type: string, id: number, payload: { name?: string; code?: string; extra?: string; status?: boolean }): Observable<AttributeItem> {
    return this.http.patch<AttributeItem>(`${API}/admin/attributes/${type}/${id}`, payload, { withCredentials: true });
  }
  deleteAttribute(type: string, id: number): Observable<unknown> {
    return this.http.delete(`${API}/admin/attributes/${type}/${id}`, { withCredentials: true });
  }
  getCatalogAttributes(): Observable<{
    originCategories: AttributeItem[];
    categories: AttributeItem[];
    colors: AttributeItem[];
    sizes: AttributeItem[];
    materials: AttributeItem[];
    collars: AttributeItem[];
    designs: AttributeItem[];
    forms: AttributeItem[];
    sleeves: AttributeItem[];
  }> {
    return this.http.get<{
      originCategories: AttributeItem[];
      categories: AttributeItem[];
      colors: AttributeItem[];
      sizes: AttributeItem[];
      materials: AttributeItem[];
      collars: AttributeItem[];
      designs: AttributeItem[];
      forms: AttributeItem[];
      sleeves: AttributeItem[];
    }>(`${API}/catalog/attributes`, { withCredentials: true });
  }

  // Variant generator & Product export
  generateVariants(payload: { baseCode?: string; productCode?: string; basePrice?: number; baseCost?: number; costPrice?: number; defaultStock?: number; colors: string[]; sizes: string[] }): Observable<GeneratedVariant[]> {
    const body = {
      productCode: payload.productCode || payload.baseCode || 'NM-SP',
      basePrice: payload.basePrice || 350000,
      costPrice: payload.costPrice || payload.baseCost || 180000,
      defaultStock: payload.defaultStock || 20,
      colors: payload.colors,
      sizes: payload.sizes,
    };
    return this.http.post<GeneratedVariant[]>(`${API}/admin/products/generate-variants`, body, { withCredentials: true });
  }
  exportProductsCsv(): Observable<Blob> {
    return this.http.get(`${API}/admin/products/export`, { responseType: 'blob', withCredentials: true });
  }

  // Fault products
  getFaultProducts(page = 0, size = 20): Observable<PageResult<FaultProduct>> {
    return this.http.get<PageResult<FaultProduct>>(`${API}/admin/fault-products`, { params: { page, size }, withCredentials: true });
  }
  createFaultProduct(payload: { productDetailId: number; quantity: number; description: string; status?: number }): Observable<FaultProduct> {
    return this.http.post<FaultProduct>(`${API}/admin/fault-products`, payload, { withCredentials: true });
  }

  // Discounts
  getDiscounts(page = 0, size = 20): Observable<PageResult<DiscountItem>> {
    return this.http.get<PageResult<DiscountItem>>(`${API}/admin/discounts`, { params: { page, size }, withCredentials: true });
  }
  createDiscount(payload: unknown): Observable<DiscountItem> {
    return this.http.post<DiscountItem>(`${API}/admin/discounts`, payload, { withCredentials: true });
  }
  updateDiscount(id: number, payload: unknown): Observable<DiscountItem> {
    return this.http.patch<DiscountItem>(`${API}/admin/discounts/${id}`, payload, { withCredentials: true });
  }

  // Vouchers
  getVouchers(page = 0, size = 20): Observable<PageResult<VoucherItem>> {
    return this.http.get<PageResult<VoucherItem>>(`${API}/admin/vouchers`, { params: { page, size }, withCredentials: true });
  }
  createVoucher(payload: unknown): Observable<VoucherItem> {
    return this.http.post<VoucherItem>(`${API}/admin/vouchers`, payload, { withCredentials: true });
  }
  updateVoucher(id: number, payload: unknown): Observable<VoucherItem> {
    return this.http.patch<VoucherItem>(`${API}/admin/vouchers/${id}`, payload, { withCredentials: true });
  }

  // Exchanges (Đổi hàng)
  getExchanges(page = 0, size = 20): Observable<PageResult<ExchangeItem>> {
    return this.http.get<PageResult<ExchangeItem>>(`${API}/admin/exchanges`, { params: { page, size }, withCredentials: true });
  }
  createExchange(payload: unknown): Observable<ExchangeItem> {
    return this.http.post<ExchangeItem>(`${API}/admin/exchanges`, payload, { withCredentials: true });
  }
  updateExchangeStatus(id: number, status: string): Observable<ExchangeItem> {
    return this.http.patch<ExchangeItem>(`${API}/admin/exchanges/${id}/status`, { status }, { withCredentials: true });
  }

  // Banners
  getBanners(): Observable<{ data: BannerItem[] }> {
    return this.http.get<{ data: BannerItem[] }>(`${API}/admin/banners`, { withCredentials: true });
  }
  createBanner(payload: unknown): Observable<BannerItem> {
    return this.http.post<BannerItem>(`${API}/admin/banners`, payload, { withCredentials: true });
  }
  updateBanner(id: number, payload: unknown): Observable<BannerItem> {
    return this.http.patch<BannerItem>(`${API}/admin/banners/${id}`, payload, { withCredentials: true });
  }
  deleteBanner(id: number): Observable<unknown> {
    return this.http.delete(`${API}/admin/banners/${id}`, { withCredentials: true });
  }
  getPublicBanners(): Observable<{ data: BannerItem[] }> {
    return this.http.get<{ data: BannerItem[] }>(`${API}/catalog/banners`, { withCredentials: true });
  }

  // Contacts
  getContacts(page = 0, size = 20): Observable<PageResult<ContactItem>> {
    return this.http.get<PageResult<ContactItem>>(`${API}/admin/contacts`, { params: { page, size }, withCredentials: true });
  }
  updateContactStatus(id: number, status: boolean): Observable<ContactItem> {
    return this.http.patch<ContactItem>(`${API}/admin/contacts/${id}/status`, { status }, { withCredentials: true });
  }
  submitContact(payload: { name: string; phone: string; email?: string; addressName?: string; ward?: string; district?: string; province?: string; content?: string }): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ success: boolean; message: string }>(`${API}/catalog/contacts`, payload, { withCredentials: true });
  }

  // Reviews moderation
  getAdminReviews(): Observable<ReviewAdminItem[]> {
    return this.http.get<ReviewAdminItem[]>(`${API}/admin/reviews`, { withCredentials: true });
  }
  toggleReviewStatus(id: number, status: boolean): Observable<ReviewAdminItem> {
    return this.http.patch<ReviewAdminItem>(`${API}/admin/reviews/${id}/status`, { status }, { withCredentials: true });
  }

  // Roles
  getRoles(): Observable<RoleItem[]> {
    return this.http.get<RoleItem[]>(`${API}/admin/roles`, { withCredentials: true });
  }
  createRole(payload: unknown): Observable<RoleItem> {
    return this.http.post<RoleItem>(`${API}/admin/roles`, payload, { withCredentials: true });
  }

  // Customers
  createCustomer(payload: unknown): Observable<Customer> {
    return this.http.post<Customer>(`${API}/admin/customers`, payload, { withCredentials: true });
  }
  getCustomerDetail(id: number): Observable<CustomerDetail> {
    return this.http.get<CustomerDetail>(`${API}/admin/customers/${id}`, { withCredentials: true });
  }
  exportCustomersCsv(): Observable<Blob> {
    return this.http.get(`${API}/admin/customers/export`, { responseType: 'blob', withCredentials: true });
  }

  // Dashboard Stats
  getDashboardStats(range = 'MONTH', from?: string, to?: string): Observable<DashboardStats> {
    let params = new HttpParams().set('range', range);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<DashboardStats>(`${API}/admin/reports/dashboard`, { params, withCredentials: true });
  }
}

export interface Address {
  id: number;
  recipientName: string;
  phone: string;
  line1: string;
  ward?: string;
  district: string;
  province: string;
  isDefault?: boolean;
}

export interface PosCart {
  cartId: number;
  items: Cart['items'];
  subtotal: number;
  customerName?: string;
  customerPhone?: string;
}

export interface AdminReturn {
  id: number;
  orderId: number;
  orderCode: string;
  reason: string;
  status: string;
  createdAt: string;
  updatedAt?: string;
}
