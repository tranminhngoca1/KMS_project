package com.kms301.nkms.engine.kabbalah;

import com.kms301.nkms.engine.NumberReducer;
import com.kms301.nkms.engine.StepResult;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

/**
 * Bộ máy tính toán trường phái KABBALAH — SEPHARIAL (Mục 6.4).
 * - Biểu đồ ma phương 3×3 (4 9 2 / 3 5 7 / 8 1 6) và 3 tầng:
 *   Tinh thần (3 - 1 - 9), Trí óc (6 - 7 - 5), Thể chất (2 - 8 - 4).
 *   Điền chữ số ngày/tháng/năm, bỏ 2 chữ số thế kỷ (vd 1983 -> 83).
 * - Số chìa khoá: Tổng các chữ số ngày + tháng + năm (bỏ 2 chữ số thế kỷ theo mặc định) -> reduceTo9.
 * - Bảng hành tinh chính (ch.III) và Màu theo số (ch.V).
 */
public class SepharialCalculator {

    private static final Map<Integer, String> MAIN_PLANETS = Map.of(
            1, "Mặt Trời (Sun)",
            2, "Mặt Trăng (Moon)",
            3, "Hoả tinh (Mars)",
            4, "Thuỷ tinh (Mercury)",
            5, "Mộc tinh (Jupiter)",
            6, "Kim tinh (Venus)",
            7, "Thổ tinh (Saturn)",
            8, "Thiên Vương tinh (Uranus)",
            9, "Hải Vương tinh (Neptune)"
    );

    private static final Map<Integer, String> LESSER_KEY_PLANETS = Map.of(
            1, "Mặt Trời (Thượng giới - Tinh thần)",
            2, "Mặt Trăng (Hạ giới - Thể chất)",
            3, "Mộc tinh (Tinh thần)",
            4, "Mặt Trời (Hạ giới - Thể chất)",
            5, "Thuỷ tinh (Trí óc)",
            6, "Kim tinh (Trí óc)",
            7, "Mặt Trăng (Thượng giới - Trí óc)",
            8, "Thổ tinh (Thể chất)",
            9, "Hoả tinh (Tinh thần)"
    );

    private static final Map<Integer, String> COLORS_BY_NUMBER = Map.of(
            1, "Trắng (White)",
            2, "Vàng (Yellow)",
            3, "Tím (Violet / Purple)",
            4, "Cam (Orange)",
            5, "Chàm (Indigo)",
            6, "Xanh nhạt (Light Blue)",
            7, "Bạc (Silver)",
            8, "Đen (Black)",
            9, "Đỏ (Red)"
    );

