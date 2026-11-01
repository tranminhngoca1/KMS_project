package com.kms301.nkms.engine;

import com.kms301.nkms.engine.kabbalah.SepharialCalculator;
import com.kms301.nkms.engine.pythagoras.BirthChart;
import com.kms301.nkms.engine.pythagoras.DecozCalculator;
import com.kms301.nkms.engine.pythagoras.QhCalculator;
import com.kms301.nkms.engine.veda.JohariCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử JUnit 5 bắt buộc cho toàn bộ bộ máy tính toán (Mục 6.6).
 */
class NumerologyEngineTest {

    private final QhCalculator qhCalculator = new QhCalculator();
    private final DecozCalculator decozCalculator = new DecozCalculator();
    private final JohariCalculator johariCalculator = new JohariCalculator();
    private final SepharialCalculator sepharialCalculator = new SepharialCalculator();

    @Test
    @DisplayName("1. 27/05/1983 -> QH chủ đạo = 8 (2+7+5+1+9+8+3 = 35 -> 8) [QH phần 5]")
    void testQhChuDao_27_05_1983() {
        LocalDate date = LocalDate.of(1983, 5, 27);
        StepResult res = qhCalculator.calculateConSoChuDao(date);
        assertEquals(8, res.getNumericValue());
        assertEquals("8", res.getDisplayValue());
        assertTrue(res.getSteps().contains("35"));
        assertTrue(res.getSteps().contains("3+5 = 8"));
    }

    @Test
    @DisplayName("2. 29/05/1948 -> DECOZ Đường đời = 11; QH chủ đạo = 11 (38 -> 11) [DECOZ ch.2]")
    void test_29_05_1948() {
        LocalDate date = LocalDate.of(1948, 5, 29);

        StepResult decoz = decozCalculator.calculateSoDuongDoi(date);
        assertEquals(11, decoz.getNumericValue());
        assertEquals("11", decoz.getDisplayValue());

        StepResult qh = qhCalculator.calculateConSoChuDao(date);
        assertEquals(11, qh.getNumericValue());
        assertEquals("11", qh.getDisplayValue());
        assertTrue(qh.getSteps().contains("38"));
    }

    @Test
    @DisplayName("3. 25/12/1964 -> DECOZ Đường đời = 3 [DECOZ ch.2]")
    void testDecozLifePath_25_12_1964() {
        LocalDate date = LocalDate.of(1964, 12, 25);
        StepResult decoz = decozCalculator.calculateSoDuongDoi(date);
        assertEquals(3, decoz.getNumericValue());
        assertEquals("3", decoz.getDisplayValue());
    }

    @Test
    @DisplayName("4. 21/04/1926 -> QH chủ đạo = 7 [QH phần 12]")
    void testQhChuDao_21_04_1926() {
        LocalDate date = LocalDate.of(1926, 4, 21);
        StepResult qh = qhCalculator.calculateConSoChuDao(date);
        assertEquals(7, qh.getNumericValue());
        assertEquals("7", qh.getDisplayValue());
    }

    @Test
    @DisplayName("5. 01/05/1974, năm 2020 -> Năm thế giới 4; PYN = 1; Chủ đạo 9; các đỉnh 6 (27 tuổi), 4 (36), 10 (45), 8 (54) [QH phần 9, 11]")
    void testQhPyramidsAndPyn_01_05_1974_Year2020() {
        LocalDate date = LocalDate.of(1974, 5, 1);

        StepResult namTG = qhCalculator.calculateNamTheGioi(2020);
        assertEquals(4, namTG.getNumericValue());

        StepResult pyn = qhCalculator.calculateNamCaNhan(date, 2020);
        assertEquals(1, pyn.getNumericValue());

        StepResult chuDao = qhCalculator.calculateConSoChuDao(date);
        assertEquals(9, chuDao.getNumericValue());

        QhCalculator.PyramidResult pyramid = qhCalculator.calculateKimTuThap(date, chuDao, 2020 - 1974);
        List<QhCalculator.PeakInfo> peaks = pyramid.getPeaks();
        assertEquals(4, peaks.size());

        assertEquals("6", peaks.get(0).getDisplayValue());
        assertEquals(27, peaks.get(0).getAgeReached());

        assertEquals("4", peaks.get(1).getDisplayValue());
        assertEquals(36, peaks.get(1).getAgeReached());

        assertEquals("10", peaks.get(2).getDisplayValue());
        assertEquals(45, peaks.get(2).getAgeReached());

        assertEquals("8", peaks.get(3).getDisplayValue());
        assertEquals(54, peaks.get(3).getAgeReached());
    }

