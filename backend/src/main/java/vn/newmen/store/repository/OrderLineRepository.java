package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.newmen.store.domain.OrderLine;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findByOrderId(Long orderId);
    List<OrderLine> findByOrderIdOrderByIdAsc(Long orderId);
    @Query("select l.variant.id, l.quantity from OrderLine l where l.order.id = :orderId and l.variant is not null order by l.id")
    List<Object[]> findVariantQuantitiesByOrderId(@Param("orderId") Long orderId);
    boolean existsByOrder_IdAndVariant_Product_Id(Long orderId, Long productId);

    @Query("select l.productName, sum(l.quantity), sum(l.lineTotal) from OrderLine l join l.order o where o.createdAt >= :from and o.createdAt < :to and o.status <> 'CANCELLED' group by l.productName order by sum(l.quantity) desc")
    List<Object[]> topProducts(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("select coalesce(sum(l.quantity), 0) from OrderLine l join l.order o where o.createdAt >= :from and o.createdAt < :to and o.status <> 'CANCELLED'")
    Long unitsSoldBetween(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