    /**
     * Số chìa khoá (SEPHARIAL ch.XIV & ch.IV):
     * Mặc định bỏ 2 chữ số thế kỷ của năm (theo cách lập biểu đồ "Hình vuông số 3"), cộng các chữ số -> reduceTo9.
     */
    public StepResult calculateSoChiaKhoa(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        int month = birthDate.getMonthValue();
        int shortYear = Math.abs(birthDate.getYear()) % 100;
        String raw = String.valueOf(day) + month + String.format("%02d", shortYear);

        StringBuilder expr = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < raw.length(); i++) {
            int d = raw.charAt(i) - '0';
            if (d == 0) {
                continue;
            }
            if (expr.length() > 0) {
                expr.append("+");
            }
            expr.append(d);
            sum += d;
        }
        if (sum == 0) {
            return new StepResult(0, "0", "0");
        }
        String prefix = String.format("Ngày %d, Tháng %d, Năm bỏ thế kỷ %02d (%s)",
                day, month, shortYear, expr);
        return NumberReducer.reduceTo9(sum, prefix);
    }

    /**
     * Số chìa khoá nếu giữ cả 4 chữ số của năm (dùng để minh hoạ xung đột nội bộ #12 của Sepharial ch.XIV).
     */
    public StepResult calculateSoChiaKhoaFullYear(LocalDate birthDate) {
        String raw = String.valueOf(birthDate.getDayOfMonth())
                + birthDate.getMonthValue()
                + birthDate.getYear();
        StringBuilder expr = new StringBuilder();
        int sum = 0;
        for (int i = 0; i < raw.length(); i++) {
            int d = raw.charAt(i) - '0';
            if (d == 0) {
                continue;
            }
            if (expr.length() > 0) {
                expr.append("+");
            }
            expr.append(d);
            sum += d;
        }
        return NumberReducer.reduceTo9(sum, "Nếu tính đủ 4 chữ số năm (" + expr + ")");
    }

    public MagicSquareChart calculateMagicSquare(LocalDate birthDate) {
        int day = birthDate.getDayOfMonth();
        int month = birthDate.getMonthValue();
        int shortYear = Math.abs(birthDate.getYear()) % 100;
        String raw = String.valueOf(day) + month + String.format("%02d", shortYear);

        int[] counts = new int[10];
        StringBuilder digitsUsed = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            int d = raw.charAt(i) - '0';
            if (d >= 1 && d <= 9) {
                counts[d]++;
                if (digitsUsed.length() > 0) {
                    digitsUsed.append(", ");
                }
                digitsUsed.append(d);
            }
        }

        int spiritualCount = counts[3] + counts[1] + counts[9];
        int mentalCount = counts[6] + counts[7] + counts[5];
        int physicalCount = counts[2] + counts[8] + counts[4];

        String steps = String.format("Ngày %d / Tháng %d / Năm %02d (bỏ 2 chữ số thế kỷ từ %d) → Các chữ số điền vào biểu đồ: [%s]",
                day, month, shortYear, birthDate.getYear(), digitsUsed);

        return new MagicSquareChart(counts, spiritualCount, mentalCount, physicalCount, steps);
    }

    public String getMainPlanet(int n) {
        return MAIN_PLANETS.getOrDefault(n, "Không xác định");
    }

    public String getLesserKeyPlanet(int n) {
        return LESSER_KEY_PLANETS.getOrDefault(n, "Không xác định");
    }

    public String getColorByNumber(int n) {
        return COLORS_BY_NUMBER.getOrDefault(n, "Không xác định");
    }

    public SepharialProfile calculateAll(LocalDate birthDate) {
        StepResult soChiaKhoa = calculateSoChiaKhoa(birthDate);
        StepResult soChiaKhoaFullYear = calculateSoChiaKhoaFullYear(birthDate);
        MagicSquareChart magicSquare = calculateMagicSquare(birthDate);
        int keyVal = soChiaKhoa.getNumericValue();
        String hanhTinhChinh = getMainPlanet(keyVal);
        String hanhTinhKhoaNho = getLesserKeyPlanet(keyVal);
        String mauSac = getColorByNumber(keyVal);

        return new SepharialProfile(soChiaKhoa, soChiaKhoaFullYear, magicSquare,
                hanhTinhChinh, hanhTinhKhoaNho, mauSac);
    }

    public static class MagicSquareChart {
        private final int[] counts;
        private final int spiritualCount; // 3 - 1 - 9
        private final int mentalCount;    // 6 - 7 - 5
        private final int physicalCount;  // 2 - 8 - 4
        private final String steps;

        public MagicSquareChart(int[] counts, int spiritualCount, int mentalCount, int physicalCount, String steps) {
            this.counts = Arrays.copyOf(counts, counts.length);
            this.spiritualCount = spiritualCount;
            this.mentalCount = mentalCount;
            this.physicalCount = physicalCount;
            this.steps = steps;
        }

        public int getCount(int digit) {
            if (digit < 1 || digit > 9) return 0;
            return counts[digit];
        }

        public String getCellText(int digit) {
            int c = getCount(digit);
            if (c <= 0) return "";
            return String.valueOf(digit).repeat(c);
        }

        public int getSpiritualCount() {
            return spiritualCount;
        }

        public int getMentalCount() {
            return mentalCount;
        }

        public int getPhysicalCount() {
            return physicalCount;
        }

        public String getSteps() {
            return steps;
        }

        public String getDominantPlane() {
            if (spiritualCount >= mentalCount && spiritualCount >= physicalCount && spiritualCount > 0) {
                return "TINH_THAN";
            }
            if (mentalCount >= spiritualCount && mentalCount >= physicalCount && mentalCount > 0) {
                return "TRI_OC";
            }
            return "THE_CHAT";
        }
    }

    public static class SepharialProfile {
        private final StepResult soChiaKhoa;
        private final StepResult soChiaKhoaFullYear;
        private final MagicSquareChart magicSquare;
        private final String hanhTinhChinh;
        private final String hanhTinhKhoaNho;
        private final String mauSac;
        private final String chuThichTen = "Sepharial dùng bảng âm vị tiếng Anh, không áp dụng cho tên tiếng Việt.";
        private final String chuThichNgayThienVan = "Sepharial tính ngày từ 12 giờ trưa; nếu bạn sinh buổi sáng, ngày theo Sepharial là ngày hôm trước.";
        private final String chuThichHanhTinh = "Sách có 2 bảng hành tinh: Bảng chính (Ch.III) và Bảng Khoá nhỏ (Ch.IV).";
        private final String chuThichGapGi = "Trường phái này chỉ có phương pháp Tarot theo năm, đã bị loại vì mang tính bói toán.";

        public SepharialProfile(StepResult soChiaKhoa, StepResult soChiaKhoaFullYear,
                                MagicSquareChart magicSquare, String hanhTinhChinh,
                                String hanhTinhKhoaNho, String mauSac) {
            this.soChiaKhoa = soChiaKhoa;
            this.soChiaKhoaFullYear = soChiaKhoaFullYear;
            this.magicSquare = magicSquare;
            this.hanhTinhChinh = hanhTinhChinh;
            this.hanhTinhKhoaNho = hanhTinhKhoaNho;
            this.mauSac = mauSac;
        }

        public StepResult getSoChiaKhoa() {
            return soChiaKhoa;
        }

        public StepResult getSoChiaKhoaFullYear() {
            return soChiaKhoaFullYear;
        }

        public MagicSquareChart getMagicSquare() {
            return magicSquare;
        }

        public String getHanhTinhChinh() {
            return hanhTinhChinh;
        }

        public String getHanhTinhKhoaNho() {
            return hanhTinhKhoaNho;
        }

        public String getMauSac() {
            return mauSac;
        }

        public String getChuThichTen() {
            return chuThichTen;
        }

        public String getChuThichNgayThienVan() {
            return chuThichNgayThienVan;
        }

        public String getChuThichHanhTinh() {
            return chuThichHanhTinh;
        }

        public String getChuThichGapGi() {
            return chuThichGapGi;
        }
    }
}

