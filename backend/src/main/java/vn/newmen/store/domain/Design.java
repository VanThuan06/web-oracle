package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.17: Bảng thiết kế (Design)
 * id, nameDesign, status
 */
@Entity
@Table(name = "designs")
public class Design extends BaseEntity {
    @Column(name = "name_design", nullable = false, length = 120)
    public String nameDesign;

    @Column(nullable = false)
    public boolean status = true;

    public Design() {}

    public Design(String nameDesign, boolean status) {
        this.nameDesign = nameDesign;
        this.status = status;
    }
}
