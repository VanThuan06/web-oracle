package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bảng 2.26: Bảng sản phẩm huỷ (FaultProduct)
 * id, productDetailId, quantity, description, status
 */
@Entity
@Table(name = "fault_products")
public class FaultProduct extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_detail_id", nullable = false)
    public ProductVariant productDetail;

    @Column(nullable = false)
    public int quantity;

    @Column(length = 255)
    public String description;

    @Column(nullable = false)
    public int status = 1; // 1: Đã huỷ, 0: Chờ xử lý

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public FaultProduct() {}

    public FaultProduct(ProductVariant productDetail, int quantity, String description, int status) {
        this.productDetail = productDetail;
        this.quantity = quantity;
        this.description = description;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}
