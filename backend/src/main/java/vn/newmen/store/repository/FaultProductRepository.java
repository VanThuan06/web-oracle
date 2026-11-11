package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.FaultProduct;

public interface FaultProductRepository extends JpaRepository<FaultProduct, Long> {
    Page<FaultProduct> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<FaultProduct> findByProductDetailId(Long productDetailId);
}
