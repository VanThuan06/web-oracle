package vn.newmen.store.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Checkout;

public interface CheckoutRepository extends JpaRepository<Checkout, Long> {
    Optional<Checkout> findByCode(String code);
    Optional<Checkout> findByCodeOrder(String codeOrder);
}
