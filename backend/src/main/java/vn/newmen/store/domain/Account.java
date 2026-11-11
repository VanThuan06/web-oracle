package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(name = "uk_account_email", columnNames = "email"))
public class Account extends BaseEntity {
    @Column(nullable = false, length = 255)
    public String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
    @Column(name = "full_name", nullable = false, length = 160)
    public String fullName;
    @Column(length = 32)
    public String phone;
    @Column(nullable = false, length = 20)
    public String role;
    @Column(nullable = false)
    public boolean active = true;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
