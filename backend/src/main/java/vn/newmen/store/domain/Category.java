package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Bảng 2.23: Bảng thể loại (Category)
 * id, nameCategory, status, idOriginCategory
 */
@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_category_slug", columnNames = "slug"))
public class Category extends BaseEntity {
    @Column(nullable = false, length = 120)
    public String name;

    @Column(name = "name_category", length = 120)
    public String nameCategory;

    @Column(nullable = false, length = 150)
    public String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public Category parent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origin_category_id")
    public OriginCategory originCategory;

    @Column(nullable = false)
    public boolean active = true;

    @Column(nullable = false)
    public boolean status = true;

    public String getName() {
        return name != null && !name.isBlank() ? name : nameCategory;
    }
}
