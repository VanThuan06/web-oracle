package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.16: Bảng màu sắc (Color)
 * id, nameColor, status
 */
@Entity
@Table(name = "colors")
public class Color extends BaseEntity {
    @Column(name = "name_color", nullable = false, length = 60)
    public String nameColor;

    @Column(name = "code_hex", length = 20)
    public String codeHex;

    @Column(nullable = false)
    public boolean status = true;

    public Color() {}

    public Color(String nameColor, String codeHex, boolean status) {
        this.nameColor = nameColor;
        this.codeHex = codeHex;
        this.status = status;
    }
}
