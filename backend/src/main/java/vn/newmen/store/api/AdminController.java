package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Account;
import vn.newmen.store.domain.Category;
import vn.newmen.store.domain.Customer;
import vn.newmen.store.domain.CustomerOrder;
import vn.newmen.store.domain.InventoryMovement;
import vn.newmen.store.domain.Product;
import vn.newmen.store.domain.ProductImage;
import vn.newmen.store.domain.ProductVariant;
import vn.newmen.store.domain.Promotion;
import vn.newmen.store.repository.AccountRepository;
import vn.newmen.store.repository.CategoryRepository;
import vn.newmen.store.repository.CustomerOrderRepository;
import vn.newmen.store.repository.CustomerRepository;
import vn.newmen.store.repository.InventoryMovementRepository;
import vn.newmen.store.repository.ProductImageRepository;
import vn.newmen.store.repository.ProductRepository;
import vn.newmen.store.repository.ProductVariantRepository;
import vn.newmen.store.repository.PromotionRepository;
import vn.newmen.store.service.CatalogService;
import vn.newmen.store.service.CurrentAccount;
import vn.newmen.store.service.LocalImageStorage;
import vn.newmen.store.service.OrderService;

@RestController
@RequestMapping("/api/v1/admin")
@Transactional(readOnly = true)
public class AdminController {
    private final CurrentAccount current;
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;
    private final CategoryRepository categories;
    private final PromotionRepository promotions;
    private final CustomerOrderRepository orderRepository;
    private final InventoryMovementRepository inventory;
    private final CatalogService catalog;
    private final LocalImageStorage imageStorage;
    private final OrderService orders;
    private final PasswordEncoder passwords;
    private final vn.newmen.store.service.SeedData seedData;

    public AdminController(CurrentAccount current, AccountRepository accounts, CustomerRepository customers,
                           ProductRepository products, ProductVariantRepository variants,
                           ProductImageRepository images, CategoryRepository categories,
                           PromotionRepository promotions, CustomerOrderRepository orderRepository,
                           InventoryMovementRepository inventory, CatalogService catalog,
                           LocalImageStorage imageStorage, OrderService orders, PasswordEncoder passwords,
                           vn.newmen.store.service.SeedData seedData) {
        this.current = current;
        this.accounts = accounts;
        this.customers = customers;
        this.products = products;
        this.variants = variants;
        this.images = images;
        this.categories = categories;
        this.promotions = promotions;
        this.orderRepository = orderRepository;
        this.inventory = inventory;
        this.catalog = catalog;
        this.imageStorage = imageStorage;
        this.orders = orders;
        this.passwords = passwords;
        this.seedData = seedData;
    }

