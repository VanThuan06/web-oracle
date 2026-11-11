package vn.newmen.store.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Form;

public interface FormRepository extends JpaRepository<Form, Long> {
    List<Form> findByStatusTrueOrderByNameFormAsc();
}