    @Test
    @DisplayName("6. Sinh 12/05, xem năm 1991 -> 12/05/1991 là Chủ nhật -> 5 + 12 + 91 + 1 = 109 -> Johari số năm = 1 [JOHARI §14]")
    void testJohariSoNam_12_05_Year1991() {
        LocalDate birthDate = LocalDate.of(1934, 5, 12);
        StepResult soNam = johariCalculator.calculateSoNam(birthDate, 1991);
        assertEquals(1, soNam.getNumericValue());
        assertEquals("1", soNam.getDisplayValue());
        assertTrue(soNam.getSteps().contains("Chủ nhật"));
        assertTrue(soNam.getSteps().contains("5 + 12 + 91 + 1 = 109"));
    }

    @Test
    @DisplayName("7. Sinh 15/05, xem năm 1992 -> DECOZ năm cá nhân: 5 + 6 + 3 = 14 -> 5 [DECOZ tr.179]")
    void testDecozPersonalYear_15_05_Year1992() {
        LocalDate birthDate = LocalDate.of(1960, 5, 15);
        StepResult pyn = decozCalculator.calculateNamCaNhan(birthDate, 1992);
        assertEquals(5, pyn.getNumericValue());
        assertTrue(pyn.getSteps().contains("5+6+3 = 14"));
    }

    @Test
    @DisplayName("8. PYN 5, tháng 7 -> Tháng cá nhân 3 [DECOZ tr.188]")
    void testPersonalMonth_Pyn5_Month7() {
        StepResult qhMonth = qhCalculator.calculateThangCaNhan(5, 7);
        assertEquals(3, qhMonth.getNumericValue());

        StepResult decozMonth = decozCalculator.calculateThangCaNhan(5, 7);
        assertEquals(3, decozMonth.getNumericValue());
    }

    @Test
    @DisplayName("9. 15/05/1949 -> DECOZ đỉnh: 11, 11, 22, 1; Đỉnh 1 kết thúc ở 36 - 7 = 29 tuổi [DECOZ tr.218-219]")
    void testDecozPinnacles_15_05_1949() {
        LocalDate date = LocalDate.of(1949, 5, 15);
        StepResult duongDoi = decozCalculator.calculateSoDuongDoi(date);
        assertEquals(7, duongDoi.getNumericValue());

        List<QhCalculator.PeakInfo> pinnacles = decozCalculator.calculatePinnacles(date, duongDoi);
        assertEquals(4, pinnacles.size());
        assertEquals("11", pinnacles.get(0).getDisplayValue());
        assertEquals(29, pinnacles.get(0).getAgeReached());

        assertEquals("11", pinnacles.get(1).getDisplayValue());
        assertEquals(38, pinnacles.get(1).getAgeReached());

        assertEquals("22", pinnacles.get(2).getDisplayValue());
        assertEquals(47, pinnacles.get(2).getAgeReached());

        assertEquals("1", pinnacles.get(3).getDisplayValue());
        assertEquals(56, pinnacles.get(3).getAgeReached());
    }

    @Test
    @DisplayName("10. 4 Ca xung đột (dữ liệu demo): 04/01/1985, 11/06/1985, 02/01/1990, 19/08/2000")
    void testFourConflictCases() {
        // Ca 1: 04/01/1985 -> QH 10 · DECOZ 1 · Johari 1
        LocalDate d1 = LocalDate.of(1985, 1, 4);
        assertEquals("10", qhCalculator.calculateConSoChuDao(d1).getDisplayValue());
        assertEquals("1", decozCalculator.calculateSoDuongDoi(d1).getDisplayValue());
        assertEquals("1", johariCalculator.calculateSoDinhMenh(d1).getDisplayValue());

        // Ca 2: 11/06/1985 -> QH 4 · DECOZ 22 · Johari 4
        LocalDate d2 = LocalDate.of(1985, 6, 11);
        assertEquals("4", qhCalculator.calculateConSoChuDao(d2).getDisplayValue());
        assertEquals("22", decozCalculator.calculateSoDuongDoi(d2).getDisplayValue());
        assertEquals("4", johariCalculator.calculateSoDinhMenh(d2).getDisplayValue());

        // Ca 3: 02/01/1990 -> QH 22/4 · DECOZ 4 · Johari 4
        LocalDate d3 = LocalDate.of(1990, 1, 2);
        assertEquals("22/4", qhCalculator.calculateConSoChuDao(d3).getDisplayValue());
        assertEquals("4", decozCalculator.calculateSoDuongDoi(d3).getDisplayValue());
        assertEquals("4", johariCalculator.calculateSoDinhMenh(d3).getDisplayValue());

        // Ca 4: 19/08/2000 -> QH 2 · DECOZ 11 · Johari 2
        LocalDate d4 = LocalDate.of(2000, 8, 19);
        assertEquals("2", qhCalculator.calculateConSoChuDao(d4).getDisplayValue());
        assertEquals("11", decozCalculator.calculateSoDuongDoi(d4).getDisplayValue());
        assertEquals("2", johariCalculator.calculateSoDinhMenh(d4).getDisplayValue());
    }

