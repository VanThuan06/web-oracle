package vn.newmen.store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    Optional<Voucher> findByCodeIgnoreCaseAndStatusTrue(String code);
    List<Voucher> findByStatusTrueAndStartDateBeforeAndEndDateAfter(LocalDateTime now1, LocalDateTime now2);
    Page<Voucher> findAllByOrderByStartDateDesc(Pageable pageable);
}
