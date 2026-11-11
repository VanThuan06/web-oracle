package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.OrderReturn;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {
    Page<OrderReturn> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<OrderReturn> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    boolean existsByOrder_IdAndAccount_Id(Long orderId, Long accountId);
}
