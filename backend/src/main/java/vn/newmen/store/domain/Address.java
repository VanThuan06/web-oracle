package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bảng 2.14: Bảng địa chỉ khách hàng
 * id, customerId, address, wardName, districtName, cityName, other, status
 */
@Entity
@Table(name = "customer_addresses")
public class Address extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    public Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    public Customer customer;

    @Column(name = "recipient_name", length = 160)
    public String recipientName;

    @Column(length = 32)
    public String phone;

    @Column(name = "line1", length = 255)
    public String line1;

    @Column(name = "address", length = 255)
    public String address;

    @Column(name = "ward", length = 120)
    public String ward;

    @Column(name = "ward_name", length = 120)
    public String wardName;

    @Column(name = "district", length = 120)
    public String district;

    @Column(name = "district_name", length = 120)
    public String districtName;

    @Column(name = "province", length = 120)
    public String province;

    @Column(name = "city_name", length = 120)
    public String cityName;

    @Column(name = "other", length = 255)
    public String other;

    @Column(name = "is_default", nullable = false)
    public boolean isDefault;

    @Column(name = "status", nullable = false)
    public boolean status = true;

    public String getFullAddress() {
        String street = address != null ? address : line1;
        String w = wardName != null ? wardName : ward;
        String d = districtName != null ? districtName : district;
        String c = cityName != null ? cityName : province;
        return String.format("%s, %s, %s, %s",
            street != null ? street : "",
            w != null ? w : "",
            d != null ? d : "",
            c != null ? c : "").replaceAll("^[ ,]+|[ ,]+$", "");
    }
}
