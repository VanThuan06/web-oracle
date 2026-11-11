package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Category;
import vn.newmen.store.domain.Collar;
import vn.newmen.store.domain.Color;
import vn.newmen.store.domain.Design;
import vn.newmen.store.domain.Form;
import vn.newmen.store.domain.Material;
import vn.newmen.store.domain.OriginCategory;
import vn.newmen.store.domain.Size;
import vn.newmen.store.domain.Sleeve;
import vn.newmen.store.repository.CategoryRepository;
import vn.newmen.store.repository.CollarRepository;
import vn.newmen.store.repository.ColorRepository;
import vn.newmen.store.repository.DesignRepository;
import vn.newmen.store.repository.FormRepository;
import vn.newmen.store.repository.MaterialRepository;
import vn.newmen.store.repository.OriginCategoryRepository;
import vn.newmen.store.repository.SizeRepository;
import vn.newmen.store.repository.SleeveRepository;
import vn.newmen.store.service.CurrentAccount;

@RestController
@RequestMapping("/api/v1/admin/attributes")
public class AdminAttributeController {
    private final CurrentAccount current;
    private final OriginCategoryRepository originCategories;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    private final CollarRepository collars;
    private final DesignRepository designs;
    private final FormRepository forms;
    private final SleeveRepository sleeves;

    public AdminAttributeController(CurrentAccount current,
                                    OriginCategoryRepository originCategories,
                                    CategoryRepository categories,
                                    ColorRepository colors,
                                    SizeRepository sizes,
                                    MaterialRepository materials,
                                    CollarRepository collars,
                                    DesignRepository designs,
                                    FormRepository forms,
                                    SleeveRepository sleeves) {
        this.current = current;
        this.originCategories = originCategories;
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.materials = materials;
        this.collars = collars;
        this.designs = designs;
        this.forms = forms;
        this.sleeves = sleeves;
    }

    @GetMapping("/{type}")
    public Map<String, List<AttributeItemResponse>> list(@PathVariable String type) {
        List<AttributeItemResponse> data = switch (normalize(type)) {
            case "origin-categories", "origincategories" -> originCategories.findAll().stream()
                .map(o -> new AttributeItemResponse(o.id, o.nameCategoryF, null, null, o.status)).toList();
            case "categories" -> categories.findAll().stream()
                .map(c -> new AttributeItemResponse(c.id, c.getName(), c.slug, c.originCategory != null ? c.originCategory.nameCategoryF : null, c.active && c.status)).toList();
            case "colors" -> colors.findAll().stream()
                .map(c -> new AttributeItemResponse(c.id, c.nameColor, c.codeHex, null, c.status)).toList();
            case "sizes" -> sizes.findAll().stream()
                .map(s -> new AttributeItemResponse(s.id, s.code, s.code, s.description, s.status)).toList();
            case "materials" -> materials.findAll().stream()
                .map(m -> new AttributeItemResponse(m.id, m.nameMaterial, null, null, m.status)).toList();
            case "collars" -> collars.findAll().stream()
                .map(c -> new AttributeItemResponse(c.id, c.nameCollar, null, null, c.status)).toList();
            case "designs" -> designs.findAll().stream()
                .map(d -> new AttributeItemResponse(d.id, d.nameDesign, null, null, d.status)).toList();
            case "forms" -> forms.findAll().stream()
                .map(f -> new AttributeItemResponse(f.id, f.nameForm, null, null, f.status)).toList();
            case "sleeves" -> sleeves.findAll().stream()
                .map(s -> new AttributeItemResponse(s.id, s.nameSleeve, null, null, s.status)).toList();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại thuộc tính không hợp lệ: " + type);
        };
        return Map.of("data", data);
    }

