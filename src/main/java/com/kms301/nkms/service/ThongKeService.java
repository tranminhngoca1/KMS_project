package com.kms301.nkms.service;

import com.kms301.nkms.model.*;
import com.kms301.nkms.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Dịch vụ cung cấp dữ liệu cho các trang phụ (/nguon, /xung-dot, /khai-niem, /thong-ke, /gioi-thieu).
 */
@Service
public class ThongKeService {

    private final TruongPhaiRepository truongPhaiRepository;
    private final NguonRepository nguonRepository;
    private final KhaiNiemRepository khaiNiemRepository;
    private final DongNghiaRepository dongNghiaRepository;
    private final CongThucRepository congThucRepository;
    private final DonViTriThucRepository donViTriThucRepository;
    private final QuanHeTriThucRepository quanHeTriThucRepository;
    private final XungDotRepository xungDotRepository;
    private final BangChuCaiRepository bangChuCaiRepository;

    private Map<String, Object> cachedThongKeData;
    private long thongKeCachedAt = 0;
    private static final long CACHE_TTL_MS = 300_000; // 5 phút cache

    private List<Nguon> cachedNguon;
    private Map<Long, Long> cachedUnitCountByNguon;
    private List<KhaiNiem> cachedKhaiNiem;
    private Map<Long, List<DongNghia>> cachedDongNghiaMap;
    private List<CongThuc> cachedCongThuc;
    private List<BangChuCai> cachedBangChuCai;

    public ThongKeService(TruongPhaiRepository truongPhaiRepository,
                          NguonRepository nguonRepository,
                          KhaiNiemRepository khaiNiemRepository,
                          DongNghiaRepository dongNghiaRepository,
                          CongThucRepository congThucRepository,
                          DonViTriThucRepository donViTriThucRepository,
                          QuanHeTriThucRepository quanHeTriThucRepository,
                          XungDotRepository xungDotRepository,
                          BangChuCaiRepository bangChuCaiRepository) {
        this.truongPhaiRepository = truongPhaiRepository;
        this.nguonRepository = nguonRepository;
        this.khaiNiemRepository = khaiNiemRepository;
        this.dongNghiaRepository = dongNghiaRepository;
        this.congThucRepository = congThucRepository;
        this.donViTriThucRepository = donViTriThucRepository;
        this.quanHeTriThucRepository = quanHeTriThucRepository;
        this.xungDotRepository = xungDotRepository;
        this.bangChuCaiRepository = bangChuCaiRepository;
    }

