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
 * ERD Hình 2.31: Bảng đổi hàng (Exchange)
 * id, idCustomer, idReturn, idEmployee, total, note, shipAddress, shipName, shipPhone, statusPay, freight
 */
@Entity
@Table(name = "exchanges")
public class Exchange extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    public Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "return_id")
    public OrderReturn orderReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    public Employee employee;

    @Column(precision = 12, scale = 2)
    public BigDecimal total = BigDecimal.ZERO;

    @Column(length = 255)
    public String note;

    @Column(name = "ship_address", length = 255)
    public String shipAddress;

    @Column(name = "ship_name", length = 160)
    public String shipName;

    @Column(name = "ship_phone", length = 32)
    public String shipPhone;

    @Column(name = "status_pay", length = 32)
    public String statusPay = "PAID";

    @Column(precision = 12, scale = 2)
    public BigDecimal freight = BigDecimal.ZERO;

    @Column(nullable = false, length = 32)
    public String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
