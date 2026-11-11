package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Sleeve;

public interface SleeveRepository extends JpaRepository<Sleeve, Long> {
    List<Sleeve> findByStatusTrueOrderByNameSleeveAsc();
}
