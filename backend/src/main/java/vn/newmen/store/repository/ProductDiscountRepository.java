package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.ProductDiscount;

public interface ProductDiscountRepository extends JpaRepository<ProductDiscount, Long> {
    List<ProductDiscount> findByProductIdAndStatusTrue(Long productId);
    List<ProductDiscount> findByDiscountId(Long discountId);
}
