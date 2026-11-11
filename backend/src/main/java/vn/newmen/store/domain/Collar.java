package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.18: Bảng cổ áo (Collar)
 * id, nameCollar, status
 */
@Entity
@Table(name = "collars")
public class Collar extends BaseEntity {
    @Column(name = "name_collar", nullable = false, length = 120)
    public String nameCollar;

    @Column(nullable = false)
    public boolean status = true;

    public Collar() {}

    public Collar(String nameCollar, boolean status) {
        this.nameCollar = nameCollar;
        this.status = status;
    }
}
