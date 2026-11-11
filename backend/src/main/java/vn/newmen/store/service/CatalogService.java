package vn.newmen.store.service;

import static vn.newmen.store.api.ApiModels.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Category;
import vn.newmen.store.domain.Product;
import vn.newmen.store.domain.ProductImage;
import vn.newmen.store.domain.ProductVariant;
import vn.newmen.store.domain.Promotion;
import vn.newmen.store.repository.CategoryRepository;
import vn.newmen.store.repository.ProductImageRepository;
import vn.newmen.store.repository.ProductRepository;
import vn.newmen.store.repository.ProductVariantRepository;
import vn.newmen.store.repository.PromotionRepository;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;
    private final CategoryRepository categories;
    private final PromotionRepository promotions;

    public CatalogService(ProductRepository products, ProductVariantRepository variants,
                          ProductImageRepository images, CategoryRepository categories,
                          PromotionRepository promotions) {
        this.products = products;
        this.variants = variants;
        this.images = images;
        this.categories = categories;
        this.promotions = promotions;
    }

    public PageResponse<ProductResponse> search(String query, Long categoryId, String color, String size,
                                                String sort, int page, int sizePerPage) {
        if (page < 0 || sizePerPage < 1 || sizePerPage > 48) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tham số phân trang không hợp lệ");
        }
        String normalizedSort = sort == null ? "newest" : sort.toLowerCase(Locale.ROOT);
        Pageable pageable = switch (normalizedSort) {
            case "price-asc", "price_asc" -> PageRequest.of(page, sizePerPage);
            case "price-desc", "price_desc" -> PageRequest.of(page, sizePerPage);
            case "bestseller", "best-seller" -> PageRequest.of(page, sizePerPage);
            case "name", "name_asc" -> PageRequest.of(page, sizePerPage, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
            case "oldest" -> PageRequest.of(page, sizePerPage, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
            default -> PageRequest.of(page, sizePerPage, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        };
        String normalizedQuery = blankToNull(query);
        String normalizedColor = blankToNull(color);
        String normalizedSize = blankToNull(size);
        Page<Product> result = switch (normalizedSort) {
            case "price-asc", "price_asc" -> products.searchPriceAscending(normalizedQuery, categoryId, normalizedColor, normalizedSize, pageable);
            case "price-desc", "price_desc" -> products.searchPriceDescending(normalizedQuery, categoryId, normalizedColor, normalizedSize, pageable);
            case "bestseller", "best-seller" -> products.searchBestsellers(normalizedQuery, categoryId, normalizedColor, normalizedSize, pageable);
            default -> products.search(normalizedQuery, categoryId, normalizedColor, normalizedSize, pageable);
        };
        return new PageResponse<>(toResponses(result.getContent()), result.getNumber(),
            result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public ProductResponse product(String slug) {
        Product product = products.findBySlugAndActiveTrue(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        return toResponse(product);
    }

    public Product productEntity(Long id) {
        return products.findWithCategoryById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
    }

    public PageResponse<CategoryResponse> categories() {
        List<CategoryResponse> rows = categories.findByActiveTrueOrderByNameAsc().stream().map(this::categoryResponse).toList();
        return new PageResponse<>(rows, 0, rows.size(), rows.size(), rows.isEmpty() ? 0 : 1);
    }

    public List<CategoryResponse> adminCategories() {
        return categories.findAll(Sort.by("name").ascending()).stream().map(this::categoryResponse).toList();
    }

    public List<ProductResponse> recommendations(Long productId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 12));
        if (productId != null) {
            Product source = products.findWithCategoryById(productId).orElse(null);
            if (source != null && source.category != null) {
                Page<Product> related = products.findByActiveTrueAndCategoryIdAndIdNotOrderByCreatedAtDescIdDesc(
                    source.category.getId(), source.id, PageRequest.of(0, boundedLimit));
                if (!related.isEmpty()) return toResponses(related.getContent());
            }
            return toResponses(products.findByActiveTrueAndIdNotOrderByCreatedAtDescIdDesc(
                productId, PageRequest.of(0, boundedLimit)).getContent());
        }
        return toResponses(products.findByActiveTrueOrderByCreatedAtDescIdDesc(PageRequest.of(0, boundedLimit)).getContent());
    }

    public List<PromotionResponse> activePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotions.findByActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByEndsAtAsc(now, now)
            .stream().map(this::promotionResponse).toList();
    }

    public Promotion activePromotion(String code) {
        LocalDateTime now = LocalDateTime.now();
        return promotions.findByCodeIgnoreCaseAndActiveTrue(code)
            .filter(promotion -> !promotion.startsAt.isAfter(now) && !promotion.endsAt.isBefore(now))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã ưu đãi không hợp lệ hoặc đã hết hạn"));
    }

    public ProductResponse toResponse(Product product) {
        List<ProductVariant> productVariants = variants.findByProductIdAndActiveTrueOrderByColorAscSizeAsc(product.id);
        List<String> productImages = images.findByProductIdOrderBySortOrderAscIdAsc(product.id).stream()
            .map(image -> image.imageUrl).toList();
        return toResponse(product, productVariants, productImages);
    }

    private List<ProductResponse> toResponses(List<Product> productRows) {
        if (productRows.isEmpty()) return List.of();
        List<Long> ids = productRows.stream().map(product -> product.id).toList();
        Map<Long, List<ProductVariant>> variantsByProduct = new HashMap<>();
        for (ProductVariant variant : variants.findByProductIdInAndActiveTrueOrderByProductIdAscColorAscSizeAsc(ids)) {
            variantsByProduct.computeIfAbsent(variant.product.getId(), ignored -> new ArrayList<>()).add(variant);
        }
        Map<Long, List<String>> imagesByProduct = new HashMap<>();
        for (ProductImage image : images.findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(ids)) {
            imagesByProduct.computeIfAbsent(image.product.getId(), ignored -> new ArrayList<>()).add(image.imageUrl);
        }
        return productRows.stream().map(product -> toResponse(product,
            variantsByProduct.getOrDefault(product.id, List.of()), imagesByProduct.getOrDefault(product.id, List.of()))).toList();
    }

    private ProductResponse toResponse(Product product, List<ProductVariant> productVariants, List<String> productImages) {
        BigDecimal price = productVariants.stream().map(variant -> variant.price).min(BigDecimal::compareTo)
            .orElse(product.originalPrice == null ? BigDecimal.ZERO : product.originalPrice);
        List<VariantResponse> variantResponses = productVariants.stream().map(variant ->
            new VariantResponse(variant.id, variant.sku, variant.barcode, variant.color, variant.size,
                variant.price, variant.stock)).toList();
        return new ProductResponse(product.id, product.slug, product.code, product.name, product.description,
            product.category == null ? null : product.category.getName(), price, product.originalPrice,
            productImages, variantResponses);
    }

    public CategoryResponse categoryResponse(Category category) {
        return new CategoryResponse(category.id, category.name, category.slug,
            category.parent == null ? null : category.parent.getId());
    }

    public PromotionResponse promotionResponse(Promotion promotion) {
        return new PromotionResponse(promotion.id, promotion.name, promotion.code, promotion.description,
            promotion.percentOff, promotion.minimumOrder, promotion.maximumDiscount,
            promotion.startsAt, promotion.endsAt);
    }

    public String primaryImage(Long productId) {
        return images.findByProductIdOrderBySortOrderAscIdAsc(productId).stream().findFirst()
            .map(image -> image.imageUrl).orElse(null);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
