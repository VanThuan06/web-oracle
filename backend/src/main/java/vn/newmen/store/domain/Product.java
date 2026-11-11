package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng 2.29: Bảng sản phẩm (Product)
 * id, code, name, price, weight, description, status, idCategory, idCollar, idDesign, idForm, idMaterial, idSleeve
 */
@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_slug", columnNames = "slug"),
    @UniqueConstraint(name = "uk_product_code", columnNames = "code")
})
public class Product extends BaseEntity {
    @Column(nullable = false, length = 150)
    public String slug;

    @Column(nullable = false, length = 64)
    public String code;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(length = 3000)
    public String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public Category category;

    @Column(length = 120)
    public String material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    public Material materialEntity;

    @Column(length = 100)
    public String collar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collar_id")
    public Collar collarEntity;

    @Column(length = 100)
    public String design;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id")
    public Design designEntity;

    @Column(length = 100)
    public String fit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
    public Form formEntity;

    @Column(length = 100)
    public String sleeve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleeve_id")
    public Sleeve sleeveEntity;

    @Column(name = "original_price", precision = 12, scale = 2)
    public BigDecimal originalPrice;

    @Column(name = "weight")
    public Double weight = 200.0;

    @Column(nullable = false)
    public boolean active = true;

    @Column(name = "status", nullable = false)
    public boolean status = true;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public boolean getActive() { return active && status; }
    public BigDecimal getPrice() { return originalPrice; }
    public Double getWeight() { return weight; }
}
