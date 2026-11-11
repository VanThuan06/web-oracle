package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Color;

public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findByStatusTrueOrderByNameColorAsc();
}
