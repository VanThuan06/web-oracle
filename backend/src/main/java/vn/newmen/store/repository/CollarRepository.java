package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Collar;

public interface CollarRepository extends JpaRepository<Collar, Long> {
    List<Collar> findByStatusTrueOrderByNameCollarAsc();
}
