package vn.newmen.store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByAccountIdOrderByIsDefaultDescIdDesc(Long accountId);
    List<Address> findByCustomerId(Long customerId);
    Optional<Address> findByIdAndAccountId(Long id, Long accountId);
}
