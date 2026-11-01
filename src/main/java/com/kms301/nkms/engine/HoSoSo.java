package com.kms301.nkms.engine;

import com.kms301.nkms.engine.kabbalah.SepharialCalculator;
import com.kms301.nkms.engine.pythagoras.DecozCalculator;
import com.kms301.nkms.engine.pythagoras.QhCalculator;
import com.kms301.nkms.engine.veda.JohariCalculator;

import java.time.LocalDate;
import java.util.Map;

/**
 * Hồ sơ số (HoSoSo) chứa toàn bộ kết quả tính toán và các bước trung gian của cả 3 trường phái
 * cho một lần tra cứu tại trang Khám phá (không lưu vào CSDL để bảo vệ quyền riêng tư).
 */
public class HoSoSo {

    private final NameNormalizer.ParsedName parsedName;
    private final LocalDate ngaySinh;
    private final int namHienTai;
    private final int thangHienTai;
    private final int tuoiHienTai;
    private final QhCalculator.QhProfile qh;
    private final DecozCalculator.DecozProfile decoz;
    private final JohariCalculator.JohariProfile johari;
    private final SepharialCalculator.SepharialProfile sepharial;
    private final String chuThichTenChung = "Johari và Phillips coi tên được gọi nhiều nhất là quan trọng nhất; hệ thống dùng họ tên đầy đủ bạn nhập.";

    public HoSoSo(NameNormalizer.ParsedName parsedName, LocalDate ngaySinh,
                  int namHienTai, int thangHienTai, int tuoiHienTai,
                  QhCalculator.QhProfile qh, DecozCalculator.DecozProfile decoz,
                  JohariCalculator.JohariProfile johari, SepharialCalculator.SepharialProfile sepharial) {
        this.parsedName = parsedName;
        this.ngaySinh = ngaySinh;
        this.namHienTai = namHienTai;
        this.thangHienTai = thangHienTai;
        this.tuoiHienTai = tuoiHienTai;
        this.qh = qh;
        this.decoz = decoz;
        this.johari = johari;
        this.sepharial = sepharial;
    }

    public static HoSoSo build(String hoTen, LocalDate ngaySinh, int namHienTai, int thangHienTai,
                               Map<Character, Integer> pythagorasMap,
                               Map<Character, Integer> unitSystemMap, boolean unitSystemActive) {
        NameNormalizer.ParsedName parsed = NameNormalizer.parse(hoTen);
        int age = Math.max(0, namHienTai - ngaySinh.getYear());

        QhCalculator qhCalculator = new QhCalculator();
        DecozCalculator decozCalculator = new DecozCalculator();
        JohariCalculator johariCalculator = new JohariCalculator();
        SepharialCalculator sepharialCalculator = new SepharialCalculator();

        QhCalculator.QhProfile qhProfile = qhCalculator.calculateAll(
                ngaySinh, parsed.getNormalizedName(), namHienTai, thangHienTai, pythagorasMap);
        DecozCalculator.DecozProfile decozProfile = decozCalculator.calculateAll(
                ngaySinh, parsed.getNormalizedName(), namHienTai, thangHienTai, pythagorasMap);
        JohariCalculator.JohariProfile johariProfile = johariCalculator.calculateAll(
                ngaySinh, parsed.getNormalizedName(), namHienTai, unitSystemMap, unitSystemActive);
        SepharialCalculator.SepharialProfile sepharialProfile = sepharialCalculator.calculateAll(ngaySinh);

        return new HoSoSo(parsed, ngaySinh, namHienTai, thangHienTai, age,
                qhProfile, decozProfile, johariProfile, sepharialProfile);
    }

    public NameNormalizer.ParsedName getParsedName() {
        return parsedName;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public int getNamHienTai() {
        return namHienTai;
    }

    public int getThangHienTai() {
        return thangHienTai;
    }

    public int getTuoiHienTai() {
        return tuoiHienTai;
    }

    public QhCalculator.QhProfile getQh() {
        return qh;
    }

    public DecozCalculator.DecozProfile getDecoz() {
        return decoz;
    }

    public JohariCalculator.JohariProfile getJohari() {
        return johari;
    }

    public SepharialCalculator.SepharialProfile getSepharial() {
        return sepharial;
    }

    public String getChuThichTenChung() {
        return chuThichTenChung;
    }
}

