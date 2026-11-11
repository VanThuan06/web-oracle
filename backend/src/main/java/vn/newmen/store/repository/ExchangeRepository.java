package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Exchange;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    Page<Exchange> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Exchange> findByCustomerId(Long customerId);
}
