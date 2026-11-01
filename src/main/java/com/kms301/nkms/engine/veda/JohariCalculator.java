package com.kms301.nkms.engine.veda;

import com.kms301.nkms.engine.NameNormalizer;
import com.kms301.nkms.engine.NumberReducer;
import com.kms301.nkms.engine.StepResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bộ máy tính toán trường phái VEDA — JOHARI (Mục 6.3).
 * Johari không có số bậc thầy (vd ngày 11 -> 2, ngày 22 -> 4).
 */
public class JohariCalculator {

    private static final Map<Integer, String> PLANET_NAMES = Map.of(
            1, "Mặt Trời (Surya / Sun)",
            2, "Mặt Trăng (Chandra / Moon)",
            3, "Mộc tinh (Guru / Jupiter)",
            4, "Rahu (La Hầu - Bắc giao điểm)",
            5, "Thuỷ tinh (Budha / Mercury)",
            6, "Kim tinh (Shukra / Venus)",
            7, "Ketu (Kế Đô - Nam giao điểm)",
            8, "Thổ tinh (Shani / Saturn)",
            9, "Hoả tinh (Mangala / Mars)"
    );

    private static final Map<Integer, Integer> EXALTED_DAY_FOR_PSYCHIC = Map.of(
            1, 28,
            2, 29,
            3, 12,
            4, 31,
            5, 23,
            6, 24,
            7, 25,
            8, 26,
            9, 27
    );

    private static final Map<Integer, Set<Integer>> FRIENDLY_MAP = Map.of(
            1, Set.of(1, 2, 3, 5, 6, 9),
            2, Set.of(1, 2, 3, 5),
            3, Set.of(1, 2, 3, 6, 9),
            4, Set.of(1, 4, 5, 6, 7, 8),
            5, Set.of(1, 4, 5, 6, 7, 8),
            6, Set.of(1, 3, 4, 5, 6, 7, 8, 9),
            7, Set.of(1, 4, 5, 6, 7),
            8, Set.of(4, 5, 6, 7, 8),
            9, Set.of(1, 2, 3, 6, 9)
    );

    private static final Map<Integer, Set<Integer>> ENEMY_MAP = Map.of(
            1, Set.of(8),
            2, Set.of(4, 7),
            3, Set.of(5),
            4, Set.of(2),
            5, Set.of(2),
            6, Set.of(),
            7, Set.of(2),
            8, Set.of(1, 2),
            9, Set.of(5)
    );

    /**
     * Số tâm hồn (Psychic Number - JOHARI §1): reduceTo9(ngày sinh) (1–9).
     */
    public StepResult calculateSoTamHon(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        if (day <= 9) {
            return new StepResult(day, String.valueOf(day), "Ngày " + day + " = " + day);
        }
        int sum = NumberReducer.digitSum(day);
        return NumberReducer.reduceTo9(sum, NumberReducer.digitExpression(day));
    }

