package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bảng 2.32: Bảng liên hệ (Contact)
 * id, name, phone, addressName, ward, district, province, status
 */
@Entity
@Table(name = "contacts")
public class Contact extends BaseEntity {
    @Column(nullable = false, length = 120)
    public String name;

    @Column(nullable = false, length = 32)
    public String phone;

    @Column(length = 255)
    public String email;

    @Column(name = "address_name", length = 255)
    public String addressName;

    @Column(length = 120)
    public String ward;

    @Column(length = 120)
    public String district;

    @Column(length = 120)
    public String province;

    @Column(length = 1000)
    public String content;

    @Column(nullable = false)
    public boolean status = true; // true: Mới/Đang xử lý, false: Đã hoàn tất

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public Contact() {}

    public Contact(String name, String phone, String email, String addressName, String ward, String district,
                   String province, String content, boolean status) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.addressName = addressName;
        this.ward = ward;
        this.district = district;
        this.province = province;
        this.content = content;
        this.status = status;
    }
}
