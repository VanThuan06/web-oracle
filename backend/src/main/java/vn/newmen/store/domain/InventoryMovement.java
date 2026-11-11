package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    public ProductVariant variant;
    @Column(name = "movement_type", nullable = false, length = 32)
    public String movementType;
    @Column(name = "quantity_delta", nullable = false)
    public int quantityDelta;
    @Column(length = 100)
    public String reference;
    @Column(name = "created_by", length = 255)
    public String createdBy;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
