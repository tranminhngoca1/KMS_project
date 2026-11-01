package com.kms301.nkms.service;

import com.kms301.nkms.model.*;
import com.kms301.nkms.engine.HoSoSo;
import com.kms301.nkms.engine.NameNormalizer;
import com.kms301.nkms.inference.InferenceEngine;
import com.kms301.nkms.inference.KhamPhaResult;
import com.kms301.nkms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Dịch vụ thực hiện tính toán và suy diễn cho trang Khám phá (/kham-pha).
 * Quyền riêng tư (Mục 2): TUYỆT ĐỐI không lưu họ tên hay ngày sinh của người dùng vào CSDL hay log.
 */
@Service
public class KhamPhaService {

    private final BangChuCaiRepository bangChuCaiRepository;
    private final BangChuCaiGiaTriRepository bangChuCaiGiaTriRepository;
    private final DonViTriThucRepository donViTriThucRepository;
    private final QuanHeTriThucRepository quanHeTriThucRepository;
    private final XungDotRepository xungDotRepository;
    private final CapTheDoiLapRepository capTheDoiLapRepository;
    private final InferenceEngine inferenceEngine;

    public KhamPhaService(BangChuCaiRepository bangChuCaiRepository,
                          BangChuCaiGiaTriRepository bangChuCaiGiaTriRepository,
                          DonViTriThucRepository donViTriThucRepository,
                          QuanHeTriThucRepository quanHeTriThucRepository,
                          XungDotRepository xungDotRepository,
                          CapTheDoiLapRepository capTheDoiLapRepository,
                          InferenceEngine inferenceEngine) {
        this.bangChuCaiRepository = bangChuCaiRepository;
        this.bangChuCaiGiaTriRepository = bangChuCaiGiaTriRepository;
        this.donViTriThucRepository = donViTriThucRepository;
        this.quanHeTriThucRepository = quanHeTriThucRepository;
        this.xungDotRepository = xungDotRepository;
        this.capTheDoiLapRepository = capTheDoiLapRepository;
        this.inferenceEngine = inferenceEngine;
    }

    @Transactional(readOnly = true)
    public KhamPhaResult khamPha(String hoTen, LocalDate ngaySinh) {
        LocalDate today = LocalDate.now();
        return khamPha(hoTen, ngaySinh, today.getYear(), today.getMonthValue());
    }

    @Transactional(readOnly = true)
    public KhamPhaResult khamPha(String hoTen, LocalDate ngaySinh, int namHienTai, int thangHienTai) {
        // 1. Nạp bảng chữ cái từ CSDL (Mục 5: không viết cứng trong code)
        Map<Character, Integer> pythagorasMap = loadAlphabetMap("PYTHAGORAS", NameNormalizer.getDefaultPythagorasMap());

        boolean unitSystemActive = false;
        Map<Character, Integer> unitSystemMap = new HashMap<>();
        Optional<BangChuCai> unitSysOpt = bangChuCaiRepository.findByMa("UNIT_SYSTEM");
        if (unitSysOpt.isPresent() && "KICH_HOAT".equalsIgnoreCase(unitSysOpt.get().getTrangThai())) {
            List<BangChuCaiGiaTri> rows = bangChuCaiGiaTriRepository.findByBangChuCaiIdOrderByKyTuAsc(unitSysOpt.get().getId());
            for (BangChuCaiGiaTri r : rows) {
                if (r.getKyTu() != null && !r.getKyTu().isBlank() && r.getGiaTri() != null) {
                    unitSystemMap.put(Character.toUpperCase(r.getKyTu().trim().charAt(0)), r.getGiaTri());
                }
            }
            if (!unitSystemMap.isEmpty()) {
                unitSystemActive = true;
            }
        }

        // 2. Xây dựng Hồ sơ số (HoSoSo) cho cả 3 trường phái
        HoSoSo hoSoSo = HoSoSo.build(hoTen, ngaySinh, namHienTai, thangHienTai,
                pythagorasMap, unitSystemMap, unitSystemActive);

        // 3. Nạp các đơn vị tri thức DA_DUYET có nhãn DUNG hoặc GOI_Y_VAN_HOA (JOIN FETCH 1 lần tránh N+1)
        List<DonViTriThuc> activeUnits = donViTriThucRepository.findByTrangThaiAndNhanSuDungInOrderByIdAsc(
                "DA_DUYET", List.of("DUNG", "GOI_Y_VAN_HOA"));
        List<QuanHeTriThuc> allRelations = quanHeTriThucRepository.findAllWithUnits();
        List<XungDot> allConflicts = xungDotRepository.findAllWithUnits();
        List<CapTheDoiLap> oppositeTagPairs = capTheDoiLapRepository.findAll();

        // 4. Suy diễn & tổng hợp Đồng thuận / Khác biệt
        return inferenceEngine.evaluate(hoSoSo, activeUnits, allRelations, allConflicts, oppositeTagPairs);
    }

    private Map<Character, Integer> loadAlphabetMap(String maBang, Map<Character, Integer> fallback) {
        Optional<BangChuCai> opt = bangChuCaiRepository.findByMa(maBang);
        if (opt.isEmpty() || !"KICH_HOAT".equalsIgnoreCase(opt.get().getTrangThai())) {
            return fallback;
        }
        List<BangChuCaiGiaTri> rows = bangChuCaiGiaTriRepository.findByBangChuCaiIdOrderByKyTuAsc(opt.get().getId());
        if (rows.isEmpty()) {
            return fallback;
        }
        Map<Character, Integer> map = new HashMap<>();
        for (BangChuCaiGiaTri r : rows) {
            if (r.getKyTu() != null && !r.getKyTu().isBlank() && r.getGiaTri() != null) {
                map.put(Character.toUpperCase(r.getKyTu().trim().charAt(0)), r.getGiaTri());
            }
        }
        return map.isEmpty() ? fallback : map;
    }
}