    @PostMapping("/{type}")
    @Transactional
    public ResponseEntity<AttributeItemResponse> create(@PathVariable String type,
                                                        @RequestBody AttributeWriteRequest req,
                                                        Authentication auth) {
        current.requireRole(auth, "ADMIN");
        String name = req.name() != null ? req.name().trim() : "";
        if (name.isBlank() && (req.code() == null || req.code().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên thuộc tính không được để trống");
        }
        boolean st = req.status() == null || req.status();

        AttributeItemResponse resp = switch (normalize(type)) {
            case "origin-categories", "origincategories" -> {
                OriginCategory oc = originCategories.save(new OriginCategory(name, st));
                yield new AttributeItemResponse(oc.id, oc.nameCategoryF, null, null, oc.status);
            }
            case "categories" -> {
                Category cat = new Category();
                cat.name = name;
                cat.nameCategory = name;
                cat.slug = req.code() != null && !req.code().isBlank() ? req.code().trim().toLowerCase(Locale.ROOT) : slugify(name);
                cat.active = st;
                cat.status = st;
                if (req.parentId() != null) {
                    originCategories.findById(req.parentId()).ifPresent(p -> cat.originCategory = p);
                }
                Category saved = categories.save(cat);
                yield new AttributeItemResponse(saved.id, saved.getName(), saved.slug,
                    saved.originCategory != null ? saved.originCategory.nameCategoryF : null, saved.status);
            }
            case "colors" -> {
                Color c = colors.save(new Color(name, req.code() != null ? req.code().trim() : req.extra(), st));
                yield new AttributeItemResponse(c.id, c.nameColor, c.codeHex, null, c.status);
            }
            case "sizes" -> {
                String code = req.code() != null && !req.code().isBlank() ? req.code().trim() : name;
                Size s = sizes.save(new Size(code, req.extra() != null ? req.extra() : req.name(), st));
                yield new AttributeItemResponse(s.id, s.code, s.code, s.description, s.status);
            }
            case "materials" -> {
                Material m = materials.save(new Material(name, st));
                yield new AttributeItemResponse(m.id, m.nameMaterial, null, null, m.status);
            }
            case "collars" -> {
                Collar c = collars.save(new Collar(name, st));
                yield new AttributeItemResponse(c.id, c.nameCollar, null, null, c.status);
            }
            case "designs" -> {
                Design d = designs.save(new Design(name, st));
                yield new AttributeItemResponse(d.id, d.nameDesign, null, null, d.status);
            }
            case "forms" -> {
                Form f = forms.save(new Form(name, st));
                yield new AttributeItemResponse(f.id, f.nameForm, null, null, f.status);
            }
            case "sleeves" -> {
                Sleeve s = sleeves.save(new Sleeve(name, st));
                yield new AttributeItemResponse(s.id, s.nameSleeve, null, null, s.status);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại thuộc tính không hợp lệ: " + type);
        };
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PatchMapping("/{type}/{id}")
    @Transactional
    public AttributeItemResponse update(@PathVariable String type,
                                        @PathVariable Long id,
                                        @RequestBody AttributeWriteRequest req,
                                        Authentication auth) {
        current.requireRole(auth, "ADMIN");
        return switch (normalize(type)) {
            case "origin-categories", "origincategories" -> {
                OriginCategory oc = originCategories.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) oc.nameCategoryF = req.name().trim();
                if (req.status() != null) oc.status = req.status();
                originCategories.save(oc);
                yield new AttributeItemResponse(oc.id, oc.nameCategoryF, null, null, oc.status);
            }
            case "categories" -> {
                Category c = categories.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) { c.name = req.name().trim(); c.nameCategory = req.name().trim(); }
                if (req.code() != null) c.slug = req.code().trim().toLowerCase(Locale.ROOT);
                if (req.status() != null) { c.status = req.status(); c.active = req.status(); }
                if (req.parentId() != null) {
                    originCategories.findById(req.parentId()).ifPresent(p -> c.originCategory = p);
                }
                categories.save(c);
                yield new AttributeItemResponse(c.id, c.getName(), c.slug,
                    c.originCategory != null ? c.originCategory.nameCategoryF : null, c.status);
            }
            case "colors" -> {
                Color c = colors.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) c.nameColor = req.name().trim();
                if (req.code() != null) c.codeHex = req.code().trim();
                if (req.status() != null) c.status = req.status();
                colors.save(c);
                yield new AttributeItemResponse(c.id, c.nameColor, c.codeHex, null, c.status);
            }
            case "sizes" -> {
                Size s = sizes.findById(id).orElseThrow(() -> notFound(id));
                if (req.code() != null) s.code = req.code().trim();
                if (req.name() != null && (req.code() == null || req.code().isBlank())) s.code = req.name().trim();
                if (req.extra() != null) s.description = req.extra();
                if (req.status() != null) s.status = req.status();
                sizes.save(s);
                yield new AttributeItemResponse(s.id, s.code, s.code, s.description, s.status);
            }
            case "materials" -> {
                Material m = materials.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) m.nameMaterial = req.name().trim();
                if (req.status() != null) m.status = req.status();
                materials.save(m);
                yield new AttributeItemResponse(m.id, m.nameMaterial, null, null, m.status);
            }
            case "collars" -> {
                Collar c = collars.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) c.nameCollar = req.name().trim();
                if (req.status() != null) c.status = req.status();
                collars.save(c);
                yield new AttributeItemResponse(c.id, c.nameCollar, null, null, c.status);
            }
            case "designs" -> {
                Design d = designs.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) d.nameDesign = req.name().trim();
                if (req.status() != null) d.status = req.status();
                designs.save(d);
                yield new AttributeItemResponse(d.id, d.nameDesign, null, null, d.status);
            }
            case "forms" -> {
                Form f = forms.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) f.nameForm = req.name().trim();
                if (req.status() != null) f.status = req.status();
                forms.save(f);
                yield new AttributeItemResponse(f.id, f.nameForm, null, null, f.status);
            }
            case "sleeves" -> {
                Sleeve s = sleeves.findById(id).orElseThrow(() -> notFound(id));
                if (req.name() != null) s.nameSleeve = req.name().trim();
                if (req.status() != null) s.status = req.status();
                sleeves.save(s);
                yield new AttributeItemResponse(s.id, s.nameSleeve, null, null, s.status);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại thuộc tính không hợp lệ: " + type);
        };
    }

    @DeleteMapping("/{type}/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable String type,
                                       @PathVariable Long id,
                                       Authentication auth) {
        current.requireRole(auth, "ADMIN");
        switch (normalize(type)) {
            case "origin-categories", "origincategories" -> {
                OriginCategory oc = originCategories.findById(id).orElseThrow(() -> notFound(id));
                oc.status = false;
                originCategories.save(oc);
            }
            case "categories" -> {
                Category c = categories.findById(id).orElseThrow(() -> notFound(id));
                c.active = false;
                c.status = false;
                categories.save(c);
            }
            case "colors" -> {
                Color c = colors.findById(id).orElseThrow(() -> notFound(id));
                c.status = false;
                colors.save(c);
            }
            case "sizes" -> {
                Size s = sizes.findById(id).orElseThrow(() -> notFound(id));
                s.status = false;
                sizes.save(s);
            }
            case "materials" -> {
                Material m = materials.findById(id).orElseThrow(() -> notFound(id));
                m.status = false;
                materials.save(m);
            }
            case "collars" -> {
                Collar c = collars.findById(id).orElseThrow(() -> notFound(id));
                c.status = false;
                collars.save(c);
            }
            case "designs" -> {
                Design d = designs.findById(id).orElseThrow(() -> notFound(id));
                d.status = false;
                designs.save(d);
            }
            case "forms" -> {
                Form f = forms.findById(id).orElseThrow(() -> notFound(id));
                f.status = false;
                forms.save(f);
            }
            case "sleeves" -> {
                Sleeve s = sleeves.findById(id).orElseThrow(() -> notFound(id));
                s.status = false;
                sleeves.save(s);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại thuộc tính không hợp lệ: " + type);
        }
        return ResponseEntity.noContent().build();
    }

    private String normalize(String type) {
        return type.toLowerCase(Locale.ROOT).replace('_', '-').trim();
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thuộc tính có mã " + id);
    }

    private String slugify(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return normalized.isBlank() ? "danh-muc" : normalized;
    }
}
