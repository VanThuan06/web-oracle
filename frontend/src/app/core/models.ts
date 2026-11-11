export interface PageResult<T> {
  data: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId?: number | null;
}

export interface ProductVariant {
  id: number;
  sku: string;
  barcode?: string;
  color: string;
  size: string;
  price: number;
  costPrice?: number;
  stock: number;
  active?: boolean;
}

export interface Product {
  id: number;
  slug: string;
  code: string;
  name: string;
  description: string;
  categoryName: string;
  categoryId?: number;
  price: number;
  originalPrice?: number | null;
  weight?: number;
  material?: string;
  collar?: string;
  design?: string;
  fit?: string;
  sleeve?: string;
  images: string[];
  variants: ProductVariant[];
  isNew?: boolean;
  isBestSeller?: boolean;
  active?: boolean;
}

export interface CartItem {
  variantId: number;
  productName: string;
  slug: string;
  color: string;
  size: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  stock: number;
}

export interface Cart {
  items: CartItem[];
  subtotal: number;
  discount: number;
  total: number;
  voucherCode?: string;
}

export interface Promotion {
  id: number;
  name: string;
  code: string;
  description?: string;
  percentOff: number;
  minimumOrder?: number;
  maximumDiscount?: number;
  startsAt?: string;
  endsAt?: string;
}

export interface OrderSummary {
  id: number;
  code: string;
  status: string;
  channel?: 'ONLINE' | 'POS';
  total: number;
  paymentMethod?: string;
  paymentType?: string;
  statusPay?: string;
  createdAt: string;
  customerName?: string;
  customerPhone?: string;
}

export interface OrderDetail extends OrderSummary {
  customerEmail?: string;
  voucherCode?: string;
  subtotal?: number;
  discount?: number;
  shippingFee?: number;
  freight?: number;
  note?: string;
  items?: Array<{ productId?: number; variantId: number; productName: string; sku?: string; color: string; size: string; quantity: number; unitPrice: number; lineTotal?: number; imageUrl?: string }>;
  history?: Array<{ previousStatus?: string; nextStatus: string; note?: string; changedBy?: string; createdAt: string }>;
  shippingAddress?: { recipientName: string; phone: string; line1: string; ward?: string; district: string; province: string };
}

export interface Customer {
  id: number;
  fullName: string;
  email?: string;
  phone?: string;
  points?: number;
  status?: boolean;
  createdAt?: string;
  ordersCount?: number;
  totalSpent?: number;
}

export interface CustomerAddress {
  id: number;
  recipientName: string;
  phone: string;
  line1: string;
  ward?: string;
  district: string;
  province: string;
  isDefault: boolean;
}

export interface CustomerDetail extends Customer {
  addresses: CustomerAddress[];
}

export interface ReportSummary {
  from?: string;
  to?: string;
  revenue: number;
  orderCount: number;
  unitsSold: number;
  productsSold?: number;
  customerCount: number;
}

export interface ApiError {
  error?: { code?: string; message?: string; details?: unknown };
}

export interface AttributeItem {
  id: number;
  name: string;
  code?: string;
  extra?: string;
  status: boolean;
}

export interface FaultProduct {
  id: number;
  productDetailId: number;
  productName: string;
  sku: string;
  color: string;
  size: string;
  quantity: number;
  description: string;
  status: number;
  createdAt: string;
}

export interface DiscountItem {
  id: number;
  name: string;
  description?: string;
  startDate: string;
  endDate: string;
  status: boolean;
  productCount: number;
}

export interface VoucherItem {
  id: number;
  code: string;
  name: string;
  description?: string;
  discount: number;
  min?: number;
  max?: number;
  startDate: string;
  endDate: string;
  type: number;
  status: boolean;
}

export interface BannerItem {
  id: number;
  title?: string;
  content?: string;
  type: number;
  url: string;
  linkUrl?: string;
  status: boolean;
  sortOrder: number;
}

export interface ContactItem {
  id: number;
  name: string;
  phone: string;
  email?: string;
  addressName?: string;
  ward?: string;
  district?: string;
  province?: string;
  content: string;
  status: boolean;
  createdAt: string;
}

export interface ReviewAdminItem {
  id: number;
  productId: number;
  productName: string;
  author: string;
  rating: number;
  content: string;
  status: boolean;
  createdAt: string;
  images: string[];
}

export interface ExchangeDetailItem {
  id: number;
  productDetailId: number;
  productName: string;
  sku: string;
  color: string;
  size: string;
  quantity: number;
  unitPrice: number;
  note?: string;
}

export interface ExchangeItem {
  id: number;
  customerId?: number;
  customerName?: string;
  returnId?: number;
  total: number;
  note?: string;
  shipAddress?: string;
  shipName?: string;
  shipPhone?: string;
  statusPay?: string;
  freight?: number;
  status: string;
  createdAt: string;
  items: ExchangeDetailItem[];
}

export interface GeneratedVariant {
  sku: string;
  barcode: string;
  color: string;
  size: string;
  price: number;
  costPrice: number;
  stock: number;
}

export interface ChartDataPoint {
  label: string;
  value: number;
  count: number;
}

export interface DashboardStats {
  totalRevenue: number;
  totalOrders: number;
  totalProductsSold: number;
  totalCustomers: number;
  revenueChart: ChartDataPoint[];
  topProducts: Array<{ productName: string; quantitySold: number; revenue: number }>;
  topCustomers: Customer[];
}

export interface RoleItem {
  id: number;
  name: string;
  description?: string;
  status: boolean;
}

export const EMPTY_CART: Cart = { items: [], subtotal: 0, discount: 0, total: 0 };