    @PostMapping("/reseed")
    public Map<String, Object> reseed(Authentication auth) {
        current.requireRole(auth, "ADMIN");
        seedData.seedAll();
        return Map.of("success", true, "message", "Đã nạp đầy đủ toàn bộ 32 bảng dữ liệu và luồng quy trình NewMen Store.");
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(@RequestParam(required = false) String query,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return catalog.search(query, categoryId, null, null, "newest", page, Math.min(size, 48));
    }

    @PostMapping("/products")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductWriteRequest request,
                                                          Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Product product = new Product();
        applyProduct(product, request, true);
        products.save(product);
        if (request.variants() != null) request.variants().forEach(variant -> saveVariant(product, variant, authentication.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.toResponse(product));
    }

    @GetMapping("/products/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ProductResponse product(@PathVariable Long id) { return catalog.toResponse(catalog.productEntity(id)); }

    @PatchMapping("/products/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductWriteRequest request,
                                         Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Product product = catalog.productEntity(id);
        applyProduct(product, request, false);
        products.save(product);
        return catalog.toResponse(product);
    }

    @DeleteMapping("/products/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Product product = catalog.productEntity(id);
        product.active = false;
        products.save(product);
        variants.findByProductIdOrderByColorAscSizeAsc(id).forEach(variant -> { variant.active = false; variants.save(variant); });
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<ImageResponse> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file,
                                                     Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Product product = catalog.productEntity(id);
        LocalImageStorage.StoredImage stored = imageStorage.store(file);
        ProductImage image = new ProductImage();
        image.product = product;
        image.fileName = stored.fileName();
        image.imageUrl = stored.url();
        image.sortOrder = images.findByProductIdOrderBySortOrderAscIdAsc(id).size();
        image.isPrimary = image.sortOrder == 0;
        images.save(image);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ImageResponse(image.id, image.imageUrl, image.fileName, image.isPrimary));
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @Transactional
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId,
                                            Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        ProductImage image = images.findByIdAndProductId(imageId, id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh sản phẩm"));
        String fileName = image.fileName;
        boolean wasPrimary = image.isPrimary;
        images.delete(image);
        imageStorage.delete(fileName);
        if (wasPrimary) images.findByProductIdOrderBySortOrderAscIdAsc(id).stream().findFirst().ifPresent(next -> {
            next.isPrimary = true;
            images.save(next);
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/{id}/variants")
    @Transactional
    public ResponseEntity<VariantResponse> createVariant(@PathVariable Long id, @Valid @RequestBody VariantWriteRequest request,
                                                          Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Product product = catalog.productEntity(id);
        ProductVariant variant = saveVariant(product, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toVariant(variant));
    }

    @PatchMapping("/variants/{id}")
    @Transactional
    public VariantResponse updateVariant(@PathVariable Long id, @Valid @RequestBody VariantUpdateRequest request,
                                         Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        ProductVariant variant = variants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));
        int oldStock = variant.stock;
        if (request.sku() != null) variant.sku = request.sku().trim();
        if (request.barcode() != null) variant.barcode = request.barcode().trim();
        if (request.color() != null) variant.color = request.color().trim();
        if (request.size() != null) variant.size = request.size().trim();
        if (request.price() != null) variant.price = request.price();
        if (request.stock() != null) variant.stock = request.stock();
        if (request.active() != null) variant.active = request.active();
        variants.save(variant);
        if (request.stock() != null && oldStock != variant.stock) {
            InventoryMovement movement = new InventoryMovement();
            movement.variant = variant;
            movement.movementType = "ADMIN_ADJUSTMENT";
            movement.quantityDelta = variant.stock - oldStock;
            movement.reference = "manual-adjustment";
            movement.createdBy = authentication.getName();
            inventory.save(movement);
        }
        return toVariant(variant);
    }

    @DeleteMapping("/variants/{id}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id, Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        ProductVariant variant = variants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));
        variant.active = false;
        variants.save(variant);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public Map<String, List<CategoryResponse>> categories() { return Map.of("data", catalog.adminCategories()); }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryWriteRequest request,
                                                            Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Category category = categoryFromRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.categoryResponse(categories.save(category)));
    }

    @PatchMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryWriteRequest request,
                                           Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Category category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
        category.name = request.name().trim();
        category.slug = request.slug().trim().toLowerCase(Locale.ROOT);
        category.parent = request.parentId() == null ? null : categories.findById(request.parentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh mục cha không tồn tại"));
        if (category.parent != null && category.parent.getId().equals(category.id)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh mục không thể là cha của chính nó");
        if (request.active() != null) category.active = request.active();
        return catalog.categoryResponse(categories.save(category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Category category = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));
        category.active = false;
        categories.save(category);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public PageResponse<OrderResponse> orders(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String query,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return orders.all(status, query, page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderDetailResponse order(@PathVariable Long id) { return orders.detailById(id); }

    @PatchMapping("/orders/{id}/status")
    public OrderDetailResponse updateOrderStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                                 Authentication authentication) {
        return orders.updateStatus(id, request.status(), request.note(), authentication.getName());
    }

    @GetMapping("/orders/{id}/invoice")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) {
        byte[] pdf = orders.invoicePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("newmen-invoice-" + id + ".pdf").build());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/pos/carts")
    public Map<String, List<PosCartResponse>> posCarts(HttpSession session) { return Map.of("data", orders.posCarts(session)); }

    @PostMapping("/pos/carts")
    public ResponseEntity<PosCartResponse> createPosCart(@Valid @RequestBody(required = false) PosCartCreateRequest request,
                                                         HttpSession session) {
        PosCartCreateRequest value = request == null ? new PosCartCreateRequest(null, null) : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(orders.createPosCart(session, value));
    }

    @DeleteMapping("/pos/carts/{cartId}")
    public ResponseEntity<Void> deletePosCart(@PathVariable String cartId, HttpSession session) {
        orders.deletePosCart(session, cartId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pos/carts/{cartId}/items")
    public PosCartResponse addPosItem(@PathVariable String cartId, @Valid @RequestBody PosCartItemRequest request,
                                      HttpSession session) {
        return orders.addPosItem(session, cartId, request);
    }

    @PatchMapping("/pos/carts/{cartId}/items/{variantId}")
    public PosCartResponse updatePosItem(@PathVariable String cartId, @PathVariable Long variantId,
                                         @Valid @RequestBody CartQuantityRequest request, HttpSession session) {
        return orders.setPosItem(session, cartId, variantId, request.quantity());
    }

    @DeleteMapping("/pos/carts/{cartId}/items/{variantId}")
    public PosCartResponse deletePosItem(@PathVariable String cartId, @PathVariable Long variantId, HttpSession session) {
        return orders.removePosItem(session, cartId, variantId);
    }

    @PostMapping("/pos/orders")
    public ResponseEntity<OrderResponse> placePosOrder(@Valid @RequestBody PosCheckoutRequest request,
                                                       Authentication authentication, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orders.createPosOrder(request, current.require(authentication), session));
    }

    @GetMapping("/pos/barcodes/{barcode}")
    public ProductResponse barcode(@PathVariable String barcode) {
        ProductVariant variant = variants.findByBarcodeIgnoreCase(barcode)
            .filter(value -> value.active && value.getProduct().getActive())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mã vạch"));
        return catalog.toResponse(catalog.productEntity(variant.getProduct().getId()));
    }

    @GetMapping("/customers")
    public PageResponse<CustomerResponse> customers(@RequestParam(required = false) String query,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(required = false) Integer pageSize,
                                                     @RequestParam(required = false) Integer size) {
        int requestedPageSize = pageSize != null ? pageSize : size == null ? 20 : size;
        Pageable sortedPageable = pageRequest(page, requestedPageSize);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<Account> result = normalizedQuery == null
            ? accounts.findByRole("CUSTOMER", sortedPageable)
            : accounts.searchByRoleAndQuery("CUSTOMER", normalizedQuery,
                PageRequest.of(sortedPageable.getPageNumber(), sortedPageable.getPageSize()));
        List<CustomerResponse> data = result.getContent().stream()
            .map(account -> new CustomerResponse(account.id, account.email, account.fullName, account.phone, account.createdAt)).toList();
        return new PageResponse<>(data, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/promotions")
    public PageResponse<PromotionResponse> promotions(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = pageRequest(page, size);
        Page<Promotion> result = promotions.findAll(pageable);
        return new PageResponse<>(result.getContent().stream().map(catalog::promotionResponse).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/promotions")
    public ResponseEntity<PromotionResponse> createPromotion(@Valid @RequestBody PromotionWriteRequest request,
                                                             Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.promotionResponse(promotions.save(promotionFrom(request, null))));
    }

    @PatchMapping("/promotions/{id}")
    public PromotionResponse updatePromotion(@PathVariable Long id, @Valid @RequestBody PromotionWriteRequest request,
                                             Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Promotion existing = promotions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi"));
        return catalog.promotionResponse(promotions.save(promotionFrom(request, existing)));
    }

    @GetMapping("/staff")
    public PageResponse<AuthResponse> staff(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Pageable pageable = pageRequest(page, size);
        Page<Account> result = accounts.findByRole("STAFF", pageable);
        return new PageResponse<>(result.getContent().stream().map(this::toAuthResponse).toList(), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/staff")
    public ResponseEntity<AuthResponse> createStaff(@Valid @RequestBody StaffWriteRequest request,
                                                    Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        if (request.password() == null || request.password().length() < 10) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mật khẩu nhân viên tối thiểu 10 ký tự");
        if (accounts.existsByEmailIgnoreCase(request.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        Account staff = new Account();
        staff.email = request.email().trim().toLowerCase(Locale.ROOT);
        staff.fullName = request.fullName().trim();
        staff.phone = request.phone();
        staff.passwordHash = passwords.encode(request.password());
        staff.role = normalizeStaffRole(request.role());
        staff.active = request.active() == null || request.active();
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(accounts.save(staff)));
    }

    @PatchMapping("/staff/{id}")
    public AuthResponse updateStaff(@PathVariable Long id, @Valid @RequestBody StaffWriteRequest request,
                                    Authentication authentication) {
        current.requireRole(authentication, "ADMIN");
        Account staff = accounts.findById(id).filter(account -> List.of("STAFF", "ADMIN").contains(account.role))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên"));
        staff.fullName = request.fullName().trim();
        staff.email = request.email().trim().toLowerCase(Locale.ROOT);
        staff.phone = request.phone();
        if (request.password() != null && !request.password().isBlank()) staff.passwordHash = passwords.encode(request.password());
        staff.role = normalizeStaffRole(request.role());
        if (request.active() != null) staff.active = request.active();
        return toAuthResponse(accounts.save(staff));
    }

    @GetMapping("/returns")
    public PageResponse<ReturnResponse> returns(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return orders.allReturns(page, size);
    }

    @PatchMapping("/returns/{id}/status")
    public ReturnResponse returnStatus(@PathVariable Long id, @Valid @RequestBody ReturnStatusRequest request,
                                       Authentication authentication) {
        return orders.updateReturnStatus(id, request.status());
    }

    @PostMapping("/products/generate-variants")
    public List<GeneratedVariantItem> generateVariants(@RequestBody GenerateVariantsRequest req) {
        List<String> colors = req.colors() != null && !req.colors().isEmpty() ? req.colors() : List.of("Trắng", "Đen");
        List<String> sizes = req.sizes() != null && !req.sizes().isEmpty() ? req.sizes() : List.of("M", "L", "XL");
        BigDecimal price = req.basePrice() != null ? req.basePrice() : new BigDecimal("350000");
        BigDecimal cost = req.costPrice() != null ? req.costPrice() : price.multiply(new BigDecimal("0.6"));
        int stock = req.defaultStock() != null ? req.defaultStock() : 20;
        String prefix = req.productCode() != null && !req.productCode().isBlank() ? req.productCode().trim().toUpperCase(Locale.ROOT) : "NM";

        List<GeneratedVariantItem> list = new ArrayList<>();
        int idx = 1;
        for (String c : colors) {
            for (String s : sizes) {
                String colorShort = c.substring(0, Math.min(2, c.length())).toUpperCase(Locale.ROOT);
                String sku = prefix + "-" + colorShort + "-" + s;
                String barcode = "893" + Math.abs((prefix + c + s).hashCode() % 1000000000L);
                list.add(new GeneratedVariantItem(sku, barcode, c, s, price, cost, stock));
                idx++;
            }
        }
        return list;
    }

    @GetMapping("/products/export")
    public ResponseEntity<byte[]> exportProductsCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFMã SP,Tên sản phẩm,Danh mục,Giá gốc,Khối lượng,Trạng thái,Số biến thể\n");
        for (Product p : products.findAll()) {
            int vCount = variants.findByProductIdOrderByColorAscSizeAsc(p.id).size();
            csv.append(p.code).append(",")
               .append("\"").append(p.name.replace("\"", "\"\"")).append("\",")
               .append(p.category != null ? p.category.getName() : "").append(",")
               .append(p.originalPrice != null ? p.originalPrice : "").append(",")
               .append(p.weight != null ? p.weight : "").append(",")
               .append(p.active && p.status ? "Đang kinh doanh" : "Ngừng kinh doanh").append(",")
               .append(vCount).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh-sach-san-pham.csv")
            .body(bytes);
    }

    @PostMapping("/customers")
    @Transactional
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody Map<String, String> req, Authentication auth) {
        String email = req.get("email");
        if (email == null || email.isBlank()) email = "pos-" + System.currentTimeMillis() + "@newmen.local";
        email = email.trim().toLowerCase(Locale.ROOT);

        if (accounts.existsByEmailIgnoreCase(email)) {
            Account acc = accounts.findByEmailIgnoreCase(email).get();
            return ResponseEntity.ok(new CustomerResponse(acc.id, acc.email, acc.fullName, acc.phone, acc.createdAt));
        }
        Account acc = new Account();
        acc.email = email;
        acc.fullName = req.get("fullName") != null && !req.get("fullName").isBlank() ? req.get("fullName").trim() : "Khách tại quầy";
        acc.phone = req.get("phone");
        acc.passwordHash = passwords.encode("NewMen123!");
        acc.role = "CUSTOMER";
        acc.active = true;
        accounts.save(acc);

        Customer c = new Customer();
        c.email = email;
        c.name = acc.fullName;
        c.phoneNumber = acc.phone;
        c.password = acc.passwordHash;
        c.account = acc;
        c.status = true;
        customers.save(c);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CustomerResponse(acc.id, acc.email, acc.fullName, acc.phone, acc.createdAt));
    }

    @GetMapping("/reports/dashboard")
    public DashboardStatsResponse dashboardStats(@RequestParam(required = false, defaultValue = "month") String range,
                                                 @RequestParam(required = false) LocalDate from,
                                                 @RequestParam(required = false) LocalDate to) {
        return orders.dashboard(range, from, to);
    }

    @GetMapping("/reports/summary")
    public ReportSummaryResponse summary(@RequestParam(required = false) LocalDate from,
                                        @RequestParam(required = false) LocalDate to) {
        return orders.summary(from, to);
    }

    @GetMapping("/reports/top-products")
    public PageResponse<TopProductResponse> topProducts(@RequestParam(required = false) LocalDate from,
                                                        @RequestParam(required = false) LocalDate to,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return orders.topProducts(from, to, page, size);
    }

    @GetMapping("/reports/summary/export")
    public ResponseEntity<byte[]> summaryCsv(@RequestParam(required = false) LocalDate from,
                                             @RequestParam(required = false) LocalDate to) {
        byte[] content = ("\uFEFF" + orders.summaryCsv(from, to)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=doanh-thu-newmen.csv")
            .body(content);
    }

    private void applyProduct(Product product, ProductWriteRequest request, boolean creating) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (creating && products.existsByCode(code)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã sản phẩm đã tồn tại");
        product.code = code;
        product.name = request.name().trim();
        product.slug = uniqueSlug(slugify(request.name()), code, product.id);
        product.description = request.description();
        product.material = request.material();
        product.collar = request.collar();
        product.design = request.design();
        product.fit = request.fit();
        product.sleeve = request.sleeve();
        product.originalPrice = request.originalPrice();
        if (request.weight() != null) product.weight = request.weight();
        product.category = request.categoryId() == null ? null : categories.findById(request.categoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh mục không tồn tại"));
        if (request.active() != null) product.active = request.active();
    }

    private ProductVariant saveVariant(Product product, VariantWriteRequest request, String actor) {
        ProductVariant variant = new ProductVariant();
        variant.product = product;
        variant.sku = request.sku().trim();
        variant.barcode = request.barcode() == null ? null : request.barcode().trim();
        variant.color = request.color().trim();
        variant.size = request.size().trim().toUpperCase(Locale.ROOT);
        variant.price = request.price();
        if (request.costPrice() != null) variant.costPrice = request.costPrice();
        variant.stock = request.stock();
        variant.active = true;
        variants.save(variant);
        if (variant.stock > 0) {
            InventoryMovement movement = new InventoryMovement();
            movement.variant = variant;
            movement.movementType = "INITIAL_STOCK";
            movement.quantityDelta = variant.stock;
            movement.reference = "variant-created";
            movement.createdBy = actor;
            inventory.save(movement);
        }
        return variant;
    }

    private Category categoryFromRequest(CategoryWriteRequest request) {
        Category category = new Category();
        category.name = request.name().trim();
        category.slug = request.slug().trim().toLowerCase(Locale.ROOT);
        category.parent = request.parentId() == null ? null : categories.findById(request.parentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh mục cha không tồn tại"));
        category.active = request.active() == null || request.active();
        return category;
    }

    private Promotion promotionFrom(PromotionWriteRequest request, Promotion promotion) {
        Promotion result = promotion == null ? new Promotion() : promotion;
        if (request.endsAt().isBefore(request.startsAt())) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày kết thúc phải sau ngày bắt đầu");
        result.name = request.name().trim();
        result.code = request.code().trim().toUpperCase(Locale.ROOT);
        result.description = request.description();
        result.percentOff = request.percentOff();
        result.minimumOrder = request.minimumOrder();
        result.maximumDiscount = request.maximumDiscount();
        result.startsAt = request.startsAt();
        result.endsAt = request.endsAt();
        if (request.active() != null) result.active = request.active();
        return result;
    }

    private VariantResponse toVariant(ProductVariant variant) {
        return new VariantResponse(variant.id, variant.sku, variant.barcode, variant.color,
            variant.size, variant.price, variant.stock);
    }

    private AuthResponse toAuthResponse(Account account) {
        return new AuthResponse(account.id, account.email, account.fullName, account.phone, account.role);
    }

    private String normalizeStaffRole(String role) {
        String normalized = role.toUpperCase(Locale.ROOT);
        if (!List.of("STAFF", "ADMIN").contains(normalized)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vai trò phải là STAFF hoặc ADMIN");
        return normalized;
    }

    private String slugify(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return normalized.isBlank() ? "san-pham" : normalized;
    }

    private String uniqueSlug(String base, String code, Long currentId) {
        String candidate = base;
        int suffix = 1;
        while (products.existsBySlug(candidate) && (currentId == null || products.findBySlugAndActiveTrue(candidate).map(existing -> !existing.id.equals(currentId)).orElse(true))) {
            candidate = base + "-" + code.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-") + (suffix++ == 1 ? "" : "-" + suffix);
        }
        return candidate;
    }

    private Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tham số phân trang không hợp lệ");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
