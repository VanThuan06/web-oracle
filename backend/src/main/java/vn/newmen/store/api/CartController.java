package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import vn.newmen.store.service.CartService;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts = carts; }

    @GetMapping
    public CartResponse get(HttpSession session) { return carts.view(session); }

    @PostMapping("/items")
    public CartResponse add(@Valid @RequestBody CartItemRequest request, HttpSession session) {
        return carts.add(session, request.variantId(), request.quantity());
    }

    @PatchMapping("/items/{variantId}")
    public CartResponse quantity(@PathVariable Long variantId, @Valid @RequestBody CartQuantityRequest request,
                                 HttpSession session) {
        return carts.setQuantity(session, variantId, request.quantity());
    }

    @DeleteMapping("/items/{variantId}")
    public CartResponse remove(@PathVariable Long variantId, HttpSession session) {
        return carts.remove(session, variantId);
    }

    @DeleteMapping("/items")
    public CartResponse clear(HttpSession session) { return carts.clear(session); }

    @PostMapping("/voucher")
    public CartResponse applyVoucher(@Valid @RequestBody VoucherRequest request, HttpSession session) {
        return carts.applyVoucher(session, request.code());
    }
}
