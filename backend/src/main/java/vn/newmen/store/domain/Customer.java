package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bảng 2.13: Bảng khách hàng
 * id, name, image, birthDate, phoneNumber, email, password, roleId, status
 */
@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(name = "uk_customer_email", columnNames = "email"))
public class Customer extends BaseEntity {
    @Column(nullable = false, length = 160)
    public String name;

    @Column(length = 255)
    public String image;

    @Column(name = "birth_date")
    public LocalDate birthDate;

    @Column(name = "phone_number", length = 32)
    public String phoneNumber;

    @Column(nullable = false, length = 255)
    public String email;

    @Column(nullable = false, length = 100)
    public String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    public Role role;

    @Column(nullable = false)
    public boolean status = true;

    @Column(nullable = false)
    public int points = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    public Account account;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
