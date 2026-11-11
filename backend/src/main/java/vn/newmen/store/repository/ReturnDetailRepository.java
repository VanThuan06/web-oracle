package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.ReturnDetail;

public interface ReturnDetailRepository extends JpaRepository<ReturnDetail, Long> {
    List<ReturnDetail> findByOrderReturnId(Long orderReturnId);
}
