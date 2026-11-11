package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByStatusTrueOrderBySortOrderAsc();
    List<Banner> findAllByOrderBySortOrderAsc();
}
