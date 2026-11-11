package vn.newmen.store.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiModels {
    private ApiModels() { }

    public record PageResponse<T>(List<T> data, int page, int size, long totalItems, int totalPages) { }
    public record CategoryResponse(Long id, String name, String slug, Long parentId) { }
    public record VariantResponse(Long id, String sku, String barcode, String color, String size,
                                  BigDecimal price, int stock) { }
    public record ProductResponse(Long id, String slug, String code, String name, String description,
                                  String categoryName, BigDecimal price, BigDecimal originalPrice,
                                  List<String> images, List<VariantResponse> variants) { }
    public record PromotionResponse(Long id, String name, String code, String description,
                                    int percentOff, BigDecimal minimumOrder, BigDecimal maximumDiscount,
                                    LocalDateTime startsAt, LocalDateTime endsAt) { }
    public record AuthResponse(Long id, String email, String fullName, String phone, String role) { }
    public record CartItemResponse(Long variantId, String productName, String slug, String color, String size,
                                   String imageUrl, BigDecimal unitPrice, int quantity, int stock) { }
    public record CartResponse(List<CartItemResponse> items, BigDecimal subtotal, BigDecimal discount,
                               BigDecimal total, String voucherCode) { }
    public record AddressResponse(Long id, String recipientName, String phone, String line1,
                                  String ward, String district, String province, boolean isDefault) { }
    public record OrderLineResponse(Long productId, Long variantId, String productName, String sku, String color,
                                    String size, String imageUrl, int quantity, BigDecimal unitPrice,
                                    BigDecimal lineTotal) { }
    public record OrderHistoryResponse(String previousStatus, String nextStatus, String note,
                                       String changedBy, LocalDateTime createdAt) { }
    public record OrderResponse(Long id, String code, String status, String channel,
                                BigDecimal total, String paymentMethod, LocalDateTime createdAt) { }
    public record OrderDetailResponse(Long id, String code, String status, String channel,
                                      String customerName, String customerEmail, String customerPhone,
                                      AddressInput shippingAddress, String paymentMethod, String voucherCode,
                                      BigDecimal subtotal, BigDecimal discount, BigDecimal shippingFee,
                                      BigDecimal total, String note, LocalDateTime createdAt,
                                      List<OrderLineResponse> items, List<OrderHistoryResponse> history) { }
    public record ProfileResponse(Long id, String email, String fullName, String phone) { }
    public record CustomerResponse(Long id, String email, String fullName, String phone, LocalDateTime createdAt) { }
    public record CustomerDetailResponse(Long id, String email, String fullName, String phone, int points,
                                         boolean status, LocalDateTime createdAt, List<AddressResponse> addresses) { }
    public record ImageResponse(Long id, String imageUrl, String fileName, boolean isPrimary) { }
    public record ReviewResponse(Long id, int rating, String content, String author, LocalDateTime createdAt) { }
    public record ReportSummaryResponse(LocalDateTime from, LocalDateTime to, BigDecimal revenue,
                                        long orderCount, long unitsSold, long customerCount) { }
    public record TopProductResponse(String productName, long quantitySold, BigDecimal revenue) { }
    public record PosCartResponse(String cartId, List<CartItemResponse> items, BigDecimal subtotal,
                                  String customerName, String customerPhone) { }
    public record ReturnResponse(Long id, Long orderId, String orderCode, String reason, String status,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record ErrorResponse(ErrorBody error) { }
    public record ErrorBody(String code, String message, Object details) { }

    // Additional models for 32 tables and Chapter 3 screens
    public record AttributeItemResponse(Long id, String name, String code, String extra, boolean status) { }
    public record AttributeWriteRequest(String name, String code, String extra, Long parentId, Boolean status) { }

    public record FaultProductResponse(Long id, Long productDetailId, String productName, String sku,
                                       String color, String size, int quantity, String description,
                                       int status, LocalDateTime createdAt) { }
    public record FaultProductWriteRequest(@NotNull Long productDetailId, @Min(1) int quantity,
                                           @NotBlank String description) { }

    public record DiscountResponse(Long id, String name, String description, LocalDateTime startDate,
                                    LocalDateTime endDate, boolean status, int productCount) { }
    public record DiscountWriteRequest(@NotBlank String name, String description,
                                       @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate,
                                       Boolean status, List<Long> productIds, Integer percent) { }

    public record VoucherResponse(Long id, String code, String name, String description, int discount,
                                  BigDecimal min, BigDecimal max, LocalDateTime startDate,
                                  LocalDateTime endDate, int type, boolean status) { }
    public record VoucherWriteRequest(@NotBlank String code, @NotBlank String name, String description,
                                      int discount, BigDecimal min, BigDecimal max,
                                      @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate,
                                      Integer type, Boolean status) { }

    public record BannerResponse(Long id, String title, String content, int type, String url,
                                 String linkUrl, boolean status, int sortOrder) { }
    public record BannerWriteRequest(String title, String content, Integer type, @NotBlank String url,
                                     String linkUrl, Boolean status, Integer sortOrder) { }

    public record ContactResponse(Long id, String name, String phone, String email, String addressName,
                                  String ward, String district, String province, String content,
                                  boolean status, LocalDateTime createdAt) { }
    public record ContactWriteRequest(@NotBlank String name, @NotBlank String phone, String email,
                                      String addressName, String ward, String district, String province,
                                      @NotBlank String content) { }

    public record ReviewAdminResponse(Long id, Long productId, String productName, String author,
                                      int rating, String content, boolean status,
                                      LocalDateTime createdAt, List<String> images) { }

    public record ExchangeItemRequest(@NotNull Long productDetailId, @Min(1) int quantity,
                                      BigDecimal unitPrice, String note) { }
    public record ExchangeCreateRequest(@NotNull Long returnId, Long customerId, String shipName,
                                        String shipPhone, String shipAddress, BigDecimal freight,
                                        String note, List<ExchangeItemRequest> items) { }
    public record ExchangeDetailItemResponse(Long id, Long productDetailId, String productName, String sku,
                                             String color, String size, int quantity, BigDecimal unitPrice, String note) { }
    public record ExchangeResponse(Long id, Long customerId, String customerName, Long returnId,
                                   BigDecimal total, String note, String shipAddress, String shipName,
                                   String shipPhone, String statusPay, BigDecimal freight, String status,
                                   LocalDateTime createdAt, List<ExchangeDetailItemResponse> items) { }

    public record GenerateVariantsRequest(List<String> colors, List<String> sizes,
                                          BigDecimal basePrice, BigDecimal costPrice,
                                          Integer defaultStock, String productCode) { }
    public record GeneratedVariantItem(String sku, String barcode, String color, String size,
                                       BigDecimal price, BigDecimal costPrice, int stock) { }

    public record ChartDataPoint(String label, BigDecimal value, long count) { }
    public record DashboardStatsResponse(BigDecimal totalRevenue, long totalOrders, long totalProductsSold,
                                         long totalCustomers, List<ChartDataPoint> revenueChart,
                                         List<TopProductResponse> topProducts, List<CustomerResponse> topCustomers) { }

    // Requests
    public record RegisterRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @Size(max = 32) String phone) { }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record ProfileUpdateRequest(@NotBlank @Size(max = 160) String fullName,
                                       @Size(max = 32) String phone) { }
    public record AddressInput(@NotBlank @Size(max = 160) String recipientName,
                               @NotBlank @Size(max = 32) String phone,
                               @NotBlank @Size(max = 255) String line1,
                               @Size(max = 120) String ward,
                               @NotBlank @Size(max = 120) String district,
                               @NotBlank @Size(max = 120) String province) { }
    public record AddressWriteRequest(@Valid @NotNull AddressInput address, boolean isDefault) { }
    public record CartItemRequest(@NotNull Long variantId, @Min(1) @Max(20) int quantity) { }
    public record CartQuantityRequest(@Min(1) @Max(20) int quantity) { }
    public record VoucherRequest(@NotBlank @Size(max = 64) String code) { }
    public record CheckoutRequest(@Size(max = 160) String customerName,
                                  @Email @Size(max = 255) String customerEmail,
                                  @Size(max = 32) String customerPhone,
                                  @Valid @NotNull AddressInput shippingAddress,
                                  @NotBlank String paymentMethod,
                                  @Size(max = 64) String voucherCode,
                                  @Size(max = 500) String note) { }
    public record ReviewRequest(@NotNull Long productId, @Min(1) @Max(5) int rating, @Size(max = 1000) String content) { }
    public record ProductWriteRequest(@NotBlank @Size(max = 64) String code,
                                      @NotBlank @Size(max = 200) String name,
                                      @Size(max = 3000) String description,
                                      Long categoryId,
                                      Long materialId,
                                      Long collarId,
                                      Long designId,
                                      Long formId,
                                      Long sleeveId,
                                      @Size(max = 120) String material,
                                      @Size(max = 100) String collar,
                                      @Size(max = 100) String design,
                                      @Size(max = 100) String fit,
                                      @Size(max = 100) String sleeve,
                                      BigDecimal originalPrice,
                                      Double weight,
                                      Boolean active,
                                      List<@Valid VariantWriteRequest> variants) { }
    public record VariantWriteRequest(@NotBlank @Size(max = 64) String sku,
                                      @Size(max = 64) String barcode,
                                      @NotBlank @Size(max = 60) String color,
                                      @NotBlank @Size(max = 24) String size,
                                      @NotNull @Positive BigDecimal price,
                                      BigDecimal costPrice,
                                      @Min(0) int stock) { }
    public record VariantUpdateRequest(@Size(max = 64) String sku, @Size(max = 64) String barcode,
                                       @Size(max = 60) String color, @Size(max = 24) String size,
                                       @Positive BigDecimal price, BigDecimal costPrice,
                                       @Min(0) Integer stock, Boolean active) { }
    public record CategoryWriteRequest(@NotBlank @Size(max = 120) String name,
                                       @NotBlank @Size(max = 150) String slug,
                                       Long parentId, Long originCategoryId, Boolean active) { }
    public record StatusUpdateRequest(@NotBlank String status, @Size(max = 500) String note) { }
    public record PromotionWriteRequest(@NotBlank @Size(max = 160) String name,
                                        @NotBlank @Size(max = 64) String code,
                                        @Size(max = 500) String description,
                                        @Min(1) @Max(90) int percentOff,
                                        @NotNull @Min(0) BigDecimal minimumOrder,
                                        @Min(0) BigDecimal maximumDiscount,
                                        @NotNull LocalDateTime startsAt,
                                        @NotNull LocalDateTime endsAt,
                                        Boolean active) { }
    public record StaffWriteRequest(@NotBlank @Size(max = 160) String fullName,
                                   @NotBlank @Email @Size(max = 255) String email,
                                   @Size(min = 8, max = 72) String password,
                                   @Size(max = 32) String phone,
                                   @NotBlank String role,
                                   Boolean active) { }
    public record PosCartCreateRequest(@Size(max = 160) String customerName,
                                      @Size(max = 32) String customerPhone) { }
    public record PosCartItemRequest(@NotNull Long variantId, @Min(1) @Max(20) int quantity) { }
    public record PosCheckoutRequest(@NotBlank String cartId,
                                     @Email @Size(max = 255) String customerEmail,
                                     @NotBlank String paymentMethod,
                                     @Size(max = 500) String note,
                                     String voucherCode,
                                     Boolean isDelivery,
                                     String shipRecipientName,
                                     String shipPhone,
                                     String shipAddress,
                                     BigDecimal shippingFee) { }
    public record ReturnCreateRequest(@NotBlank @Size(max = 500) String reason, String note, String video) { }
    public record ReturnStatusRequest(@NotBlank String status) { }
}
