package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.20: Bảng chất liệu (Material)
 * id, nameMaterial, status
 */
@Entity
@Table(name = "materials")
public class Material extends BaseEntity {
    @Column(name = "name_material", nullable = false, length = 120)
    public String nameMaterial;

    @Column(nullable = false)
    public boolean status = true;

    public Material() {}

    public Material(String nameMaterial, boolean status) {
        this.nameMaterial = nameMaterial;
        this.status = status;
    }
}
