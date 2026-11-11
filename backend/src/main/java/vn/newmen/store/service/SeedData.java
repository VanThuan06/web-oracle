package vn.newmen.store.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.newmen.store.domain.*;
import vn.newmen.store.repository.*;

@Component
public class SeedData implements CommandLineRunner {
    private final AccountRepository accounts;
    private final RoleRepository roles;
    private final EmployeeRepository employees;
    private final CustomerRepository customers;
    private final AddressRepository addresses;
    private final OriginCategoryRepository originCategories;
    private final CategoryRepository categories;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final MaterialRepository materials;
    private final CollarRepository collars;
    private final DesignRepository designs;
    private final FormRepository forms;
    private final SleeveRepository sleeves;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductImageRepository images;
    private final FaultProductRepository faultProducts;
    private final DiscountRepository discounts;
    private final ProductDiscountRepository productDiscounts;
    private final VoucherRepository vouchers;
    private final PromotionRepository promotions;
    private final CheckoutRepository checkouts;
    private final CheckoutDetailRepository checkoutDetails;
    private final CustomerOrderRepository orders;
    private final OrderLineRepository orderLines;
    private final OrderStatusHistoryRepository orderHistory;
    private final OrderReturnRepository orderReturns;
    private final ReturnDetailRepository returnDetails;
    private final ExchangeRepository exchanges;
    private final ExchangeDetailRepository exchangeDetails;
    private final ReviewRepository reviews;
    private final RatingImageRepository ratingImages;
    private final BannerRepository banners;
    private final ContactRepository contacts;
    private final InventoryMovementRepository inventory;
    private final PasswordEncoder passwordEncoder;

    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;

