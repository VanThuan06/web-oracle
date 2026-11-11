package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.21: Bảng tay áo (Sleeve)
 * id, nameSleeve, status
 */
@Entity
@Table(name = "sleeves")
public class Sleeve extends BaseEntity {
    @Column(name = "name_sleeve", nullable = false, length = 120)
    public String nameSleeve;

    @Column(nullable = false)
    public boolean status = true;

    public Sleeve() {}

    public Sleeve(String nameSleeve, boolean status) {
        this.nameSleeve = nameSleeve;
        this.status = status;
    }
}
