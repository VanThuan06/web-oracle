package vn.newmen.store.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.newmen.store.domain.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<Employee> findByStatusTrue(Pageable pageable);

    @Query("select e from Employee e where lower(e.name) like lower(concat('%', :q, '%')) or lower(e.email) like lower(concat('%', :q, '%')) or e.phone like concat('%', :q, '%')")
    Page<Employee> search(@Param("q") String query, Pageable pageable);
}
