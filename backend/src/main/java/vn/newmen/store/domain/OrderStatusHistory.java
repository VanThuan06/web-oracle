package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    public CustomerOrder order;
    @Column(name = "previous_status", length = 32)
    public String previousStatus;
    @Column(name = "next_status", nullable = false, length = 32)
    public String nextStatus;
    @Column(length = 500)
    public String note;
    @Column(name = "changed_by", length = 255)
    public String changedBy;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
