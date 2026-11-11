package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.newmen.store.service.CatalogService;
import vn.newmen.store.service.LocalImageStorage;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalog;
    private final LocalImageStorage imageStorage;

    public CatalogController(CatalogService catalog, LocalImageStorage imageStorage) {
        this.catalog = catalog;
        this.imageStorage = imageStorage;
    }

    @GetMapping("/catalog/products")
    public PageResponse<ProductResponse> products(@RequestParam(required = false) String query,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String color,
                                                  @RequestParam(name = "clothingSize", required = false) String clothingSize,
                                                  @RequestParam(name = "variantSize", required = false) String variantSize,
                                                  @RequestParam(defaultValue = "newest") String sort,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                                  @RequestParam(name = "size", required = false) Integer sizeAlias) {
        int pageLength = pageSize != null ? pageSize : sizeAlias != null ? sizeAlias : 12;
        return catalog.search(query, categoryId, color,
            clothingSize == null ? variantSize : clothingSize, sort, page, pageLength);
    }

    @GetMapping("/catalog/products/{slug}")
    public ProductResponse product(@PathVariable String slug) { return catalog.product(slug); }

    @GetMapping("/catalog/categories")
    public Map<String, List<CategoryResponse>> categories() {
        return Map.of("data", catalog.categories().data());
    }

    @GetMapping("/catalog/recommendations")
    public Map<String, List<ProductResponse>> recommendations(@RequestParam(required = false) Long productId,
                                                               @RequestParam(defaultValue = "8") int limit) {
        return Map.of("data", catalog.recommendations(productId, limit));
    }

    @GetMapping("/promotions/active")
    public Map<String, List<PromotionResponse>> promotions() { return Map.of("data", catalog.activePromotions()); }

    @GetMapping("/media/{fileName:.+}")
    public ResponseEntity<FileSystemResource> image(@PathVariable String fileName) {
        Path path = imageStorage.resolve(fileName);
        MediaType type = fileName.endsWith(".png") ? MediaType.IMAGE_PNG
            : fileName.endsWith(".webp") ? MediaType.parseMediaType("image/webp") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noCache()).body(new FileSystemResource(path));
    }
}
