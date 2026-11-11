package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng 2.15: Bảng hoá đơn (Order / CustomerOrder)
 * id, paymentType, address, districtName, cityName, statusPay, status, customerId,
 * employeeId, voucherId, shipPhone, code, wardName, freight, sale_form, shipName
 */
@Entity
@Table(name = "customer_orders", uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_code", columnNames = "code"),
    @UniqueConstraint(name = "uk_order_idempotency", columnNames = "idempotency_key")
})
public class CustomerOrder extends BaseEntity {
    @Column(nullable = false, length = 32)
    public String code;

    @Column(name = "idempotency_key", length = 100)
    public String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    public String requestHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    public Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    public Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    public Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    public Voucher voucher;

    @Column(name = "customer_name", nullable = false, length = 160)
    public String customerName;

    @Column(name = "customer_email", nullable = false, length = 255)
    public String customerEmail;

    @Column(name = "customer_phone", nullable = false, length = 32)
    public String customerPhone;

    @Column(name = "ship_recipient_name", nullable = false, length = 160)
    public String shipRecipientName;

    @Column(name = "ship_name", length = 160)
    public String shipName;

    @Column(name = "ship_phone", nullable = false, length = 32)
    public String shipPhone;

    @Column(name = "ship_line1", nullable = false, length = 255)
    public String shipLine1;

    @Column(name = "address", length = 255)
    public String address;

    @Column(name = "ship_ward", length = 120)
    public String shipWard;

    @Column(name = "ward_name", length = 120)
    public String wardName;

    @Column(name = "ship_district", nullable = false, length = 120)
    public String shipDistrict;

    @Column(name = "district_name", length = 120)
    public String districtName;

    @Column(name = "ship_province", nullable = false, length = 120)
    public String shipProvince;

    @Column(name = "city_name", length = 120)
    public String cityName;

    @Column(name = "payment_method", nullable = false, length = 32)
    public String paymentMethod = "COD";

    @Column(name = "payment_type", length = 32)
    public String paymentType = "COD";

    @Column(name = "status_pay", length = 32)
    public String statusPay = "UNPAID";

    @Column(nullable = false, length = 24)
    public String channel = "ONLINE";

    @Column(name = "sale_form", length = 32)
    public String saleForm = "ONLINE";

    @Column(nullable = false, length = 32)
    public String status = "PENDING";

    @Column(name = "voucher_code", length = 64)
    public String voucherCode;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    public BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "freight", precision = 12, scale = 2)
    public BigDecimal freight = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal total = BigDecimal.ZERO;

    @Column(length = 500)
    public String note;

    @Column(name = "created_by", length = 255)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
