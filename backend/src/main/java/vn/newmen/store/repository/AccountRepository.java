package vn.newmen.store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.newmen.store.domain.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Page<Account> findByRole(String role, Pageable pageable);
    List<Account> findTop100ByRoleOrderByCreatedAtDesc(String role);

    @Query(value = """
        select a.* from accounts a
        where a.role = :role
          and (lower(a.email) like '%' || lower(:query) || '%'
               or lower(a.full_name) like '%' || lower(:query) || '%'
               or lower(a.phone) like '%' || lower(:query) || '%')
        order by a.created_at desc
        """, countQuery = """
        select count(*) from accounts a
        where a.role = :role
          and (lower(a.email) like '%' || lower(:query) || '%'
               or lower(a.full_name) like '%' || lower(:query) || '%'
               or lower(a.phone) like '%' || lower(:query) || '%')
        """, nativeQuery = true)
    Page<Account> searchByRoleAndQuery(@Param("role") String role, @Param("query") String query, Pageable pageable);
}
