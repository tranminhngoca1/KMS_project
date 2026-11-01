package com.kms301.nkms.engine.pythagoras;

import com.kms301.nkms.engine.NameNormalizer;
import com.kms301.nkms.engine.NumberReducer;
import com.kms301.nkms.engine.StepResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Bộ máy tính toán trường phái PYTHAGORAS — nhánh QH / PHILLIPS (Mục 6.1).
 */
public class QhCalculator {

    /**
     * Con số chủ đạo: Cộng tất cả chữ số của ngày, tháng, năm sinh -> reduceQH (2–11, 22/4).
     */
    public StepResult calculateConSoChuDao(LocalDate birthDate) {
        String rawDigits = String.valueOf(birthDate.getDayOfMonth())
                + birthDate.getMonthValue()
                + birthDate.getYear();
        StringBuilder expr = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < rawDigits.length(); i++) {
            int d = rawDigits.charAt(i) - '0';
            if (d == 0 && rawDigits.length() > 1) {
                continue;
            }
            if (expr.length() > 0) {
                expr.append("+");
            }
            expr.append(d);
            sum += d;
        }
        return NumberReducer.reduceQH(sum, expr.toString());
    }

    /**
     * Con số ngày sinh: Ngày 1–11 giữ nguyên; ngày 22 -> 22/4; các ngày khác lấy digitSum (1–11, 22/4).
     */
    public StepResult calculateConSoNgaySinh(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        if (day >= 1 && day <= 11) {
            return new StepResult(day, String.valueOf(day), "Ngày " + day + " (1–11 giữ nguyên) = " + day);
        }
        if (day == 22) {
            return new StepResult(22, "22/4", "Ngày 22 (số bậc thầy giữ nguyên) = 22/4");
        }
        int sum = NumberReducer.digitSum(day);
        String expr = NumberReducer.digitExpression(day);
        return new StepResult(sum, String.valueOf(sum), expr + " = " + sum);
    }

    /**
     * Biểu đồ ngày sinh + Mũi tên + Số cô lập + Biểu đồ tên chồng lên (phá thế).
     */
    public BirthChart calculateBirthChart(LocalDate birthDate, String fullName, Map<Character, Integer> alphabetMap) {
        return new BirthChart(birthDate, fullName, alphabetMap);
    }

    /**
     * Con số linh hồn (QH phần 12): Tổng nguyên âm của họ tên -> rút gọn đến <= 11 (1–11).
     */
    public StepResult calculateConSoLinhHon(String rawName, Map<Character, Integer> alphabetMap) {
        Map<Character, Integer> map = (alphabetMap == null || alphabetMap.isEmpty())
                ? NameNormalizer.getDefaultPythagorasMap()
                : alphabetMap;
        String norm = NameNormalizer.normalize(rawName);
        StringBuilder letters = new StringBuilder();
        StringBuilder nums = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            if (NameNormalizer.isVowel(c)) {
                int val = map.getOrDefault(c, 0);
                if (val > 0) {
                    if (letters.length() > 0) {
                        letters.append(", ");
                        nums.append("+");
                    }
                    letters.append(c).append("(").append(val).append(")");
                    nums.append(val);
                    sum += val;
                }
            }
        }
        if (sum == 0) {
            return new StepResult(0, "0", "Không có nguyên âm");
        }
        String prefix = "Nguyên âm [" + letters + "]: " + nums;
        return NumberReducer.reduceQHFlexible(sum, prefix, false);
    }

    /**
     * Con số biểu đạt (QH phần 12): Tổng phụ âm -> rút gọn đến <= 11, giữ 22/4 (1–11, 22/4).
     */
    public StepResult calculateConSoBieuDat(String rawName, Map<Character, Integer> alphabetMap) {
        Map<Character, Integer> map = (alphabetMap == null || alphabetMap.isEmpty())
                ? NameNormalizer.getDefaultPythagorasMap()
                : alphabetMap;
        String norm = NameNormalizer.normalize(rawName);
        StringBuilder letters = new StringBuilder();
        StringBuilder nums = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            if (NameNormalizer.isConsonant(c)) {
                int val = map.getOrDefault(c, 0);
                if (val > 0) {
                    if (letters.length() > 0) {
                        letters.append(", ");
                        nums.append("+");
                    }
                    letters.append(c).append("(").append(val).append(")");
                    nums.append(val);
                    sum += val;
                }
            }
        }
        if (sum == 0) {
            return new StepResult(0, "0", "Không có phụ âm");
        }
        String prefix = "Phụ âm [" + letters + "]: " + nums;
        return NumberReducer.reduceQHFlexible(sum, prefix, true);
    }

    /**
     * Con số tên riêng (QH phần 12): Linh hồn + Biểu đạt -> reduceQH (2–11, 22/4).
     */
    public StepResult calculateConSoTenRieng(StepResult linhHon, StepResult bieuDat) {
        int lh = linhHon.getNumericValue();
        int bd = bieuDat.getNumericValue() == 22 ? 4 : bieuDat.getNumericValue();
        int sum = lh + bd;
        String prefix = "Linh hồn (" + linhHon.getDisplayValue() + ") + Biểu đạt (" + bieuDat.getDisplayValue() + ") = " + lh + "+" + bd;
        return NumberReducer.reduceQH(sum, prefix);
    }

    /**
     * Năm thế giới: digitSum(năm hiện tại) -> reduceTo9 (1–9).
     */
    public StepResult calculateNamTheGioi(int currentYear) {
        int sum = NumberReducer.digitSum(currentYear);
        String expr = NumberReducer.digitExpression(currentYear);
        return NumberReducer.reduceTo9(sum, expr);
    }

    /**
     * Năm cá nhân (PYN): Năm thế giới + digitSum(tháng sinh) + digitSum(ngày sinh) -> reduceTo9 (1–9).
     */
    public StepResult calculateNamCaNhan(LocalDate birthDate, int currentYear) {
        StepResult namTG = calculateNamTheGioi(currentYear);
        int sumMonth = NumberReducer.digitSum(birthDate.getMonthValue());
        int sumDay = NumberReducer.digitSum(birthDate.getDayOfMonth());
        int total = namTG.getNumericValue() + sumMonth + sumDay;
        String prefix = "Năm TG (" + namTG.getNumericValue() + ") + Tháng (" + sumMonth + ") + Ngày (" + sumDay + ") = "
                + namTG.getNumericValue() + "+" + sumMonth + "+" + sumDay;
        return NumberReducer.reduceTo9(total, prefix);
    }

    /**
     * Tháng cá nhân: PYN + reduceTo9(tháng dương lịch hiện tại) -> reduceTo9 (1–9).
     */
    public StepResult calculateThangCaNhan(int pyn, int currentMonth) {
        StepResult redMonth = NumberReducer.reduceTo9(currentMonth);
        int total = pyn + redMonth.getNumericValue();
        String prefix = "PYN (" + pyn + ") + Tháng (" + redMonth.getNumericValue() + ") = " + pyn + "+" + redMonth.getNumericValue();
        return NumberReducer.reduceTo9(total, prefix);
    }

    /**
     * Kim tự tháp, 4 đỉnh & Ba giai đoạn đời (QH phần 10, 11; PHILLIPS ch.10, 11).
     */
    public PyramidResult calculateKimTuThap(LocalDate birthDate, StepResult conSoChuDao, int currentAge) {
        StepResult baseMonth = NumberReducer.reduceTo9(NumberReducer.digitSum(birthDate.getMonthValue()),
                NumberReducer.digitExpression(birthDate.getMonthValue()));
        StepResult baseDay = NumberReducer.reduceTo9(NumberReducer.digitSum(birthDate.getDayOfMonth()),
                NumberReducer.digitExpression(birthDate.getDayOfMonth()));
        StepResult baseYear = NumberReducer.reduceTo9(NumberReducer.digitSum(birthDate.getYear()),
                NumberReducer.digitExpression(birthDate.getYear()));

        int m = baseMonth.getNumericValue();
        int d = baseDay.getNumericValue();
        int y = baseYear.getNumericValue();

        StepResult peak1 = NumberReducer.reduceTo9(m + d, "Chân Tháng (" + m + ") + Chân Ngày (" + d + ")");
        StepResult peak2 = NumberReducer.reduceTo9(d + y, "Chân Ngày (" + d + ") + Chân Năm (" + y + ")");
        StepResult peak3 = NumberReducer.reduceQHFlexible(peak1.getNumericValue() + peak2.getNumericValue(),
                "Đỉnh 1 (" + peak1.getNumericValue() + ") + Đỉnh 2 (" + peak2.getNumericValue() + ")", false);
        StepResult peak4 = NumberReducer.reduceQHFlexible(m + y,
                "Chân Tháng (" + m + ") + Chân Năm (" + y + ")", false);

        int chuDaoSub = ("22/4".equals(conSoChuDao.getDisplayValue()) || conSoChuDao.getNumericValue() == 22)
                ? 4
                : conSoChuDao.getNumericValue();
        int agePeak1 = 36 - chuDaoSub;
        int agePeak2 = agePeak1 + 9;
        int agePeak3 = agePeak2 + 9;
        int agePeak4 = agePeak3 + 9;

        List<PeakInfo> peaks = List.of(
                new PeakInfo(1, peak1.getNumericValue(), peak1.getDisplayValue(), agePeak1, peak1.getSteps()),
                new PeakInfo(2, peak2.getNumericValue(), peak2.getDisplayValue(), agePeak2, peak2.getSteps()),
                new PeakInfo(3, peak3.getNumericValue(), peak3.getDisplayValue(), agePeak3, peak3.getSteps()),
                new PeakInfo(4, peak4.getNumericValue(), peak4.getDisplayValue(), agePeak4, peak4.getSteps())
        );

        PeakInfo currentPeak;
        PeakInfo nextPeak;
        if (currentAge <= agePeak1) {
            currentPeak = peaks.get(0);
            nextPeak = peaks.get(1);
        } else if (currentAge <= agePeak2) {
            currentPeak = peaks.get(1);
            nextPeak = peaks.get(2);
        } else if (currentAge <= agePeak3) {
            currentPeak = peaks.get(2);
            nextPeak = peaks.get(3);
        } else {
            currentPeak = peaks.get(3);
            nextPeak = null;
        }

        String currentPhaseCode;
        String currentPhaseName;
        if (currentAge < agePeak1) {
            currentPhaseCode = "NIEN_THIEU";
            currentPhaseName = "Giai đoạn Niên thiếu (0 – " + agePeak1 + " tuổi)";
        } else if (currentAge < agePeak4) {
            currentPhaseCode = "TRUONG_THANH";
            currentPhaseName = "Giai đoạn Trưởng thành (" + agePeak1 + " – " + agePeak4 + " tuổi, 27 năm Kim tự tháp)";
        } else {
            currentPhaseCode = "VIEN_MAN";
            currentPhaseName = "Giai đoạn Viên mãn (từ " + agePeak4 + " tuổi trở đi)";
        }

        String ageStep = "36 − " + chuDaoSub + " (từ Chủ đạo " + conSoChuDao.getDisplayValue() + ") = " + agePeak1
                + " tuổi (Đỉnh 1), cộng 9 năm cho mỗi đỉnh tiếp theo: " + agePeak2 + ", " + agePeak3 + ", " + agePeak4;

        return new PyramidResult(m, d, y, peaks, currentPeak, nextPeak,
                currentPhaseCode, currentPhaseName, ageStep);
    }

    public QhProfile calculateAll(LocalDate birthDate, String fullName, int targetYear, int targetMonth,
                                  Map<Character, Integer> alphabetMap) {
        StepResult chuDao = calculateConSoChuDao(birthDate);
        StepResult ngaySinh = calculateConSoNgaySinh(birthDate);
        BirthChart birthChart = calculateBirthChart(birthDate, fullName, alphabetMap);
        StepResult linhHon = calculateConSoLinhHon(fullName, alphabetMap);
        StepResult bieuDat = calculateConSoBieuDat(fullName, alphabetMap);
        StepResult tenRieng = calculateConSoTenRieng(linhHon, bieuDat);
        StepResult namTheGioi = calculateNamTheGioi(targetYear);
        StepResult namCaNhanTruoc = calculateNamCaNhan(birthDate, targetYear - 1);
        StepResult namCaNhan = calculateNamCaNhan(birthDate, targetYear);
        StepResult namCaNhanSau = calculateNamCaNhan(birthDate, targetYear + 1);
        StepResult thangCaNhan = calculateThangCaNhan(namCaNhan.getNumericValue(), targetMonth);
        int currentAge = Math.max(0, targetYear - birthDate.getYear());
        PyramidResult kimTuThap = calculateKimTuThap(birthDate, chuDao, currentAge);

        return new QhProfile(chuDao, ngaySinh, birthChart, linhHon, bieuDat, tenRieng,
                namTheGioi, namCaNhanTruoc, namCaNhan, namCaNhanSau, thangCaNhan, kimTuThap);
    }

    public static class PeakInfo {
        private final int index;
        private final int numericValue;
        private final String displayValue;
        private final int ageReached;
        private final String steps;

        public PeakInfo(int index, int numericValue, String displayValue, int ageReached, String steps) {
            this.index = index;
            this.numericValue = numericValue;
            this.displayValue = displayValue;
            this.ageReached = ageReached;
            this.steps = steps;
        }

        public int getIndex() {
            return index;
        }

        public int getNumericValue() {
            return numericValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public int getAgeReached() {
            return ageReached;
        }

        public String getSteps() {
            return steps;
        }
    }

    public static class PyramidResult {
        private final int baseMonth;
        private final int baseDay;
        private final int baseYear;
        private final List<PeakInfo> peaks;
        private final PeakInfo currentPeak;
        private final PeakInfo nextPeak;
        private final String currentPhaseCode;
        private final String currentPhaseName;
        private final String ageFormulaSteps;

        public PyramidResult(int baseMonth, int baseDay, int baseYear, List<PeakInfo> peaks,
                             PeakInfo currentPeak, PeakInfo nextPeak,
                             String currentPhaseCode, String currentPhaseName, String ageFormulaSteps) {
            this.baseMonth = baseMonth;
            this.baseDay = baseDay;
            this.baseYear = baseYear;
            this.peaks = Collections.unmodifiableList(new ArrayList<>(peaks));
            this.currentPeak = currentPeak;
            this.nextPeak = nextPeak;
            this.currentPhaseCode = currentPhaseCode;
            this.currentPhaseName = currentPhaseName;
            this.ageFormulaSteps = ageFormulaSteps;
        }

        public int getBaseMonth() {
            return baseMonth;
        }

        public int getBaseDay() {
            return baseDay;
        }

        public int getBaseYear() {
            return baseYear;
        }

        public List<PeakInfo> getPeaks() {
            return peaks;
        }

        public PeakInfo getCurrentPeak() {
            return currentPeak;
        }

        public PeakInfo getNextPeak() {
            return nextPeak;
        }

        public String getCurrentPhaseCode() {
            return currentPhaseCode;
        }

        public String getCurrentPhaseName() {
            return currentPhaseName;
        }

        public String getAgeFormulaSteps() {
            return ageFormulaSteps;
        }
    }

    public static class QhProfile {
        private final StepResult conSoChuDao;
        private final StepResult conSoNgaySinh;
        private final BirthChart birthChart;
        private final StepResult conSoLinhHon;
        private final StepResult conSoBieuDat;
        private final StepResult conSoTenRieng;
        private final StepResult namTheGioi;
        private final StepResult namCaNhanTruoc;
        private final StepResult namCaNhan;
        private final StepResult namCaNhanSau;
        private final StepResult thangCaNhan;
        private final PyramidResult kimTuThap;

        public QhProfile(StepResult conSoChuDao, StepResult conSoNgaySinh, BirthChart birthChart,
                         StepResult conSoLinhHon, StepResult conSoBieuDat, StepResult conSoTenRieng,
                         StepResult namTheGioi, StepResult namCaNhanTruoc, StepResult namCaNhan,
                         StepResult namCaNhanSau, StepResult thangCaNhan, PyramidResult kimTuThap) {
            this.conSoChuDao = conSoChuDao;
            this.conSoNgaySinh = conSoNgaySinh;
            this.birthChart = birthChart;
            this.conSoLinhHon = conSoLinhHon;
            this.conSoBieuDat = conSoBieuDat;
            this.conSoTenRieng = conSoTenRieng;
            this.namTheGioi = namTheGioi;
            this.namCaNhanTruoc = namCaNhanTruoc;
            this.namCaNhan = namCaNhan;
            this.namCaNhanSau = namCaNhanSau;
            this.thangCaNhan = thangCaNhan;
            this.kimTuThap = kimTuThap;
        }

        public StepResult getConSoChuDao() {
            return conSoChuDao;
        }

        public StepResult getConSoNgaySinh() {
            return conSoNgaySinh;
        }

        public BirthChart getBirthChart() {
            return birthChart;
        }

        public StepResult getConSoLinhHon() {
            return conSoLinhHon;
        }

        public StepResult getConSoBieuDat() {
            return conSoBieuDat;
        }

        public StepResult getConSoTenRieng() {
            return conSoTenRieng;
        }

        public StepResult getNamTheGioi() {
            return namTheGioi;
        }

        public StepResult getNamCaNhanTruoc() {
            return namCaNhanTruoc;
        }

        public StepResult getNamCaNhan() {
            return namCaNhan;
        }

        public StepResult getNamCaNhanSau() {
            return namCaNhanSau;
        }

        public StepResult getThangCaNhan() {
            return thangCaNhan;
        }

        public PyramidResult getKimTuThap() {
            return kimTuThap;
        }
    }
}

