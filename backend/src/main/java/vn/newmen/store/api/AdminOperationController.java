package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Address;
import vn.newmen.store.domain.Banner;
import vn.newmen.store.domain.Contact;
import vn.newmen.store.domain.Customer;
import vn.newmen.store.domain.Employee;
import vn.newmen.store.domain.Exchange;
import vn.newmen.store.domain.ExchangeDetail;
import vn.newmen.store.domain.FaultProduct;
import vn.newmen.store.domain.InventoryMovement;
import vn.newmen.store.domain.OrderReturn;
import vn.newmen.store.domain.ProductVariant;
import vn.newmen.store.domain.Review;
import vn.newmen.store.domain.Role;
import vn.newmen.store.repository.AddressRepository;
import vn.newmen.store.repository.BannerRepository;
import vn.newmen.store.repository.ContactRepository;
import vn.newmen.store.repository.CustomerRepository;
import vn.newmen.store.repository.EmployeeRepository;
import vn.newmen.store.repository.ExchangeDetailRepository;
import vn.newmen.store.repository.ExchangeRepository;
import vn.newmen.store.repository.FaultProductRepository;
import vn.newmen.store.repository.InventoryMovementRepository;
import vn.newmen.store.repository.OrderReturnRepository;
import vn.newmen.store.repository.ProductVariantRepository;
import vn.newmen.store.repository.RatingImageRepository;
import vn.newmen.store.repository.ReviewRepository;
import vn.newmen.store.repository.RoleRepository;
import vn.newmen.store.service.CurrentAccount;

@RestController
@RequestMapping("/api/v1/admin")
@Transactional(readOnly = true)
public class AdminOperationController {
    private final CurrentAccount current;
    private final FaultProductRepository faultProducts;
    private final ProductVariantRepository variants;
    private final InventoryMovementRepository inventory;
    private final ExchangeRepository exchanges;
    private final ExchangeDetailRepository exchangeDetails;
    private final OrderReturnRepository orderReturns;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final AddressRepository addresses;
    private final BannerRepository banners;
    private final ContactRepository contacts;
    private final ReviewRepository reviews;
    private final RatingImageRepository ratingImages;
    private final RoleRepository roles;

    public AdminOperationController(CurrentAccount current,
                                    FaultProductRepository faultProducts,
                                    ProductVariantRepository variants,
                                    InventoryMovementRepository inventory,
                                    ExchangeRepository exchanges,
                                    ExchangeDetailRepository exchangeDetails,
                                    OrderReturnRepository orderReturns,
                                    CustomerRepository customers,
                                    EmployeeRepository employees,
                                    AddressRepository addresses,
                                    BannerRepository banners,
                                    ContactRepository contacts,
                                    ReviewRepository reviews,
                                    RatingImageRepository ratingImages,
                                    RoleRepository roles) {
        this.current = current;
        this.faultProducts = faultProducts;
        this.variants = variants;
        this.inventory = inventory;
        this.exchanges = exchanges;
        this.exchangeDetails = exchangeDetails;
        this.orderReturns = orderReturns;
        this.customers = customers;
        this.employees = employees;
        this.addresses = addresses;
        this.banners = banners;
        this.contacts = contacts;
        this.reviews = reviews;
        this.ratingImages = ratingImages;
        this.roles = roles;
    }

    // --- FAULT PRODUCTS ---
    @GetMapping("/fault-products")
    public PageResponse<FaultProductResponse> listFaultProducts(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<FaultProduct> result = faultProducts.findAllByOrderByCreatedAtDesc(pageable);
        List<FaultProductResponse> data = result.getContent().stream().map(f -> {
            ProductVariant pv = f.productDetail;
            String pName = pv != null && pv.getProduct() != null ? pv.getProduct().getName() : "Sản phẩm";
            return new FaultProductResponse(f.id, pv != null ? pv.id : null, pName,
                pv != null ? pv.sku : "", pv != null ? pv.color : "", pv != null ? pv.size : "",
                f.quantity, f.description, f.status, f.createdAt);
        }).toList();
        return new PageResponse<>(data, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/fault-products")
    @Transactional
    public ResponseEntity<FaultProductResponse> createFaultProduct(@Valid @RequestBody FaultProductWriteRequest req,
                                                                   Authentication auth) {
        current.requireRole(auth, "ADMIN");
        ProductVariant variant = variants.findById(req.productDetailId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));
        if (variant.stock < req.quantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tồn không đủ để ghi nhận hủy (Tồn: " + variant.stock + ")");
        }
        // Deduct stock
        variant.stock -= req.quantity();
        variants.save(variant);

        // Record movement
        InventoryMovement movement = new InventoryMovement();
        movement.variant = variant;
        movement.movementType = "FAULT_WRITE_OFF";
        movement.quantityDelta = -req.quantity();
        movement.reference = "fault-" + System.currentTimeMillis();
        movement.createdBy = auth.getName();
        inventory.save(movement);

        FaultProduct fp = new FaultProduct();
        fp.productDetail = variant;
        fp.quantity = req.quantity();
        fp.description = req.description();
        fp.status = 1;
        faultProducts.save(fp);

        String pName = variant.getProduct() != null ? variant.getProduct().getName() : "Sản phẩm";
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new FaultProductResponse(fp.id, variant.id, pName, variant.sku, variant.color, variant.size,
                fp.quantity, fp.description, fp.status, fp.createdAt)
        );
    }

