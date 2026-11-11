package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Bảng 2.31: Bảng role (id, name, status)
 */
@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uk_role_name", columnNames = "name"))
public class Role extends BaseEntity {
    @Column(nullable = false, length = 50)
    public String name;

    @Column(length = 200)
    public String description;

    @Column(nullable = false)
    public boolean status = true;

    public Role() {}

    public Role(String name, String description, boolean status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }
}
