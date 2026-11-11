package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Size;

public interface SizeRepository extends JpaRepository<Size, Long> {
    List<Size> findByStatusTrueOrderByCodeAsc();
}
