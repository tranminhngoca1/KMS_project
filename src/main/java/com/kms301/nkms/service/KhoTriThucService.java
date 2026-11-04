package com.kms301.nkms.service;

import com.kms301.nkms.model.*;
import com.kms301.nkms.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Dịch vụ quản trị Kho tri thức (/kho, /kho/{id}):
 * - Tìm kiếm toàn văn kết hợp 9 bộ lọc phía server và phân trang (10, 20, 50).
 * - Thêm mới đơn vị tri thức (kèm gợi ý đơn vị cùng khái niệm + giá trị ở nguồn khác để nối quan hệ/xung đột).
 * - Chỉnh sửa (tự động tăng phien_ban và lưu lich_su_phien_ban kèm lý do).
 * - Duyệt (NHAP -> DA_DUYET) và Xoá mềm (-> LUU_TRU).
 * - Nhập 26 giá trị A–Z cho bảng chữ cái Unit System của Johari và tự động chuyển trạng thái KICH_HOAT.
 */
@Service
public class KhoTriThucService {

    private final DonViTriThucRepository donViTriThucRepository;
    private final TruongPhaiRepository truongPhaiRepository;
    private final NguonRepository nguonRepository;
    private final KhaiNiemRepository khaiNiemRepository;
    private final TheRepository theRepository;
    private final QuanHeTriThucRepository quanHeTriThucRepository;
    private final XungDotRepository xungDotRepository;
    private final LichSuPhienBanRepository lichSuPhienBanRepository;
    private final BangChuCaiRepository bangChuCaiRepository;
    private final BangChuCaiGiaTriRepository bangChuCaiGiaTriRepository;

    public KhoTriThucService(DonViTriThucRepository donViTriThucRepository,
                             TruongPhaiRepository truongPhaiRepository,
                             NguonRepository nguonRepository,
                             KhaiNiemRepository khaiNiemRepository,
                             TheRepository theRepository,
                             QuanHeTriThucRepository quanHeTriThucRepository,
                             XungDotRepository xungDotRepository,
                             LichSuPhienBanRepository lichSuPhienBanRepository,
                             BangChuCaiRepository bangChuCaiRepository,
                             BangChuCaiGiaTriRepository bangChuCaiGiaTriRepository) {
        this.donViTriThucRepository = donViTriThucRepository;
        this.truongPhaiRepository = truongPhaiRepository;
        this.nguonRepository = nguonRepository;
        this.khaiNiemRepository = khaiNiemRepository;
        this.theRepository = theRepository;
        this.quanHeTriThucRepository = quanHeTriThucRepository;
        this.xungDotRepository = xungDotRepository;
        this.lichSuPhienBanRepository = lichSuPhienBanRepository;
        this.bangChuCaiRepository = bangChuCaiRepository;
        this.bangChuCaiGiaTriRepository = bangChuCaiGiaTriRepository;
    }

