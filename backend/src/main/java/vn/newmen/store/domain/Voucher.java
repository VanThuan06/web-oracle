package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng 2.36: Bảng ưu đãi (Voucher)
 * id, code, status, description, discount, max, min, name, endDate, startDate, type
 */
@Entity
@Table(name = "vouchers", uniqueConstraints = @UniqueConstraint(name = "uk_voucher_code", columnNames = "code"))
public class Voucher extends BaseEntity {
    @Column(nullable = false, length = 64)
    public String code;

    @Column(nullable = false, length = 160)
    public String name;

    @Column(length = 255)
    public String description;

    @Column(nullable = false)
    public int discount; // % giảm hoặc giá trị giảm

    @Column(precision = 12, scale = 2)
    public BigDecimal min; // Giá trị đơn hàng tối thiểu

    @Column(precision = 12, scale = 2)
    public BigDecimal max; // Giảm tối đa

    @Column(name = "start_date", nullable = false)
    public LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    public LocalDateTime endDate;

    @Column(nullable = false)
    public int type = 0; // 0: Phần trăm, 1: Tiền mặt cố định

    @Column(nullable = false)
    public boolean status = true;

    public Voucher() {}

    public Voucher(String code, String name, String description, int discount, BigDecimal min, BigDecimal max,
                   LocalDateTime startDate, LocalDateTime endDate, int type, boolean status) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.discount = discount;
        this.min = min;
        this.max = max;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.status = status;
    }
}
