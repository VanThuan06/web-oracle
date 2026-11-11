package vn.newmen.store.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.newmen.store.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    Optional<Product> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = "category")
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findWithCategoryById(@Param("id") Long id);

    boolean existsBySlug(String slug);
    boolean existsByCode(String code);
    Optional<Product> findByCode(String code);

    @EntityGraph(attributePaths = "category")
    @Query("""
        select distinct p from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        """)
    Page<Product> search(@Param("query") String query, @Param("categoryId") Long categoryId,
                         @Param("color") String color, @Param("size") String size, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query(value = """
        select p from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        order by coalesce((select min(v.price) from ProductVariant v where v.product = p and v.active = true), p.originalPrice, 0.00) asc, p.createdAt desc, p.id desc
        """, countQuery = """
        select count(p) from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        """)
    Page<Product> searchPriceAscending(@Param("query") String query, @Param("categoryId") Long categoryId,
                                       @Param("color") String color, @Param("size") String size, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query(value = """
        select p from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        order by coalesce((select min(v.price) from ProductVariant v where v.product = p and v.active = true), p.originalPrice, 0.00) desc, p.createdAt desc, p.id desc
        """, countQuery = """
        select count(p) from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        """)
    Page<Product> searchPriceDescending(@Param("query") String query, @Param("categoryId") Long categoryId,
                                        @Param("color") String color, @Param("size") String size, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query(value = """
        select p from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        order by coalesce((select sum(l.quantity) from OrderLine l where l.variant.product = p and l.order.status <> 'CANCELLED'), 0) desc, p.createdAt desc, p.id desc
        """, countQuery = """
        select count(p) from Product p
        where p.active = true
          and (:query is null or lower(p.name) like lower(concat('%', :query, '%'))
               or lower(p.code) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.category.id = :categoryId)
          and (:color is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.color) = lower(:color)))
          and (:size is null or exists (select v.id from ProductVariant v where v.product = p and v.active = true and lower(v.size) = lower(:size)))
        """)
    Page<Product> searchBestsellers(@Param("query") String query, @Param("categoryId") Long categoryId,
                                    @Param("color") String color, @Param("size") String size, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDescIdDesc(Long categoryId, Long id, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrueAndIdNotOrderByCreatedAtDescIdDesc(Long id, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByActiveTrueOrderByCreatedAtDescIdDesc(Pageable pageable);
}
