package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bảng 2.34: Bảng sản phẩm giảm giá (ProductDiscount)
 * id, percent, status, idProduct, idDiscount
 */
@Entity
@Table(name = "product_discounts")
public class ProductDiscount extends BaseEntity {
    @Column(nullable = false)
    public int percent;

    @Column(nullable = false)
    public boolean status = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    public Discount discount;

    public ProductDiscount() {}

    public ProductDiscount(int percent, boolean status, Product product, Discount discount) {
        this.percent = percent;
        this.status = status;
        this.product = product;
        this.discount = discount;
    }
}
