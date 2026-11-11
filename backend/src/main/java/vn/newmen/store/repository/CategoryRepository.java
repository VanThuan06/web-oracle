package vn.newmen.store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderByNameAsc();
    Optional<Category> findBySlugAndActiveTrue(String slug);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
