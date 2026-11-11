import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import {
  AttributeItem,
  BannerItem,
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
  Product,
  ProductVariant,
  Promotion,
  ReportSummary,
  ReviewAdminItem,
  RoleItem,
  VoucherItem,
} from '../core/models';
import { AdminReturn, PosCart, StoreApiService } from '../core/store-api.service';

export type AdminTab =
  | 'overview'
  | 'pos'
  | 'orders'
  | 'products'
  | 'attributes'
  | 'fault-products'
  | 'promotions'
  | 'customers'
  | 'staff'
  | 'banners'
  | 'feedback';

export type AttributeType =
  | 'origin-categories'
  | 'categories'
  | 'colors'
  | 'sizes'
  | 'materials'
  | 'collars'
  | 'designs'
  | 'forms'
  | 'sleeves';

export interface AttributeCategoryOption {
  key: AttributeType;
  label: string;
}

interface StaffMember {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: string;
  active?: boolean;
}

function removeVietnameseAccents(str: string): string {
  return (str || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim();
}

@Component({
  selector: 'nm-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css',
})
export class AdminComponent implements OnInit {
  private readonly api = inject(StoreApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // App & Shell State
  readonly tab = signal<AdminTab>('overview');
  readonly user = signal<{ fullName: string; role: string; email?: string } | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly globalSearch = signal('');

  // 1. Overview / Dashboard
  readonly dashboardStats = signal<DashboardStats | null>(null);
  readonly dashboardRange = signal<'DAY' | 'WEEK' | 'MONTH' | 'YEAR'>('MONTH');
  readonly reportFrom = signal(this.daysAgo(30));
  readonly reportTo = signal(this.today());
  readonly summary = signal<ReportSummary | null>(null);

  // 2. Products & Variants
  readonly products = signal<Product[]>([]);
  readonly productsPage = signal(0);
  readonly productsTotalPages = signal(0);
  readonly productFilterCategory = signal<number | null>(null);
  readonly productFilterQuery = signal('');
  readonly expandedProductId = signal<number | null>(null);
  readonly productModalOpen = signal(false);
  readonly editingProduct = signal<Product | null>(null);

  // Variant generator state for Add/Edit Product
  readonly selectedColors = signal<string[]>(['Trắng', 'Đen']);
  readonly selectedSizes = signal<string[]>(['S', 'M', 'L', 'XL']);
  readonly generatedVariants = signal<GeneratedVariant[]>([]);

  // Fast attribute modal (inline create attribute while adding product)
  readonly fastAttributeModalOpen = signal(false);
  readonly fastAttributeType = signal<AttributeType>('colors');
  readonly fastAttributeForm = this.fb.group({
    name: ['', Validators.required],
    code: [''],
    extra: [''],
  });

  // Product Form
  readonly productForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(140)]],
    code: ['', [Validators.required, Validators.maxLength(40)]],
    categoryId: [null as number | null, Validators.required],
    price: [350000, [Validators.required, Validators.min(0)]],
    costPrice: [180000, [Validators.min(0)]],
    defaultStock: [20, [Validators.min(0)]],
    weight: [300, [Validators.min(0)]],
    collar: ['Cổ bẻ (Regular)'],
    design: ['Trơn basic'],
    form: ['Regular fit'],
    material: ['Cotton 100%'],
    sleeve: ['Tay ngắn'],
    description: [''],
  });

  // Single variant inline add/edit form
  readonly variantForm = this.fb.group({
    sku: ['', Validators.required],
    barcode: [''],
    color: ['', Validators.required],
    size: ['', Validators.required],
    price: [350000, [Validators.required, Validators.min(0)]],
    costPrice: [180000, [Validators.min(0)]],
    stock: [10, [Validators.required, Validators.min(0)]],
  });

  // Catalog Attributes Metadata
  readonly catalogAttributes = signal<{
    originCategories: AttributeItem[];
    categories: AttributeItem[];
    colors: AttributeItem[];
    sizes: AttributeItem[];
    materials: AttributeItem[];
    collars: AttributeItem[];
    designs: AttributeItem[];
    forms: AttributeItem[];
    sleeves: AttributeItem[];
  }>({
    originCategories: [],
    categories: [],
    colors: [],
    sizes: [],
    materials: [],
    collars: [],
    designs: [],
    forms: [],
    sleeves: [],
  });

  // 3. POS (Bán hàng tại quầy)
  readonly posCarts = signal<PosCart[]>([]);
  readonly activeCart = signal<number | null>(null);
  readonly posCatalog = signal<Product[]>([]);
  readonly posSelectedCategory = signal<string>('ALL');
  readonly posCategories = signal<string[]>([]);
  readonly posViewMode = signal<'grid' | 'table'>('grid');
  readonly posMatches = signal<Product[]>([]);
  readonly posQuery = signal('');
  readonly posCustomerQuery = signal('');
  readonly posCustomerMatches = signal<Customer[]>([]);
  readonly selectedPosCustomer = signal<Customer | null>(null);
  readonly posPaymentMethod = signal<'CASH' | 'TRANSFER' | 'CARD'>('CASH');
  readonly posDelivery = signal(false);
  readonly posFreight = signal(0);
  readonly posVoucherCode = signal('');
  readonly posVoucherDiscount = signal(0);

  readonly posDisplayProducts = computed(() => {
    const list = this.posCatalog();
    const rawQuery = this.posQuery().trim();
    const selectedCat = this.posSelectedCategory();

    return list.filter((p) => {
      if (selectedCat !== 'ALL' && p.categoryName !== selectedCat) {
        return false;
      }
      if (!rawQuery) return true;

      const q = removeVietnameseAccents(rawQuery);
      const name = removeVietnameseAccents(p.name || '');
      const code = (p.code || '').toLowerCase();

      if (name.includes(q) || code.includes(q)) return true;

      return p.variants?.some((v) =>
        (v.sku && v.sku.toLowerCase().includes(q)) ||
        (v.barcode && v.barcode.toLowerCase().includes(q)) ||
        removeVietnameseAccents(v.color || '').includes(q) ||
        (v.size && v.size.toLowerCase().includes(q))
      );
    });
  });
  readonly posShipForm = this.fb.group({
    recipientName: [''],
    phone: [''],
    line1: [''],
    ward: [''],
    district: [''],
    province: ['Hà Nội'],
    note: [''],
  });
  readonly quickCustomerModalOpen = signal(false);
  readonly quickCustomerForm = this.fb.group({
    fullName: ['', Validators.required],
    phone: ['', Validators.required],
    email: [''],
    address: [''],
  });

  // 4. Orders & Returns/Exchanges
  readonly orders = signal<OrderSummary[]>([]);
  readonly ordersPage = signal(0);
  readonly ordersTotalPages = signal(0);
  readonly orderFilterStatus = signal<string>('ALL');
  readonly orderFilterQuery = signal('');
  readonly selectedOrderDetail = signal<OrderDetail | null>(null);
  readonly orderDetailModalOpen = signal(false);
  readonly pendingOrderCount = signal(0);

  // Exchanges & Returns sub-section
  readonly returnsSubTab = signal<'returns' | 'exchanges'>('returns');
  readonly returnsList = signal<AdminReturn[]>([]);
  readonly exchangesList = signal<ExchangeItem[]>([]);
  readonly exchangeModalOpen = signal(false);
  readonly exchangeForm = this.fb.group({
    returnId: [null as number | null],
    productDetailId: [null as number | null, Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    unitPrice: [0, [Validators.required, Validators.min(0)]],
    freight: [0],
    note: [''],
    shipName: [''],
    shipPhone: [''],
    shipAddress: [''],
  });

  // 5. Attributes Management
  readonly currentAttributeType = signal<AttributeType>('origin-categories');
  readonly attributeList = signal<AttributeItem[]>([]);
  readonly attributeModalOpen = signal(false);
  readonly editingAttribute = signal<AttributeItem | null>(null);
  readonly attributeForm = this.fb.group({
    name: ['', Validators.required],
    code: [''],
    extra: [''],
    status: [true],
  });

  readonly attributeCategories: AttributeCategoryOption[] = [
    { key: 'origin-categories', label: 'Danh mục cha' },
    { key: 'categories', label: 'Danh mục con' },
    { key: 'colors', label: 'Màu sắc' },
    { key: 'sizes', label: 'Kích cỡ' },
    { key: 'materials', label: 'Chất liệu' },
    { key: 'collars', label: 'Cổ áo' },
    { key: 'forms', label: 'Kiểu dáng' },
    { key: 'designs', label: 'Thiết kế' },
    { key: 'sleeves', label: 'Tay áo' },
  ];

  // 6. Fault Products (Sản phẩm hủy/lỗi)
  readonly faultProducts = signal<FaultProduct[]>([]);
  readonly faultProductModalOpen = signal(false);
  readonly faultProductForm = this.fb.group({
    productDetailId: [null as number | null, Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    description: ['', Validators.required],
  });

  // 7. Promotions & Vouchers
  readonly promoTab = signal<'discounts' | 'vouchers'>('discounts');
  readonly discounts = signal<DiscountItem[]>([]);
  readonly vouchers = signal<VoucherItem[]>([]);
  readonly discountModalOpen = signal(false);
  readonly voucherModalOpen = signal(false);
  readonly discountForm = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    percent: [15, [Validators.required, Validators.min(1), Validators.max(100)]],
    startDate: [this.today(), Validators.required],
    endDate: [this.daysFromNow(30), Validators.required],
  });
  readonly voucherForm = this.fb.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    discount: [10, [Validators.required, Validators.min(1), Validators.max(100)]],
    min: [300000],
    max: [100000],
    startDate: [this.today(), Validators.required],
    endDate: [this.daysFromNow(60), Validators.required],
    type: [1],
  });

  // 8. Customers & Addresses
  readonly customers = signal<Customer[]>([]);
  readonly customersPage = signal(0);
  readonly customersTotalPages = signal(0);
  readonly selectedCustomerDetail = signal<CustomerDetail | null>(null);
  readonly customerDetailModalOpen = signal(false);

  // 9. Staff & Roles
  readonly staffTab = signal<'staff' | 'roles'>('staff');
  readonly staff = signal<StaffMember[]>([]);
  readonly roles = signal<RoleItem[]>([]);
  readonly staffModalOpen = signal(false);
  readonly roleModalOpen = signal(false);
  readonly staffForm = this.fb.group({
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.minLength(8)],
    phone: [''],
    role: ['STAFF', Validators.required],
  });
  readonly roleForm = this.fb.group({
    name: ['', Validators.required],
    description: [''],
  });

  // 10. Banners
  readonly banners = signal<BannerItem[]>([]);
  readonly bannerModalOpen = signal(false);
  readonly bannerForm = this.fb.group({
    title: [''],
    content: [''],
    type: [1],
    url: ['', Validators.required],
    linkUrl: ['/catalog'],
    sortOrder: [0],
  });

  // 11. Feedback (Contacts & Reviews)
  readonly feedbackTab = signal<'contacts' | 'reviews'>('contacts');
  readonly contacts = signal<ContactItem[]>([]);
  readonly reviews = signal<ReviewAdminItem[]>([]);

  // Order status labels & colors
  readonly orderStatuses: Array<[string, string]> = [
    ['PENDING', 'Chờ duyệt'],
    ['CONFIRMED', 'Đã duyệt'],
    ['PREPARING', 'Chuẩn bị hàng'],
    ['READY_TO_SHIP', 'Chờ giao hàng'],
    ['SHIPPING', 'Đang giao'],
    ['COMPLETED', 'Hoàn thành'],
    ['CANCELLED', 'Đã hủy'],
  ];

  // Admin Auth / Login Form
  readonly loginForm = this.fb.group({
    email: ['admin@newmen.local', [Validators.required, Validators.email]],
    password: ['ChangeMe123!', [Validators.required]],
  });
  readonly loginSubmitting = signal(false);

  quickLogin(email = 'admin@newmen.local', password = 'ChangeMe123!'): void {
    this.loginForm.patchValue({ email, password });
    this.submitLogin();
  }

  submitLogin(): void {
    if (this.loginForm.invalid) return;
    this.loginSubmitting.set(true);
    this.error.set('');
    const val = this.loginForm.getRawValue();
    this.api.csrf().pipe(
      switchMap(() => this.api.login(val.email!, val.password!)),
      switchMap(() => this.api.me()),
      finalize(() => this.loginSubmitting.set(false))
    ).subscribe({
      next: (user) => {
        if (!['ADMIN', 'STAFF'].includes(user.role)) {
          this.error.set('Tài khoản này không có quyền truy cập bảng quản trị (yêu cầu vai trò ADMIN hoặc STAFF).');
          return;
        }
        this.user.set(user);
        this.notice.set(`Xin chào ${user.fullName}, bạn đã đăng nhập thành công!`);
        this.loadTab();
        this.loadCatalogAttributes();
        this.checkPendingOrders();
      },
      error: () => {
        this.error.set('Đăng nhập không thành công. Vui lòng kiểm tra lại email hoặc mật khẩu.');
      }
    });
  }

  ngOnInit(): void {
    this.api.me().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (user) => {
        if (!['ADMIN', 'STAFF'].includes(user.role)) {
          this.error.set('Tài khoản này không có quyền truy cập bảng quản trị.');
          return;
        }
        this.user.set(user);
        this.loadOverview();
        this.loadCatalogAttributes();
        this.checkPendingOrders();
      },
      error: () => this.error.set('Vui lòng đăng nhập bằng tài khoản quản trị viên (admin@newmen.local).'),
    });
  }

  // --- TOPBAR & NAVIGATION ---
  selectTab(tab: AdminTab): void {
    this.tab.set(tab);
    this.error.set('');
    this.notice.set('');
    this.loadTab(tab);
  }

  loadTab(tab = this.tab()): void {
    if (tab === 'overview') this.loadOverview();
    if (tab === 'pos') {
      this.loadPosCarts();
      this.loadPosProducts();
    }
    if (tab === 'orders') this.loadOrders();
    if (tab === 'products') this.loadProducts();
    if (tab === 'attributes') this.loadAttributes();
    if (tab === 'fault-products') this.loadFaultProducts();
    if (tab === 'promotions') this.loadPromotions();
    if (tab === 'customers') this.loadCustomers();
    if (tab === 'staff') this.loadStaffAndRoles();
    if (tab === 'banners') this.loadBanners();
    if (tab === 'feedback') this.loadFeedback();
  }

  checkPendingOrders(): void {
    this.api.adminOrders(0).subscribe({
      next: (res) => {
        const count = res.data?.filter((o) => o.status === 'PENDING').length ?? 0;
        this.pendingOrderCount.set(count);
      },
    });
  }

  signOut(): void {
    this.api.csrf().pipe(switchMap(() => this.api.logout())).subscribe({
      next: () => {
        this.user.set(null);
        this.router.navigate(['/']);
      },
      error: () => {
        this.user.set(null);
        this.router.navigate(['/']);
      },
    });
  }

  triggerReseed(): void {
    this.busy.set(true);
    this.api.reseed().pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (res) => {
        this.notice.set(res.message || 'Đã nạp đầy đủ dữ liệu mẫu toàn diện.');
        this.loadTab();
        this.loadCatalogAttributes();
        this.checkPendingOrders();
      },
      error: () => this.showError('Không thể nạp dữ liệu mẫu.'),
    });
  }

  // --- 1. OVERVIEW & REPORTS ---
  loadOverview(): void {
    this.api.getDashboardStats(this.dashboardRange(), this.reportFrom(), this.reportTo()).subscribe({
      next: (stats) => this.dashboardStats.set(stats),
      error: () => {
        // Fallback to legacy summary
        this.api.adminSummary(this.reportFrom(), this.reportTo()).subscribe({
          next: (sum) => this.summary.set(sum),
          error: () => this.showError('Không thể tải dữ liệu thống kê.'),
        });
      },
    });
  }

  setDashboardRange(range: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'): void {
    this.dashboardRange.set(range);
    this.loadOverview();
  }

  exportReport(): void {
    this.api.exportSummary(this.reportFrom(), this.reportTo()).subscribe({
      next: (blob) => this.downloadFile(blob, `bao-cao-doanh-thu-${this.reportFrom()}-${this.reportTo()}.csv`),
      error: () => this.showError('Không thể tải file báo cáo CSV.'),
    });
  }

  // --- 2. PRODUCTS & ATTRIBUTES METADATA ---
  loadCatalogAttributes(): void {
    this.api.getCatalogAttributes().subscribe({
      next: (data) => {
        if (!data) return;
        this.catalogAttributes.set({
          originCategories: data.originCategories ?? [],
          categories: data.categories ?? [],
          colors: data.colors ?? [],
          sizes: (data.sizes ?? []).map((s: any) => ({ ...s, name: s.name || s.code })),
          materials: data.materials ?? [],
          collars: data.collars ?? [],
          designs: data.designs ?? [],
          forms: data.forms ?? [],
          sleeves: data.sleeves ?? [],
        });
      },
      error: () => {},
    });
  }

  loadProducts(page = this.productsPage()): void {
    this.api.adminProducts(page).subscribe({
      next: (result) => {
        this.products.set(result.data ?? []);
        this.productsPage.set(result.page ?? page);
        this.productsTotalPages.set(result.totalPages ?? 0);
      },
      error: () => this.showError('Không thể tải danh sách sản phẩm.'),
    });
  }

  openAddProductModal(): void {
    this.editingProduct.set(null);
    const attrs = this.catalogAttributes();
    const firstCat = attrs?.categories?.[0];
    this.productForm.reset({
      name: '',
      code: 'NM-SP-' + Math.floor(1000 + Math.random() * 9000),
      categoryId: firstCat?.id ?? null,
      price: 350000,
      costPrice: 180000,
      defaultStock: 20,
      weight: 300,
      collar: attrs?.collars?.[0]?.name ?? 'Cổ bẻ (Regular)',
      design: attrs?.designs?.[0]?.name ?? 'Trơn basic',
      form: attrs?.forms?.[0]?.name ?? 'Regular fit',
      material: attrs?.materials?.[0]?.name ?? 'Cotton 100%',
      sleeve: attrs?.sleeves?.[0]?.name ?? 'Tay ngắn',
      description: '',
    });
    const defaultColors: string[] = (attrs?.colors?.length
      ? attrs.colors.slice(0, 2).map((c) => c.name || '').filter((n): n is string => Boolean(n))
      : ['Trắng', 'Đen']);
    const defaultSizes: string[] = (attrs?.sizes?.length
      ? attrs.sizes.slice(0, 4).map((s) => s.name || s.code || '').filter((n): n is string => Boolean(n))
      : ['S', 'M', 'L', 'XL']);
    this.selectedColors.set(defaultColors.length ? defaultColors : ['Trắng', 'Đen']);
    this.selectedSizes.set(defaultSizes.length ? defaultSizes : ['S', 'M', 'L', 'XL']);
    this.generateVariantMatrix();
    this.productModalOpen.set(true);
  }

  editProduct(product: Product): void {
    this.editingProduct.set(product);
    this.productForm.patchValue({
      name: product.name,
      code: product.code,
      categoryId: product.categoryId ?? null,
      price: product.price,
      costPrice: product.variants?.[0]?.costPrice ?? 180000,
      defaultStock: product.variants?.[0]?.stock ?? 20,
      weight: product.weight ?? 300,
      collar: product.collar ?? 'Cổ bẻ (Regular)',
      design: product.design ?? 'Trơn basic',
      form: product.fit ?? 'Regular fit',
      material: product.material ?? 'Cotton 100%',
      sleeve: product.sleeve ?? 'Tay ngắn',
      description: product.description,
    });
    this.productModalOpen.set(true);
  }

  toggleColor(colorName: string): void {
    const current = this.selectedColors();
    if (current.includes(colorName)) {
      if (current.length > 1) this.selectedColors.set(current.filter((c) => c !== colorName));
    } else {
      this.selectedColors.set([...current, colorName]);
    }
    this.generateVariantMatrix();
  }

  toggleSize(sizeCode: string): void {
    const current = this.selectedSizes();
    if (current.includes(sizeCode)) {
      if (current.length > 1) this.selectedSizes.set(current.filter((s) => s !== sizeCode));
    } else {
      this.selectedSizes.set([...current, sizeCode]);
    }
    this.generateVariantMatrix();
  }

  generateVariantMatrix(): void {
    const formVal = this.productForm.getRawValue();
    const colors = this.selectedColors();
    const sizes = this.selectedSizes();
    if (!colors.length || !sizes.length) {
      this.generatedVariants.set([]);
      return;
    }
    const payload = {
      productCode: formVal.code || 'NM-SP',
      basePrice: formVal.price || 350000,
      costPrice: formVal.costPrice || 180000,
      defaultStock: formVal.defaultStock || 20,
      colors,
      sizes,
    };
    this.api.generateVariants(payload).subscribe({
      next: (res: any) => {
        const list = Array.isArray(res) ? res : (res?.data ?? []);
        if (list.length > 0) {
          this.generatedVariants.set(list);
        } else {
          this.fallbackGenerateVariants(payload);
        }
      },
      error: () => this.fallbackGenerateVariants(payload),
    });
  }

  private fallbackGenerateVariants(payload: { productCode: string; basePrice: number; costPrice: number; defaultStock: number; colors: string[]; sizes: string[] }): void {
    const prefix = payload.productCode.trim().toUpperCase();
    const list: GeneratedVariant[] = [];
    for (const c of payload.colors) {
      for (const s of payload.sizes) {
        const colorShort = c.substring(0, Math.min(2, c.length)).toUpperCase();
        const sku = `${prefix}-${colorShort}-${s}`;
        const barcode = '893' + Math.abs((prefix + c + s).split('').reduce((acc, ch) => acc * 31 + ch.charCodeAt(0), 0) % 1000000000);
        list.push({
          sku,
          barcode,
          color: c,
          size: s,
          price: payload.basePrice,
          costPrice: payload.costPrice,
          stock: payload.defaultStock,
        });
      }
    }
    this.generatedVariants.set(list);
  }

  saveProduct(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    const val = this.productForm.getRawValue();
    this.busy.set(true);
    const editing = this.editingProduct();

    if (editing) {
      this.api.csrf().pipe(
        switchMap(() => this.api.updateProduct(editing.id, {
          name: val.name,
          code: val.code,
          description: val.description,
          categoryId: val.categoryId,
          collar: val.collar,
          design: val.design,
          fit: val.form,
          material: val.material,
          sleeve: val.sleeve,
          originalPrice: val.price,
          weight: val.weight,
        })),
        finalize(() => this.busy.set(false))
      ).subscribe({
        next: () => {
          this.notice.set('Đã cập nhật sản phẩm thành công.');
          this.productModalOpen.set(false);
          this.loadProducts();
        },
        error: () => this.showError('Không thể cập nhật sản phẩm.'),
      });
    } else {
      const variantsToAdd = this.generatedVariants().map((v) => ({
        sku: v.sku,
        barcode: v.barcode,
        color: v.color,
        size: v.size,
        price: v.price,
        costPrice: v.costPrice,
        stock: v.stock,
      }));
      const createPayload = {
        name: val.name,
        code: val.code,
        description: val.description,
        categoryId: val.categoryId,
        collar: val.collar,
        design: val.design,
        fit: val.form,
        material: val.material,
        sleeve: val.sleeve,
        originalPrice: val.price,
        weight: val.weight,
        variants: variantsToAdd,
      };
      this.api.csrf().pipe(
        switchMap(() => this.api.createProduct(createPayload)),
        finalize(() => this.busy.set(false))
      ).subscribe({
        next: (created) => {
          this.notice.set(`Đã thêm sản phẩm "${created.name}" với ${variantsToAdd.length} biến thể.`);
          this.productModalOpen.set(false);
          this.loadProducts();
        },
        error: () => this.showError('Không thể tạo sản phẩm. Vui lòng kiểm tra mã sản phẩm.'),
      });
    }
  }

  toggleVariants(product: Product): void {
    this.expandedProductId.set(this.expandedProductId() === product.id ? null : product.id);
  }

  saveVariantDetails(variant: ProductVariant): void {
    this.api.csrf().pipe(
      switchMap(() => this.api.updateVariant(variant.id, { price: variant.price, stock: variant.stock }))
    ).subscribe({
      next: () => this.notice.set(`Đã cập nhật biến thể ${variant.sku}.`),
      error: () => this.showError('Không thể cập nhật biến thể.'),
    });
  }

  removeVariant(variant: ProductVariant): void {
    if (!confirm(`Bạn có chắc chắn muốn xóa biến thể ${variant.sku}?`)) return;
    this.api.csrf().pipe(switchMap(() => this.api.deleteVariant(variant.id))).subscribe({
      next: () => {
        this.notice.set(`Đã xóa biến thể ${variant.sku}.`);
        this.loadProducts();
      },
      error: () => this.showError('Không thể xóa biến thể đang có trong đơn hàng.'),
    });
  }

  uploadImage(product: Product, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      this.showError('Chỉ nhận ảnh JPEG, PNG hoặc WebP tối đa 5 MiB.');
      return;
    }
    this.api.csrf().pipe(switchMap(() => this.api.uploadProductImage(product.id, file))).subscribe({
      next: () => {
        this.notice.set(`Đã tải ảnh lên cho sản phẩm ${product.name}.`);
        this.loadProducts();
      },
      error: () => this.showError('Tải ảnh thất bại.'),
    });
  }

  exportProductsCsv(): void {
    this.api.exportProductsCsv().subscribe({
      next: (blob) => this.downloadFile(blob, `san-pham-newmen-${this.today()}.csv`),
      error: () => this.showError('Không thể xuất file CSV sản phẩm.'),
    });
  }

  // Fast attribute add modal
  openFastAttributeModal(type: AttributeType): void {
    this.fastAttributeType.set(type);
    this.fastAttributeForm.reset();
    this.fastAttributeModalOpen.set(true);
  }

  saveFastAttribute(): void {
    if (this.fastAttributeForm.invalid) return;
    const val = this.fastAttributeForm.getRawValue();
    const type = this.fastAttributeType();
    this.api.csrf().pipe(
      switchMap(() => this.api.createAttribute(type, {
        name: val.name || '',
        code: val.code || undefined,
        extra: val.extra || undefined,
        status: true,
      }))
    ).subscribe({
      next: (item) => {
        this.notice.set(`Đã thêm thuộc tính "${item.name}".`);
        this.fastAttributeModalOpen.set(false);
        this.loadCatalogAttributes();
      },
      error: () => this.showError('Không thể thêm thuộc tính.'),
    });
  }

  // --- 3. POS (BÁN HÀNG TẠI QUẦY) ---
  loadPosCarts(): void {
    this.api.posCarts().subscribe({
      next: (res) => {
        const list = res.data ?? [];
        this.posCarts.set(list);
        if (list.length > 0) {
          if (!this.activeCart() || !list.some((c) => c.cartId === this.activeCart())) {
            this.activeCart.set(list[0].cartId);
          }
        } else {
          this.newPosCart();
        }
      },
      error: () => this.showError('Không thể tải hóa đơn POS.'),
    });
  }

  loadPosProducts(): void {
    this.api.catalog({ pageSize: 48 }).subscribe({
      next: (res) => {
        const list = res.data ?? [];
        this.posCatalog.set(list);
        const cats = Array.from(new Set(list.map((p) => p.categoryName).filter((c): c is string => Boolean(c))));
        this.posCategories.set(cats);
      },
      error: () => {
        this.api.adminProducts(0).subscribe({
          next: (res) => {
            const list = res.data ?? [];
            this.posCatalog.set(list);
            const cats = Array.from(new Set(list.map((p) => p.categoryName).filter((c): c is string => Boolean(c))));
            this.posCategories.set(cats);
          },
        });
      },
    });
  }

  getProductTotalStock(p: Product): number {
    return p.variants?.reduce((sum, v) => sum + (v.stock || 0), 0) ?? 0;
  }

  newPosCart(): void {
    if (this.posCarts().length >= 5) {
      this.showError('Mỗi nhân viên chỉ có thể mở tối đa 5 hóa đơn nháp cùng lúc.');
      return;
    }
    this.api.csrf().pipe(switchMap(() => this.api.createPosCart())).subscribe({
      next: (cart) => {
        this.posCarts.update((list) => [...list, cart]);
        this.activeCart.set(cart.cartId);
        this.notice.set('Đã mở hóa đơn nháp mới.');
      },
      error: () => this.showError('Không thể tạo hóa đơn mới.'),
    });
  }

  closePosCart(cart: PosCart): void {
    this.api.csrf().pipe(switchMap(() => this.api.deletePosCart(cart.cartId))).subscribe({
      next: () => {
        this.posCarts.update((list) => list.filter((c) => c.cartId !== cart.cartId));
        if (this.activeCart() === cart.cartId) {
          this.activeCart.set(this.posCarts()[0]?.cartId ?? null);
        }
      },
    });
  }

  searchPos(): void {
    const q = this.posQuery().trim();
    if (!q) return;

    // 1. Quét khớp barcode
    const qLower = q.toLowerCase();
    for (const p of this.posCatalog()) {
      const matched = p.variants?.find((v) => v.barcode && v.barcode.toLowerCase() === qLower);
      if (matched) {
        this.addPosItem(p, matched.id);
        this.posQuery.set('');
        this.notice.set(`Đã quét mã vạch: ${p.name} (${matched.color} · ${matched.size})`);
        return;
      }
    }

    // 2. Khớp mã SKU biến thể
    for (const p of this.posCatalog()) {
      const matched = p.variants?.find((v) => v.sku && v.sku.toLowerCase() === qLower);
      if (matched) {
        this.addPosItem(p, matched.id);
        this.posQuery.set('');
        this.notice.set(`Đã thêm theo mã SKU: ${p.name} (${matched.color} · ${matched.size})`);
        return;
      }
    }
  }

  addPosItem(product: Product, variantId: number): void {
    let cartId = this.activeCart();
    if (!cartId) {
      this.api.csrf().pipe(
        switchMap(() => this.api.createPosCart()),
        switchMap((cart) => {
          this.posCarts.update((list) => [...list, cart]);
          this.activeCart.set(cart.cartId);
          return this.api.addPosItem(cart.cartId, variantId, 1);
        })
      ).subscribe({
        next: (cart) => {
          this.posCarts.update((items) => items.map((c) => c.cartId === cart.cartId ? cart : c));
          this.notice.set(`Đã thêm ${product.name} vào hóa đơn.`);
        },
        error: () => this.showError('Không thể thêm sản phẩm vào hóa đơn (hết hàng).'),
      });
      return;
    }
    this.api.csrf().pipe(switchMap(() => this.api.addPosItem(cartId!, variantId, 1))).subscribe({
      next: (cart) => {
        this.posCarts.update((items) => items.map((c) => c.cartId === cart.cartId ? cart : c));
        this.notice.set(`Đã thêm ${product.name} vào hóa đơn.`);
      },
      error: () => this.showError('Không thể thêm sản phẩm vào hóa đơn (hết hàng).'),
    });
  }

  updatePosQuantity(cart: PosCart, variantId: number, quantity: number): void {
    if (quantity < 1) {
      this.api.csrf().pipe(switchMap(() => this.api.deletePosItem(cart.cartId, variantId))).subscribe({
        next: (updated) => this.posCarts.update((items) => items.map((c) => c.cartId === updated.cartId ? updated : c)),
      });
      return;
    }
    this.api.csrf().pipe(switchMap(() => this.api.updatePosItem(cart.cartId, variantId, quantity))).subscribe({
      next: (updated) => this.posCarts.update((items) => items.map((c) => c.cartId === updated.cartId ? updated : c)),
      error: () => this.showError('Số lượng vượt quá tồn kho.'),
    });
  }

  searchPosCustomer(): void {
    const q = this.posCustomerQuery().trim();
    if (!q) {
      this.posCustomerMatches.set([]);
      return;
    }
    this.api.customers(0).subscribe({
      next: (res) => {
        const matches = res.data?.filter(c =>
          c.fullName.toLowerCase().includes(q.toLowerCase()) ||
          (c.phone && c.phone.includes(q))
        ) ?? [];
        this.posCustomerMatches.set(matches);
      },
    });
  }

  selectPosCustomer(c: Customer): void {
    this.selectedPosCustomer.set(c);
    this.posCustomerMatches.set([]);
    this.posCustomerQuery.set(c.fullName + (c.phone ? ' (' + c.phone + ')' : ''));
  }

  openQuickCustomerModal(): void {
    this.quickCustomerForm.reset();
    this.quickCustomerModalOpen.set(true);
  }

  saveQuickCustomer(): void {
    if (this.quickCustomerForm.invalid) return;
    const val = this.quickCustomerForm.getRawValue();
    this.api.csrf().pipe(
      switchMap(() => this.api.createCustomer({
        fullName: val.fullName,
        phone: val.phone,
        email: val.email || undefined,
        line1: val.address || undefined,
      }))
    ).subscribe({
      next: (c) => {
        this.selectPosCustomer(c);
        this.quickCustomerModalOpen.set(false);
        this.notice.set(`Đã thêm khách hàng ${c.fullName}.`);
      },
      error: () => this.showError('Không thể thêm khách hàng.'),
    });
  }

  applyPosVoucher(): void {
    const code = this.posVoucherCode().trim();
    if (!code) return;
    this.api.promotions().subscribe({
      next: (res) => {
        const match = res.data?.find((p) => p.code.toLowerCase() === code.toLowerCase());
        if (match) {
          const cart = this.selectedPosCart();
          const sub = cart?.subtotal ?? 0;
          const discount = Math.min(Math.round((sub * match.percentOff) / 100), match.maximumDiscount ?? 999999999);
          this.posVoucherDiscount.set(discount);
          this.notice.set(`Áp dụng mã ${match.code}: giảm ${discount.toLocaleString('vi-VN')}₫.`);
        } else {
          this.showError('Mã ưu đãi không hợp lệ hoặc đã hết hạn.');
        }
      },
    });
  }

  finishPosOrder(cart: PosCart): void {
    if (!cart.items?.length) {
      this.showError('Hóa đơn chưa có sản phẩm.');
      return;
    }
    const customer = this.selectedPosCustomer();
    const shipVal = this.posShipForm.getRawValue();
    const isDelivery = this.posDelivery();
    const shipAddr = isDelivery ? [shipVal.line1, shipVal.ward, shipVal.district, shipVal.province].filter(Boolean).join(', ') : 'Tại quầy NewMen Store';
    const email = (customer?.email && customer.email.includes('@')) ? customer.email : 'walk-in@newmen.local';
    const paymentMethod = this.posPaymentMethod() === 'CASH' ? 'CASH' : 'BANK_TRANSFER_SAMPLE';

    const payload = {
      cartId: String(cart.cartId),
      paymentMethod,
      customerEmail: email,
      isDelivery,
      shippingFee: isDelivery ? (this.posFreight() || 0) : 0,
      voucherCode: this.posVoucherCode() || null,
      shipRecipientName: isDelivery ? (shipVal.recipientName || 'Khách nhận') : (customer?.fullName || 'Khách lẻ tại quầy'),
      shipPhone: isDelivery ? (shipVal.phone || '') : (customer?.phone || ''),
      shipAddress: shipAddr,
      note: shipVal.note || 'Bán hàng tại quầy NewMen POS',
    };

    this.busy.set(true);
    this.api.csrf().pipe(
      switchMap(() => this.api.posCheckout(payload)),
      finalize(() => this.busy.set(false))
    ).subscribe({
      next: (order) => {
        this.notice.set(`Hoàn tất hóa đơn ${order.code}. Tổng tiền: ${order.total.toLocaleString('vi-VN')}₫.`);
        // In hóa đơn ngay
        this.api.invoice(order.id).subscribe({
          next: (blob) => this.downloadFile(blob, `hoa-don-${order.code}.pdf`),
        });
        // Remove completed cart
        this.posCarts.update((items) => items.filter((c) => c.cartId !== cart.cartId));
        this.activeCart.set(this.posCarts()[0]?.cartId ?? null);
        this.selectedPosCustomer.set(null);
        this.posVoucherCode.set('');
        this.posVoucherDiscount.set(0);
      },
      error: () => this.showError('Không thể thanh toán hóa đơn. Vui lòng thử lại.'),
    });
  }

  selectedPosCart(): PosCart | undefined {
    return this.posCarts().find((item) => item.cartId === this.activeCart());
  }

  // --- 4. ORDERS & RETURNS/EXCHANGES ---
  loadOrders(page = this.ordersPage()): void {
    this.api.adminOrders(page).subscribe({
      next: (result) => {
        let items = result.data ?? [];
        if (this.orderFilterStatus() !== 'ALL') {
          items = items.filter((o) => o.status === this.orderFilterStatus());
        }
        this.orders.set(items);
        this.ordersPage.set(result.page ?? page);
        this.ordersTotalPages.set(result.totalPages ?? 0);
        this.checkPendingOrders();
      },
      error: () => this.showError('Không thể tải danh sách đơn hàng.'),
    });
    this.loadReturnsAndExchanges();
  }

  setOrderFilterStatus(status: string): void {
    this.orderFilterStatus.set(status);
    this.loadOrders(0);
  }

  viewOrderDetail(order: OrderSummary): void {
    this.api.adminOrderDetail(order.id).subscribe({
      next: (detail) => {
        this.selectedOrderDetail.set(detail);
        this.orderDetailModalOpen.set(true);
      },
      error: () => this.showError('Không thể tải chi tiết hóa đơn.'),
    });
  }

  updateOrderStatus(orderId: number, status: string): void {
    this.api.csrf().pipe(switchMap(() => this.api.updateOrderStatus(orderId, status))).subscribe({
      next: (updated) => {
        this.orders.update((list) => list.map((o) => o.id === orderId ? updated : o));
        if (this.selectedOrderDetail()?.id === orderId) {
          this.selectedOrderDetail.update((d) => d ? { ...d, status: updated.status } : null);
        }
        this.notice.set(`Đã cập nhật trạng thái đơn ${updated.code} sang ${this.statusLabel(status)}.`);
        this.checkPendingOrders();
      },
      error: () => this.showError('Không thể cập nhật trạng thái đơn hàng.'),
    });
  }

  printInvoice(orderId: number, code: string): void {
    this.api.invoice(orderId).subscribe({
      next: (blob) => this.downloadFile(blob, `hoa-don-${code}.pdf`),
      error: () => this.showError('Không thể tải hóa đơn PDF.'),
    });
  }

  // Returns & Exchanges
  loadReturnsAndExchanges(): void {
    this.api.adminReturns().subscribe({
      next: (res) => this.returnsList.set(res.data ?? []),
    });
    this.api.getExchanges(0, 20).subscribe({
      next: (res) => this.exchangesList.set(res.data ?? []),
    });
  }

  updateReturnStatus(ret: AdminReturn, status: string): void {
    this.api.csrf().pipe(switchMap(() => this.api.updateReturnStatus(ret.id, status))).subscribe({
      next: () => {
        this.returnsList.update((list) => list.map((r) => r.id === ret.id ? { ...r, status } : r));
        this.notice.set(`Đã cập nhật yêu cầu đổi trả ${ret.orderCode}.`);
      },
      error: () => this.showError('Không thể cập nhật yêu cầu đổi trả.'),
    });
  }

  openCreateExchangeModal(ret?: AdminReturn): void {
    if (!this.products().length) this.loadProducts();
    this.exchangeForm.reset({
      returnId: ret?.id ?? null,
      quantity: 1,
      unitPrice: 350000,
      freight: 0,
      note: ret ? `Đổi cho đơn ${ret.orderCode}` : '',
      shipName: '',
      shipPhone: '',
      shipAddress: '',
    });
    this.exchangeModalOpen.set(true);
  }

  saveExchange(): void {
    if (this.exchangeForm.invalid) return;
    const val = this.exchangeForm.getRawValue();
    this.api.csrf().pipe(
      switchMap(() => this.api.createExchange({
        returnId: val.returnId,
        items: [{
          productDetailId: val.productDetailId,
          quantity: val.quantity,
          unitPrice: val.unitPrice,
          note: val.note,
        }],
        freight: val.freight || 0,
        note: val.note,
        shipName: val.shipName,
        shipPhone: val.shipPhone,
        shipAddress: val.shipAddress,
      }))
    ).subscribe({
      next: () => {
        this.notice.set('Đã tạo phiếu đổi hàng thành công.');
        this.exchangeModalOpen.set(false);
        this.loadReturnsAndExchanges();
      },
      error: () => this.showError('Không thể tạo phiếu đổi hàng.'),
    });
  }

  updateExchangeStatus(ex: ExchangeItem, status: string): void {
    this.api.csrf().pipe(switchMap(() => this.api.updateExchangeStatus(ex.id, status))).subscribe({
      next: (updated) => {
        this.exchangesList.update((list) => list.map((e) => e.id === ex.id ? updated : e));
        this.notice.set('Đã cập nhật trạng thái phiếu đổi hàng.');
      },
      error: () => this.showError('Không thể cập nhật phiếu đổi hàng.'),
    });
  }

  // --- 5. ATTRIBUTES MANAGEMENT (9 TABLES) ---
  loadAttributes(type = this.currentAttributeType()): void {
    this.currentAttributeType.set(type);
    this.api.getAttributes(type).subscribe({
      next: (res) => this.attributeList.set(res.data ?? []),
      error: () => this.showError('Không thể tải thuộc tính.'),
    });
  }

  openAddAttributeModal(): void {
    this.editingAttribute.set(null);
    this.attributeForm.reset({ name: '', code: '', extra: '', status: true });
    this.attributeModalOpen.set(true);
  }

  editAttribute(item: AttributeItem): void {
    this.editingAttribute.set(item);
    this.attributeForm.patchValue({
      name: item.name,
      code: item.code || '',
      extra: item.extra || '',
      status: item.status,
    });
    this.attributeModalOpen.set(true);
  }

  saveAttribute(): void {
    if (this.attributeForm.invalid) return;
    const val = this.attributeForm.getRawValue();
    const type = this.currentAttributeType();
    const editing = this.editingAttribute();

    const request = editing
      ? this.api.updateAttribute(type, editing.id, {
          name: val.name || undefined,
          code: val.code || undefined,
          extra: val.extra || undefined,
          status: val.status ?? true,
        })
      : this.api.createAttribute(type, {
          name: val.name || '',
          code: val.code || undefined,
          extra: val.extra || undefined,
          status: val.status ?? true,
        });

    this.api.csrf().pipe(switchMap(() => request)).subscribe({
      next: () => {
        this.notice.set(editing ? 'Đã cập nhật thuộc tính.' : 'Đã tạo thuộc tính mới.');
        this.attributeModalOpen.set(false);
        this.loadAttributes();
        this.loadCatalogAttributes();
      },
      error: () => this.showError('Không thể lưu thuộc tính.'),
    });
  }

  deleteAttribute(item: AttributeItem): void {
    if (!confirm(`Xóa thuộc tính "${item.name}"?`)) return;
    this.api.csrf().pipe(switchMap(() => this.api.deleteAttribute(this.currentAttributeType(), item.id))).subscribe({
      next: () => {
        this.notice.set(`Đã xóa thuộc tính "${item.name}".`);
        this.loadAttributes();
        this.loadCatalogAttributes();
      },
      error: () => this.showError('Không thể xóa thuộc tính đang được gắn vào sản phẩm.'),
    });
  }

  // --- 6. FAULT PRODUCTS (SẢN PHẨM HỦY/LỖI) ---
  loadFaultProducts(): void {
    this.api.getFaultProducts(0, 30).subscribe({
      next: (res) => this.faultProducts.set(res.data ?? []),
      error: (err) => this.showError('Không thể tải danh sách sản phẩm lỗi/hủy.', err),
    });
  }

  openFaultProductModal(): void {
    if (!this.products().length) this.loadProducts();
    this.faultProductForm.reset({ quantity: 1, description: 'Lỗi đường may / rách vải kho' });
    this.faultProductModalOpen.set(true);
  }

  saveFaultProduct(): void {
    if (this.faultProductForm.invalid) return;
    const val = this.faultProductForm.getRawValue();
    this.api.csrf().pipe(
      switchMap(() => this.api.createFaultProduct({
        productDetailId: val.productDetailId!,
        quantity: val.quantity!,
        description: val.description!,
      }))
    ).subscribe({
      next: (item) => {
        this.notice.set(`Đã ghi nhận hủy ${item.quantity} sản phẩm (${item.productName}).`);
        this.faultProductModalOpen.set(false);
        this.loadFaultProducts();
        this.loadProducts();
      },
      error: () => this.showError('Không thể ghi nhận hủy sản phẩm (số lượng vượt tồn kho).'),
    });
  }

  // --- 7. PROMOTIONS & VOUCHERS ---
  loadPromotions(): void {
    this.api.getDiscounts(0, 30).subscribe({
      next: (res) => this.discounts.set(res.data ?? []),
    });
    this.api.getVouchers(0, 30).subscribe({
      next: (res) => this.vouchers.set(res.data ?? []),
    });
  }

  saveDiscount(): void {
    if (this.discountForm.invalid) return;
    const val = this.discountForm.getRawValue();
    this.api.csrf().pipe(
      switchMap(() => this.api.createDiscount({
        name: val.name || '',
        description: val.description || '',
        startDate: (val.startDate || this.today()) + 'T00:00:00',
        endDate: (val.endDate || this.daysFromNow(30)) + 'T23:59:59',
        percent: val.percent || 15,
        status: true,
      }))
    ).subscribe({
      next: () => {
        this.notice.set('Đã tạo chương trình khuyến mãi mới.');
        this.discountModalOpen.set(false);
        this.loadPromotions();
      },
      error: () => this.showError('Không thể tạo chương trình khuyến mãi.'),
    });
  }

  saveVoucher(): void {
    if (this.voucherForm.invalid) return;
    const val = this.voucherForm.getRawValue();
    this.api.csrf().pipe(
      switchMap(() => this.api.createVoucher({
        code: (val.code || '').toUpperCase(),
        name: val.name || '',
        description: val.description || '',
        discount: val.discount || 10,
        min: val.min ?? 0,
        max: val.max ?? 0,
        startDate: (val.startDate || this.today()) + 'T00:00:00',
        endDate: (val.endDate || this.daysFromNow(60)) + 'T23:59:59',
        type: val.type ?? 1,
        status: true,
      }))
    ).subscribe({
      next: () => {
        this.notice.set('Đã tạo phiếu giảm giá Voucher thành công.');
        this.voucherModalOpen.set(false);
        this.loadPromotions();
      },
      error: () => this.showError('Không thể tạo voucher (trùng mã).'),
    });
  }

  // --- 8. CUSTOMERS & ADDRESS BOOK ---
  loadCustomers(page = this.customersPage()): void {
    this.api.customers(page).subscribe({
      next: (result) => {
        this.customers.set(result.data ?? []);
        this.customersPage.set(result.page ?? page);
        this.customersTotalPages.set(result.totalPages ?? 0);
      },
      error: () => this.showError('Không thể tải danh sách khách hàng.'),
    });
  }

  viewCustomerDetail(customer: Customer): void {
    this.api.getCustomerDetail(customer.id).subscribe({
      next: (detail) => {
        this.selectedCustomerDetail.set(detail);
        this.customerDetailModalOpen.set(true);
      },
      error: () => this.showError('Không thể tải sổ địa chỉ khách hàng.'),
    });
  }

  exportCustomersCsv(): void {
    this.api.exportCustomersCsv().subscribe({
      next: (blob) => this.downloadFile(blob, `khach-hang-newmen-${this.today()}.csv`),
      error: () => this.showError('Không thể xuất file CSV khách hàng.'),
    });
  }

  // --- 9. STAFF & ROLES ---
  loadStaffAndRoles(): void {
    this.api.staff().subscribe({
      next: (res) => this.staff.set(res.data ?? []),
    });
    this.api.getRoles().subscribe({
      next: (res) => this.roles.set(res),
    });
  }

  saveStaff(): void {
    if (this.staffForm.invalid) return;
    this.api.csrf().pipe(switchMap(() => this.api.createStaff(this.staffForm.getRawValue()))).subscribe({
      next: () => {
        this.notice.set('Đã tạo tài khoản nhân viên mới.');
        this.staffModalOpen.set(false);
        this.loadStaffAndRoles();
      },
      error: () => this.showError('Không thể tạo nhân viên (trùng email hoặc mật khẩu quá ngắn).'),
    });
  }

  toggleStaff(member: StaffMember): void {
    const active = member.active === false;
    this.api.csrf().pipe(switchMap(() => this.api.updateStaff(member.id, { active }))).subscribe({
      next: () => {
        this.staff.update((list) => list.map((m) => m.id === member.id ? { ...m, active } : m));
        this.notice.set(active ? `Đã kích hoạt tài khoản ${member.fullName}.` : `Đã khóa tài khoản ${member.fullName}.`);
      },
      error: () => this.showError('Không thể cập nhật nhân viên.'),
    });
  }

  saveRole(): void {
    if (this.roleForm.invalid) return;
    this.api.csrf().pipe(switchMap(() => this.api.createRole(this.roleForm.getRawValue()))).subscribe({
      next: () => {
        this.notice.set('Đã thêm vai trò mới.');
        this.roleModalOpen.set(false);
        this.loadStaffAndRoles();
      },
      error: () => this.showError('Không thể tạo vai trò.'),
    });
  }

  // --- 10. BANNERS ---
  loadBanners(): void {
    this.api.getBanners().subscribe({
      next: (res) => this.banners.set(res.data ?? []),
      error: () => this.showError('Không thể tải banner.'),
    });
  }

  saveBanner(): void {
    if (this.bannerForm.invalid) return;
    this.api.csrf().pipe(switchMap(() => this.api.createBanner(this.bannerForm.getRawValue()))).subscribe({
      next: () => {
        this.notice.set('Đã tạo banner quảng cáo mới.');
        this.bannerModalOpen.set(false);
        this.loadBanners();
      },
      error: () => this.showError('Không thể tạo banner.'),
    });
  }

  toggleBannerStatus(banner: BannerItem): void {
    const status = !banner.status;
    this.api.csrf().pipe(switchMap(() => this.api.updateBanner(banner.id, { status }))).subscribe({
      next: () => {
        this.banners.update((list) => list.map((b) => b.id === banner.id ? { ...b, status } : b));
        this.notice.set('Đã cập nhật trạng thái banner.');
      },
      error: () => this.showError('Không thể cập nhật banner.'),
    });
  }

  deleteBanner(banner: BannerItem): void {
    if (!confirm('Xóa banner này?')) return;
    this.api.csrf().pipe(switchMap(() => this.api.deleteBanner(banner.id))).subscribe({
      next: () => {
        this.banners.update((list) => list.filter((b) => b.id !== banner.id));
        this.notice.set('Đã xóa banner.');
      },
      error: () => this.showError('Không thể xóa banner.'),
    });
  }

  // --- 11. FEEDBACK (CONTACTS & REVIEWS) ---
  loadFeedback(): void {
    this.api.getContacts(0, 30).subscribe({
      next: (res) => this.contacts.set(res.data ?? []),
    });
    this.api.getAdminReviews().subscribe({
      next: (res) => this.reviews.set(res),
    });
  }

  toggleContactStatus(c: ContactItem): void {
    const status = !c.status;
    this.api.csrf().pipe(switchMap(() => this.api.updateContactStatus(c.id, status))).subscribe({
      next: (updated) => {
        this.contacts.update((list) => list.map((item) => item.id === c.id ? updated : item));
        this.notice.set(status ? 'Đã đánh dấu đã phản hồi.' : 'Đã đánh dấu chưa phản hồi.');
      },
      error: () => this.showError('Không thể cập nhật trạng thái liên hệ.'),
    });
  }

  toggleReviewStatus(r: ReviewAdminItem): void {
    const status = !r.status;
    this.api.csrf().pipe(switchMap(() => this.api.toggleReviewStatus(r.id, status))).subscribe({
      next: (updated) => {
        this.reviews.update((list) => list.map((item) => item.id === r.id ? updated : item));
        this.notice.set(status ? 'Đã hiển thị đánh giá lên trang sản phẩm.' : 'Đã ẩn đánh giá.');
      },
      error: () => this.showError('Không thể cập nhật đánh giá.'),
    });
  }

  // --- HELPERS ---
  stockCount(product: Product): number {
    return product.variants?.reduce((sum, v) => sum + v.stock, 0) ?? 0;
  }

  statusLabel(status: string): string {
    return (
      {
        PENDING: 'Chờ duyệt',
        CONFIRMED: 'Đã duyệt',
        PREPARING: 'Chuẩn bị hàng',
        READY_TO_SHIP: 'Chờ giao hàng',
        SHIPPING: 'Đang giao',
        COMPLETED: 'Hoàn thành',
        CANCELLED: 'Đã hủy',
        APPROVED: 'Đã duyệt',
        REJECTED: 'Từ chối',
        RECEIVED: 'Đã nhận hàng',
      } as Record<string, string>
    )[status] ?? status;
  }

  availableOrderStatuses(current: string): Array<[string, string]> {
    const next: Record<string, string[]> = {
      PENDING: ['CONFIRMED', 'CANCELLED'],
      CONFIRMED: ['PREPARING', 'CANCELLED'],
      PREPARING: ['READY_TO_SHIP', 'CANCELLED'],
      READY_TO_SHIP: ['SHIPPING'],
      SHIPPING: ['COMPLETED'],
      COMPLETED: [],
      CANCELLED: [],
    };
    const allowed = new Set([current, ...(next[current] ?? [])]);
    return this.orderStatuses.filter(([status]) => allowed.has(status));
  }

  showError(msg: string, err?: unknown): void {
    if (err && typeof err === 'object' && 'status' in err) {
      const status = (err as { status: number }).status;
      if (status === 401 || status === 403) {
        this.user.set(null);
        this.error.set('Phiên đăng nhập đã hết hạn hoặc chưa xác thực. Vui lòng đăng nhập lại bên dưới.');
        return;
      }
    }
    this.error.set(msg);
  }

  private today(): string {
    return this.localDate(new Date());
  }

  private daysAgo(days: number): string {
    const d = new Date();
    d.setDate(d.getDate() - days);
    return this.localDate(d);
  }

  private daysFromNow(days: number): string {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return this.localDate(d);
  }

  private localDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private downloadFile(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }

  // Bar height & SVG layout helpers for Revenue Chart
  barHeight(val: number, max: number): number {
    if (!val || val <= 0 || !max || max <= 0) return 0;
    return Math.max(4, Math.round((val / max) * 125));
  }

  chartBarX(index: number, total: number): number {
    if (!total || total <= 0) return 60;
    const step = 700 / total;
    const barW = this.chartBarWidth(total);
    return 60 + index * step + (step - barW) / 2;
  }

  chartBarWidth(total: number): number {
    if (!total || total <= 0) return 24;
    const step = 700 / total;
    return Math.max(8, Math.min(32, Math.floor(step * 0.65)));
  }

  shouldShowChartLabel(index: number, total: number, value: number): boolean {
    if (total <= 8) return true;
    if (value > 0) return true;
    const step = Math.ceil(total / 6);
    return index === 0 || index === total - 1 || index % step === 0;
  }

  maxChartValue(): number {
    const points = this.dashboardStats()?.revenueChart ?? [];
    if (!points.length) return 1000000;
    return Math.max(...points.map((p) => p.value), 1000000);
  }
}
