package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.24: Bảng kích thước (Size)
 * id, code, status, description
 */
@Entity
@Table(name = "sizes")
public class Size extends BaseEntity {
    @Column(nullable = false, length = 20)
    public String code;

    @Column(length = 255)
    public String description;

    @Column(nullable = false)
    public boolean status = true;

    public Size() {}

    public Size(String code, String description, boolean status) {
        this.code = code;
        this.description = description;
        this.status = status;
    }
}
