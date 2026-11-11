package vn.newmen.store.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.newmen.store.domain.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndActiveTrueOrderByColorAscSizeAsc(Long productId);
    List<ProductVariant> findByProductIdInAndActiveTrueOrderByProductIdAscColorAscSizeAsc(Collection<Long> productIds);
    List<ProductVariant> findByProductIdOrderByColorAscSizeAsc(Long productId);
    Optional<ProductVariant> findByBarcodeIgnoreCase(String barcode);
    boolean existsBySku(String sku);
    Optional<ProductVariant> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}
