package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Material;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByStatusTrueOrderByNameMaterialAsc();
}
