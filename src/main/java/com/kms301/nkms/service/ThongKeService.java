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
    public Map<String, Object> buildThongKeData() {
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

        return data;
    }

    @Transactional(readOnly = true)
    public List<Nguon> getAllNguon() {
        return nguonRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getUnitCountByNguon() {
        Map<Long, Long> map = new LinkedHashMap<>();
        for (Nguon n : nguonRepository.findAll()) {
            map.put(n.getId(), donViTriThucRepository.countByNguonId(n.getId()));
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Page<XungDot> getXungDotPage(int page, int size) {
        int validSize = (size == 10 || size == 20 || size == 50) ? size : 20;
        return xungDotRepository.findAllByOrderByIdAsc(PageRequest.of(Math.max(0, page), validSize));
    }

    @Transactional(readOnly = true)
    public List<KhaiNiem> getAllKhaiNiem() {
        return khaiNiemRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<DongNghia>> getDongNghiaByKhaiNiem() {
        Map<Long, List<DongNghia>> map = new LinkedHashMap<>();
        for (DongNghia dn : dongNghiaRepository.findAllByOrderByKhaiNiemIdAscIdAsc()) {
            if (dn.getKhaiNiem() != null) {
                map.computeIfAbsent(dn.getKhaiNiem().getId(), k -> new ArrayList<>()).add(dn);
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<CongThuc> getAllCongThuc() {
        return congThucRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<BangChuCai> getAllBangChuCai() {
        return bangChuCaiRepository.findAll();
    }
}