    // --- EXCHANGES ---
    @GetMapping("/exchanges")
    public PageResponse<ExchangeResponse> listExchanges(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Exchange> result = exchanges.findAllByOrderByCreatedAtDesc(pageable);
        List<ExchangeResponse> data = result.getContent().stream().map(this::toExchangeResponse).toList();
        return new PageResponse<>(data, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/exchanges")
    @Transactional
    public ResponseEntity<ExchangeResponse> createExchange(@Valid @RequestBody ExchangeCreateRequest req,
                                                           Authentication auth) {
        OrderReturn ret = orderReturns.findById(req.returnId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu trả hàng"));

        Exchange ex = new Exchange();
        ex.orderReturn = ret;
        ex.shipName = req.shipName();
        ex.shipPhone = req.shipPhone();
        ex.shipAddress = req.shipAddress();
        ex.freight = req.freight() != null ? req.freight() : BigDecimal.ZERO;
        ex.note = req.note();
        ex.status = "APPROVED";

        if (req.customerId() != null) {
            customers.findById(req.customerId()).ifPresent(c -> ex.customer = c);
        } else if (ret.customer != null) {
            ex.customer = ret.customer;
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;
        exchanges.save(ex);

        if (req.items() != null) {
            for (ExchangeItemRequest item : req.items()) {
                ProductVariant pv = variants.findById(item.productDetailId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm đổi"));
                BigDecimal up = item.unitPrice() != null ? item.unitPrice() : pv.price;
                itemsTotal = itemsTotal.add(up.multiply(BigDecimal.valueOf(item.quantity())));
                exchangeDetails.save(new ExchangeDetail(ex, pv, item.quantity(), up, item.note()));

                // Deduct stock for new exchanged items
                if (pv.stock >= item.quantity()) {
                    pv.stock -= item.quantity();
                    variants.save(pv);
                }
            }
        }
        ex.total = itemsTotal.add(ex.freight);
        exchanges.save(ex);

        return ResponseEntity.status(HttpStatus.CREATED).body(toExchangeResponse(ex));
    }

    @PatchMapping("/exchanges/{id}/status")
    @Transactional
    public ExchangeResponse updateExchangeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Exchange ex = exchanges.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy"));
        if (body.containsKey("status")) ex.status = body.get("status");
        exchanges.save(ex);
        return toExchangeResponse(ex);
    }

    // --- BANNERS ---
    @GetMapping("/banners")
    public List<BannerResponse> listBanners() {
        return banners.findAllByOrderBySortOrderAsc().stream().map(b ->
            new BannerResponse(b.id, b.title, b.content, b.type, b.url, b.linkUrl, b.status, b.sortOrder)
        ).toList();
    }

    @PostMapping("/banners")
    @Transactional
    public ResponseEntity<BannerResponse> createBanner(@Valid @RequestBody BannerWriteRequest req, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        Banner b = new Banner(req.title(), req.content(), req.type() != null ? req.type() : 1,
            req.url(), req.linkUrl(), req.status() == null || req.status(), req.sortOrder() != null ? req.sortOrder() : 0);
        banners.save(b);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new BannerResponse(b.id, b.title, b.content, b.type, b.url, b.linkUrl, b.status, b.sortOrder)
        );
    }

    @PatchMapping("/banners/{id}")
    @Transactional
    public BannerResponse updateBanner(@PathVariable Long id, @RequestBody BannerWriteRequest req, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        Banner b = banners.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy banner"));
        if (req.title() != null) b.title = req.title();
        if (req.content() != null) b.content = req.content();
        if (req.type() != null) b.type = req.type();
        if (req.url() != null) b.url = req.url();
        if (req.linkUrl() != null) b.linkUrl = req.linkUrl();
        if (req.status() != null) b.status = req.status();
        if (req.sortOrder() != null) b.sortOrder = req.sortOrder();
        banners.save(b);
        return new BannerResponse(b.id, b.title, b.content, b.type, b.url, b.linkUrl, b.status, b.sortOrder);
    }

    @DeleteMapping("/banners/{id}")
    @Transactional
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        banners.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- CONTACTS ---
    @GetMapping("/contacts")
    public PageResponse<ContactResponse> listContacts(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Contact> result = contacts.findAllByOrderByCreatedAtDesc(pageable);
        List<ContactResponse> data = result.getContent().stream().map(c ->
            new ContactResponse(c.id, c.name, c.phone, c.email, c.addressName, c.ward, c.district, c.province, c.content, c.status, c.createdAt)
        ).toList();
        return new PageResponse<>(data, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PatchMapping("/contacts/{id}/status")
    @Transactional
    public ContactResponse updateContactStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Contact c = contacts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy"));
        if (body.containsKey("status")) c.status = body.get("status");
        contacts.save(c);
        return new ContactResponse(c.id, c.name, c.phone, c.email, c.addressName, c.ward, c.district, c.province, c.content, c.status, c.createdAt);
    }

    // --- REVIEWS & RATINGS MODERATION ---
    @GetMapping("/reviews")
    public List<ReviewAdminResponse> listReviews() {
        return reviews.findAll().stream().map(r -> {
            String pName = r.product != null ? r.product.getName() : "Sản phẩm";
            String author = r.customer != null ? r.customer.name : (r.account != null ? r.account.fullName : "Khách hàng");
            List<String> imgs = ratingImages.findByRatingId(r.id).stream().map(ri -> ri.image).toList();
            return new ReviewAdminResponse(r.id, r.product != null ? r.product.id : null, pName, author, r.rating, r.content, r.status, r.createdAt, imgs);
        }).toList();
    }

    @PatchMapping("/reviews/{id}/status")
    @Transactional
    public ReviewAdminResponse toggleReviewStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Review r = reviews.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy"));
        if (body.containsKey("status")) r.status = body.get("status");
        reviews.save(r);
        String pName = r.product != null ? r.product.getName() : "Sản phẩm";
        String author = r.customer != null ? r.customer.name : (r.account != null ? r.account.fullName : "Khách hàng");
        List<String> imgs = ratingImages.findByRatingId(r.id).stream().map(ri -> ri.image).toList();
        return new ReviewAdminResponse(r.id, r.product != null ? r.product.id : null, pName, author, r.rating, r.content, r.status, r.createdAt, imgs);
    }

    // --- ROLES ---
    @GetMapping("/roles")
    public List<Role> listRoles() {
        return roles.findAll();
    }

    @PostMapping("/roles")
    @Transactional
    public ResponseEntity<Role> createRole(@RequestBody Map<String, String> body, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên vai trò không được để trống");
        }
        String upper = name.trim().toUpperCase(java.util.Locale.ROOT);
        if (roles.findByNameIgnoreCase(upper).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã vai trò đã tồn tại");
        }
        Role role = new Role(upper, body.get("description"), true);
        roles.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    // --- CUSTOMERS EXPORT & DETAIL ---
    @GetMapping("/customers/export")
    public ResponseEntity<byte[]> exportCustomersCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFMã KH,Họ và tên,Email,Số điện thoại,Điểm tích lũy,Trạng thái,Ngày tạo\n");
        for (Customer c : customers.findAll()) {
            csv.append(c.id).append(",")
               .append("\"").append(c.name != null ? c.name.replace("\"", "\"\"") : "").append("\",")
               .append(c.email != null ? c.email : "").append(",")
               .append(c.phoneNumber != null ? c.phoneNumber : "").append(",")
               .append(c.points).append(",")
               .append(c.status ? "Hoạt động" : "Khóa").append(",")
               .append(c.createdAt).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh-sach-khach-hang.csv")
            .body(bytes);
    }

    @GetMapping("/customers/{id:[0-9]+}")
    public CustomerDetailResponse customerDetail(@PathVariable Long id) {
        Customer c = customers.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        List<AddressResponse> addrList = addresses.findByCustomerId(id).stream().map(a ->
            new AddressResponse(a.id, a.recipientName, a.phone, a.line1 != null ? a.line1 : a.address,
                a.ward != null ? a.ward : a.wardName,
                a.district != null ? a.district : a.districtName,
                a.province != null ? a.province : a.cityName, a.isDefault)
        ).toList();
        return new CustomerDetailResponse(c.id, c.email, c.name, c.phoneNumber, c.points, c.status, c.createdAt, addrList);
    }

    private ExchangeResponse toExchangeResponse(Exchange ex) {
        List<ExchangeDetailItemResponse> items = exchangeDetails.findByExchangeId(ex.id).stream().map(d -> {
            ProductVariant pv = d.productDetail;
            String pn = pv != null && pv.getProduct() != null ? pv.getProduct().getName() : "Sản phẩm";
            return new ExchangeDetailItemResponse(d.id, pv != null ? pv.id : null, pn,
                pv != null ? pv.sku : "", pv != null ? pv.color : "", pv != null ? pv.size : "",
                d.quantity, d.unitPrice, d.note);
        }).toList();
        String cName = ex.customer != null ? ex.customer.name : ex.shipName;
        return new ExchangeResponse(ex.id, ex.customer != null ? ex.customer.id : null, cName,
            ex.orderReturn != null ? ex.orderReturn.id : null, ex.total, ex.note,
            ex.shipAddress, ex.shipName, ex.shipPhone, ex.statusPay, ex.freight, ex.status, ex.createdAt, items);
    }
}
