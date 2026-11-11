package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng 2.40: Bảng checkout
 * id, code, status, code_order
 */
@Entity
@Table(name = "checkouts")
public class Checkout extends BaseEntity {
    @Column(nullable = false, length = 64)
    public String code;

    @Column(name = "code_order", length = 64)
    public String codeOrder;

    @Column(nullable = false)
    public int status = 1; // 1: Đang chờ thanh toán, 2: Đã thanh toán, 0: Hủy

    @Column(name = "total_amount", precision = 12, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_type", length = 32)
    public String paymentType = "COD";

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public Checkout() {}

    public Checkout(String code, String codeOrder, int status, BigDecimal totalAmount, String paymentType) {
        this.code = code;
        this.codeOrder = codeOrder;
        this.status = status;
        this.totalAmount = totalAmount;
        this.paymentType = paymentType;
    }
}
