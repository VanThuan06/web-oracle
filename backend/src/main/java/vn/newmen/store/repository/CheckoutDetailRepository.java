package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.CheckoutDetail;

public interface CheckoutDetailRepository extends JpaRepository<CheckoutDetail, Long> {
    List<CheckoutDetail> findByCheckoutId(Long checkoutId);
}
