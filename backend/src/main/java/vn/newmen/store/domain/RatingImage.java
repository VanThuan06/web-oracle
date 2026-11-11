package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bảng 2.27: Bảng ảnh rating (RatingImage)
 * id, image, status, idRating
 */
@Entity
@Table(name = "rating_images")
public class RatingImage extends BaseEntity {
    @Column(nullable = false, length = 255)
    public String image;

    @Column(nullable = false)
    public boolean status = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rating_id", nullable = false)
    public Review rating;

    public RatingImage() {}

    public RatingImage(String image, boolean status, Review rating) {
        this.image = image;
        this.status = status;
        this.rating = rating;
    }
}
