package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng 2.37: Bảng trả hàng (OrderReturn / Return)
 * id, video, status, note, reason, total, idOrder, idCustomer
 */
@Entity
@Table(name = "order_returns")
public class OrderReturn extends BaseEntity {
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    public CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    public Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    public Customer customer;

    @Column(nullable = false, length = 500)
    public String reason;

    @Column(length = 255)
    public String note;

    @Column(length = 255)
    public String video;

    @Column(precision = 12, scale = 2)
    public BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, length = 24)
    public String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
