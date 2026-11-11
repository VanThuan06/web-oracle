package vn.newmen.store.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Discount;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByStatusTrueAndStartDateBeforeAndEndDateAfter(LocalDateTime now1, LocalDateTime now2);
    Page<Discount> findAllByOrderByStartDateDesc(Pageable pageable);
}
