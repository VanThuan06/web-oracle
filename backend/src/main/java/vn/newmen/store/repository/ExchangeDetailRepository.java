package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.ExchangeDetail;

public interface ExchangeDetailRepository extends JpaRepository<ExchangeDetail, Long> {
    List<ExchangeDetail> findByExchangeId(Long exchangeId);
}
