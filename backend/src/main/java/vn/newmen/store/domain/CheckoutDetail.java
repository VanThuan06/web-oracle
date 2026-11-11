package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Bảng 2.41: Bảng checkout chi tiết (CheckoutDetail)
 * id, idCheckout, idProductDetail, quantity, status
 */
@Entity
@Table(name = "checkout_details")
public class CheckoutDetail extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkout_id", nullable = false)
    public Checkout checkout;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_detail_id", nullable = false)
    public ProductVariant productDetail;

    @Column(nullable = false)
    public int quantity;

    @Column(precision = 12, scale = 2)
    public BigDecimal price;

    @Column(nullable = false)
    public int status = 1;

    public CheckoutDetail() {}

    public CheckoutDetail(Checkout checkout, ProductVariant productDetail, int quantity, BigDecimal price, int status) {
        this.checkout = checkout;
        this.productDetail = productDetail;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }
}
