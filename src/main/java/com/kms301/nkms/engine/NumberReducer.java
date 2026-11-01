package com.kms301.nkms.engine;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Các hàm rút gọn số dùng chung cho cả 3 trường phái (Mục 4 của đặc tả).
 */
public final class NumberReducer {

    private NumberReducer() {
    }

    /**
     * Tổng các chữ số của số nguyên không âm n.
     */
    public static int digitSum(int n) {
        int val = Math.abs(n);
        int sum = 0;
        String s = String.valueOf(val);
        for (int i = 0; i < s.length(); i++) {
            sum += s.charAt(i) - '0';
        }
        return sum;
    }

    /**
     * Biểu thức cộng các chữ số của n, ví dụ 35 -> "3+5".
     */
    public static String digitExpression(int n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) {
                sb.append("+");
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Rút gọn về 1..9 (reduceTo9).
     */
    public static StepResult reduceTo9(int n) {
        return reduceTo9(n, null);
    }

    public static StepResult reduceTo9(int n, String initialPrefix) {
        Set<Integer> intermediates = new LinkedHashSet<>();
        StringBuilder steps = new StringBuilder();
        int current = Math.abs(n);
        intermediates.add(current);

        if (initialPrefix != null && !initialPrefix.isBlank()) {
            steps.append(initialPrefix).append(" = ").append(current);
        } else {
            steps.append(current);
        }

        while (current > 9) {
            String expr = digitExpression(current);
            current = digitSum(current);
            intermediates.add(current);
            steps.append(" → ").append(expr).append(" = ").append(current);
        }

        return new StepResult(current, String.valueOf(current), steps.toString(), intermediates);
    }

    /**
     * Rút gọn giữ số bậc thầy 11 và 22 (DECOZ: reduceKeepMaster).
     */
    public static StepResult reduceKeepMaster(int n) {
        return reduceKeepMaster(n, null);
    }

    public static StepResult reduceKeepMaster(int n, String initialPrefix) {
        Set<Integer> intermediates = new LinkedHashSet<>();
        StringBuilder steps = new StringBuilder();
        int current = Math.abs(n);
        intermediates.add(current);

        if (initialPrefix != null && !initialPrefix.isBlank()) {
            steps.append(initialPrefix).append(" = ").append(current);
        } else {
            steps.append(current);
        }

        while (current > 9 && current != 11 && current != 22) {
            String expr = digitExpression(current);
            current = digitSum(current);
            intermediates.add(current);
            steps.append(" → ").append(expr).append(" = ").append(current);
        }

        return new StepResult(current, String.valueOf(current), steps.toString(), intermediates);
    }

    /**
     * Rút gọn theo QH/PHILLIPS (reduceQH):
     * Lặp cho đến khi 2 <= n <= 11 hoặc n == 22 (hiển thị "22/4").
     */
    public static StepResult reduceQH(int n) {
        return reduceQH(n, null);
    }

    public static StepResult reduceQH(int n, String initialPrefix) {
        return reduceQHFlexible(n, initialPrefix, true);
    }

    /**
     * Rút gọn đến <= 11, tuỳ chọn giữ 22/4 hay không (dùng cho Linh hồn chỉ 1-11, Biểu đạt 1-11 và 22/4).
     */
    public static StepResult reduceQHFlexible(int n, String initialPrefix, boolean keep22Slash4) {
        Set<Integer> intermediates = new LinkedHashSet<>();
        StringBuilder steps = new StringBuilder();
        int current = Math.abs(n);
        intermediates.add(current);

        if (initialPrefix != null && !initialPrefix.isBlank()) {
            steps.append(initialPrefix).append(" = ").append(current);
        } else {
            steps.append(current);
        }

        while (current > 11 && !(keep22Slash4 && current == 22)) {
            String expr = digitExpression(current);
            current = digitSum(current);
            intermediates.add(current);
            steps.append(" → ").append(expr).append(" = ").append(current);
        }

        String display = (keep22Slash4 && current == 22) ? "22/4" : String.valueOf(current);
        if (keep22Slash4 && current == 22) {
            steps.append(" (22/4)");
        }
        return new StepResult(current, display, steps.toString(), intermediates);
    }
}

