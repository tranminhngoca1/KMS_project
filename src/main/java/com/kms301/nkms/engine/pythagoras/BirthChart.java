package com.kms301.nkms.engine.pythagoras;

import com.kms301.nkms.engine.NameNormalizer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Biểu đồ ngày sinh 3×3 cố định (QH phần 6; PHILLIPS ch.5) và Biểu đồ tên chồng lên (QH phần 7, 12):
 * - Hàng trên (Trí não): 3 - 6 - 9
 * - Hàng giữa (Tâm hồn): 2 - 5 - 8
 * - Hàng dưới (Thể chất): 1 - 4 - 7
 * Điền mọi chữ số của ngày/tháng/năm (năm đủ 4 chữ số), bỏ số 0.
 * Số cô lập: số ở góc (1, 3, 7, 9) có mặt trong biểu đồ ngày sinh nhưng mọi ô kề đều trống.
 * Ô kề: 1:{2,4,5}, 3:{2,5,6}, 7:{4,5,8}, 9:{5,6,8}.
 */
public class BirthChart {

    private static final Map<Integer, int[]> CORNER_NEIGHBORS = Map.of(
            1, new int[]{2, 4, 5},
            3, new int[]{2, 5, 6},
            7, new int[]{4, 5, 8},
            9, new int[]{5, 6, 8}
    );

    private final int[] birthCounts;    // index 1..9
    private final int[] nameCounts;     // index 1..9
    private final int[] combinedCounts; // index 1..9
    private final Set<Integer> isolatedNumbers;
    private final Arrows arrows;
    private final String steps;

    public BirthChart(LocalDate birthDate, String normalizedName, Map<Character, Integer> alphabetMap) {
        this.birthCounts = new int[10];
        this.nameCounts = new int[10];
        this.combinedCounts = new int[10];

        String dateDigits = String.format("%02d%02d%04d",
                birthDate.getDayOfMonth(), birthDate.getMonthValue(), birthDate.getYear());
        StringBuilder birthStepBuilder = new StringBuilder("Chữ số ngày sinh (bỏ 0): ");
        boolean first = true;
        for (int i = 0; i < dateDigits.length(); i++) {
            int d = dateDigits.charAt(i) - '0';
            if (d >= 1 && d <= 9) {
                birthCounts[d]++;
                if (!first) {
                    birthStepBuilder.append(", ");
                }
                birthStepBuilder.append(d);
                first = false;
            }
        }

        Map<Character, Integer> mapToUse = (alphabetMap == null || alphabetMap.isEmpty())
                ? NameNormalizer.getDefaultPythagorasMap()
                : alphabetMap;
        String norm = NameNormalizer.normalize(normalizedName);
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            Integer val = mapToUse.get(c);
            if (val != null && val >= 1 && val <= 9) {
                nameCounts[val]++;
            }
        }

        for (int d = 1; d <= 9; d++) {
            combinedCounts[d] = birthCounts[d] + nameCounts[d];
        }

        Set<Integer> isolated = new LinkedHashSet<>();
        for (int corner : new int[]{1, 3, 7, 9}) {
            if (birthCounts[corner] > 0) {
                int[] neighbors = CORNER_NEIGHBORS.get(corner);
                boolean allEmpty = true;
                for (int nb : neighbors) {
                    if (birthCounts[nb] > 0) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) {
                    isolated.add(corner);
                }
            }
        }
        this.isolatedNumbers = Collections.unmodifiableSet(isolated);
        this.arrows = new Arrows(this.birthCounts, this.combinedCounts);
        this.steps = birthStepBuilder.toString();
    }

    public int getBirthCount(int digit) {
        if (digit < 1 || digit > 9) return 0;
        return birthCounts[digit];
    }

    public int getNameCount(int digit) {
        if (digit < 1 || digit > 9) return 0;
        return nameCounts[digit];
    }

    public int getCombinedCount(int digit) {
        if (digit < 1 || digit > 9) return 0;
        return combinedCounts[digit];
    }

    public String getBirthCellText(int digit) {
        int count = getBirthCount(digit);
        if (count <= 0) return "";
        return String.valueOf(digit).repeat(count);
    }

    public String getNameCellText(int digit) {
        int count = getNameCount(digit);
        if (count <= 0) return "";
        return String.valueOf(digit).repeat(count);
    }

    public int[] getBirthCounts() {
        return Arrays.copyOf(birthCounts, birthCounts.length);
    }

    public int[] getNameCounts() {
        return Arrays.copyOf(nameCounts, nameCounts.length);
    }

    public int[] getCombinedCounts() {
        return Arrays.copyOf(combinedCounts, combinedCounts.length);
    }

    public Set<Integer> getIsolatedNumbers() {
        return isolatedNumbers;
    }

    public Arrows getArrows() {
        return arrows;
    }

    public String getSteps() {
        return steps;
    }
}

