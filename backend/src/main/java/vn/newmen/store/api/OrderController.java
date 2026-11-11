package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Account;
import vn.newmen.store.domain.CustomerOrder;
import vn.newmen.store.domain.Product;
import vn.newmen.store.domain.Review;
import vn.newmen.store.repository.CustomerOrderRepository;
import vn.newmen.store.repository.OrderLineRepository;
import vn.newmen.store.repository.ProductRepository;
import vn.newmen.store.repository.ReviewRepository;
import vn.newmen.store.service.CurrentAccount;
import vn.newmen.store.service.OrderService;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderService orders;
    private final CurrentAccount currentAccount;
    private final CustomerOrderRepository orderRepository;
    private final OrderLineRepository orderLines;
    private final ProductRepository products;
    private final ReviewRepository reviews;

    public OrderController(OrderService orders, CurrentAccount currentAccount, CustomerOrderRepository orderRepository,
                           OrderLineRepository orderLines, ProductRepository products, ReviewRepository reviews) {
        this.orders = orders;
        this.currentAccount = currentAccount;
        this.orderRepository = orderRepository;
        this.orderLines = orderLines;
        this.products = products;
        this.reviews = reviews;
    }

    @PostMapping("/checkout")
    public org.springframework.http.ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request,
                                                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                                                           Authentication authentication,
                                                                           HttpSession session) {
        Account account = currentAccount.optional(authentication).orElse(null);
        OrderResponse order = orders.checkout(request, idempotencyKey, account, session);
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/orders/mine")
    public PageResponse<OrderResponse> mine(Authentication authentication,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size) {
        return orders.mine(currentAccount.require(authentication), page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderDetailResponse detail(@PathVariable Long id, Authentication authentication) {
        return orders.mineDetail(id, currentAccount.require(authentication));
    }

    @PostMapping("/orders/{id}/reviews")
    public ReviewResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
                                 Authentication authentication) {
        Account account = currentAccount.require(authentication);
        CustomerOrder order = orderRepository.findByIdAndAccountId(id, account.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!"COMPLETED".equals(order.status)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể đánh giá đơn đã hoàn thành");
        if (!orderLines.existsByOrder_IdAndVariant_Product_Id(id, request.productId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sản phẩm không thuộc đơn hàng này");
        }
        if (reviews.findByOrder_IdAndProduct_Id(id, request.productId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã đánh giá sản phẩm này trong đơn hàng");
        }
        Product product = products.findById(request.productId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        Review review = new Review();
        review.account = account;
        review.order = order;
        review.product = product;
        review.rating = request.rating();
        review.content = request.content();
        reviews.save(review);
        return new ReviewResponse(review.id, review.rating, review.content, account.fullName, review.createdAt);
    }

    @PostMapping("/orders/{id}/returns")
    public ReturnResponse requestReturn(@PathVariable Long id, @Valid @RequestBody ReturnCreateRequest request,
                                        Authentication authentication) {
        return orders.requestReturn(id, currentAccount.require(authentication), request.reason());
    }
}
