package vn.newmen.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Bảng 2.19: Bảng kiểu dáng (Form)
 * id, nameForm, status
 */
@Entity
@Table(name = "forms")
public class Form extends BaseEntity {
    @Column(name = "name_form", nullable = false, length = 120)
    public String nameForm;

    @Column(nullable = false)
    public boolean status = true;

    public Form() {}

    public Form(String nameForm, boolean status) {
        this.nameForm = nameForm;
        this.status = status;
    }
}
