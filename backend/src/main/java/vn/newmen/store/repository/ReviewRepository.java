package vn.newmen.store.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByOrder_IdAndProduct_Id(Long orderId, Long productId);
    @org.springframework.data.jpa.repository.Query("select avg(r.rating) from Review r where r.product.id = :productId")
    Double averageForProduct(@org.springframework.data.repository.query.Param("productId") Long productId);
}
