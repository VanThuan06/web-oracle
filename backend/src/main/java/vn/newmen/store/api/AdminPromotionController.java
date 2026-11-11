package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Discount;
import vn.newmen.store.domain.Product;
import vn.newmen.store.domain.ProductDiscount;
import vn.newmen.store.domain.Voucher;
import vn.newmen.store.repository.DiscountRepository;
import vn.newmen.store.repository.ProductDiscountRepository;
import vn.newmen.store.repository.ProductRepository;
import vn.newmen.store.repository.VoucherRepository;
import vn.newmen.store.service.CurrentAccount;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPromotionController {
    private final CurrentAccount current;
    private final DiscountRepository discounts;
    private final ProductDiscountRepository productDiscounts;
    private final VoucherRepository vouchers;
    private final ProductRepository products;

    public AdminPromotionController(CurrentAccount current,
                                    DiscountRepository discounts,
                                    ProductDiscountRepository productDiscounts,
                                    VoucherRepository vouchers,
                                    ProductRepository products) {
        this.current = current;
        this.discounts = discounts;
        this.productDiscounts = productDiscounts;
        this.vouchers = vouchers;
        this.products = products;
    }

    // Discounts
    @GetMapping("/discounts")
    public PageResponse<DiscountResponse> listDiscounts(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Discount> result = discounts.findAllByOrderByStartDateDesc(pageable);
        List<DiscountResponse> data = result.getContent().stream().map(d -> {
            int pCount = productDiscounts.findByDiscountId(d.id).size();
            return new DiscountResponse(d.id, d.name, d.description, d.startDate, d.endDate, d.status, pCount);
        }).toList();
        return new PageResponse<>(data, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/discounts")
    @Transactional
    public ResponseEntity<DiscountResponse> createDiscount(@Valid @RequestBody DiscountWriteRequest req, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        Discount d = new Discount(req.name(), req.description(), req.startDate(), req.endDate(), req.status() == null || req.status());
        discounts.save(d);
        if (req.productIds() != null && !req.productIds().isEmpty()) {
            int pct = req.percent() != null ? req.percent() : 10;
            for (Long pid : req.productIds()) {
                products.findById(pid).ifPresent(p -> productDiscounts.save(new ProductDiscount(pct, true, p, d)));
            }
        }
        int pCount = productDiscounts.findByDiscountId(d.id).size();
        return ResponseEntity.status(HttpStatus.CREATED).body(new DiscountResponse(d.id, d.name, d.description, d.startDate, d.endDate, d.status, pCount));
    }

    @PatchMapping("/discounts/{id}")
    @Transactional
    public DiscountResponse updateDiscount(@PathVariable Long id, @RequestBody DiscountWriteRequest req, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        Discount d = discounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt khuyến mãi"));
        if (req.name() != null) d.name = req.name().trim();
        if (req.description() != null) d.description = req.description();
        if (req.startDate() != null) d.startDate = req.startDate();
        if (req.endDate() != null) d.endDate = req.endDate();
        if (req.status() != null) d.status = req.status();
        discounts.save(d);
        int pCount = productDiscounts.findByDiscountId(d.id).size();
        return new DiscountResponse(d.id, d.name, d.description, d.startDate, d.endDate, d.status, pCount);
    }

    // Vouchers
    @GetMapping("/vouchers")
    public PageResponse<VoucherResponse> listVouchers(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<Voucher> result = vouchers.findAllByOrderByStartDateDesc(pageable);
        List<VoucherResponse> data = result.getContent().stream().map(v ->
            new VoucherResponse(v.id, v.code, v.name, v.description, v.discount, v.min, v.max, v.startDate, v.endDate, v.type, v.status)
        ).toList();
        return new PageResponse<>(data, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @PostMapping("/vouchers")
    @Transactional
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherWriteRequest req, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        if (vouchers.findByCodeIgnoreCase(req.code()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã voucher đã tồn tại");
        }
        Voucher v = new Voucher(req.code().trim().toUpperCase(), req.name().trim(), req.description(),
            req.discount(), req.min(), req.max(), req.startDate(), req.endDate(),
            req.type() == null ? 0 : req.type(), req.status() == null || req.status());
        vouchers.save(v);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new VoucherResponse(v.id, v.code, v.name, v.description, v.discount, v.min, v.max, v.startDate, v.endDate, v.type, v.status)
        );
    }

    @PatchMapping("/vouchers/{id}")
    @Transactional
    public VoucherResponse updateVoucher(@PathVariable Long id, @RequestBody VoucherWriteRequest req, Authentication auth) {
        current.requireRole(auth, "ADMIN");
        Voucher v = vouchers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy voucher"));
        if (req.name() != null) v.name = req.name().trim();
        if (req.code() != null) v.code = req.code().trim().toUpperCase();
        if (req.description() != null) v.description = req.description();
        if (req.discount() > 0) v.discount = req.discount();
        if (req.min() != null) v.min = req.min();
        if (req.max() != null) v.max = req.max();
        if (req.startDate() != null) v.startDate = req.startDate();
        if (req.endDate() != null) v.endDate = req.endDate();
        if (req.type() != null) v.type = req.type();
        if (req.status() != null) v.status = req.status();
        vouchers.save(v);
        return new VoucherResponse(v.id, v.code, v.name, v.description, v.discount, v.min, v.max, v.startDate, v.endDate, v.type, v.status);
    }
}
