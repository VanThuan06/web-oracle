package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.22: Bảng thể loại cha (OriginCategory)
 * id, nameCategoryF, status
 */
@Entity
@Table(name = "origin_categories")
public class OriginCategory extends BaseEntity {
    @Column(name = "name_category_f", nullable = false, length = 120)
    public String nameCategoryF;

    @Column(nullable = false)
    public boolean status = true;

    public OriginCategory() {}

    public OriginCategory(String nameCategoryF, boolean status) {
        this.nameCategoryF = nameCategoryF;
        this.status = status;
    }
}
