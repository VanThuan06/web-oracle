package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * Bảng 2.30: Bảng sản phẩm chi tiết (ProductDetail / ProductVariant)
 * id, barcode, quantity, price/costPrice, status, idProduct, idColor, idSize
 */
@Entity
@Table(name = "product_details", uniqueConstraints = {
    @UniqueConstraint(name = "uk_variant_sku", columnNames = "sku"),
    @UniqueConstraint(name = "uk_variant_barcode", columnNames = "barcode")
})
public class ProductVariant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    @Column(nullable = false, length = 64)
    public String sku;

    @Column(length = 64)
    public String barcode;

    @Column(nullable = false, length = 60)
    public String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    public Color colorEntity;

    @Column(nullable = false, length = 24)
    public String size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id")
    public Size sizeEntity;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal price;

    @Column(name = "cost_price", precision = 12, scale = 2)
    public BigDecimal costPrice;

    @Column(name = "quantity", nullable = false)
    public int stock;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "status", nullable = false)
    public boolean status = true;

    public Product getProduct() { return product; }
    public int getQuantity() { return stock; }
    public void setQuantity(int q) { this.stock = q; }
    public boolean getStatus() { return active && status; }
}
