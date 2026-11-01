package com.kms301.nkms.inference;

import com.kms301.nkms.model.DonViTriThuc;
import com.kms301.nkms.model.The;
import com.kms301.nkms.engine.HoSoSo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Kết quả suy diễn đầy đủ cho trang Khám phá (/kham-pha):
 * chứa Hồ sơ số (HoSoSo) và kết quả khớp quy tắc + tổng hợp Đồng thuận / Khác biệt cho 3 câu hỏi:
 * - AI ("Tôi là ai?")
 * - GAP_GI ("Tôi đang gặp gì?")
 * - DI_THE_NAO ("Tôi nên đi thế nào?")
 */
public class KhamPhaResult {

    private final HoSoSo hoSoSo;
    private final Map<String, QuestionResult> ketQuaTheoCauHoi; // key: "AI", "GAP_GI", "DI_THE_NAO"

    public KhamPhaResult(HoSoSo hoSoSo, Map<String, QuestionResult> ketQuaTheoCauHoi) {
        this.hoSoSo = hoSoSo;
        this.ketQuaTheoCauHoi = ketQuaTheoCauHoi;
    }

    public HoSoSo getHoSoSo() {
        return hoSoSo;
    }

    public Map<String, QuestionResult> getKetQuaTheoCauHoi() {
        return ketQuaTheoCauHoi;
    }

    public QuestionResult getCauHoi(String maCauHoi) {
        return ketQuaTheoCauHoi.get(maCauHoi);
    }

    /**
     * Một đơn vị tri thức đã khớp quy tắc, kèm nhãn chỉ số kích hoạt và ghi chú tinh chỉnh (nếu có phá thế/ngoại lệ).
     */
    public static class MatchedUnit {
        private final DonViTriThuc donVi;
        private final String chiSoKichHoat;
        private final String chuThichTinhChinh;
        private final DonViTriThuc donViTinhChinh;
        private final String cacBuocTinh;

        public MatchedUnit(DonViTriThuc donVi, String chiSoKichHoat, String chuThichTinhChinh, DonViTriThuc donViTinhChinh) {
            this(donVi, chiSoKichHoat, chuThichTinhChinh, donViTinhChinh, null);
        }

        public MatchedUnit(DonViTriThuc donVi, String chiSoKichHoat, String chuThichTinhChinh,
                           DonViTriThuc donViTinhChinh, String cacBuocTinh) {
            this.donVi = donVi;
            this.chiSoKichHoat = chiSoKichHoat;
            this.chuThichTinhChinh = chuThichTinhChinh;
            this.donViTinhChinh = donViTinhChinh;
            this.cacBuocTinh = cacBuocTinh;
        }

        public DonViTriThuc getDonVi() {
            return donVi;
        }

        public String getChiSoKichHoat() {
            return chiSoKichHoat;
        }

        public String getChuThichTinhChinh() {
            return chuThichTinhChinh;
        }

        public DonViTriThuc getDonViTinhChinh() {
            return donViTinhChinh;
        }

        public String getCacBuocTinh() {
            return cacBuocTinh;
        }

        public boolean isGoiYVanHoa() {
            return "GOI_Y_VAN_HOA".equalsIgnoreCase(donVi.getNhanSuDung());
        }
    }

    /**
     * Một điểm đồng thuận giữa >= 2 trường phái (Mục 9.2).
     */
    public static class ConsensusItem {
        private final The the;
        private final int soTruongPhai;
        private final int soNguonGocDocLap;
        private final String cauTomTat;
        private final List<MatchedUnit> danChung;

        public ConsensusItem(The the, int soTruongPhai, int soNguonGocDocLap,
                             String cauTomTat, List<MatchedUnit> danChung) {
            this.the = the;
            this.soTruongPhai = soTruongPhai;
            this.soNguonGocDocLap = soNguonGocDocLap;
            this.cauTomTat = cauTomTat;
            this.danChung = Collections.unmodifiableList(new ArrayList<>(danChung));
        }

        public The getThe() {
            return the;
        }

        public int getSoTruongPhai() {
            return soTruongPhai;
        }

        public int getSoNguonGocDocLap() {
            return soNguonGocDocLap;
        }