    /**
     * Số định mệnh (Destiny Number - JOHARI §1): reduceTo9(digitSum ngày + tháng + năm) (1–9).
     */
    public StepResult calculateSoDinhMenh(LocalDate birthDate) {
        String raw = String.valueOf(birthDate.getDayOfMonth())
                + birthDate.getMonthValue()
                + birthDate.getYear();
        StringBuilder expr = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < raw.length(); i++) {
            int d = raw.charAt(i) - '0';
            if (d == 0 && raw.length() > 1) {
                continue;
            }
            if (expr.length() > 0) {
                expr.append("+");
            }
            expr.append(d);
            sum += d;
        }
        return NumberReducer.reduceTo9(sum, expr.toString());
    }

    /**
     * Số tên (Name Number - JOHARI §1):
     * Dùng bảng Unit System -> reduceTo9. Khi bảng chưa có dữ liệu (tạm tắt), trả về trạng thái tắt.
     */
    public NameNumberResult calculateSoTen(String rawName, Map<Character, Integer> unitSystemMap, boolean active) {
        if (!active || unitSystemMap == null || unitSystemMap.isEmpty()) {
            return new NameNumberResult(false, null, "Chưa có dữ liệu bảng chữ cái từ nguồn");
        }
        String norm = NameNormalizer.normalize(rawName);
        int sum = 0;
        StringBuilder expr = new StringBuilder();
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            Integer val = unitSystemMap.get(c);
            if (val != null && val > 0) {
                if (expr.length() > 0) {
                    expr.append("+");
                }
                expr.append(c).append("(").append(val).append(")");
                sum += val;
            }
        }
        if (sum == 0) {
            return new NameNumberResult(true, new StepResult(0, "0", "Không có ký tự khớp"), "0");
        }
        StepResult res = NumberReducer.reduceTo9(sum, "Unit System [" + expr + "]");
        return new NameNumberResult(true, res, res.getDisplayValue());
    }

    /**
     * Số năm (JOHARI §14):
     * tháng sinh + ngày sinh + 2 chữ số cuối của năm cần xem + số của thứ trong tuần mà sinh nhật rơi vào trong năm đó
     * (CN=1, T2=2, ..., T7=7) -> reduceTo9 (1–9).
     */
    public StepResult calculateSoNam(LocalDate birthDate, int targetYear) {
        int month = birthDate.getMonthValue();
        int day = birthDate.getDayOfMonth();
        int lastTwoDigits = Math.abs(targetYear) % 100;

        int bdayInTargetYear = day;
        if (month == 2 && day == 29 && !Year.isLeap(targetYear)) {
            bdayInTargetYear = 28;
        }
        LocalDate birthdayInYear = LocalDate.of(targetYear, month, bdayInTargetYear);
        DayOfWeek dow = birthdayInYear.getDayOfWeek();
        int dowNumber = toJohariDayOfWeekNumber(dow);
        String dowVi = toVietnameseDayOfWeek(dow);

        int sum = month + day + lastTwoDigits + dowNumber;
        String prefix = String.format("%02d/%02d/%d là %s (%d) → %d + %d + %d + %d",
                bdayInTargetYear, month, targetYear, dowVi, dowNumber,
                month, day, lastTwoDigits, dowNumber);
        return NumberReducer.reduceTo9(sum, prefix);
    }

    public static int toJohariDayOfWeekNumber(DayOfWeek dow) {
        return switch (dow) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 3;
            case WEDNESDAY -> 4;
            case THURSDAY -> 5;
            case FRIDAY -> 6;
            case SATURDAY -> 7;
        };
    }

    public static String toVietnameseDayOfWeek(DayOfWeek dow) {
        return switch (dow) {
            case SUNDAY -> "Chủ nhật";
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
        };
    }

    /**
     * Kiểm tra Số "vượng" (Exalted Number - JOHARI §1):
     * 1<-28, 2<-29, 3<-12, 4<-31, 5<-23, 6<-24, 7<-25, 8<-26, 9<-27.
     */
    public boolean isExaltedPsychicDay(int psychicNumber, int dayOfMonth) {
        Integer expectedDay = EXALTED_DAY_FOR_PSYCHIC.get(psychicNumber);
        return expectedDay != null && expectedDay == dayOfMonth;
    }

    public String getPlanetName(int number) {
        return PLANET_NAMES.getOrDefault(number, "Không xác định");
    }

    public String getOddEvenDescription(int number) {
        if (number == 6) {
            return "Số chẵn (tĩnh, âm, mặt trăng) — đặc biệt số 6 là 'bạn chung' (Universal Friend) kết nối cả nhóm chẵn (2+2+2) và nhóm lẻ (3+3)";
        }
        if (number % 2 != 0) {
            return "Số lẻ (động, dương, năng lượng mặt trời)";
        }
        return "Số chẵn (tĩnh, âm, năng lượng mặt trăng)";
    }

    /**
     * Kiểm tra sự xuất hiện của chữ số 0 trong ngày, tháng, năm sinh (JOHARI §1).
     * Mức ảnh hưởng: ngày > tháng > năm.
     */
    public ZeroInfo analyzeZero(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        int month = birthDate.getMonthValue();
        String yearStr = String.format("%04d", birthDate.getYear());

        boolean inDay = (day == 10 || day == 20 || day == 30);
        boolean inMonth = (month == 10);
        boolean inYear = yearStr.indexOf('0') >= 0;

        List<String> positions = new ArrayList<>();
        String highestLevel = "KHONG";
        if (inDay) {
            positions.add("Ngày sinh (" + day + ")");
            highestLevel = "NGAY";
        }
        if (inMonth) {
            positions.add("Tháng sinh (10)");
            if ("KHONG".equals(highestLevel)) {
                highestLevel = "THANG";
            }
        }
        if (inYear) {
            positions.add("Năm sinh (" + yearStr + ")");
            if ("KHONG".equals(highestLevel)) {
                highestLevel = "NAM";
            }
        }
        boolean hasZero = inDay || inMonth || inYear;
        return new ZeroInfo(hasZero, inDay, inMonth, inYear, highestLevel, Collections.unmodifiableList(positions));
    }

    /**
     * Quan hệ giữa Số tâm hồn và Số định mệnh (và Số tên khi có) theo ma trận Johari (§4–12, Bảng tổng kết).
     */
    public String evaluateCompatibility(int numA, int numB) {
        if (numA == 6 || numB == 6) {
            return "HỢP (Số 6 là 'bạn chung' - Universal Friend, tạo sự hài hoà nhưng cần tránh ỷ lại)";
        }
        boolean aFriendB = FRIENDLY_MAP.getOrDefault(numA, Set.of()).contains(numB);
        boolean aEnemyB = ENEMY_MAP.getOrDefault(numA, Set.of()).contains(numB);
        if (aFriendB) {
            return "THÂN THIỆN / HỢP (Tạo sự thoải mái nội tâm; Johari lưu ý hai số thân thiện dễ làm bạn thư giãn nên cần chủ động duy trì động lực)";
        }
        if (aEnemyB) {
            return "KHÔNG HỢP / ĐỐI NGHỊCH (Tạo độ căng giữa mong muốn nội tâm và hoàn cảnh; Johari cho rằng cặp số đối nghịch lại giúp bạn luôn tỉnh táo, nỗ lực và tiến bộ)";
        }
        return "TRUNG TÍNH (Không hỗ trợ cũng không cản trở trực tiếp)";
    }

    public JohariProfile calculateAll(LocalDate birthDate, String fullName, int targetYear,
                                      Map<Character, Integer> unitSystemMap, boolean unitSystemActive) {
        StepResult tamHon = calculateSoTamHon(birthDate);
        StepResult dinhMenh = calculateSoDinhMenh(birthDate);
        NameNumberResult soTen = calculateSoTen(fullName, unitSystemMap, unitSystemActive);
        int day = birthDate.getDayOfMonth();
        Integer soKep = (day >= 10) ? day : null;
        boolean soVuong = isExaltedPsychicDay(tamHon.getNumericValue(), day);
        String hanhTinhTamHon = getPlanetName(tamHon.getNumericValue());
        String hanhTinhDinhMenh = getPlanetName(dinhMenh.getNumericValue());
        String chanLeTamHon = getOddEvenDescription(tamHon.getNumericValue());
        ZeroInfo zeroInfo = analyzeZero(birthDate);
        StepResult soNamTruoc = calculateSoNam(birthDate, targetYear - 1);
        StepResult soNam = calculateSoNam(birthDate, targetYear);
        StepResult soNamSau = calculateSoNam(birthDate, targetYear + 1);
        String quanHeTamHonDinhMenh = evaluateCompatibility(tamHon.getNumericValue(), dinhMenh.getNumericValue());

        return new JohariProfile(tamHon, dinhMenh, soTen, soKep, soVuong,
                hanhTinhTamHon, hanhTinhDinhMenh, chanLeTamHon, zeroInfo,
                soNamTruoc, soNam, soNamSau, quanHeTamHonDinhMenh);
    }

    public static class NameNumberResult {
        private final boolean active;
        private final StepResult stepResult;
        private final String displayMessage;

        public NameNumberResult(boolean active, StepResult stepResult, String displayMessage) {
            this.active = active;
            this.stepResult = stepResult;
            this.displayMessage = displayMessage;
        }

        public boolean isActive() {
            return active;
        }

        public StepResult getStepResult() {
            return stepResult;
        }

        public String getDisplayMessage() {
            return displayMessage;
        }
    }

    public static class ZeroInfo {
        private final boolean hasZero;
        private final boolean inDay;
        private final boolean inMonth;
        private final boolean inYear;
        private final String highestLevel;
        private final List<String> positions;

        public ZeroInfo(boolean hasZero, boolean inDay, boolean inMonth, boolean inYear,
                        String highestLevel, List<String> positions) {
            this.hasZero = hasZero;
            this.inDay = inDay;
            this.inMonth = inMonth;
            this.inYear = inYear;
            this.highestLevel = highestLevel;
            this.positions = positions;
        }

        public boolean isHasZero() {
            return hasZero;
        }

        public boolean isInDay() {
            return inDay;
        }

        public boolean isInMonth() {
            return inMonth;
        }

        public boolean isInYear() {
            return inYear;
        }

        public String getHighestLevel() {
            return highestLevel;
        }

        public List<String> getPositions() {
            return positions;
        }
    }

    public static class JohariProfile {
        private final StepResult soTamHon;
        private final StepResult soDinhMenh;
        private final NameNumberResult soTen;
        private final Integer soKepNgaySinh;
        private final boolean laSoVuong;
        private final String hanhTinhTamHon;
        private final String hanhTinhDinhMenh;
        private final String chanLeTamHon;
        private final ZeroInfo zeroInfo;
        private final StepResult soNamTruoc;
        private final StepResult soNam;
        private final StepResult soNamSau;
        private final String quanHeTamHonDinhMenh;

        public JohariProfile(StepResult soTamHon, StepResult soDinhMenh, NameNumberResult soTen,
                             Integer soKepNgaySinh, boolean laSoVuong,
                             String hanhTinhTamHon, String hanhTinhDinhMenh, String chanLeTamHon,
                             ZeroInfo zeroInfo, StepResult soNamTruoc, StepResult soNam, StepResult soNamSau,
                             String quanHeTamHonDinhMenh) {
            this.soTamHon = soTamHon;
            this.soDinhMenh = soDinhMenh;
            this.soTen = soTen;
            this.soKepNgaySinh = soKepNgaySinh;
            this.laSoVuong = laSoVuong;
            this.hanhTinhTamHon = hanhTinhTamHon;
            this.hanhTinhDinhMenh = hanhTinhDinhMenh;
            this.chanLeTamHon = chanLeTamHon;
            this.zeroInfo = zeroInfo;
            this.soNamTruoc = soNamTruoc;
            this.soNam = soNam;
            this.soNamSau = soNamSau;
            this.quanHeTamHonDinhMenh = quanHeTamHonDinhMenh;
        }

        public StepResult getSoTamHon() {
            return soTamHon;
        }

        public StepResult getSoDinhMenh() {
            return soDinhMenh;
        }

        public NameNumberResult getSoTen() {
            return soTen;
        }

        public Integer getSoKepNgaySinh() {
            return soKepNgaySinh;
        }

        public boolean isLaSoVuong() {
            return laSoVuong;
        }

        public String getHanhTinhTamHon() {
            return hanhTinhTamHon;
        }

        public String getHanhTinhDinhMenh() {
            return hanhTinhDinhMenh;
        }

        public String getChanLeTamHon() {
            return chanLeTamHon;
        }

        public ZeroInfo getZeroInfo() {
            return zeroInfo;
        }

        public StepResult getSoNamTruoc() {
            return soNamTruoc;
        }

        public StepResult getSoNam() {
            return soNam;
        }

        public StepResult getSoNamSau() {
            return soNamSau;
        }

        public String getQuanHeTamHonDinhMenh() {
            return quanHeTamHonDinhMenh;
        }
    }
}

