package vn.newmen.store.service;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.ProductVariant;
import vn.newmen.store.domain.Promotion;
import vn.newmen.store.repository.ProductVariantRepository;

@Service
public class CartService {
    private static final String SESSION_KEY = CartService.class.getName() + ".cart";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final ProductVariantRepository variants;
    private final CatalogService catalog;

    public CartService(ProductVariantRepository variants, CatalogService catalog) {
        this.variants = variants;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public CartResponse view(HttpSession session) {
        return viewState(state(session));
    }

    @Transactional(readOnly = true)
    public CartResponse viewState(State state) {
        List<CartItemResponse> items = state.quantities.entrySet().stream().map(entry -> {
            ProductVariant variant = variants.findById(entry.getKey()).orElse(null);
            if (variant == null || !variant.active || !variant.getProduct().getActive()) return null;
            return new CartItemResponse(variant.id, variant.getProduct().getName(), variant.getProduct().getSlug(),
                variant.color, variant.size, catalog.primaryImage(variant.getProduct().getId()), variant.price,
                entry.getValue(), variant.stock);
        }).filter(item -> item != null).toList();
        BigDecimal subtotal = items.stream().map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = discount(state.voucherCode, subtotal);
        return new CartResponse(items, subtotal, discount, subtotal.subtract(discount).max(ZERO), state.voucherCode);
    }

    @Transactional
    public CartResponse add(HttpSession session, Long variantId, int quantity) {
        ProductVariant variant = variant(variantId);
        State state = state(session);
        int total = state.quantities.getOrDefault(variantId, 0) + quantity;
        if (total > 20 || total > variant.stock) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số lượng vượt quá tồn kho hoặc giới hạn mỗi dòng");
        }
        state.quantities.put(variantId, total);
        return view(session);
    }

    @Transactional
    public CartResponse setQuantity(HttpSession session, Long variantId, int quantity) {
        ProductVariant variant = variant(variantId);
        if (quantity > variant.stock) throw new ResponseStatusException(HttpStatus.CONFLICT, "Số lượng vượt quá tồn kho");
        State state = state(session);
        if (!state.quantities.containsKey(variantId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không có trong giỏ");
        state.quantities.put(variantId, quantity);
        return view(session);
    }

    @Transactional
    public CartResponse remove(HttpSession session, Long variantId) {
        state(session).quantities.remove(variantId);
        return view(session);
    }

    @Transactional
    public CartResponse clear(HttpSession session) {
        State state = state(session);
        state.quantities.clear();
        state.voucherCode = null;
        return view(session);
    }

    @Transactional
    public CartResponse applyVoucher(HttpSession session, String code) {
        CartResponse current = view(session);
        Promotion promotion = catalog.activePromotion(code.trim());
        if (current.subtotal().compareTo(promotion.minimumOrder) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Đơn hàng chưa đạt giá trị tối thiểu của mã ưu đãi");
        }
        state(session).voucherCode = promotion.code;
        return view(session);
    }

    public State state(HttpSession session) {
        Object current = session.getAttribute(SESSION_KEY);
        if (current instanceof State cart) return cart;
        State cart = new State();
        session.setAttribute(SESSION_KEY, cart);
        return cart;
    }

    @Transactional(readOnly = true)
    public ProductVariant variant(Long variantId) {
        ProductVariant variant = variants.findById(variantId)
            .filter(value -> value.active && value.getProduct().getActive())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể sản phẩm"));
        return variant;
    }

    public BigDecimal discount(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) return ZERO;
        try {
            Promotion promotion = catalog.activePromotion(code);
            if (subtotal.compareTo(promotion.minimumOrder) < 0) return ZERO;
            BigDecimal discount = subtotal.multiply(BigDecimal.valueOf(promotion.percentOff))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promotion.maximumDiscount != null) discount = discount.min(promotion.maximumDiscount);
            return discount.min(subtotal);
        } catch (ResponseStatusException exception) {
            return ZERO;
        }
    }

    public static class State {
        public Map<Long, Integer> quantities = new LinkedHashMap<>();
        public String voucherCode;
    }
}
