package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * ERD Hình 2.31: Bảng đổi hàng chi tiết (ExchangeDetail)
 * id, idExchange, idProductDetail, quantity, unitPrice, note
 */
@Entity
@Table(name = "exchange_details")
public class ExchangeDetail extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_id", nullable = false)
    public Exchange exchange;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_detail_id", nullable = false)
    public ProductVariant productDetail;

    @Column(nullable = false)
    public int quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    public BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(length = 255)
    public String note;

    @Column(nullable = false)
    public int status = 1;

    public ExchangeDetail() {}

    public ExchangeDetail(Exchange exchange, ProductVariant productDetail, int quantity, BigDecimal unitPrice, String note) {
        this.exchange = exchange;
        this.productDetail = productDetail;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.note = note;
    }
}