    @Transactional(readOnly = true)
    public Page<DonViTriThuc> searchDonVi(String tuKhoa,
                                          Long truongPhaiId,
                                          Long nguonId,
                                          Long khaiNiemId,
                                          String cauHoi,
                                          String loai,
                                          String mucTinCay,
                                          String nhanSuDung,
                                          String trangThai,
                                          Long theId,
                                          int page,
                                          int size) {
        int validSize = (size == 10 || size == 20 || size == 50) ? size : 20;
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.ASC, "id"));

        Specification<DonViTriThuc> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tuKhoa != null && !tuKhoa.isBlank()) {
                String pattern = "%" + tuKhoa.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("ma")), pattern),
                        cb.like(cb.lower(root.get("tieuDe")), pattern),
                        cb.like(cb.lower(root.get("noiDung")), pattern),
                        cb.like(cb.lower(root.get("chuong")), pattern),
                        cb.like(cb.lower(root.get("ghiChu")), pattern)
                ));
            }
            if (truongPhaiId != null) {
                predicates.add(cb.equal(root.get("truongPhai").get("id"), truongPhaiId));
            }
            if (nguonId != null) {
                predicates.add(cb.equal(root.get("nguon").get("id"), nguonId));
            }
            if (khaiNiemId != null) {
                predicates.add(cb.equal(root.get("khaiNiem").get("id"), khaiNiemId));
            }
            if (cauHoi != null && !cauHoi.isBlank()) {
                predicates.add(cb.equal(root.get("cauHoi"), cauHoi.trim()));
            }
            if (loai != null && !loai.isBlank()) {
                predicates.add(cb.equal(root.get("loai"), loai.trim()));
            }
            if (mucTinCay != null && !mucTinCay.isBlank()) {
                predicates.add(cb.equal(root.get("mucTinCay"), mucTinCay.trim()));
            }
            if (nhanSuDung != null && !nhanSuDung.isBlank()) {
                predicates.add(cb.equal(root.get("nhanSuDung"), nhanSuDung.trim()));
            }
            if (trangThai != null && !trangThai.isBlank()) {
                predicates.add(cb.equal(root.get("trangThai"), trangThai.trim()));
            }
            if (theId != null) {
                Join<DonViTriThuc, The> theJoin = root.join("danhSachThe");
                predicates.add(cb.equal(theJoin.get("id"), theId));
                if (query != null) {
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return donViTriThucRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<DonViTriThuc> findById(Long id) {
        return donViTriThucRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<QuanHeTriThuc> findRelations(Long donViId) {
        return quanHeTriThucRepository.findAllRelatedToDonVi(donViId);
    }

    @Transactional(readOnly = true)
    public List<XungDot> findConflictsForUnit(Long donViId) {
        return xungDotRepository.findByDonViId(donViId);
    }

    @Transactional(readOnly = true)
    public List<LichSuPhienBan> findVersionHistory(Long donViId) {
        return lichSuPhienBanRepository.findByDonViIdOrderByPhienBanDescIdDesc(donViId);
    }

    /**
     * Tìm các đơn vị ở nguồn khác có cùng khai_niem_id + gia_tri_kich_hoat để gợi ý nối quan hệ / xung đột (Mục 10.2).
     */
    @Transactional(readOnly = true)
    public List<DonViTriThuc> findSuggestedRelatedUnits(DonViTriThuc unit) {
        if (unit.getKhaiNiem() == null || unit.getGiaTriKichHoat() == null || unit.getGiaTriKichHoat().isBlank()) {
            return List.of();
        }
        Long excludeId = unit.getId() != null ? unit.getId() : -1L;
        return donViTriThucRepository.findByKhaiNiemIdAndGiaTriKichHoatAndIdNot(
                unit.getKhaiNiem().getId(), unit.getGiaTriKichHoat(), excludeId);
    }

    @Transactional
    public DonViTriThuc createDonVi(DonViTriThuc formUnit, List<Long> theIds, String currentUsername) {
        formUnit.setPhienBan(1);
        formUnit.setNguoiTao(currentUsername != null ? currentUsername : "admin");
        if ("DA_DUYET".equalsIgnoreCase(formUnit.getTrangThai())) {
            formUnit.setNguoiDuyet(currentUsername != null ? currentUsername : "admin");
        }
        if (theIds != null && !theIds.isEmpty()) {
            formUnit.setDanhSachThe(new LinkedHashSet<>(theRepository.findAllById(theIds)));
        } else {
            formUnit.setDanhSachThe(new LinkedHashSet<>());
        }
        return donViTriThucRepository.save(formUnit);
    }

    @Transactional
    public DonViTriThuc updateDonVi(Long id, DonViTriThuc updated, List<Long> theIds,
                                    String lyDoSua, String currentUsername) {
        DonViTriThuc existing = donViTriThucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị tri thức ID = " + id));

        // Lưu bản chụp phiên bản cũ vào lich_su_phien_ban (Mục 7.5 & 10.2)
        LichSuPhienBan ls = new LichSuPhienBan();
        ls.setDonVi(existing);
        ls.setPhienBan(existing.getPhienBan());
        ls.setSnapshot(existing.getNoiDung(), existing.getNhanSuDung(), existing.getMucTinCay());
        ls.setNguoiSua(currentUsername != null ? currentUsername : "admin");
        ls.setLyDo((lyDoSua != null && !lyDoSua.isBlank()) ? lyDoSua.trim() : "Cập nhật nội dung / nhãn quản trị");
        lichSuPhienBanRepository.save(ls);

        // Tăng phiên bản và cập nhật
        existing.setPhienBan(existing.getPhienBan() + 1);
        existing.setMa(updated.getMa());
        existing.setTieuDe(updated.getTieuDe());
        existing.setLoai(updated.getLoai());
        existing.setKhaiNiem(updated.getKhaiNiem());
        existing.setTruongPhai(updated.getTruongPhai());
        existing.setNguon(updated.getNguon());
        existing.setGiaTriKichHoat(updated.getGiaTriKichHoat());
        existing.setDieuKienJson(updated.getDieuKienJson());
        existing.setNoiDung(updated.getNoiDung());
        existing.setCauHoi(updated.getCauHoi());
        existing.setChuong(updated.getChuong());
        existing.setTrangTu(updated.getTrangTu());
        existing.setTrangDen(updated.getTrangDen());
        existing.setMucTinCay(updated.getMucTinCay());
        existing.setNhanSuDung(updated.getNhanSuDung());
        existing.setLyDoNhan(updated.getLyDoNhan());
        existing.setTrangThai(updated.getTrangThai());
        existing.setGhiChu(updated.getGhiChu());

        if ("DA_DUYET".equalsIgnoreCase(updated.getTrangThai()) && existing.getNguoiDuyet() == null) {
            existing.setNguoiDuyet(currentUsername != null ? currentUsername : "admin");
        }

        if (theIds != null) {
            existing.setDanhSachThe(new LinkedHashSet<>(theRepository.findAllById(theIds)));
        }

        return donViTriThucRepository.save(existing);
    }

    @Transactional
    public void approveDonVi(Long id, String currentUsername) {
        DonViTriThuc existing = donViTriThucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị tri thức ID = " + id));
        existing.setTrangThai("DA_DUYET");
        existing.setNguoiDuyet(currentUsername != null ? currentUsername : "admin");
        donViTriThucRepository.save(existing);
    }

    @Transactional
    public void archiveDonVi(Long id, String currentUsername, String lyDo) {
        DonViTriThuc existing = donViTriThucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị tri thức ID = " + id));

        LichSuPhienBan ls = new LichSuPhienBan();
        ls.setDonVi(existing);
        ls.setPhienBan(existing.getPhienBan());
        ls.setSnapshot(existing.getNoiDung(), existing.getNhanSuDung(), existing.getMucTinCay());
        ls.setNguoiSua(currentUsername != null ? currentUsername : "admin");
        ls.setLyDo((lyDo != null && !lyDo.isBlank()) ? lyDo : "Chuyển sang trạng thái Lưu trữ (LUU_TRU)");
        lichSuPhienBanRepository.save(ls);

        existing.setPhienBan(existing.getPhienBan() + 1);
        existing.setTrangThai("LUU_TRU");
        donViTriThucRepository.save(existing);
    }

    /**
     * Lưu 26 giá trị A–Z cho bảng Unit System của Johari và tự động chuyển sang trạng thái KICH_HOAT (Mục 5 & 10.2).
     * Sử dụng synchronized và làm mới dữ liệu để tránh lỗi Duplicate Entry khi người dùng click đúp nút Lưu.
     */
    @Transactional
    public synchronized void saveUnitSystemAlphabet(Map<String, Integer> letterValues) {
        BangChuCai unitSys = bangChuCaiRepository.findByMa("UNIT_SYSTEM")
                .or(() -> bangChuCaiRepository.findByMa("JOHARI_UNIT_SYSTEM"))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy bảng chữ cái UNIT_SYSTEM trong CSDL"));

        for (char c = 'A'; c <= 'Z'; c++) {
            String key = String.valueOf(c);
            Integer val = letterValues.get(key);
            if (val == null || val < 1 || val > 9) {
                throw new IllegalArgumentException("Giá trị của chữ cái " + key + " phải nằm trong khoảng 1–9.");
            }
        }

        // Xóa sạch các giá trị cũ của bảng này để đảm bảo thao tác mang tính lũy đẳng (idempotent),
        // tránh hoàn toàn lỗi trùng lặp Unique Key (uk_bcc_ky_tu) khi bấm submit nhanh hoặc click đúp.
        bangChuCaiGiaTriRepository.deleteByBangChuCaiId(unitSys.getId());
        bangChuCaiGiaTriRepository.flush();

        List<BangChuCaiGiaTri> rowsToInsert = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            String letter = String.valueOf(c);
            BangChuCaiGiaTri row = new BangChuCaiGiaTri();
            row.setBangChuCai(unitSys);
            row.setKyTu(letter);
            row.setGiaTri(letterValues.get(letter));
            rowsToInsert.add(row);
        }
        bangChuCaiGiaTriRepository.saveAll(rowsToInsert);

        unitSys.setTrangThai("KICH_HOAT");
        unitSys.setGhiChu("Đã được quản trị viên nhập đủ 26 chữ cái A–Z từ sách in của Harish Johari và kích hoạt.");
        bangChuCaiRepository.save(unitSys);
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getUnitSystemMapForView() {
        Map<String, Integer> map = new LinkedHashMap<>();
        Optional<BangChuCai> opt = bangChuCaiRepository.findByMa("UNIT_SYSTEM")
                .or(() -> bangChuCaiRepository.findByMa("JOHARI_UNIT_SYSTEM"));
        if (opt.isPresent()) {
            List<BangChuCaiGiaTri> rows = bangChuCaiGiaTriRepository.findByBangChuCaiIdOrderByKyTuAsc(opt.get().getId());
            for (BangChuCaiGiaTri r : rows) {
                map.put(r.getKyTu(), r.getGiaTri());
            }
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Optional<BangChuCai> getUnitSystemInfo() {
        return bangChuCaiRepository.findByMa("UNIT_SYSTEM")
                .or(() -> bangChuCaiRepository.findByMa("JOHARI_UNIT_SYSTEM"));
    }

    private List<TruongPhai> cachedTruongPhai;
    private List<Nguon> cachedNguon;
    private List<KhaiNiem> cachedKhaiNiem;
    private List<The> cachedThe;

    public synchronized List<TruongPhai> getAllTruongPhai() {
        if (cachedTruongPhai != null) return cachedTruongPhai;
        cachedTruongPhai = truongPhaiRepository.findAll();
        return cachedTruongPhai;
    }

    public synchronized List<Nguon> getAllNguon() {
        if (cachedNguon != null) return cachedNguon;
        cachedNguon = nguonRepository.findAll();
        return cachedNguon;
    }

    public synchronized List<KhaiNiem> getAllKhaiNiem() {
        if (cachedKhaiNiem != null) return cachedKhaiNiem;
        cachedKhaiNiem = khaiNiemRepository.findAllByOrderByIdAsc();
        return cachedKhaiNiem;
    }

    public synchronized List<The> getAllThe() {
        if (cachedThe != null) return cachedThe;
        cachedThe = theRepository.findAllByOrderByNhomAscTenAsc();
        return cachedThe;
    }
}
