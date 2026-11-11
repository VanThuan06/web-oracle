package vn.newmen.store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.newmen.store.domain.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    boolean existsByCode(String code);
    Optional<CustomerOrder> findByCode(String code);
    Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
    Page<CustomerOrder> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);
    Optional<CustomerOrder> findByIdAndAccountId(Long id, Long accountId);
    List<CustomerOrder> findByCreatedAtBetweenAndStatusNot(LocalDateTime from, LocalDateTime to, String status);

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to and o.status <> 'CANCELLED'")
    java.math.BigDecimal revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("select o from CustomerOrder o where (:status is null or o.status = :status) and (:query is null or lower(o.code) like lower(concat('%', :query, '%')) or lower(o.customerName) like lower(concat('%', :query, '%')) or lower(o.customerPhone) like lower(concat('%', :query, '%'))) order by o.createdAt desc")
    Page<CustomerOrder> search(@Param("status") String status, @Param("query") String query, Pageable pageable);

    @Query("select count(distinct o.customerEmail) from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to and o.status <> 'CANCELLED'")
    long distinctCustomersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
