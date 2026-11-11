package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bảng 2.35: Bảng ảnh sản phẩm (ProductImage)
 * id, mainImage, urlImage, status, idProduct, idColor, idProductDetail
 */
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    public ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    public Color colorEntity;

    @Column(name = "file_name", nullable = false, length = 120)
    public String fileName;

    @Column(name = "image_url", nullable = false, length = 255)
    public String imageUrl;

    @Column(name = "url_image", length = 255)
    public String urlImage;

    @Column(name = "sort_order", nullable = false)
    public int sortOrder;

    @Column(name = "is_primary", nullable = false)
    public boolean isPrimary;

    @Column(name = "main_image", nullable = false)
    public boolean mainImage = false;

    @Column(name = "status", nullable = false)
    public boolean status = true;

    public ProductImage() {}

    public ProductImage(Product product, String url, boolean isPrimary) {
        this.product = product;
        this.imageUrl = url;
        this.urlImage = url;
        this.fileName = (url != null && url.contains("/")) ? url.substring(url.lastIndexOf('/') + 1) : "image.jpg";
        this.isPrimary = isPrimary;
        this.mainImage = isPrimary;
        this.status = true;
    }

    public String getUrl() {
        return imageUrl != null ? imageUrl : urlImage;
    }
}
