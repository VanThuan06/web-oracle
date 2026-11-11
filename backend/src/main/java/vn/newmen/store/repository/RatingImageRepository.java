package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.RatingImage;

public interface RatingImageRepository extends JpaRepository<RatingImage, Long> {
    List<RatingImage> findByRatingId(Long ratingId);
}
