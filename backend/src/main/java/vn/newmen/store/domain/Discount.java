package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bảng 2.33: Bảng khuyến mãi (Discount)
 * id, name, description, startDate, endDate, status
 */
@Entity
@Table(name = "discounts")
public class Discount extends BaseEntity {
    @Column(nullable = false, length = 255)
    public String name;

    @Column(length = 255)
    public String description;

    @Column(name = "start_date", nullable = false)
    public LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDateTime endDate;

    @Column(nullable = false)
    public boolean status = true;

    public Discount() {}

    public Discount(String name, String description, LocalDateTime startDate, LocalDateTime endDate, boolean status) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }
}
