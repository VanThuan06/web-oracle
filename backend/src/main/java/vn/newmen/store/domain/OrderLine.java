package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Bảng 2.39: Bảng hoá đơn chi tiết (OrderDetail / OrderLine)
 * id, note, status, quantity, unitPrice, idProductDetail, idOrder, discount, returnQuantity
 */
@Entity
@Table(name = "order_lines")
public class OrderLine extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    public CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    public ProductVariant variant;

    @Column(name = "product_name", nullable = false, length = 200)
    public String productName;

    @Column(nullable = false, length = 64)
    public String sku;

    @Column(nullable = false, length = 60)
    public String color;

    @Column(nullable = false, length = 24)
    public String size;

    @Column(name = "image_url", length = 255)
    public String imageUrl;

    @Column(nullable = false)
    public int quantity;

    @Column(name = "return_quantity", nullable = false)
    public int returnQuantity = 0;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    public BigDecimal unitPrice;

    @Column(name = "discount", precision = 12, scale = 2)
    public BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    public BigDecimal lineTotal;

    @Column(length = 255)
    public String note;

    @Column(nullable = false)
    public int status = 1; // 1: Bình thường, 2: Đã đổi, 3: Đã trả
}
