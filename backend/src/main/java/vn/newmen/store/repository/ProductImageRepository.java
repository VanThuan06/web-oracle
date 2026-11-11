package vn.newmen.store.repository;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
    List<ProductImage> findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(Collection<Long> productIds);
    java.util.Optional<ProductImage> findByIdAndProductId(Long id, Long productId);
}
