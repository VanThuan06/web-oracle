package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.OriginCategory;

public interface OriginCategoryRepository extends JpaRepository<OriginCategory, Long> {
    List<OriginCategory> findByStatusTrueOrderByNameCategoryFAsc();
}