    @Test
    @DisplayName("11. 'Nguyễn Văn Đức' -> NGUYEN VAN DUC; Linh hồn 10 (19), Biểu đạt 6 (33), Tên riêng 7 (16) [Mục 3.2, 6.1]")
    void testName_NguyenVanDuc() {
        String raw = "  Nguyễn   Văn Đức ";
        NameNormalizer.ParsedName parsed = NameNormalizer.parse(raw);
        assertEquals("NGUYEN VAN DUC", parsed.getNormalizedName());
        assertEquals("NGUYEN", parsed.getHo());
        assertEquals(List.of("VAN"), parsed.getTenDem());
        assertEquals("DUC", parsed.getTenGoi());

        StepResult linhHon = qhCalculator.calculateConSoLinhHon(raw, null);
        assertEquals(10, linhHon.getNumericValue());
        assertTrue(linhHon.getSteps().contains("19"));

        StepResult bieuDat = qhCalculator.calculateConSoBieuDat(raw, null);
        assertEquals(6, bieuDat.getNumericValue());
        assertTrue(bieuDat.getSteps().contains("33"));

        StepResult tenRieng = qhCalculator.calculateConSoTenRieng(linhHon, bieuDat);
        assertEquals(7, tenRieng.getNumericValue());
        assertTrue(tenRieng.getSteps().contains("16"));
    }

    @Test
    @DisplayName("12. 'Lê Thị Thuỳ' -> LE THI THUY; Linh hồn 24 -> 6; Biểu đạt 23 -> 5; Tên riêng 11 [Mục 3.2, 6.1]")
    void testName_LeThiThuy() {
        String raw = "Lê Thị Thuỳ";
        assertEquals("LE THI THUY", NameNormalizer.normalize(raw));

        StepResult linhHon = qhCalculator.calculateConSoLinhHon(raw, null);
        assertEquals(6, linhHon.getNumericValue());
        assertTrue(linhHon.getSteps().contains("24"));

        StepResult bieuDat = qhCalculator.calculateConSoBieuDat(raw, null);
        assertEquals(5, bieuDat.getNumericValue());
        assertTrue(bieuDat.getSteps().contains("23"));

        StepResult tenRieng = qhCalculator.calculateConSoTenRieng(linhHon, bieuDat);
        assertEquals(11, tenRieng.getNumericValue());
        assertEquals("11", tenRieng.getDisplayValue());
    }

    @Test
    @DisplayName("13. Kiểm tra Biểu đồ ngày sinh, Mũi tên, Số cô lập, Phá thế bằng tên và Kiểm tra ngày sinh")
    void testBirthChartArrowsIsolatedAndValidation() {
        // Kiểm tra ngày nhuận hợp lệ và không hợp lệ
        assertDoesNotThrow(() -> DateValidator.parseAndValidate("29/02/2020", LocalDate.of(2026, 10, 31)));
        assertThrows(IllegalArgumentException.class, () -> DateValidator.parseAndValidate("29/02/2021", LocalDate.of(2026, 10, 31)));
        assertThrows(IllegalArgumentException.class, () -> DateValidator.parseAndValidate("31/12/1899", LocalDate.of(2026, 10, 31)));

        // Ngày sinh 11/01/1999 -> chỉ có chữ số 1 (x4) và 9 (x3), các ô 2,3,4,5,6,7,8 đều trống!
        // -> Số 1 và số 9 đều bị cô lập; mũi tên 2-5-8, 3-6-9, 4-5-6, 3-5-7 đều trống.
        LocalDate d = LocalDate.of(1999, 1, 11);
        // Tên "NGUYEN" có N(5), G(7), U(3), Y(7), E(5), N(5) -> lấp đầy 3, 5, 7 -> phá thế mũi tên trống 3-5-7!
        BirthChart chart = qhCalculator.calculateBirthChart(d, "NGUYEN", null);
        assertEquals(Set.of(1, 9), chart.getIsolatedNumbers());
        assertTrue(chart.getArrows().hasArrow("3-5-7", "TRONG"));
        assertTrue(chart.getArrows().isCompensatedByName("3-5-7"));
    }

    @Test
    @DisplayName("14. Kiểm tra Decoz Personality (Thomas John Hancock = 5, Nợ nghiệp 14) và Sepharial Ma phương")
    void testDecozPersonalityAndSepharial() {
        StepResult personality = decozCalculator.calculateNhanCach("Thomas John Hancock", null);
        assertEquals(5, personality.getNumericValue());
        assertTrue(personality.getIntermediateSums().contains(14));

        // Sepharial: 25/06/1899 -> ngày 25, tháng 6, năm 99 -> tổng chữ số 2+5+6+9+9 = 31 -> 4
        SepharialCalculator.SepharialProfile seph = sepharialCalculator.calculateAll(LocalDate.of(1899, 6, 25));
        assertEquals(4, seph.getSoChiaKhoa().getNumericValue());
        assertEquals(2, seph.getMagicSquare().getCount(9));
        assertEquals(1, seph.getMagicSquare().getCount(6));
        assertEquals(1, seph.getMagicSquare().getCount(5));
        assertEquals(1, seph.getMagicSquare().getCount(2));
    }
}

