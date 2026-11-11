package vn.newmen.store.service;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Account;
import vn.newmen.store.domain.CustomerOrder;
import vn.newmen.store.domain.InventoryMovement;
import vn.newmen.store.domain.OrderLine;
import vn.newmen.store.domain.OrderReturn;
import vn.newmen.store.domain.OrderStatusHistory;
import vn.newmen.store.domain.ProductVariant;
import vn.newmen.store.domain.Promotion;
import vn.newmen.store.repository.AccountRepository;
import vn.newmen.store.repository.CustomerOrderRepository;
import vn.newmen.store.repository.InventoryMovementRepository;
import vn.newmen.store.repository.OrderLineRepository;
import vn.newmen.store.repository.OrderReturnRepository;
import vn.newmen.store.repository.OrderStatusHistoryRepository;
import vn.newmen.store.repository.ProductVariantRepository;

@Service
public class OrderService {
    private final CustomerOrderRepository orders;
    private final ProductVariantRepository variants;
    private final OrderLineRepository lines;
    private final OrderStatusHistoryRepository history;
    private final InventoryMovementRepository inventory;
    private final OrderReturnRepository returns;
    private final AccountRepository accounts;
    private final CartService carts;
    private final CatalogService catalog;
    private final java.math.BigDecimal shippingFee;
    private final java.math.BigDecimal freeShippingThreshold;

