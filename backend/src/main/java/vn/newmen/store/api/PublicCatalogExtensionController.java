package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.newmen.store.domain.Banner;
import vn.newmen.store.domain.Contact;
import vn.newmen.store.repository.BannerRepository;
import vn.newmen.store.repository.CategoryRepository;
import vn.newmen.store.repository.CollarRepository;
import vn.newmen.store.repository.ColorRepository;
import vn.newmen.store.repository.ContactRepository;
import vn.newmen.store.repository.DesignRepository;
import vn.newmen.store.repository.FormRepository;
import vn.newmen.store.repository.MaterialRepository;
import vn.newmen.store.repository.OriginCategoryRepository;
import vn.newmen.store.repository.SizeRepository;
import vn.newmen.store.repository.SleeveRepository;
import vn.newmen.store.service.SeedData;

@RestController
@RequestMapping("/api/v1/catalog")
public class PublicCatalogExtensionController {
    private final BannerRepository banners;
    private final ContactRepository contacts;
    private final OriginCategoryRepository originCategories;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    private final CollarRepository collars;
    private final DesignRepository designs;
    private final FormRepository forms;
    private final SleeveRepository sleeves;
    private final SeedData seedData;

    public PublicCatalogExtensionController(BannerRepository banners,
                                            ContactRepository contacts,
                                            OriginCategoryRepository originCategories,
                                            CategoryRepository categories,
                                            ColorRepository colors,
                                            SizeRepository sizes,
                                            MaterialRepository materials,
                                            CollarRepository collars,
                                            DesignRepository designs,
                                            FormRepository forms,
                                            SleeveRepository sleeves,
                                            SeedData seedData) {
        this.banners = banners;
        this.contacts = contacts;
        this.originCategories = originCategories;
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.materials = materials;
        this.collars = collars;
        this.designs = designs;
        this.forms = forms;
        this.sleeves = sleeves;
        this.seedData = seedData;
    }

    @GetMapping("/reseed")
    public Map<String, Object> triggerReseed() {
        seedData.seedAll();
        return Map.of("success", true, "message", "Đã nạp đầy đủ toàn bộ 32 bảng dữ liệu và luồng quy trình NewMen Store.");
    }

    @GetMapping("/banners")
    public Map<String, List<BannerResponse>> getActiveBanners() {
        List<BannerResponse> list = banners.findByStatusTrueOrderBySortOrderAsc().stream().map(b ->
            new BannerResponse(b.id, b.title, b.content, b.type, b.url, b.linkUrl, b.status, b.sortOrder)
        ).toList();
        return Map.of("data", list);
    }

    @PostMapping("/contacts")
    public ResponseEntity<Map<String, Object>> submitContact(@RequestBody ContactWriteRequest req) {
        Contact c = new Contact(req.name(), req.phone(), req.email(), req.addressName(),
            req.ward(), req.district(), req.province(), req.content(), true);
        contacts.save(c);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi sớm nhất."));
    }

    @GetMapping("/attributes")
    public Map<String, Object> getCatalogAttributes() {
        Map<String, Object> map = new HashMap<>();
        map.put("originCategories", originCategories.findByStatusTrueOrderByNameCategoryFAsc().stream().map(o -> Map.of("id", o.id, "name", o.nameCategoryF)).toList());
        map.put("categories", categories.findAll().stream().filter(c -> c.active).map(c -> Map.of("id", c.id, "name", c.name, "slug", c.slug)).toList());
        map.put("colors", colors.findByStatusTrueOrderByNameColorAsc().stream().map(c -> Map.of("id", c.id, "name", c.nameColor, "hex", c.codeHex != null ? c.codeHex : "#000000")).toList());
        map.put("sizes", sizes.findByStatusTrueOrderByCodeAsc().stream().map(s -> Map.of("id", s.id, "code", s.code, "name", s.code, "desc", s.description != null ? s.description : "")).toList());
        map.put("materials", materials.findByStatusTrueOrderByNameMaterialAsc().stream().map(m -> Map.of("id", m.id, "name", m.nameMaterial)).toList());
        map.put("collars", collars.findByStatusTrueOrderByNameCollarAsc().stream().map(c -> Map.of("id", c.id, "name", c.nameCollar)).toList());
        map.put("designs", designs.findByStatusTrueOrderByNameDesignAsc().stream().map(d -> Map.of("id", d.id, "name", d.nameDesign)).toList());
        map.put("forms", forms.findByStatusTrueOrderByNameFormAsc().stream().map(f -> Map.of("id", f.id, "name", f.nameForm)).toList());
        map.put("sleeves", sleeves.findByStatusTrueOrderByNameSleeveAsc().stream().map(s -> Map.of("id", s.id, "name", s.nameSleeve)).toList());
        return map;
    }
}