        public String getCauTomTat() {
            return cauTomTat;
        }

        public List<MatchedUnit> getDanChung() {
            return danChung;
        }
    }

    /**
     * Một điểm khác biệt giữa các trường phái / nhánh (Mục 9.3: loại a, b, c).
     */
    public static class DifferenceItem {
        private final String loai; // "KHAC_GIA_TRI", "MAU_THUAN_TRUC_TIEP", "THE_DOI_LAP"
        private final String tieuDe;
        private final String moTa;
        private final String maXungDot;
        private final Long xungDotId;
        private final List<MatchedUnit> danChung;

        public DifferenceItem(String loai, String tieuDe, String moTa,
                              String maXungDot, Long xungDotId, List<MatchedUnit> danChung) {
            this.loai = loai;
            this.tieuDe = tieuDe;
            this.moTa = moTa;
            this.maXungDot = maXungDot;
            this.xungDotId = xungDotId;
            this.danChung = danChung == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(danChung));
        }

        public String getLoai() {
            return loai;
        }

        public String getTieuDe() {
            return tieuDe;
        }

        public String getMoTa() {
            return moTa;
        }

        public String getMaXungDot() {
            return maXungDot;
        }

        public Long getXungDotId() {
            return xungDotId;
        }

        public List<MatchedUnit> getDanChung() {
            return danChung;
        }
    }

    /**
     * Toàn bộ kết quả của một tab câu hỏi (AI, GAP_GI, hoặc DI_THE_NAO).
     */
    public static class QuestionResult {
        private final String maCauHoi;
        private final String tenCauHoi;
        private final List<ConsensusItem> dongThuan;
        private final List<DifferenceItem> khacBiet;
        private final List<MatchedUnit> pythagorasQhPhillips;
        private final List<MatchedUnit> pythagorasDecoz;
        private final List<MatchedUnit> vedaJohari;
        private final List<MatchedUnit> vedaJohariGoiYVanHoa;
        private final List<MatchedUnit> kabbalahSepharial;

        public QuestionResult(String maCauHoi, String tenCauHoi,
                              List<ConsensusItem> dongThuan,
                              List<DifferenceItem> khacBiet,
                              List<MatchedUnit> pythagorasQhPhillips,
                              List<MatchedUnit> pythagorasDecoz,
                              List<MatchedUnit> vedaJohari,
                              List<MatchedUnit> vedaJohariGoiYVanHoa,
                              List<MatchedUnit> kabbalahSepharial) {
            this.maCauHoi = maCauHoi;
            this.tenCauHoi = tenCauHoi;
            this.dongThuan = Collections.unmodifiableList(new ArrayList<>(dongThuan));
            this.khacBiet = Collections.unmodifiableList(new ArrayList<>(khacBiet));
            this.pythagorasQhPhillips = Collections.unmodifiableList(new ArrayList<>(pythagorasQhPhillips));
            this.pythagorasDecoz = Collections.unmodifiableList(new ArrayList<>(pythagorasDecoz));
            this.vedaJohari = Collections.unmodifiableList(new ArrayList<>(vedaJohari));
            this.vedaJohariGoiYVanHoa = Collections.unmodifiableList(new ArrayList<>(vedaJohariGoiYVanHoa));
            this.kabbalahSepharial = Collections.unmodifiableList(new ArrayList<>(kabbalahSepharial));
        }

        public String getMaCauHoi() {
            return maCauHoi;
        }

        public String getTenCauHoi() {
            return tenCauHoi;
        }

        public List<ConsensusItem> getDongThuan() {
            return dongThuan;
        }

        public List<DifferenceItem> getKhacBiet() {
            return khacBiet;
        }

        public List<MatchedUnit> getPythagorasQhPhillips() {
            return pythagorasQhPhillips;
        }

        public List<MatchedUnit> getPythagorasDecoz() {
            return pythagorasDecoz;
        }

        public List<MatchedUnit> getVedaJohari() {
            return vedaJohari;
        }

        public List<MatchedUnit> getVedaJohariGoiYVanHoa() {
            return vedaJohariGoiYVanHoa;
        }

        public List<MatchedUnit> getKabbalahSepharial() {
            return kabbalahSepharial;
        }
    }
}