    public OrderService(CustomerOrderRepository orders, ProductVariantRepository variants, OrderLineRepository lines,
                        OrderStatusHistoryRepository history, InventoryMovementRepository inventory,
                        OrderReturnRepository returns, AccountRepository accounts, CartService carts,
                        CatalogService catalog, @Value("${app.checkout.shipping-fee:25000}") java.math.BigDecimal shippingFee,
                        @Value("${app.checkout.free-shipping-threshold:899000}") java.math.BigDecimal freeShippingThreshold) {
        this.orders = orders;
        this.variants = variants;
        this.lines = lines;
        this.history = history;
        this.inventory = inventory;
        this.returns = returns;
        this.accounts = accounts;
        this.carts = carts;
        this.catalog = catalog;
        this.shippingFee = shippingFee;
        this.freeShippingThreshold = freeShippingThreshold;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request, String idempotencyKey, Account account, HttpSession session) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key là bắt buộc và tối đa 100 ký tự");
        }
        String requestHash = sha256(request.toString());
        var existing = orders.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!requestHash.equals(existing.get().requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key đã được dùng với nội dung khác");
            }
            return orderResponse(existing.get());
        }
        CartService.State state = carts.state(session);
        if (state.quantities.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Giỏ hàng đang trống");
        AddressInput address = request.shippingAddress();
        String customerName = account == null ? required(request.customerName(), "Tên người nhận") : account.fullName;
        String customerEmail = account == null ? required(request.customerEmail(), "Email") : account.email;
        String customerPhone = account == null ? required(request.customerPhone(), "Số điện thoại") : safe(account.phone, request.customerPhone());
        String voucher = request.voucherCode() == null || request.voucherCode().isBlank() ? state.voucherCode : request.voucherCode().trim();
        CustomerOrder order = createOrder(state.quantities, account, customerName, customerEmail, customerPhone,
            address, request.paymentMethod(), voucher, request.note(), "ONLINE", account == null ? "guest" : account.email,
            "PENDING", idempotencyKey, requestHash, true);
        return orderResponse(order);
    }

    @Transactional
    public OrderResponse createPosOrder(PosCheckoutRequest request, Account staff, HttpSession session) {
        PosState state = posState(session);
        PosDraft draft = state.carts.get(request.cartId());
        if (draft == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ bán tại quầy");
        if (draft.quantities.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Giỏ bán tại quầy đang trống");
        String customerName = draft.customerName == null || draft.customerName.isBlank() ? "Khách lẻ" : draft.customerName;
        String customerPhone = draft.customerPhone == null || draft.customerPhone.isBlank() ? "N/A" : draft.customerPhone;
        String email = request.customerEmail() == null || request.customerEmail().isBlank() ? "walk-in@newmen.local" : request.customerEmail();
        AddressInput storeAddress = new AddressInput(customerName, customerPhone, "Bán tại quầy", null, "Cửa hàng", "Hà Nội");
        CustomerOrder order = createOrder(draft.quantities, null, customerName, email, customerPhone,
            storeAddress, request.paymentMethod(), null, request.note(), "POS", staff.email, "COMPLETED",
            "POS-" + UUID.randomUUID(), sha256(request.toString() + UUID.randomUUID()), false);
        state.carts.remove(request.cartId());
        return orderResponse(order);
    }

    private CustomerOrder createOrder(Map<Long, Integer> requested, Account account, String customerName,
                                      String customerEmail, String customerPhone, AddressInput address,
                                      String paymentMethod, String voucherCode, String note, String channel,
                                      String createdBy, String initialStatus, String idempotencyKey,
                                      String requestHash, boolean includeShipping) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        List<String> allowedMethods = channel.equals("POS")
            ? List.of("CASH", "BANK_TRANSFER_SAMPLE") : List.of("COD", "BANK_TRANSFER_SAMPLE");
        if (!allowedMethods.contains(method)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Phương thức thanh toán không được hỗ trợ");
        Map<Long, ProductVariant> locked = new TreeMap<>();
        for (Long variantId : requested.keySet()) {
            ProductVariant variant = variants.findByIdForUpdate(variantId)
                .filter(value -> value.active && value.getProduct().getActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Một sản phẩm trong giỏ không còn kinh doanh"));
            int quantity = requested.getOrDefault(variantId, 0);
            if (quantity < 1 || quantity > 20 || quantity > variant.stock) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tồn kho đã thay đổi; hãy cập nhật lại giỏ hàng");
            }
            locked.put(variantId, variant);
        }
        java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
        for (var entry : requested.entrySet()) {
            subtotal = subtotal.add(locked.get(entry.getKey()).price.multiply(java.math.BigDecimal.valueOf(entry.getValue())));
        }
        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
        String appliedVoucher = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            Promotion promotion = catalog.activePromotion(voucherCode);
            if (subtotal.compareTo(promotion.minimumOrder) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Đơn hàng chưa đạt giá trị tối thiểu của mã ưu đãi");
            }
            discount = subtotal.multiply(java.math.BigDecimal.valueOf(promotion.percentOff))
                .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            if (promotion.maximumDiscount != null) discount = discount.min(promotion.maximumDiscount);
            appliedVoucher = promotion.code;
        }
        CustomerOrder order = new CustomerOrder();
        order.code = "NM-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        order.idempotencyKey = idempotencyKey;
        order.requestHash = requestHash;
        order.account = account;
        order.customerName = customerName;
        order.customerEmail = customerEmail;
        order.customerPhone = customerPhone;
        order.shipRecipientName = address.recipientName();
        order.shipPhone = address.phone();
        order.shipLine1 = address.line1();
        order.shipWard = address.ward();
        order.shipDistrict = address.district();
        order.shipProvince = address.province();
        order.paymentMethod = method;
        order.channel = channel;
        order.status = initialStatus;
        order.voucherCode = appliedVoucher;
        order.subtotal = subtotal;
        order.discount = discount;
        order.shippingFee = includeShipping && subtotal.compareTo(freeShippingThreshold) < 0
            ? shippingFee : java.math.BigDecimal.ZERO;
        order.total = subtotal.subtract(discount).add(order.shippingFee);
        order.note = note;
        order.createdBy = createdBy;
        order.createdAt = LocalDateTime.now();
        order.updatedAt = order.createdAt;
        orders.save(order);
        for (var entry : requested.entrySet()) {
            ProductVariant variant = locked.get(entry.getKey());
            int quantity = entry.getValue();
            OrderLine line = new OrderLine();
            line.order = order;
            line.variant = variant;
            line.productName = variant.getProduct().getName();
            line.sku = variant.sku;
            line.color = variant.color;
            line.size = variant.size;
            line.imageUrl = catalog.primaryImage(variant.getProduct().getId());
            line.quantity = quantity;
            line.unitPrice = variant.price;
            line.lineTotal = variant.price.multiply(java.math.BigDecimal.valueOf(quantity));
            lines.save(line);
            variant.stock -= quantity;
            variants.save(variant);
            InventoryMovement movement = new InventoryMovement();
            movement.variant = variant;
            movement.movementType = channel.equals("POS") ? "POS_SALE" : "ONLINE_SALE";
            movement.quantityDelta = -quantity;
            movement.reference = order.code;
            movement.createdBy = createdBy;
            inventory.save(movement);
        }
        recordStatus(order, null, initialStatus, channel.equals("POS") ? "Thanh toán tại quầy" : "Khách đặt hàng", createdBy);
        return order;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> mine(Account account, int page, int size) {
        Pageable pageable = pageRequest(page, size);
        Page<CustomerOrder> result = orders.findByAccountIdOrderByCreatedAtDesc(account.id, pageable);
        return new PageResponse<>(result.getContent().stream().map(this::orderResponse).toList(), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse mineDetail(Long id, Account account) {
        CustomerOrder order = orders.findByIdAndAccountId(id, account.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        return detail(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> all(String status, String query, int page, int size) {
        Pageable pageable = pageRequest(page, size);
        Page<CustomerOrder> result = orders.search(blankToNull(status), blankToNull(query), pageable);
        return new PageResponse<>(result.getContent().stream().map(this::orderResponse).toList(), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse detailById(Long id) {
        return detail(orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng")));
    }

    @Transactional
    public OrderDetailResponse updateStatus(Long id, String next, String note, String actor) {
        CustomerOrder order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        String old = order.status;
        String status = next.trim().toUpperCase(Locale.ROOT);
        Map<String, List<String>> transitions = Map.of(
            "PENDING", List.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", List.of("PREPARING", "CANCELLED"),
            "PREPARING", List.of("READY_TO_SHIP", "CANCELLED"),
            "READY_TO_SHIP", List.of("SHIPPING", "CANCELLED"),
            "SHIPPING", List.of("COMPLETED"),
            "COMPLETED", List.of(),
            "CANCELLED", List.of());
        if (!transitions.containsKey(status) || !transitions.getOrDefault(old, List.of()).contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể chuyển trạng thái đơn hàng sang " + status);
        }
        if (status.equals("CANCELLED")) restoreStock(order, actor);
        order.status = status;
        order.updatedAt = LocalDateTime.now();
        orders.save(order);
        recordStatus(order, old, status, note, actor);
        return detail(order);
    }

    @Transactional
    public ReturnResponse requestReturn(Long orderId, Account customer, String reason) {
        CustomerOrder order = orders.findByIdAndAccountId(orderId, customer.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!"COMPLETED".equals(order.status)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể yêu cầu đổi trả cho đơn đã hoàn thành");
        if (returns.existsByOrder_IdAndAccount_Id(orderId, customer.id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã có yêu cầu đổi trả");
        OrderReturn request = new OrderReturn();
        request.order = order;
        request.account = customer;
        request.reason = reason;
        return returnResponse(returns.save(request));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnResponse> allReturns(int page, int size) {
        Page<OrderReturn> result = returns.findAllByOrderByCreatedAtDesc(pageRequest(page, size));
        return new PageResponse<>(result.getContent().stream().map(this::returnResponse).toList(), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public ReturnResponse updateReturnStatus(Long id, String status) {
        OrderReturn request = returns.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu đổi trả"));
        String next = status.toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED", "RECEIVED").contains(next)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Trạng thái đổi trả không hợp lệ");
        }
        boolean allowedTransition = switch (request.status) {
            case "PENDING" -> List.of("APPROVED", "REJECTED").contains(next);
            case "APPROVED" -> "RECEIVED".equals(next);
            default -> false;
        };
        if (!allowedTransition) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể chuyển trạng thái đổi trả từ " + request.status + " sang " + next);
        }
        request.status = next;
        request.updatedAt = LocalDateTime.now();
        return returnResponse(returns.save(request));
    }

    public PosCartResponse createPosCart(HttpSession session, PosCartCreateRequest request) {
        PosState state = posState(session);
        if (state.carts.size() >= 5) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mỗi nhân viên được mở tối đa 5 hóa đơn nháp");
        String cartId = UUID.randomUUID().toString();
        PosDraft draft = new PosDraft();
        draft.customerName = request.customerName();
        draft.customerPhone = request.customerPhone();
        state.carts.put(cartId, draft);
        return posCartResponse(cartId, draft);
    }

    public List<PosCartResponse> posCarts(HttpSession session) {
        return posState(session).carts.entrySet().stream().map(entry -> posCartResponse(entry.getKey(), entry.getValue())).toList();
    }

    public PosCartResponse addPosItem(HttpSession session, String cartId, PosCartItemRequest request) {
        PosDraft draft = posDraft(session, cartId);
        ProductVariant variant = carts.variant(request.variantId());
        int quantity = draft.quantities.getOrDefault(variant.id, 0) + request.quantity();
        if (quantity > 20 || quantity > variant.stock) throw new ResponseStatusException(HttpStatus.CONFLICT, "Số lượng vượt quá tồn kho hoặc giới hạn mỗi dòng");
        draft.quantities.put(variant.id, quantity);
        return posCartResponse(cartId, draft);
    }

    public PosCartResponse setPosItem(HttpSession session, String cartId, Long variantId, int quantity) {
        PosDraft draft = posDraft(session, cartId);
        ProductVariant variant = carts.variant(variantId);
        if (!draft.quantities.containsKey(variantId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm chưa có trong hóa đơn");
        if (quantity > variant.stock) throw new ResponseStatusException(HttpStatus.CONFLICT, "Số lượng vượt quá tồn kho");
        draft.quantities.put(variantId, quantity);
        return posCartResponse(cartId, draft);
    }

    public PosCartResponse removePosItem(HttpSession session, String cartId, Long variantId) {
        PosDraft draft = posDraft(session, cartId);
        draft.quantities.remove(variantId);
        return posCartResponse(cartId, draft);
    }

    public void deletePosCart(HttpSession session, String cartId) {
        posState(session).carts.remove(cartId);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard(String range, LocalDate from, LocalDate to) {
        LocalDate finish = to == null ? LocalDate.now() : to;
        LocalDate start = from;
        if (start == null) {
            start = switch (range != null ? range.toLowerCase(Locale.ROOT) : "month") {
                case "day" -> finish;
                case "week" -> finish.minusDays(6);
                case "year" -> finish.minusYears(1).plusDays(1);
                default -> finish.minusDays(29);
            };
        }
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = finish.plusDays(1).atStartOfDay();
        java.math.BigDecimal revenue = orders.revenueBetween(startAt, endAt);
        if (revenue == null) revenue = java.math.BigDecimal.ZERO;
        long orderCount = orders.countByCreatedAtBetween(startAt, endAt);
        Long units = lines.unitsSoldBetween(startAt, endAt);
        long unitsSold = units != null ? units : 0L;
        long customersCount = orders.distinctCustomersBetween(startAt, endAt);

        List<ChartDataPoint> chartPoints = new ArrayList<>();
        List<CustomerOrder> orderRows = orders.findByCreatedAtBetweenAndStatusNot(startAt, endAt, "CANCELLED");

        if ("day".equalsIgnoreCase(range)) {
            Map<String, java.math.BigDecimal> hourMap = new LinkedHashMap<>();
            Map<String, Long> countMap = new LinkedHashMap<>();
            for (int h = 8; h <= 22; h += 2) {
                String label = String.format("%02d:00", h);
                hourMap.put(label, java.math.BigDecimal.ZERO);
                countMap.put(label, 0L);
            }
            for (CustomerOrder o : orderRows) {
                int hour = o.createdAt.getHour();
                int bucket = Math.max(8, Math.min(22, (hour / 2) * 2));
                String label = String.format("%02d:00", bucket);
                if (hourMap.containsKey(label)) {
                    hourMap.put(label, hourMap.get(label).add(o.total != null ? o.total : java.math.BigDecimal.ZERO));
                    countMap.put(label, countMap.get(label) + 1);
                }
            }
            for (String k : hourMap.keySet()) {
                chartPoints.add(new ChartDataPoint(k, hourMap.get(k), countMap.get(k)));
            }
        } else if ("year".equalsIgnoreCase(range)) {
            Map<String, java.math.BigDecimal> monthMap = new LinkedHashMap<>();
            Map<String, Long> countMap = new LinkedHashMap<>();
            for (int i = 11; i >= 0; i--) {
                java.time.YearMonth ym = java.time.YearMonth.from(finish.minusMonths(i));
                String label = "Tháng " + ym.getMonthValue();
                monthMap.put(label, java.math.BigDecimal.ZERO);
                countMap.put(label, 0L);
            }
            for (CustomerOrder o : orderRows) {
                String label = "Tháng " + o.createdAt.getMonthValue();
                if (monthMap.containsKey(label)) {
                    monthMap.put(label, monthMap.get(label).add(o.total != null ? o.total : java.math.BigDecimal.ZERO));
                    countMap.put(label, countMap.get(label) + 1);
                }
            }
            for (String k : monthMap.keySet()) {
                chartPoints.add(new ChartDataPoint(k, monthMap.get(k), countMap.get(k)));
            }
        } else {
            Map<String, java.math.BigDecimal> dayMap = new LinkedHashMap<>();
            Map<String, Long> countMap = new LinkedHashMap<>();
            LocalDate cur = start;
            while (!cur.isAfter(finish)) {
                String label = cur.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                dayMap.put(label, java.math.BigDecimal.ZERO);
                countMap.put(label, 0L);
                cur = cur.plusDays(1);
            }
            for (CustomerOrder o : orderRows) {
                String label = o.createdAt.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
                if (dayMap.containsKey(label)) {
                    dayMap.put(label, dayMap.get(label).add(o.total != null ? o.total : java.math.BigDecimal.ZERO));
                    countMap.put(label, countMap.get(label) + 1);
                }
            }
            for (String k : dayMap.keySet()) {
                chartPoints.add(new ChartDataPoint(k, dayMap.get(k), countMap.get(k)));
            }
        }

        List<TopProductResponse> top = topProducts(start, finish, 0, 5).data();
        List<CustomerResponse> topCust = accounts.findByRole("CUSTOMER", PageRequest.of(0, 5))
            .getContent().stream().map(a -> new CustomerResponse(a.id, a.email, a.fullName, a.phone, a.createdAt)).toList();

        return new DashboardStatsResponse(revenue, orderCount, unitsSold, customersCount, chartPoints, top, topCust);
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse summary(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now().minusDays(29) : from;
        LocalDate finish = to == null ? LocalDate.now() : to;
        if (finish.isBefore(start)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày kết thúc phải sau ngày bắt đầu");
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = finish.plusDays(1).atStartOfDay();
        return new ReportSummaryResponse(startAt, endAt.minusNanos(1), orders.revenueBetween(startAt, endAt),
            orders.countByCreatedAtBetween(startAt, endAt.minusNanos(1)), lines.unitsSoldBetween(startAt, endAt),
            orders.distinctCustomersBetween(startAt, endAt));
    }

    @Transactional(readOnly = true)
    public PageResponse<TopProductResponse> topProducts(LocalDate from, LocalDate to, int page, int size) {
        LocalDate start = from == null ? LocalDate.now().minusDays(29) : from;
        LocalDate finish = to == null ? LocalDate.now() : to;
        if (finish.isBefore(start)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày kết thúc phải sau ngày bắt đầu");
        List<Object[]> rows = lines.topProducts(start.atStartOfDay(), finish.plusDays(1).atStartOfDay());
        List<TopProductResponse> all = rows.stream().map(row -> new TopProductResponse((String) row[0],
            ((Number) row[1]).longValue(), (java.math.BigDecimal) row[2])).toList();
        int boundedPage = Math.max(0, page), boundedSize = Math.max(1, Math.min(size, 100));
        int startIndex = Math.min(boundedPage * boundedSize, all.size());
        int endIndex = Math.min(startIndex + boundedSize, all.size());
        return new PageResponse<>(all.subList(startIndex, endIndex), boundedPage, boundedSize, all.size(),
            (int) Math.ceil((double) all.size() / boundedSize));
    }

    public String summaryCsv(LocalDate from, LocalDate to) {
        ReportSummaryResponse report = summary(from, to);
        return "from,to,revenue_vnd,orders,units_sold,customers\r\n" + quote(report.from().toLocalDate().toString()) + ","
            + quote(report.to().toLocalDate().toString()) + "," + report.revenue().toPlainString() + ","
            + report.orderCount() + "," + report.unitsSold() + "," + report.customerCount() + "\r\n";
    }

    @Transactional(readOnly = true)
    public byte[] invoicePdf(Long id) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        List<String> text = new ArrayList<>();
        text.add("NEWMEN STORE - SALES INVOICE");
        text.add("Invoice: " + order.code + "    Date: " + order.createdAt);
        text.add("Customer: " + order.customerName + "    Phone: " + order.customerPhone);
        text.add("Address: " + order.shipLine1 + ", " + order.shipDistrict + ", " + order.shipProvince);
        text.add("Payment: " + order.paymentMethod + "    Channel: " + order.channel + "    Status: " + order.status);
        text.add("------------------------------------------------------------");
        for (OrderLine line : lines.findByOrderIdOrderByIdAsc(order.id)) {
            text.add(line.productName + " / " + line.color + " / " + line.size + " x" + line.quantity
                + "  " + line.lineTotal.toPlainString() + " VND");
        }
        text.add("------------------------------------------------------------");
        text.add("Subtotal: " + order.subtotal.toPlainString() + " VND");
        text.add("Discount: " + order.discount.toPlainString() + " VND");
        text.add("Shipping: " + order.shippingFee.toPlainString() + " VND");
        text.add("TOTAL: " + order.total.toPlainString() + " VND");
        text.add("Thank you for shopping at NewMen Store.");
        return minimalPdf(text);
    }

    public OrderResponse orderResponse(CustomerOrder order) {
        return new OrderResponse(order.id, order.code, order.status, order.channel, order.total, order.paymentMethod, order.createdAt);
    }

    private OrderDetailResponse detail(CustomerOrder order) {
        AddressInput address = new AddressInput(order.shipRecipientName, order.shipPhone, order.shipLine1,
            order.shipWard, order.shipDistrict, order.shipProvince);
        return new OrderDetailResponse(order.id, order.code, order.status, order.channel, order.customerName,
            order.customerEmail, order.customerPhone, address, order.paymentMethod, order.voucherCode,
            order.subtotal, order.discount, order.shippingFee, order.total, order.note, order.createdAt,
            lines.findByOrderIdOrderByIdAsc(order.id).stream().map(line -> new OrderLineResponse(
                line.variant == null ? null : line.variant.getProduct().getId(), line.variant == null ? null : line.variant.getId(),
                line.productName, line.sku, line.color, line.size,
                line.imageUrl, line.quantity, line.unitPrice, line.lineTotal)).toList(),
            history.findByOrderIdOrderByCreatedAtAsc(order.id).stream().map(item -> new OrderHistoryResponse(
                item.previousStatus, item.nextStatus, item.note, item.changedBy, item.createdAt)).toList());
    }

    private void restoreStock(CustomerOrder order, String actor) {
        for (Object[] line : lines.findVariantQuantitiesByOrderId(order.id)) {
            Long variantId = (Long) line[0];
            int quantity = ((Number) line[1]).intValue();
            ProductVariant variant = variants.findByIdForUpdate(variantId).orElse(null);
            if (variant == null) continue;
            variant.stock += quantity;
            variants.save(variant);
            InventoryMovement movement = new InventoryMovement();
            movement.variant = variant;
            movement.movementType = "ORDER_CANCELLED";
            movement.quantityDelta = quantity;
            movement.reference = order.code;
            movement.createdBy = actor;
            inventory.save(movement);
        }
    }

    private void recordStatus(CustomerOrder order, String previous, String next, String note, String actor) {
        OrderStatusHistory item = new OrderStatusHistory();
        item.order = order;
        item.previousStatus = previous;
        item.nextStatus = next;
        item.note = note;
        item.changedBy = actor;
        item.createdAt = LocalDateTime.now();
        history.save(item);
    }

    private ReturnResponse returnResponse(OrderReturn request) {
        return new ReturnResponse(request.id, request.order.id, request.order.code, request.reason,
            request.status, request.createdAt, request.updatedAt);
    }

    private PosCartResponse posCartResponse(String cartId, PosDraft draft) {
        CartService.State state = new CartService.State();
        state.quantities = draft.quantities;
        CartResponse view = carts.viewState(state);
        return new PosCartResponse(cartId, view.items(), view.subtotal(), draft.customerName, draft.customerPhone);
    }

    private PosDraft posDraft(HttpSession session, String cartId) {
        PosDraft draft = posState(session).carts.get(cartId);
        if (draft == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giỏ bán tại quầy");
        return draft;
    }

    private PosState posState(HttpSession session) {
        Object existing = session.getAttribute("newmen.pos.carts");
        if (existing instanceof PosState state) return state;
        PosState state = new PosState();
        session.setAttribute("newmen.pos.carts", state);
        return state;
    }

    private Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tham số phân trang không hợp lệ");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, label + " là bắt buộc");
        return value.trim();
    }

    private String safe(String preferred, String fallback) {
        String value = preferred == null || preferred.isBlank() ? fallback : preferred;
        return required(value, "Số điện thoại");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash checkout request", exception);
        }
    }

    private String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private byte[] minimalPdf(List<String> rows) {
        StringBuilder stream = new StringBuilder("BT /F1 11 Tf 48 790 Td 15 TL\n");
        for (String row : rows) stream.append('(').append(pdfText(row)).append(") Tj T*\n");
        stream.append("ET\n");
        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objects = List.of(
            ascii("<< /Type /Catalog /Pages 2 0 R >>"),
            ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
            ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"),
            ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
            concat(ascii("<< /Length " + streamBytes.length + " >>\nstream\n"), streamBytes, ascii("endstream"))
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, ascii("%PDF-1.4\n% NewMen invoice\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, ascii((i + 1) + " 0 obj\n"));
            write(out, objects.get(i));
            write(out, ascii("\nendobj\n"));
        }
        int xrefOffset = out.size();
        write(out, ascii("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (Integer offset : offsets) write(out, ascii(String.format(Locale.ROOT, "%010d 00000 n \n", offset)));
        write(out, ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF"));
        return out.toByteArray();
    }

    private String pdfText(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D');
        String ascii = normalized.replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private byte[] ascii(String value) { return value.getBytes(StandardCharsets.US_ASCII); }
    private byte[] concat(byte[] first, byte[] second, byte[] third) {
        byte[] out = new byte[first.length + second.length + third.length];
        System.arraycopy(first, 0, out, 0, first.length);
        System.arraycopy(second, 0, out, first.length, second.length);
        System.arraycopy(third, 0, out, first.length + second.length, third.length);
        return out;
    }
    private void write(ByteArrayOutputStream out, byte[] bytes) {
        out.write(bytes, 0, bytes.length);
    }

    public static class PosState { public Map<String, PosDraft> carts = new LinkedHashMap<>(); }
    public static class PosDraft {
        public Map<Long, Integer> quantities = new LinkedHashMap<>();
        public String customerName;
        public String customerPhone;
    }
}
