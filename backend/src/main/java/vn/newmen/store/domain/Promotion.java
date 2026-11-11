package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_code", columnNames = "code"))
public class Promotion extends BaseEntity {
    @Column(nullable = false, length = 160)
    public String name;
    @Column(nullable = false, length = 64)
    public String code;
    @Column(length = 500)
    public String description;
    @Column(name = "percent_off", nullable = false)
    public int percentOff;
    @Column(name = "minimum_order", nullable = false, precision = 12, scale = 2)
    public BigDecimal minimumOrder = BigDecimal.ZERO;
    @Column(name = "maximum_discount", precision = 12, scale = 2)
    public BigDecimal maximumDiscount;
    @Column(name = "starts_at", nullable = false)
    public LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false)
    public LocalDateTime endsAt;
    @Column(nullable = false)
    public boolean active = true;
}