    public SeedData(AccountRepository accounts, RoleRepository roles, EmployeeRepository employees,
                    CustomerRepository customers, AddressRepository addresses,
                    OriginCategoryRepository originCategories, CategoryRepository categories,
                    ColorRepository colors, SizeRepository sizes, MaterialRepository materials,
                    CollarRepository collars, DesignRepository designs, FormRepository forms,
                    SleeveRepository sleeves, ProductRepository products, ProductVariantRepository variants,
                    ProductImageRepository images, FaultProductRepository faultProducts,
                    DiscountRepository discounts, ProductDiscountRepository productDiscounts,
                    VoucherRepository vouchers, PromotionRepository promotions,
                    CheckoutRepository checkouts, CheckoutDetailRepository checkoutDetails,
                    CustomerOrderRepository orders, OrderLineRepository orderLines,
                    OrderStatusHistoryRepository orderHistory, OrderReturnRepository orderReturns,
                    ReturnDetailRepository returnDetails, ExchangeRepository exchanges,
                    ExchangeDetailRepository exchangeDetails, ReviewRepository reviews,
                    RatingImageRepository ratingImages, BannerRepository banners,
                    ContactRepository contacts, InventoryMovementRepository inventory,
                    PasswordEncoder passwordEncoder,
                    @Value("${app.seed.enabled:false}") boolean enabled,
                    @Value("${app.seed.admin-email:}") String adminEmail,
                    @Value("${app.seed.admin-password:}") String adminPassword) {
        this.accounts = accounts;
        this.roles = roles;
        this.employees = employees;
        this.customers = customers;
        this.addresses = addresses;
        this.originCategories = originCategories;
        this.categories = categories;
        this.colors = colors;
        this.sizes = sizes;
        this.materials = materials;
        this.collars = collars;
        this.designs = designs;
        this.forms = forms;
        this.sleeves = sleeves;
        this.products = products;
        this.variants = variants;
        this.images = images;
        this.faultProducts = faultProducts;
        this.discounts = discounts;
        this.productDiscounts = productDiscounts;
        this.vouchers = vouchers;
        this.promotions = promotions;
        this.checkouts = checkouts;
        this.checkoutDetails = checkoutDetails;
        this.orders = orders;
        this.orderLines = orderLines;
        this.orderHistory = orderHistory;
        this.orderReturns = orderReturns;
        this.returnDetails = returnDetails;
        this.exchanges = exchanges;
        this.exchangeDetails = exchangeDetails;
        this.reviews = reviews;
        this.ratingImages = ratingImages;
        this.banners = banners;
        this.contacts = contacts;
        this.inventory = inventory;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminEmail = (adminEmail == null || adminEmail.isBlank()) ? "admin@newmen.local" : adminEmail;
        this.adminPassword = (adminPassword == null || adminPassword.length() < 10) ? "ChangeMe123!" : adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) return;
        seedAll();
    }

    @Transactional
    public void seedAll() {
        // ==============================================================
        // 1. NHÓM NGƯỜI DÙNG & PHÂN QUYỀN (4 BẢNG: Role, Employee, Customer, Address)
        // ==============================================================
        Role roleAdmin = getOrCreateRole("ADMIN", "Quản trị viên toàn hệ thống");
        Role roleStaff = getOrCreateRole("STAFF", "Nhân viên bán hàng & vận hành");
        Role roleCustomer = getOrCreateRole("CUSTOMER", "Khách hàng mua sắm");

        // 1.1 Admin Employee Account
        Account adminAcc = accounts.findByEmailIgnoreCase(adminEmail).orElseGet(() -> {
            Account acc = new Account();
            acc.email = adminEmail.toLowerCase();
            acc.passwordHash = passwordEncoder.encode(adminPassword);
            acc.fullName = "Nguyễn Thành Hiếu (Quản trị viên)";
            acc.phone = "0988773373";
            acc.role = "ADMIN";
            acc.active = true;
            return accounts.save(acc);
        });

        if (!employees.existsByEmailIgnoreCase(adminEmail)) {
            Employee emp = new Employee();
            emp.name = adminAcc.fullName;
            emp.email = adminAcc.email;
            emp.phone = adminAcc.phone;
            emp.password = adminAcc.passwordHash;
            emp.role = roleAdmin;
            emp.status = true;
            emp.address = "12 Lê Thái Tổ, Hoàn Kiếm, Hà Nội";
            emp.birthDate = LocalDate.of(1995, 5, 20);
            emp.account = adminAcc;
            employees.save(emp);
        }

        // 1.2 Staff Employees (Bán hàng, Kho, Chăm sóc KH)
        seedStaffEmployee("staff@newmen.local", "Nguyễn Văn Nhân (NV Thu ngân POS)", "0912345678",
            "34 Cầu Giấy, Hà Nội", LocalDate.of(1998, 10, 15), roleStaff);
        seedStaffEmployee("kho.le@newmen.local", "Lê Hoàng Kho (Điều phối kho)", "0915678901",
            "56 Nguyễn Trãi, Thanh Xuân, Hà Nội", LocalDate.of(1996, 3, 12), roleStaff);
        seedStaffEmployee("tuvan.mai@newmen.local", "Mai Phương Thảo (CSKH Online)", "0918765432",
            "78 Xã Đàn, Đống Đa, Hà Nội", LocalDate.of(2000, 7, 25), roleStaff);

        // 1.3 Customers & Addresses
        // Sample Customer "hieu" (Mẫu chuẩn trong Đồ án Hình 3.5 & 3.6)
        String custEmail = "hieu@newmen.local";
        Account custAcc = accounts.findByEmailIgnoreCase(custEmail).orElseGet(() -> {
            Account acc = new Account();
            acc.email = custEmail;
            acc.passwordHash = passwordEncoder.encode("ChangeMe123!");
            acc.fullName = "hieu";
            acc.phone = "0988377373";
            acc.role = "CUSTOMER";
            acc.active = true;
            return accounts.save(acc);
        });
        Customer customerHieu = customers.findByEmailIgnoreCase(custEmail).orElseGet(() -> {
            Customer c = new Customer();
            c.name = "hieu";
            c.email = custAcc.email;
            c.phoneNumber = custAcc.phone;
            c.password = custAcc.passwordHash;
            c.role = roleCustomer;
            c.points = 180;
            c.status = true;
            c.birthDate = LocalDate.of(2002, 8, 18);
            c.account = custAcc;
            return customers.save(c);
        });

        if (addresses.findByCustomerId(customerHieu.id).isEmpty()) {
            Address addr = new Address();
            addr.account = custAcc;
            addr.customer = customerHieu;
            addr.recipientName = "hieu";
            addr.phone = "0988377373";
            addr.line1 = "123, Phường Bưởi, Quận Tây Hồ";
            addr.address = "123, Phường Bưởi";
            addr.ward = "Phường Bưởi";
            addr.wardName = "Phường Bưởi";
            addr.district = "Quận Tây Hồ";
            addr.districtName = "Quận Tây Hồ";
            addr.province = "Hà Nội";
            addr.cityName = "Hà Nội";
            addr.other = "Giao giờ hành chính";
            addr.isDefault = true;
            addr.status = true;
            addresses.save(addr);
        }

        // Additional Customers with full addresses across Vietnam
        Customer custLong = seedCustomer("Trần Minh Long", "long.tran@gmail.com", "0977223344",
            "Số 45 Lê Văn Lương", "Phường Nhân Chính", "Quận Thanh Xuân", "Hà Nội", roleCustomer, 95);
        Customer custDuc = seedCustomer("Lê Văn Đức", "duc.le@outlook.com", "0933556677",
            "Số 88 Kim Mã", "Phường Kim Mã", "Quận Ba Đình", "Hà Nội", roleCustomer, 240);
        Customer custBao = seedCustomer("Hoàng Quốc Bảo", "bao.hoang@gmail.com", "0911889900",
            "Số 12 Nguyễn Trãi", "Phường Thượng Đình", "Quận Thanh Xuân", "Hà Nội", roleCustomer, 40);
        Customer custNam = seedCustomer("Nguyễn Hải Nam", "nam.nguyen@company.vn", "0966445566",
            "Số 250 Hoàng Hoa Thám", "Phường Thụy Khuê", "Quận Tây Hồ", "Hà Nội", roleCustomer, 320);
        Customer custTuan = seedCustomer("Vũ Minh Tuấn", "tuan.vu@saigon.vn", "0903123789",
            "Số 15 Nguyễn Thị Minh Khai", "Phường Bến Nghé", "Quận 1", "TP. Hồ Chí Minh", roleCustomer, 190);

        // ==============================================================
        // 2. NHÓM THUỘC TÍNH SẢN PHẨM & DANH MỤC (9 BẢNG)
        // ==============================================================
        OriginCategory origFormal = getOrCreateOriginCategory("Thời Trang Công Sở & Quý Ông");
        OriginCategory origCasual = getOrCreateOriginCategory("Thời Trang Năng Động & Dạo Phố");
        OriginCategory origLuxury = getOrCreateOriginCategory("Bộ Sưu Tập Cao Cấp Signature");
        OriginCategory origWinter = getOrCreateOriginCategory("Thời Trang Thu Đông & Giữ Nhiệt");

        // 9 Categories
        Category catShirtWork = getOrCreateCategory("Áo sơ mi công sở", "ao-so-mi-cong-so", origFormal);
        Category catShirtCasual = getOrCreateCategory("Áo sơ mi họa tiết / đũi", "ao-so-mi-hoa-tiet", origCasual);
        Category catPoloSport = getOrCreateCategory("Áo polo thể thao & dạo phố", "ao-polo-the-thao", origCasual);
        Category catPoloSilk = getOrCreateCategory("Áo polo lụa lạnh công sở", "ao-polo-lua-lanh", origLuxury);
        Category catTeeCotton = getOrCreateCategory("Áo thun cotton Compact", "ao-thun-cotton", origCasual);
        Category catTeeGraphic = getOrCreateCategory("Áo thun Graphic thời trang", "ao-thun-graphic", origCasual);
        Category catJacket = getOrCreateCategory("Áo khoác gió & Bomber", "ao-khoac-gio-bomber", origWinter);
        Category catKnit = getOrCreateCategory("Áo len & Nỉ dệt kim", "ao-len-ni", origWinter);
        Category catBlazer = getOrCreateCategory("Áo vest & Blazer nam", "ao-vest-blazer", origLuxury);

        // 10 Colors
        Color colWhite = getOrCreateColor("Trắng", "#FFFFFF");
        Color colBlack = getOrCreateColor("Đen", "#111111");
        Color colNavy = getOrCreateColor("Xanh Navy", "#1A2B4C");
        Color colMoss = getOrCreateColor("Xanh Rêu", "#4A5D4E");
        Color colGray = getOrCreateColor("Xám Tiêu", "#8A8D8F");
        Color colBeige = getOrCreateColor("Be / Kem", "#E8DCCA");
        Color colBrown = getOrCreateColor("Nâu Đất", "#5C4033");
        Color colBurgundy = getOrCreateColor("Đỏ Đô / Bóc Đô", "#7A1C28");
        Color colPastelBlue = getOrCreateColor("Xanh Pastel", "#A0C4E2");
        Color colOchre = getOrCreateColor("Vàng Bò", "#C68B59");

        // 5 Sizes
        Size sizeS = getOrCreateSize("S", "Dành cho nam dưới 58kg, cao 1m60 - 1m65");
        Size sizeM = getOrCreateSize("M", "Dành cho nam 58 - 66kg, cao 1m65 - 1m70");
        Size sizeL = getOrCreateSize("L", "Dành cho nam 66 - 74kg, cao 1m70 - 1m76");
        Size sizeXL = getOrCreateSize("XL", "Dành cho nam 74 - 83kg, cao 1m75 - 1m82");
        Size sizeXXL = getOrCreateSize("XXL", "Dành cho nam trên 83kg, cao trên 1m80");

        // 8 Materials
        Material matCotton = getOrCreateMaterial("Cotton Compact 100% thoáng mát");
        Material matBamboo = getOrCreateMaterial("Sợi Tre Bamboo kháng khuẩn khử mùi");
        Material matOxford = getOrCreateMaterial("Vải Oxford dệt chéo đứng phom");
        Material matPique = getOrCreateMaterial("Vải dệt Piqué mắt chim cao cấp");
        Material matPoly = getOrCreateMaterial("Poly Dù 2 lớp trượt nước cản gió");
        Material matModal = getOrCreateMaterial("Lụa Băng Modal co giãn 4 chiều");
        Material matLinen = getOrCreateMaterial("Đũi Linen tự nhiên thoáng khí");
        Material matWool = getOrCreateMaterial("Len lông cừu Wool dệt kim");

        // 7 Collars
        Collar colButtonDown = getOrCreateCollar("Cổ bẻ cài khuy (Button-down)");
        Collar colRegular = getOrCreateCollar("Cổ bẻ Regular tiêu chuẩn");
        Collar colPolo = getOrCreateCollar("Cổ polo bo dệt kẻ chỉ");
        Collar colMandarin = getOrCreateCollar("Cổ tàu / Cổ trụ thanh lịch");
        Collar colRound = getOrCreateCollar("Cổ tròn thể thao (Crew neck)");
        Collar colVNeck = getOrCreateCollar("Cổ tim trẻ trung (V-neck)");
        Collar colDanton = getOrCreateCollar("Cổ bẻ danton thời trang");

        // 6 Designs
        Design desBasic = getOrCreateDesign("Trơn tối giản (Minimalism)");
        Design desStripe = getOrCreateDesign("Kẻ sọc caro nhuyễn");
        Design desEmbroider = getOrCreateDesign("Thêu logo ngực tinh tế");
        Design desTypo = getOrCreateDesign("In typo typography nghệ thuật");
        Design desBorder = getOrCreateDesign("Phối viền bo cổ và gấu tay");
        Design desJacquard = getOrCreateDesign("Họa tiết chìm Jacquard sang trọng");

        // 4 Forms
        Form formSlim = getOrCreateForm("Slim Fit (Ôm nhẹ tôn dáng)");
        Form formRegular = getOrCreateForm("Regular Fit (Vừa vặn thoải mái)");
        Form formRelaxed = getOrCreateForm("Relaxed Fit (Phóng khoáng hiện đại)");
        Form formOversize = getOrCreateForm("Oversized (Cá tính đường phố)");

        // 3 Sleeves
        Sleeve slvShort = getOrCreateSleeve("Tay ngắn");
        Sleeve slvLong = getOrCreateSleeve("Tay dài");
        Sleeve slvHalf = getOrCreateSleeve("Tay lỡ");

        // ==============================================================
        // 3. NHÓM SẢN PHẨM & BIẾN THỂ KHO (12 SẢN PHẨM PHONG PHÚ, 40+ BIẾN THỂ)
        // ==============================================================
        // Product 1: áo sơ mi đỏ bóc do 9999 (Đồ án Hình 3.1 & 3.5 & 3.6)
        Product p1 = getOrCreateProduct("SP0000006", "áo sơ mi đỏ bóc do 9999", "ao-so-mi-do-boc-do-9999",
            catShirtWork, matOxford, colButtonDown, desBasic, formSlim, slvLong,
            "Áo sơ mi dáng công sở sang trọng, sắc đỏ bóc đô tôn da phái mạnh, chất vải oxford dệt đứng phom.",
            new BigDecimal("500000"), 220.0, "/media/product-oxford-red.jpg");
        createVariantIfAbsent(p1, "SP0000006-XANH-M", "8931234560078", "Xanh", "M", new BigDecimal("500000"), new BigDecimal("320000"), 123, colNavy, sizeM);
        createVariantIfAbsent(p1, "SP0000006-XANH-L", "8931234560079", "Xanh", "L", new BigDecimal("500000"), new BigDecimal("320000"), 85, colNavy, sizeL);
        createVariantIfAbsent(p1, "SP0000006-TRANG-M", "8931234560080", "Trắng", "M", new BigDecimal("500000"), new BigDecimal("320000"), 90, colWhite, sizeM);

        // Product 2: Áo sơ mi Oxford Classic trắng
        Product p2 = getOrCreateProduct("NM-SM-002", "Áo sơ mi Oxford Classic trắng", "ao-so-mi-oxford-classic-trang",
            catShirtWork, matOxford, colButtonDown, desBasic, formRegular, slvLong,
            "Chất vải Oxford dệt sợi đôi cao cấp, đứng phom, cổ áo đứng giữ cravat chuẩn mực cho quý ông công sở.",
            new BigDecimal("590000"), 250.0, "/media/product-shirt-white.jpg");
        createVariantIfAbsent(p2, "NM-SM-002-TR-M", "893200100101", "Trắng", "M", new BigDecimal("590000"), new BigDecimal("380000"), 45, colWhite, sizeM);
        createVariantIfAbsent(p2, "NM-SM-002-TR-L", "893200100102", "Trắng", "L", new BigDecimal("590000"), new BigDecimal("380000"), 40, colWhite, sizeL);
        createVariantIfAbsent(p2, "NM-SM-002-XN-M", "893200100103", "Xanh Navy", "M", new BigDecimal("590000"), new BigDecimal("380000"), 35, colNavy, sizeM);
        createVariantIfAbsent(p2, "NM-SM-002-XN-L", "893200100104", "Xanh Navy", "L", new BigDecimal("590000"), new BigDecimal("380000"), 30, colNavy, sizeL);

        // Product 3: Áo sơ mi Linen đũi cổ tàu Mandarin
        Product p3 = getOrCreateProduct("NM-SM-003", "Áo sơ mi Linen đũi cổ tàu Mandarin", "ao-so-mi-linen-dui-co-tau",
            catShirtCasual, matLinen, colMandarin, desBasic, formRelaxed, slvLong,
            "Chất liệu 100% đũi tự nhiên mộc mạc, thấm hút mồ hôi tối đa, phong cách thư thái lãng tử.",
            new BigDecimal("650000"), 230.0, "/media/product-shirt-linen.jpg");
        createVariantIfAbsent(p3, "NM-SM-003-BE-M", "893200200101", "Be / Kem", "M", new BigDecimal("650000"), new BigDecimal("420000"), 28, colBeige, sizeM);
        createVariantIfAbsent(p3, "NM-SM-003-BE-L", "893200200102", "Be / Kem", "L", new BigDecimal("650000"), new BigDecimal("420000"), 34, colBeige, sizeL);
        createVariantIfAbsent(p3, "NM-SM-003-TR-L", "893200200103", "Trắng", "L", new BigDecimal("650000"), new BigDecimal("420000"), 25, colWhite, sizeL);

        // Product 4: Áo polo Piqué Signature thêu ngực
        Product p4 = getOrCreateProduct("NM-PL-001", "Áo polo Piqué Signature thêu ngực", "ao-polo-pique-signature",
            catPoloSport, matPique, colPolo, desEmbroider, formSlim, slvShort,
            "Vải dệt piqué mắt chim thoáng khí, bo cổ chống xoăn giãn, thêu logo NewMen sang trọng trước ngực.",
            new BigDecimal("420000"), 200.0, "/media/product-polo-navy.jpg");
        createVariantIfAbsent(p4, "NM-PL-001-NV-M", "893300100101", "Xanh Navy", "M", new BigDecimal("420000"), new BigDecimal("260000"), 60, colNavy, sizeM);
        createVariantIfAbsent(p4, "NM-PL-001-NV-L", "893300100102", "Xanh Navy", "L", new BigDecimal("420000"), new BigDecimal("260000"), 50, colNavy, sizeL);
        createVariantIfAbsent(p4, "NM-PL-001-RE-M", "893300100103", "Xanh Rêu", "M", new BigDecimal("420000"), new BigDecimal("260000"), 40, colMoss, sizeM);
        createVariantIfAbsent(p4, "NM-PL-001-DE-L", "893300100104", "Đen", "L", new BigDecimal("420000"), new BigDecimal("260000"), 35, colBlack, sizeL);

        // Product 5: Áo polo lụa Modal công sở bo viền
        Product p5 = getOrCreateProduct("NM-PL-002", "Áo polo lụa Modal công sở bo viền", "ao-polo-lua-modal-cong-so",
            catPoloSilk, matModal, colPolo, desBorder, formRegular, slvShort,
            "Sợi lụa lạnh mát rượi chạm vào da, phối viền cổ thanh lịch, không nhăn và giữ form tuyệt đối.",
            new BigDecimal("480000"), 210.0, "/media/product-polo-modal.jpg");
        createVariantIfAbsent(p5, "NM-PL-002-XT-M", "893300200101", "Xám Tiêu", "M", new BigDecimal("480000"), new BigDecimal("310000"), 55, colGray, sizeM);
        createVariantIfAbsent(p5, "NM-PL-002-XT-L", "893300200102", "Xám Tiêu", "L", new BigDecimal("480000"), new BigDecimal("310000"), 48, colGray, sizeL);
        createVariantIfAbsent(p5, "NM-PL-002-TR-M", "893300200103", "Trắng", "M", new BigDecimal("480000"), new BigDecimal("310000"), 62, colWhite, sizeM);

        // Product 6: Áo thun cotton Compact Essential
        Product p6 = getOrCreateProduct("NM-TS-001", "Áo thun cotton Compact Essential", "ao-thun-cotton-compact-essential",
            catTeeCotton, matCotton, colRound, desBasic, formRegular, slvShort,
            "Sợi cotton chải kỹ mịn màng, co giãn 4 chiều, không xù lông và thấm hút mồ hôi tối đa.",
            new BigDecimal("250000"), 180.0, "/media/product-tee-black.jpg");
        createVariantIfAbsent(p6, "NM-TS-001-DE-M", "893100100101", "Đen", "M", new BigDecimal("250000"), new BigDecimal("140000"), 80, colBlack, sizeM);
        createVariantIfAbsent(p6, "NM-TS-001-DE-L", "893100100102", "Đen", "L", new BigDecimal("250000"), new BigDecimal("140000"), 75, colBlack, sizeL);
        createVariantIfAbsent(p6, "NM-TS-001-TR-M", "893100100103", "Trắng", "M", new BigDecimal("250000"), new BigDecimal("140000"), 90, colWhite, sizeM);
        createVariantIfAbsent(p6, "NM-TS-001-TR-L", "893100100104", "Trắng", "L", new BigDecimal("250000"), new BigDecimal("140000"), 85, colWhite, sizeL);

        // Product 7: Áo thun Graphic NewMen Heritage
        Product p7 = getOrCreateProduct("NM-TS-002", "Áo thun Graphic NewMen Heritage", "ao-thun-graphic-newmen-heritage",
            catTeeGraphic, matCotton, colRound, desTypo, formOversize, slvShort,
            "Thiết kế graphic biểu tượng di sản thương hiệu NewMen Hà Nội, form rộng trẻ trung cá tính.",
            new BigDecimal("290000"), 190.0, "/media/product-tee-graphic.jpg");
        createVariantIfAbsent(p7, "NM-TS-002-BE-M", "893100200101", "Be / Kem", "M", new BigDecimal("290000"), new BigDecimal("160000"), 40, colBeige, sizeM);
        createVariantIfAbsent(p7, "NM-TS-002-BE-L", "893100200102", "Be / Kem", "L", new BigDecimal("290000"), new BigDecimal("160000"), 38, colBeige, sizeL);
        createVariantIfAbsent(p7, "NM-TS-002-DE-XL", "893100200103", "Đen", "XL", new BigDecimal("290000"), new BigDecimal("160000"), 45, colBlack, sizeXL);

        // Product 8: Áo khoác gió Urban Shell 2 lớp
        Product p8 = getOrCreateProduct("NM-JK-001", "Áo khoác gió Urban Shell 2 lớp", "ao-khoac-gio-urban-shell",
            catJacket, matPoly, colMandarin, desBasic, formRegular, slvLong,
            "Vải poly 2 lớp tráng màng cản gió, chống mưa phùn nhẹ, khóa kéo YKK bền bỉ và túi tiện ích.",
            new BigDecimal("850000"), 350.0, "/media/product-jacket-wind.jpg");
        createVariantIfAbsent(p8, "NM-JK-001-DE-L", "893400100101", "Đen", "L", new BigDecimal("850000"), new BigDecimal("550000"), 30, colBlack, sizeL);
        createVariantIfAbsent(p8, "NM-JK-001-DE-XL", "893400100102", "Đen", "XL", new BigDecimal("850000"), new BigDecimal("550000"), 25, colBlack, sizeXL);
        createVariantIfAbsent(p8, "NM-JK-001-XT-L", "893400100103", "Xám Tiêu", "L", new BigDecimal("850000"), new BigDecimal("550000"), 28, colGray, sizeL);

        // Product 9: Áo khoác Bomber nỉ phối da lộn
        Product p9 = getOrCreateProduct("NM-JK-002", "Áo khoác Bomber nỉ phối da lộn", "ao-khoac-bomber-ni-da-lon",
            catJacket, matPoly, colDanton, desBorder, formSlim, slvLong,
            "Phối da lộn cổ điển, bo dệt gấu co giãn dày dặn, tôn vóc dáng nam tính mạnh mẽ.",
            new BigDecimal("950000"), 420.0, "/media/product-jacket-bomber.jpg");
        createVariantIfAbsent(p9, "NM-JK-002-ND-M", "893400200101", "Nâu Đất", "M", new BigDecimal("950000"), new BigDecimal("600000"), 20, colBrown, sizeM);
        createVariantIfAbsent(p9, "NM-JK-002-ND-L", "893400200102", "Nâu Đất", "L", new BigDecimal("950000"), new BigDecimal("600000"), 22, colBrown, sizeL);
        createVariantIfAbsent(p9, "NM-JK-002-DE-L", "893400200103", "Đen", "L", new BigDecimal("950000"), new BigDecimal("600000"), 25, colBlack, sizeL);

        // Product 10: Áo len dệt kim cổ tròn Merino Wool
        Product p10 = getOrCreateProduct("NM-LN-001", "Áo len dệt kim cổ tròn Merino Wool", "ao-len-det-kim-merino-wool",
            catKnit, matWool, colRound, desBasic, formRegular, slvLong,
            "100% sợi len lông cừu Merino mềm xốp nhẹ tênh, giữ ấm vượt trội mà không gây ngứa dặm.",
            new BigDecimal("720000"), 300.0, "/media/product-knit-wool.jpg");
        createVariantIfAbsent(p10, "NM-LN-001-NV-M", "893500100101", "Xanh Navy", "M", new BigDecimal("720000"), new BigDecimal("450000"), 35, colNavy, sizeM);
        createVariantIfAbsent(p10, "NM-LN-001-XT-L", "893500100102", "Xám Tiêu", "L", new BigDecimal("720000"), new BigDecimal("450000"), 30, colGray, sizeL);
        createVariantIfAbsent(p10, "NM-LN-001-ND-L", "893500100103", "Nâu Đất", "L", new BigDecimal("720000"), new BigDecimal("450000"), 25, colBrown, sizeL);

        // Product 11: Áo Blazer may đo hiện đại Modern Tailor
        Product p11 = getOrCreateProduct("NM-BZ-001", "Áo Blazer may đo hiện đại Modern Tailor", "ao-blazer-modern-tailor",
            catBlazer, matOxford, colRegular, desBasic, formSlim, slvLong,
            "Dáng áo blazer may đo cách tân không đệm vai cứng nhắc, trẻ trung mặc đi làm hoặc tiệc tối.",
            new BigDecimal("1450000"), 480.0, "/media/product-blazer-black.jpg");
        createVariantIfAbsent(p11, "NM-BZ-001-DE-M", "893600100101", "Đen", "M", new BigDecimal("1450000"), new BigDecimal("900000"), 18, colBlack, sizeM);
        createVariantIfAbsent(p11, "NM-BZ-001-DE-L", "893600100102", "Đen", "L", new BigDecimal("1450000"), new BigDecimal("900000"), 20, colBlack, sizeL);
        createVariantIfAbsent(p11, "NM-BZ-001-NV-L", "893600100103", "Xanh Navy", "L", new BigDecimal("1450000"), new BigDecimal("900000"), 15, colNavy, sizeL);

        // Product 12: Áo sơ mi kẻ sọc nhuyễn sợi tre Bamboo
        Product p12 = getOrCreateProduct("NM-SM-004", "Áo sơ mi kẻ sọc nhuyễn sợi tre Bamboo", "ao-so-mi-ke-soc-bamboo",
            catShirtWork, matBamboo, colButtonDown, desStripe, formSlim, slvLong,
            "Sợi tre Bamboo thiên nhiên khử mùi, bề mặt dệt kẻ nhuyễn sọc tạo hiệu ứng thon gọn vóc dáng.",
            new BigDecimal("620000"), 230.0, "/media/product-shirt-stripe.jpg");
        createVariantIfAbsent(p12, "NM-SM-004-XP-M", "893200300101", "Xanh Pastel", "M", new BigDecimal("620000"), new BigDecimal("390000"), 40, colPastelBlue, sizeM);
        createVariantIfAbsent(p12, "NM-SM-004-XP-L", "893200300102", "Xanh Pastel", "L", new BigDecimal("620000"), new BigDecimal("390000"), 42, colPastelBlue, sizeL);
        createVariantIfAbsent(p12, "NM-SM-004-TR-L", "893200300103", "Trắng", "L", new BigDecimal("620000"), new BigDecimal("390000"), 38, colWhite, sizeL);

        // ==============================================================
        // 4. SẢN PHẨM LỖI & HỦY KHO (FAULTPRODUCT - BẢNG 2.26)
        // ==============================================================
        if (faultProducts.count() < 4) {
            ProductVariant vFault1 = variants.findBySku("SP0000006-XANH-M").orElse(null);
            if (vFault1 != null && faultProducts.findByProductDetailId(vFault1.id).isEmpty()) {
                faultProducts.save(new FaultProduct(vFault1, 2, "Lỗi bung chỉ nẹp tay áo trong quá trình kiểm kê kho", 1));
            }
            ProductVariant vFault2 = variants.findBySku("NM-PL-001-NV-M").orElse(null);
            if (vFault2 != null && faultProducts.findByProductDetailId(vFault2.id).isEmpty()) {
                faultProducts.save(new FaultProduct(vFault2, 1, "Ố màu ở gấu áo do ẩm bao bì khi vận chuyển đường bộ", 1));
            }
            ProductVariant vFault3 = variants.findBySku("NM-TS-001-TR-L").orElse(null);
            if (vFault3 != null && faultProducts.findByProductDetailId(vFault3.id).isEmpty()) {
                faultProducts.save(new FaultProduct(vFault3, 3, "Lỗi dệt xước sợi vải mặt sau áo thun", 1));
            }
            ProductVariant vFault4 = variants.findBySku("NM-JK-001-DE-L").orElse(null);
            if (vFault4 != null && faultProducts.findByProductDetailId(vFault4.id).isEmpty()) {
                faultProducts.save(new FaultProduct(vFault4, 1, "Khóa kéo YKK bị kẹt răng cưa không khép kín", 1));
            }
        }

        // ==============================================================
        // 5. KHUYẾN MÃI & PHIẾU GIẢM GIÁ (3 BẢNG: Discount, ProductDiscount, Voucher)
        // ==============================================================
        Discount discSummer = getOrCreateDiscount("HÈ TỚI MẶC MỚI LÀ VUI - SALE UPTO 49%",
            "Chương trình khuyến mãi hè sôi động dành cho toàn bộ áo nam NewMen",
            LocalDateTime.now().minusDays(10), LocalDateTime.now().plusMonths(3));
        Discount discGentleman = getOrCreateDiscount("TUẦN LỄ VÀNG SƠ MI CÔNG SỞ QUÝ ÔNG",
            "Ưu đãi đặc quyền dành riêng cho dòng áo sơ mi may đo cao cấp",
            LocalDateTime.now().minusDays(5), LocalDateTime.now().plusDays(20));
        Discount discGrandOpening = getOrCreateDiscount("ĐẠI TIỆC KHAI TRƯƠNG NEWMEN STORE HÀ NỘI",
            "Mừng khai trương diện mạo showroom 12 Lê Thái Tổ",
            LocalDateTime.now().minusMonths(1), LocalDateTime.now().minusDays(1));

        if (productDiscounts.count() < 10) {
            List<Product> allP = products.findAll();
            for (int i = 0; i < allP.size(); i++) {
                Product p = allP.get(i);
                if (i % 2 == 0) {
                    productDiscounts.save(new ProductDiscount(15, true, p, discSummer));
                }
                if (p.category != null && p.category.slug != null && p.category.slug.contains("so-mi")) {
                    productDiscounts.save(new ProductDiscount(10, true, p, discGentleman));
                }
            }
        }

        // 6 Vouchers
        Voucher v10 = getOrCreateVoucher("NEWMEN10", "Giảm 10% đơn hàng",
            "Áp dụng cho đơn từ 500.000đ, giảm tối đa 150.000đ",
            10, new BigDecimal("500000"), new BigDecimal("150000"), 0);
        Voucher vWelcome = getOrCreateVoucher("WELCOME50", "Ưu đãi khách hàng mới",
            "Giảm trực tiếp 50.000đ cho đơn đầu tiên từ 300.000đ",
            50000, new BigDecimal("300000"), new BigDecimal("50000"), 1);
        Voucher vSummer = getOrCreateVoucher("SUMMER20", "Ưu đãi mùa hè rực rỡ",
            "Giảm 20% cho đơn hàng từ 700.000đ, tối đa 200.000đ",
            20, new BigDecimal("700000"), new BigDecimal("200000"), 0);
        Voucher vFreeship = getOrCreateVoucher("FREESHIP", "Miễn phí vận chuyển toàn quốc",
            "Áp dụng cho mọi đơn hàng từ 499.000đ",
            30000, new BigDecimal("499000"), new BigDecimal("30000"), 1);
        Voucher vVip = getOrCreateVoucher("VIPMEMBER", "Đặc quyền thành viên VIP",
            "Giảm 15% cho hóa đơn từ 800.000đ, giảm tối đa 300.000đ",
            15, new BigDecimal("800000"), new BigDecimal("300000"), 0);
        Voucher vPolo = getOrCreateVoucher("POLO100K", "Combo 2 Áo Polo tặng 100K",
            "Giảm ngay 100.000đ khi đặt mua áo polo từ 600.000đ",
            100000, new BigDecimal("600000"), new BigDecimal("100000"), 1);

        // Keep legacy Promotion entity synced
        if (promotions.count() == 0) {
            Promotion p = new Promotion();
            p.name = v10.name;
            p.code = v10.code;
            p.description = v10.description;
            p.percentOff = v10.discount;
            p.minimumOrder = v10.min;
            p.maximumDiscount = v10.max;
            p.startsAt = v10.startDate;
            p.endsAt = v10.endDate;
            promotions.save(p);
        }

        // ==============================================================
        // 6. ĐƠN HÀNG TOÀN DIỆN (8 BẢNG - ĐỦ 7 TRẠNG THÁI + CẢ ONLINE & POS)
        // ==============================================================
        ProductVariant vShirtRed = variants.findBySku("SP0000006-XANH-M").orElse(null);
        ProductVariant vShirtWhite = variants.findBySku("NM-SM-002-TR-M").orElse(null);
        ProductVariant vPoloNavy = variants.findBySku("NM-PL-001-NV-M").orElse(null);
        ProductVariant vTeeBlack = variants.findBySku("NM-TS-001-DE-M").orElse(null);
        ProductVariant vJacketBlack = variants.findBySku("NM-JK-001-DE-L").orElse(null);
        ProductVariant vBlazerBlack = variants.findBySku("NM-BZ-001-DE-M").orElse(null);

        if (orders.count() < 9 && vShirtRed != null && vShirtWhite != null && vPoloNavy != null) {
            // Đơn 1: HD0000000010 - Trạng thái: PENDING (Chờ duyệt) - Online [Theo đồ án]
            seedOrder("HD0000000010", customerHieu, custAcc, "hieu", "0988377373", "hieu@newmen.local",
                "123, Phường Bưởi, Quận Tây Hồ, Hà Nội", "123, Phường Bưởi", "Phường Bưởi", "Quận Tây Hồ", "Hà Nội",
                "COD", "COD", "UNPAID", "ONLINE", "PENDING",
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("20900"), new BigDecimal("1020900"),
                null, "Giao buổi sáng trước 11h", LocalDateTime.now().minusHours(2),
                List.of(new OrderLineItem(vShirtRed, 2, new BigDecimal("500000"), "Màu xanh, size M")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Đơn hàng online đặt thành công qua website", "hieu@newmen.local")));

            // Đơn 2: HD0000000009 - Trạng thái: CONFIRMED (Đã duyệt) - Online
            seedOrder("HD0000000009", custLong, custLong.account, "Trần Minh Long", "0977223344", "long.tran@gmail.com",
                "Số 45 Lê Văn Lương, Phường Nhân Chính, Quận Thanh Xuân, Hà Nội", "Số 45 Lê Văn Lương", "Phường Nhân Chính", "Quận Thanh Xuân", "Hà Nội",
                "BANK_TRANSFER", "CHUYEN_KHOAN", "PAID", "ONLINE", "CONFIRMED",
                new BigDecimal("590000"), new BigDecimal("59000"), BigDecimal.ZERO, new BigDecimal("531000"),
                v10, "Đã chuyển khoản VietQR", LocalDateTime.now().minusHours(5),
                List.of(new OrderLineItem(vShirtWhite, 1, new BigDecimal("590000"), "Size M màu trắng")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Đặt hàng thành công", "long.tran@gmail.com"),
                        new StatusHistoryRecord("PENDING", "CONFIRMED", "Kế toán xác nhận đã nhận tiền chuyển khoản", "staff@newmen.local")));

            // Đơn 3: HD0000000008 - Trạng thái: PREPARING (Đang chuẩn bị hàng) - Online
            seedOrder("HD0000000008", custDuc, custDuc.account, "Lê Văn Đức", "0933556677", "duc.le@outlook.com",
                "Số 88 Kim Mã, Phường Kim Mã, Quận Ba Đình, Hà Nội", "Số 88 Kim Mã", "Phường Kim Mã", "Quận Ba Đình", "Hà Nội",
                "BANK_TRANSFER", "CHUYEN_KHOAN", "PAID", "ONLINE", "PREPARING",
                new BigDecimal("840000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("840000"),
                null, "Đóng gói cẩn thận hộp quà", LocalDateTime.now().minusHours(8),
                List.of(new OrderLineItem(vPoloNavy, 2, new BigDecimal("420000"), "Áo polo thêu ngực")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Đã nhận đơn", "duc.le@outlook.com"),
                        new StatusHistoryRecord("PENDING", "CONFIRMED", "Đã duyệt đơn", "staff@newmen.local"),
                        new StatusHistoryRecord("CONFIRMED", "PREPARING", "Kho lấy hàng đóng gói nẹp hộp", "kho.le@newmen.local")));

            // Đơn 4: HD0000000007 - Trạng thái: READY_TO_SHIP (Chờ giao hàng) - Online
            seedOrder("HD0000000007", custBao, custBao.account, "Hoàng Quốc Bảo", "0911889900", "bao.hoang@gmail.com",
                "Số 12 Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Hà Nội", "Số 12 Nguyễn Trãi", "Phường Thượng Đình", "Quận Thanh Xuân", "Hà Nội",
                "COD", "COD", "UNPAID", "ONLINE", "READY_TO_SHIP",
                new BigDecimal("650000"), BigDecimal.ZERO, new BigDecimal("25000"), new BigDecimal("675000"),
                null, "Giao trong ngày", LocalDateTime.now().minusHours(12),
                List.of(new OrderLineItem(vShirtWhite, 1, new BigDecimal("590000"), "Sơ mi Oxford")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Đã đặt", "bao.hoang@gmail.com"),
                        new StatusHistoryRecord("PENDING", "CONFIRMED", "Xác nhận địa chỉ giao", "staff@newmen.local"),
                        new StatusHistoryRecord("CONFIRMED", "PREPARING", "Đã đóng gói xong", "kho.le@newmen.local"),
                        new StatusHistoryRecord("PREPARING", "READY_TO_SHIP", "Đã in phiếu vận đơn, chờ bưu tá lấy", "kho.le@newmen.local")));

            // Đơn 5: HD0000000006 - Trạng thái: SHIPPING (Đang giao hàng) - Online
            seedOrder("HD0000000006", custTuan, custTuan.account, "Vũ Minh Tuấn", "0903123789", "tuan.vu@saigon.vn",
                "Số 15 Nguyễn Thị Minh Khai, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", "Số 15 Nguyễn Thị Minh Khai", "Phường Bến Nghé", "Quận 1", "TP. Hồ Chí Minh",
                "BANK_TRANSFER", "CHUYEN_KHOAN", "PAID", "ONLINE", "SHIPPING",
                new BigDecimal("950000"), new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("850000"),
                vSummer, "Giao hàng chuyển phát nhanh đường bay", LocalDateTime.now().minusDays(1),
                List.of(new OrderLineItem(vJacketBlack != null ? vJacketBlack : vPoloNavy, 1, new BigDecimal("850000"), "Áo khoác gió")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Khách đặt đơn liên tỉnh", "tuan.vu@saigon.vn"),
                        new StatusHistoryRecord("PENDING", "CONFIRMED", "Đã duyệt đơn", "staff@newmen.local"),
                        new StatusHistoryRecord("CONFIRMED", "PREPARING", "Đóng gói bọc bóng khí", "kho.le@newmen.local"),
                        new StatusHistoryRecord("PREPARING", "SHIPPING", "Bàn giao đơn vị vận chuyển ViettelPost", "kho.le@newmen.local")));

            // Đơn 6: HD0000000005 - Trạng thái: COMPLETED (Hoàn thành) - Online
            CustomerOrder oCompleted = seedOrder("HD0000000005", custNam, custNam.account, "Nguyễn Hải Nam", "0966445566", "nam.nguyen@company.vn",
                "Số 250 Hoàng Hoa Thám, Phường Thụy Khuê, Quận Tây Hồ, Hà Nội", "Số 250 Hoàng Hoa Thám", "Phường Thụy Khuê", "Quận Tây Hồ", "Hà Nội",
                "BANK_TRANSFER", "CHUYEN_KHOAN", "PAID", "ONLINE", "COMPLETED",
                new BigDecimal("1450000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1450000"),
                null, "Khách hàng VIP nhận hàng hài lòng", LocalDateTime.now().minusDays(3),
                List.of(new OrderLineItem(vBlazerBlack != null ? vBlazerBlack : vShirtWhite, 1, new BigDecimal("1450000"), "Blazer may đo")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Khách đặt", "nam.nguyen@company.vn"),
                        new StatusHistoryRecord("PENDING", "CONFIRMED", "Duyệt đơn", "staff@newmen.local"),
                        new StatusHistoryRecord("CONFIRMED", "PREPARING", "Soạn đồ", "kho.le@newmen.local"),
                        new StatusHistoryRecord("PREPARING", "SHIPPING", "Đang giao", "kho.le@newmen.local"),
                        new StatusHistoryRecord("SHIPPING", "COMPLETED", "Khách đã nhận hàng và thanh toán đủ", "staff@newmen.local")));

            // Đơn 7: HD-POS-1001 - Trạng thái: COMPLETED (Hoàn thành) - Bán hàng tại quầy POS Tiền mặt
            seedOrder("HD-POS-1001", null, null, "Khách mua tại quầy 12 Lê Thái Tổ", "0900000001", "pos@newmen.local",
                "Nhận trực tiếp tại showroom NewMen Store", "12 Lê Thái Tổ", "Hàng Trống", "Hoàn Kiếm", "Hà Nội",
                "CASH", "TIEN_MAT", "PAID", "POS", "COMPLETED",
                new BigDecimal("420000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("420000"),
                null, "Khách thanh toán tiền mặt 500k thối 80k", LocalDateTime.now().minusDays(2),
                List.of(new OrderLineItem(vPoloNavy, 1, new BigDecimal("420000"), "Khách thử vừa size")),
                List.of(new StatusHistoryRecord(null, "COMPLETED", "Thanh toán thành công tại quầy thu ngân POS", "staff@newmen.local")));

            // Đơn 8: HD-POS-1002 - Trạng thái: COMPLETED (Hoàn thành) - Bán hàng tại quầy POS Quét mã QR
            seedOrder("HD-POS-1002", customerHieu, custAcc, "Nguyễn Thành Hiếu", "0988377373", "hieu@newmen.local",
                "Nhận trực tiếp tại quầy", "12 Lê Thái Tổ", "Hàng Trống", "Hoàn Kiếm", "Hà Nội",
                "TRANSFER", "CHUYEN_KHOAN_QR", "PAID", "POS", "COMPLETED",
                new BigDecimal("1010000"), new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("910000"),
                vWelcome, "Khách quét mã VietQR tại quầy", LocalDateTime.now().minusDays(1),
                List.of(new OrderLineItem(vShirtWhite, 1, new BigDecimal("590000"), "Sơ mi Oxford trắng"),
                        new OrderLineItem(vPoloNavy, 1, new BigDecimal("420000"), "Polo Signature")),
                List.of(new StatusHistoryRecord(null, "COMPLETED", "Thu ngân xác nhận tiền vào tài khoản VietQR", "staff@newmen.local")));

            // Đơn 9: HD0000000004 - Trạng thái: CANCELLED (Đã hủy) - Online
            seedOrder("HD0000000004", custBao, custBao.account, "Hoàng Quốc Bảo", "0911889900", "bao.hoang@gmail.com",
                "Số 12 Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Hà Nội", "Số 12 Nguyễn Trãi", "Phường Thượng Đình", "Quận Thanh Xuân", "Hà Nội",
                "COD", "COD", "UNPAID", "ONLINE", "CANCELLED",
                new BigDecimal("620000"), BigDecimal.ZERO, new BigDecimal("25000"), new BigDecimal("645000"),
                null, "Khách đổi ý muốn đặt mẫu áo khoác", LocalDateTime.now().minusDays(4),
                List.of(new OrderLineItem(vShirtWhite, 1, new BigDecimal("620000"), "Sơ mi kẻ sọc")),
                List.of(new StatusHistoryRecord(null, "PENDING", "Khách đặt đơn", "bao.hoang@gmail.com"),
                        new StatusHistoryRecord("PENDING", "CANCELLED", "Khách gọi hotline yêu cầu hủy đơn", "staff@newmen.local")));

            // ==============================================================
            // 7. ĐỔI HÀNG & TRẢ HÀNG (4 BẢNG: Return, ReturnDetail, Exchange, ExchangeDetail)
            // ==============================================================
            if (orderReturns.count() == 0 && oCompleted != null) {
                // Return 1: Đã duyệt đổi hàng cho đơn HD0000000005
                OrderReturn ret1 = new OrderReturn();
                ret1.order = oCompleted;
                ret1.account = custNam.account;
                ret1.customer = custNam;
                ret1.reason = "Khách muốn đổi sang size lớn hơn do mặc ôm sát";
                ret1.note = "Áo còn nguyên tem mác NewMen, chưa giặt ủi";
                ret1.status = "APPROVED";
                ret1.total = new BigDecimal("1450000");
                orderReturns.save(ret1);

                OrderLine firstLine = orderLines.findByOrderId(oCompleted.id).stream().findFirst().orElse(null);
                if (firstLine != null) {
                    returnDetails.save(new ReturnDetail(ret1, firstLine, 1, 1));
                }

                // Exchange 1: Phiếu đổi hàng đi kèm Return 1
                Exchange ex1 = new Exchange();
                ex1.customer = custNam;
                ex1.orderReturn = ret1;
                ex1.shipName = custNam.name;
                ex1.shipPhone = custNam.phoneNumber;
                ex1.shipAddress = "Số 250 Hoàng Hoa Thám, Quận Tây Hồ, Hà Nội";
                ex1.note = "Đổi sang size L màu Xanh Navy";
                ex1.total = new BigDecimal("1450000");
                ex1.status = "COMPLETED";
                exchanges.save(ex1);
                exchangeDetails.save(new ExchangeDetail(ex1, vShirtWhite, 1, new BigDecimal("1450000"), "Đã xuất kho gửi shipper"));

                // Return 2: Đang chờ xử lý
                CustomerOrder oPrep = orders.findByCode("HD0000000008").orElse(null);
                if (oPrep != null) {
                    OrderReturn ret2 = new OrderReturn();
                    ret2.order = oPrep;
                    ret2.account = custDuc.account;
                    ret2.customer = custDuc;
                    ret2.reason = "Khách đặt nhầm màu Xanh Navy muốn đổi sang màu Trắng";
                    ret2.note = "Khách liên hệ qua hotline xin đổi trước khi ship";
                    ret2.status = "PENDING";
                    ret2.total = new BigDecimal("840000");
                    orderReturns.save(ret2);

                    OrderLine linePrep = orderLines.findByOrderId(oPrep.id).stream().findFirst().orElse(null);
                    if (linePrep != null) {
                        returnDetails.save(new ReturnDetail(ret2, linePrep, 1, 1));
                    }
                }
            }
        }

        // ==============================================================
        // 8. ĐÁNH GIÁ SẢN PHẨM & ẢNH REVIEW (2 BẢNG: Review, RatingImage)
        // ==============================================================
        if (reviews.count() < 5) {
            CustomerOrder o10 = orders.findByCode("HD0000000010").orElse(null);
            CustomerOrder o9 = orders.findByCode("HD0000000009").orElse(null);
            CustomerOrder o8 = orders.findByCode("HD0000000008").orElse(null);
            CustomerOrder o7 = orders.findByCode("HD0000000007").orElse(null);
            CustomerOrder o6 = orders.findByCode("HD0000000006").orElse(null);
            CustomerOrder o5 = orders.findByCode("HD0000000005").orElse(null);
            CustomerOrder fallbackOrder = orders.findAll().stream().findFirst().orElse(null);

            seedReview(p1, customerHieu, o10 != null ? o10 : fallbackOrder, 5,
                "Áo sơ mi màu đỏ bóc đô mặc cực kỳ tôn da và lịch lãm, form dáng chuẩn mực công sở, đường may nẹp sắc sảo rất đáng tiền!",
                "/media/review-oxford-1.jpg");
            seedReview(p2, custLong, o9 != null ? o9 : fallbackOrder, 5,
                "Sơ mi Oxford trắng tinh khôi, vải dệt sợi đôi đứng phom, phối với quần âu hay kaki đều rất đẹp và nam tính.",
                "/media/review-shirt-white.jpg");
            seedReview(p4, custDuc, o8 != null ? o8 : fallbackOrder, 5,
                "Vải dệt cá sấu piqué dày dặn, bo cổ không bị quăn sau khi giặt máy, mặc đi làm cả ngày không hề bí bách.",
                null);
            seedReview(p6, custBao, o7 != null ? o7 : fallbackOrder, 4,
                "Chất cotton compact sờ rất mịn tay, mát mẻ thấm hút mồ hôi tốt khi hoạt động ngoài trời, giá 250k quá hợp lý.",
                null);
            seedReview(p8, custTuan, o6 != null ? o6 : fallbackOrder, 5,
                "Áo khoác gió nhẹ tênh, cản gió và đi mưa nhỏ đường xa rất thoải mái, màu đen mặc nhìn phong trần chuẩn nam tính.",
                "/media/review-jacket-1.jpg");
            seedReview(p11, custNam, o5 != null ? o5 : fallbackOrder, 5,
                "Form blazer may đo cách tân rất đẹp, mặc vào tự tin ngay trong các cuộc họp với đối tác, đóng gói hộp cứng cáp sang trọng.",
                null);
        }

        // ==============================================================
        // 9. BANNER QUẢNG CÁO TIẾP THỊ (BẢNG 2.25)
        // ==============================================================
        if (banners.count() < 3) {
            banners.save(new Banner("HÈ TỚI MẶC MỚI LÀ VUI", "SALE UPTO 49% - Toàn bộ sơ mi và polo năng động",
                1, "/media/banner-summer.jpg", "/catalog", true, 1));
            banners.save(new Banner("BỘ SƯU TẬP QUÝ ÔNG CÔNG SỞ 2026", "Lịch lãm, đứng phom, định hình phong thái tự tin",
                1, "/media/banner-office.jpg", "/catalog?sort=newest", true, 2));
            banners.save(new Banner("NEWMEN ESSENTIALS - MẶC ĐẸP MỖI NGÀY", "Những chiếc áo thun và polo cotton tinh gọn cho nhịp sống hiện đại",
                1, "/media/banner-casual.jpg", "/catalog?sort=bestseller", true, 3));
        }

        // ==============================================================
        // 10. LIÊN HỆ & TƯ VẤN KHÁCH HÀNG (BẢNG 2.32)
        // ==============================================================
        if (contacts.count() < 4) {
            contacts.save(new Contact("Đặng Anh Tuấn", "0915123456", "tuan.dang@techcorp.vn",
                "Tòa nhà Landmark 72, Phạm Hùng", "Mễ Trì", "Nam Từ Liêm", "Hà Nội",
                "Doanh nghiệp chúng tôi muốn đặt may 50 áo polo đồng phục thêu logo cho sự kiện nội bộ, xin tư vấn báo giá.",
                true));
            contacts.save(new Contact("Trịnh Mỹ Linh", "0987654321", "linh.trinh@gmail.com",
                "Số 88 Phố Huế", "Hàng Bài", "Hoàn Kiếm", "Hà Nội",
                "Shop có nhận giao hàng hỏa tốc trong 2h tại khu vực nội thành Hà Nội trong chiều nay không ạ?",
                true));
            contacts.save(new Contact("Phạm Quang Vinh", "0938112233", "vinh.pham@gmail.com",
                "Số 35 Đỗ Đức Dục", "Mễ Trì", "Nam Từ Liêm", "Hà Nội",
                "Tôi cao 1m78 nặng 75kg thì nên chọn sơ mi Oxford size L hay XL để mặc vừa vặn sơ vin đi làm?",
                false));
            contacts.save(new Contact("Nguyễn Thị Mai", "0976554433", "mai.nguyen@vietin.vn",
                "Số 102 Bà Triệu", "Hàng Bài", "Hoàn Kiếm", "Hà Nội",
                "Tôi muốn mua tặng bạn trai áo khoác gió size L, nếu không vừa bên mình có hỗ trợ đổi tận nơi không?",
                false));
        }
    }

    // ==============================================================
    // HELPER METHODS
    // ==============================================================
    private Role getOrCreateRole(String name, String desc) {
        return roles.findByNameIgnoreCase(name).orElseGet(() -> roles.save(new Role(name, desc, true)));
    }

    private void seedStaffEmployee(String email, String name, String phone, String address, LocalDate birth, Role role) {
        Account acc = accounts.findByEmailIgnoreCase(email).orElseGet(() -> {
            Account a = new Account();
            a.email = email;
            a.passwordHash = passwordEncoder.encode("ChangeMe123!");
            a.fullName = name;
            a.phone = phone;
            a.role = "STAFF";
            a.active = true;
            return accounts.save(a);
        });
        if (!employees.existsByEmailIgnoreCase(email)) {
            Employee emp = new Employee();
            emp.name = name;
            emp.email = email;
            emp.phone = phone;
            emp.password = acc.passwordHash;
            emp.role = role;
            emp.status = true;
            emp.address = address;
            emp.birthDate = birth;
            emp.account = acc;
            employees.save(emp);
        }
    }

    private Customer seedCustomer(String name, String email, String phone, String line1, String ward, String district, String city, Role role, int points) {
        Account acc = accounts.findByEmailIgnoreCase(email).orElseGet(() -> {
            Account a = new Account();
            a.fullName = name;
            a.email = email;
            a.phone = phone;
            a.passwordHash = passwordEncoder.encode("Customer123!");
            a.role = "CUSTOMER";
            a.active = true;
            return accounts.save(a);
        });

        Customer c = customers.findByEmailIgnoreCase(email).orElseGet(() -> {
            Customer cust = new Customer();
            cust.name = name;
            cust.email = email;
            cust.phoneNumber = phone;
            cust.password = acc.passwordHash;
            cust.role = role;
            cust.points = points;
            cust.status = true;
            cust.birthDate = LocalDate.of(1998, 4, 15);
            cust.account = acc;
            return customers.save(cust);
        });

        if (addresses.findByCustomerId(c.id).isEmpty()) {
            Address a = new Address();
            a.account = acc;
            a.customer = c;
            a.recipientName = name;
            a.phone = phone;
            a.line1 = line1;
            a.address = line1;
            a.ward = ward;
            a.wardName = ward;
            a.district = district;
            a.districtName = district;
            a.province = city;
            a.cityName = city;
            a.isDefault = true;
            a.status = true;
            addresses.save(a);
        }
        return c;
    }

    private OriginCategory getOrCreateOriginCategory(String name) {
        return originCategories.findAll().stream().filter(o -> o.nameCategoryF.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> originCategories.save(new OriginCategory(name, true)));
    }

    private Color getOrCreateColor(String name, String hex) {
        return colors.findAll().stream().filter(c -> c.nameColor.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> colors.save(new Color(name, hex, true)));
    }

    private Size getOrCreateSize(String code, String desc) {
        return sizes.findAll().stream().filter(s -> s.code.equalsIgnoreCase(code)).findFirst()
            .orElseGet(() -> sizes.save(new Size(code, desc, true)));
    }

    private Material getOrCreateMaterial(String name) {
        return materials.findAll().stream().filter(m -> m.nameMaterial.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> materials.save(new Material(name, true)));
    }

    private Collar getOrCreateCollar(String name) {
        return collars.findAll().stream().filter(c -> c.nameCollar.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> collars.save(new Collar(name, true)));
    }

    private Design getOrCreateDesign(String name) {
        return designs.findAll().stream().filter(d -> d.nameDesign.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> designs.save(new Design(name, true)));
    }

    private Form getOrCreateForm(String name) {
        return forms.findAll().stream().filter(f -> f.nameForm.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> forms.save(new Form(name, true)));
    }

    private Sleeve getOrCreateSleeve(String name) {
        return sleeves.findAll().stream().filter(s -> s.nameSleeve.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> sleeves.save(new Sleeve(name, true)));
    }

    private Category getOrCreateCategory(String name, String slug, OriginCategory origin) {
        return categories.findBySlugAndActiveTrue(slug).orElseGet(() -> {
            Category c = new Category();
            c.name = name;
            c.nameCategory = name;
            c.slug = slug;
            c.originCategory = origin;
            c.active = true;
            c.status = true;
            return categories.save(c);
        });
    }

    private Product getOrCreateProduct(String code, String name, String slug, Category cat,
                                       Material mat, Collar col, Design des, Form form, Sleeve slv,
                                       String desc, BigDecimal price, Double weight, String imageUrl) {
        return products.findByCode(code).orElseGet(() -> {
            Product p = new Product();
            p.code = code;
            p.name = name;
            p.slug = slug;
            p.category = cat;
            p.material = mat.nameMaterial;
            p.materialEntity = mat;
            p.collar = col.nameCollar;
            p.collarEntity = col;
            p.design = des.nameDesign;
            p.designEntity = des;
            p.fit = form.nameForm;
            p.formEntity = form;
            p.sleeve = slv.nameSleeve;
            p.sleeveEntity = slv;
            p.description = desc;
            p.originalPrice = price;
            p.weight = weight;
            p.active = true;
            p.status = true;
            Product saved = products.save(p);

            ProductImage pi = new ProductImage(saved, imageUrl, true);
            images.save(pi);
            return saved;
        });
    }

    private void createVariantIfAbsent(Product product, String sku, String barcode, String color, String size,
                                       BigDecimal price, BigDecimal costPrice, int stock, Color colEntity, Size sizeEntity) {
        if (!variants.existsBySku(sku)) {
            ProductVariant v = new ProductVariant();
            v.product = product;
            v.sku = sku;
            v.barcode = barcode;
            v.color = color;
            v.colorEntity = colEntity;
            v.size = size;
            v.sizeEntity = sizeEntity;
            v.price = price;
            v.costPrice = costPrice;
            v.stock = stock;
            v.active = true;
            v.status = true;
            variants.save(v);

            InventoryMovement m = new InventoryMovement();
            m.variant = v;
            m.movementType = "INITIAL_STOCK";
            m.quantityDelta = stock;
            m.reference = "initial-seed";
            m.createdBy = "system";
            inventory.save(m);
        }
    }

    private Discount getOrCreateDiscount(String name, String desc, LocalDateTime start, LocalDateTime end) {
        return discounts.findAll().stream().filter(d -> d.name.equalsIgnoreCase(name)).findFirst()
            .orElseGet(() -> discounts.save(new Discount(name, desc, start, end, true)));
    }

    private Voucher getOrCreateVoucher(String code, String name, String desc, int discount, BigDecimal min, BigDecimal max, int type) {
        return vouchers.findByCodeIgnoreCase(code).orElseGet(() ->
            vouchers.save(new Voucher(code, name, desc, discount, min, max,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusMonths(6), type, true))
        );
    }

    private record OrderLineItem(ProductVariant variant, int quantity, BigDecimal unitPrice, String note) {}
    private record StatusHistoryRecord(String prev, String next, String note, String actor) {}

    private CustomerOrder seedOrder(String code, Customer customer, Account account,
                                   String recipientName, String phone, String email,
                                   String fullAddress, String line1, String ward, String district, String city,
                                   String paymentMethod, String paymentType, String statusPay,
                                   String channel, String status,
                                   BigDecimal subtotal, BigDecimal discount, BigDecimal freight, BigDecimal total,
                                   Voucher voucher, String note, LocalDateTime createdAt,
                                   List<OrderLineItem> items, List<StatusHistoryRecord> history) {
        if (orders.existsByCode(code)) {
            return orders.findByCode(code).orElse(null);
        }

        CustomerOrder o = new CustomerOrder();
        o.code = code;
        o.customer = customer;
        o.account = account;
        o.customerName = recipientName;
        o.customerPhone = phone;
        o.customerEmail = email;
        o.shipRecipientName = recipientName;
        o.shipPhone = phone;
        o.shipLine1 = fullAddress;
        o.address = line1;
        o.shipWard = ward;
        o.wardName = ward;
        o.shipDistrict = district;
        o.districtName = district;
        o.shipProvince = city;
        o.cityName = city;
        o.paymentMethod = paymentMethod;
        o.paymentType = paymentType;
        o.statusPay = statusPay;
        o.channel = channel;
        o.saleForm = channel;
        o.status = status;
        o.subtotal = subtotal;
        o.discount = discount;
        o.shippingFee = freight;
        o.freight = freight;
        o.total = total;
        o.voucher = voucher;
        o.voucherCode = voucher != null ? voucher.code : null;
        o.note = note;
        o.createdAt = createdAt;
        o.updatedAt = createdAt.plusMinutes(15);
        CustomerOrder saved = orders.save(o);

        for (OrderLineItem item : items) {
            OrderLine line = new OrderLine();
            line.order = saved;
            line.variant = item.variant;
            line.productName = item.variant.getProduct().getName();
            line.sku = item.variant.sku;
            line.color = item.variant.color;
            line.size = item.variant.size;
            line.quantity = item.quantity;
            line.unitPrice = item.unitPrice;
            line.lineTotal = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));
            line.note = item.note;
            orderLines.save(line);
        }

        for (StatusHistoryRecord h : history) {
            OrderStatusHistory hist = new OrderStatusHistory();
            hist.order = saved;
            hist.previousStatus = h.prev;
            hist.nextStatus = h.next;
            hist.note = h.note;
            hist.changedBy = h.actor;
            hist.createdAt = createdAt.plusMinutes(10);
            orderHistory.save(hist);
        }

        // Checkout & CheckoutDetail
        Checkout chk = new Checkout("CHK-" + code, code, 1, total, paymentMethod);
        checkouts.save(chk);
        for (OrderLineItem item : items) {
            checkoutDetails.save(new CheckoutDetail(chk, item.variant, item.quantity, item.unitPrice, 1));
        }

        return saved;
    }

    private void seedReview(Product product, Customer customer, CustomerOrder order, int rating, String content, String imageUrl) {
        if (product == null) return;
        CustomerOrder targetOrder = order;
        if (targetOrder == null) {
            targetOrder = orders.findAll().stream().findFirst().orElse(null);
        }
        if (targetOrder == null) return;
        if (reviews.findByOrder_IdAndProduct_Id(targetOrder.id, product.id).isPresent()) return;

        Review r = new Review();
        r.product = product;
        r.order = targetOrder;
        r.customer = customer != null ? customer : targetOrder.customer;
        r.account = (customer != null && customer.account != null) ? customer.account : targetOrder.account;
        if (r.account == null) {
            r.account = accounts.findAll().stream().findFirst().orElse(null);
        }
        if (r.account == null) return;

        r.rating = rating;
        r.content = content;
        r.status = true;
        reviews.save(r);

        if (imageUrl != null) {
            ratingImages.save(new RatingImage(imageUrl, true, r));
        }
    }
}
