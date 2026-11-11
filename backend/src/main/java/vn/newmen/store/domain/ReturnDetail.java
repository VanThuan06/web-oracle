package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bảng 2.38: Bảng trả hàng chi tiết (ReturnDetail)
 * id, quantity, status, idOrderDetail, idReturn
 */
@Entity
@Table(name = "return_details")
public class ReturnDetail extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    public OrderReturn orderReturn;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "order_detail_id", nullable = false)
    public OrderLine orderDetail;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public int status = 1; // 1: Chấp nhận trả, 2: Từ chối

    public ReturnDetail() {}

    public ReturnDetail(OrderReturn orderReturn, OrderLine orderDetail, int quantity, int status) {
        this.orderReturn = orderReturn;
        this.orderDetail = orderDetail;
        this.quantity = quantity;
        this.status = status;
    }
}
