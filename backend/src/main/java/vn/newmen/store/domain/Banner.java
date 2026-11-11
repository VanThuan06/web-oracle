package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.25: Bảng banner
 * id, content, type, url, status
 */
@Entity
@Table(name = "banners")
public class Banner extends BaseEntity {
    @Column(length = 255)
    public String title;

    @Column(length = 500)
    public String content;

    @Column(nullable = false)
    public int type = 1; // 1: Home Top, 2: Popup, 3: Category

    @Column(nullable = false, length = 255)
    public String url;

    @Column(name = "link_url", length = 255)
    public String linkUrl;

    @Column(nullable = false)
    public boolean status = true;

    @Column(name = "sort_order", nullable = false)
    public int sortOrder = 0;

    public Banner() {}

    public Banner(String title, String content, int type, String url, String linkUrl, boolean status, int sortOrder) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.url = url;
        this.linkUrl = linkUrl;
        this.status = status;
        this.sortOrder = sortOrder;
    }
}