    @Transactional(readOnly = true)
    public synchronized Map<String, Object> buildThongKeData() {
        long now = System.currentTimeMillis();
        if (cachedThongKeData != null && (now - thongKeCachedAt < CACHE_TTL_MS)) {
            return new LinkedHashMap<>(cachedThongKeData);
        }

        Map<String, Object> data = new LinkedHashMap<>();

        long tongDonVi = donViTriThucRepository.count();
        data.put("tongDonVi", tongDonVi);

        // Theo trường phái
        Map<String, Long> theoTruongPhai = new LinkedHashMap<>();
        for (TruongPhai tp : truongPhaiRepository.findAll()) {
            theoTruongPhai.put(tp.getTen() + " (" + tp.getMa() + ")", donViTriThucRepository.countByTruongPhaiId(tp.getId()));
        }
        data.put("theoTruongPhai", theoTruongPhai);

        // Theo nguồn sách
        Map<String, Long> theoNguon = new LinkedHashMap<>();
        for (Nguon ng : nguonRepository.findAll()) {
            theoNguon.put(ng.getMa() + " — " + ng.getTenSach(), donViTriThucRepository.countByNguonId(ng.getId()));
        }
        data.put("theoNguon", theoNguon);

        // Theo nhãn sử dụng
        Map<String, Long> theoNhan = new LinkedHashMap<>();
        theoNhan.put("DUNG (Đưa vào suy diễn chính)", donViTriThucRepository.countByNhanSuDung("DUNG"));
        theoNhan.put("GOI_Y_VAN_HOA (Hiển thị riêng ở mục Gợi ý văn hoá Vệ Đà)", donViTriThucRepository.countByNhanSuDung("GOI_Y_VAN_HOA"));
        theoNhan.put("THAN_TRONG (Chỉ lưu trong Kho, không hiện ở Khám phá)", donViTriThucRepository.countByNhanSuDung("THAN_TRONG"));
        theoNhan.put("KHONG_DUNG (Tri thức lịch sử/bói toán/y học, không hiện ở Khám phá)", donViTriThucRepository.countByNhanSuDung("KHONG_DUNG"));
        data.put("theoNhan", theoNhan);

        // Theo mức tin cậy
        Map<String, Long> theoTinCay = new LinkedHashMap<>();
        theoTinCay.put("CAO (Công thức, quy tắc tính toán xác định)", donViTriThucRepository.countByMucTinCay("CAO"));
        theoTinCay.put("TRUNG_BINH (Diễn giải tính cách, xu hướng phổ biến)", donViTriThucRepository.countByMucTinCay("TRUNG_BINH"));
        theoTinCay.put("THAP (Luận đoán mang tính cá nhân của tác giả / thực hành tôn giáo)", donViTriThucRepository.countByMucTinCay("THAP"));
        data.put("theoTinCay", theoTinCay);

        // Theo trạng thái duyệt
        Map<String, Long> theoTrangThai = new LinkedHashMap<>();
        theoTrangThai.put("DA_DUYET (Đã duyệt)", donViTriThucRepository.countByTrangThai("DA_DUYET"));
        theoTrangThai.put("NHAP (Nháp - Chờ xác minh thêm)", donViTriThucRepository.countByTrangThai("NHAP"));
        theoTrangThai.put("LUU_TRU (Lưu trữ / Đã xoá mềm)", donViTriThucRepository.countByTrangThai("LUU_TRU"));
        data.put("theoTrangThai", theoTrangThai);

        // Theo số trang trích dẫn (Provenance)
        long coSoTrang = donViTriThucRepository.countHasPageNumber();
        long thieuSoTrang = donViTriThucRepository.countMissingPageNumber();
        data.put("coSoTrang", coSoTrang);
        data.put("thieuSoTrang", thieuSoTrang);
        double tyLeSoTrang = tongDonVi > 0 ? (coSoTrang * 100.0 / tongDonVi) : 0.0;
        data.put("tyLeSoTrang", String.format("%.1f%%", tyLeSoTrang));

        // Thống kê xung đột & quan hệ
        data.put("tongXungDot", xungDotRepository.count());
        data.put("xungDotDaXuLy", xungDotRepository.countByTrangThai("DA_XU_LY"));
        data.put("xungDotChoXacMinh", xungDotRepository.countByTrangThai("CHO_XAC_MINH"));
        data.put("tongQuanHe", quanHeTriThucRepository.count());
        data.put("tongKhaiNiem", khaiNiemRepository.count());
        data.put("tongCongThuc", congThucRepository.count());

        cachedThongKeData = data;
        thongKeCachedAt = now;
        return data;
    }

    @Transactional(readOnly = true)
    public synchronized List<Nguon> getAllNguon() {
        if (cachedNguon != null) return cachedNguon;
        cachedNguon = nguonRepository.findAll();
        return cachedNguon;
    }

    @Transactional(readOnly = true)
    public synchronized Map<Long, Long> getUnitCountByNguon() {
        if (cachedUnitCountByNguon != null) return cachedUnitCountByNguon;
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Nguon n : getAllNguon()) {
            map.put(n.getId(), donViTriThucRepository.countByNguonId(n.getId()));
        }
        cachedUnitCountByNguon = map;
        return map;
    }

    @Transactional(readOnly = true)
    public Page<XungDot> getXungDotPage(int page, int size) {
        int validSize = (size == 10 || size == 20 || size == 50) ? size : 20;
        return xungDotRepository.findAllByOrderByIdAsc(PageRequest.of(Math.max(0, page), validSize));
    }

    @Transactional(readOnly = true)
    public synchronized List<KhaiNiem> getAllKhaiNiem() {
        if (cachedKhaiNiem != null) return cachedKhaiNiem;
        cachedKhaiNiem = khaiNiemRepository.findAllByOrderByIdAsc();
        return cachedKhaiNiem;
    }

    @Transactional(readOnly = true)
    public synchronized Map<Long, List<DongNghia>> getDongNghiaByKhaiNiem() {
        if (cachedDongNghiaMap != null) return cachedDongNghiaMap;
        Map<Long, List<DongNghia>> map = new LinkedHashMap<>();
        for (DongNghia dn : dongNghiaRepository.findAllByOrderByKhaiNiemIdAscIdAsc()) {
            if (dn.getKhaiNiem() != null) {
                map.computeIfAbsent(dn.getKhaiNiem().getId(), k -> new ArrayList<>()).add(dn);
            }
        }
        cachedDongNghiaMap = map;
        return map;
    }

    @Transactional(readOnly = true)
    public synchronized List<CongThuc> getAllCongThuc() {
        if (cachedCongThuc != null) return cachedCongThuc;
        cachedCongThuc = congThucRepository.findAllByOrderByIdAsc();
        return cachedCongThuc;
    }

    @Transactional(readOnly = true)
    public synchronized List<BangChuCai> getAllBangChuCai() {
        if (cachedBangChuCai != null) return cachedBangChuCai;
        cachedBangChuCai = bangChuCaiRepository.findAll();
        return cachedBangChuCai;
    }
}

