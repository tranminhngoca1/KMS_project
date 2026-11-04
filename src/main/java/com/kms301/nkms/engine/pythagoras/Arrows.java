package com.kms301.nkms.engine.pythagoras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mũi tên cá tính trên Biểu đồ ngày sinh (QH phần 7; PHILLIPS ch.6).
 * 8 bộ đường thẳng: 1-5-9, 3-5-7, 3-6-9, 2-5-8, 1-4-7, 1-2-3, 4-5-6, 7-8-9.
 * Bộ 1-2-3 không có bản trống -> tổng 15 mũi tên.
 * Khi biểu đồ tên lấp đầy cả 3 ô của một mũi tên trống trong biểu đồ ngày sinh -> đánh dấu "đã được tên bù" (phá thế).
 */
public class Arrows {

    public static final List<ArrowDef> DEFINITIONS = List.of(
            new ArrowDef("1-5-9", new int[]{1, 5, 9}, true, "Mũi tên Quyết tâm (1-5-9)", "Mũi tên Trì hoãn (trống 1-5-9)"),
            new ArrowDef("3-5-7", new int[]{3, 5, 7}, true, "Mũi tên Tâm linh / Nhạy bén (3-5-7)", "Mũi tên Hoài nghi / Enquirer (trống 3-5-7)"),
            new ArrowDef("3-6-9", new int[]{3, 6, 9}, true, "Mũi tên Trí tuệ / Trí nhớ (3-6-9)", "Mũi tên Trí nhớ ngắn hạn (trống 3-6-9)"),
            new ArrowDef("2-5-8", new int[]{2, 5, 8}, true, "Mũi tên Cân bằng cảm xúc (2-5-8)", "Mũi tên Nhạy cảm quá mức (trống 2-5-8)"),
            new ArrowDef("1-4-7", new int[]{1, 4, 7}, true, "Mũi tên Thực tế / Tháo vát (1-4-7)", "Mũi tên Thiếu trật tự (trống 1-4-7)"),
            new ArrowDef("1-2-3", new int[]{1, 2, 3}, false, "Mũi tên Kế hoạch (1-2-3)", null),
            new ArrowDef("4-5-6", new int[]{4, 5, 6}, true, "Mũi tên Ý chí (4-5-6)", "Mũi tên Uất giận / Nản lòng (trống 4-5-6)"),
            new ArrowDef("7-8-9", new int[]{7, 8, 9}, true, "Mũi tên Hoạt động (7-8-9)", "Mũi tên Thụ động (trống 7-8-9)")
    );

    private final Set<String> presentArrows;           // vd "1-5-9"
    private final Set<String> emptyArrows;             // vd "3-5-7"
    private final Set<String> compensatedEmptyArrows;  // mũi tên trống đã được tên bù
    private final List<EvaluatedArrow> evaluatedArrows;

    public Arrows(int[] birthCounts, int[] combinedCounts) {
        Set<String> present = new LinkedHashSet<>();
        Set<String> empty = new LinkedHashSet<>();
        Set<String> compensated = new LinkedHashSet<>();
        List<EvaluatedArrow> evaluated = new ArrayList<>();

        for (ArrowDef def : DEFINITIONS) {
            int[] cells = def.getCells();
            boolean allBirthPresent = birthCounts[cells[0]] > 0
                    && birthCounts[cells[1]] > 0
                    && birthCounts[cells[2]] > 0;
            boolean allBirthEmpty = birthCounts[cells[0]] == 0
                    && birthCounts[cells[1]] == 0
                    && birthCounts[cells[2]] == 0;

            if (allBirthPresent) {
                present.add(def.getCode());
                evaluated.add(new EvaluatedArrow(def.getCode(), "CO", def.getPresentName(), false));
            } else if (allBirthEmpty && def.isHasEmptyVariant()) {
                empty.add(def.getCode());
                boolean filledByName = combinedCounts[cells[0]] > 0
                        && combinedCounts[cells[1]] > 0
                        && combinedCounts[cells[2]] > 0;
                if (filledByName) {
                    compensated.add(def.getCode());
                }
                evaluated.add(new EvaluatedArrow(def.getCode(), "TRONG", def.getEmptyName(), filledByName));
            }
        }

        this.presentArrows = Collections.unmodifiableSet(present);
        this.emptyArrows = Collections.unmodifiableSet(empty);
        this.compensatedEmptyArrows = Collections.unmodifiableSet(compensated);
        this.evaluatedArrows = Collections.unmodifiableList(evaluated);
    }

    public Set<String> getPresentArrows() {
        return presentArrows;
    }

    public Set<String> getEmptyArrows() {
        return emptyArrows;
    }

    public Set<String> getCompensatedEmptyArrows() {
        return compensatedEmptyArrows;
    }

    public List<EvaluatedArrow> getEvaluatedArrows() {
        return evaluatedArrows;
    }

    public boolean hasArrow(String code, String state) {
        if ("CO".equalsIgnoreCase(state)) {
            return presentArrows.contains(code);
        } else if ("TRONG".equalsIgnoreCase(state)) {
            return emptyArrows.contains(code);
        }
        return false;
    }

    public boolean isCompensatedByName(String code) {
        return compensatedEmptyArrows.contains(code);
    }

    public String getPresentArrowsLabel() {
        String res = evaluatedArrows.stream()
                .filter(a -> "CO".equalsIgnoreCase(a.getState()))
                .map(EvaluatedArrow::getLabel)
                .collect(Collectors.joining(", "));
        return res.isEmpty() ? "Không có trục đầy (năng lượng phân bổ cân bằng)" : res;
    }

    public String getEmptyArrowsLabel() {
        String res = evaluatedArrows.stream()
                .filter(a -> "TRONG".equalsIgnoreCase(a.getState()))
                .map(a -> a.getLabel() + (a.isCompensatedByName() ? " (đã được hoá giải bởi tên riêng)" : ""))
                .collect(Collectors.joining(", "));
        return res.isEmpty() ? "Không có trục trống lớn" : res;
    }

    public String getShortBadge() {
        if (!presentArrows.isEmpty()) {
            return String.join(", ", presentArrows);
        }
        if (!emptyArrows.isEmpty()) {
            return "Trống " + String.join(", ", emptyArrows);
        }
        return "Cân bằng";
    }

    public static class ArrowDef {
        private final String code;
        private final int[] cells;
        private final boolean hasEmptyVariant;
        private final String presentName;
        private final String emptyName;

        public ArrowDef(String code, int[] cells, boolean hasEmptyVariant, String presentName, String emptyName) {
            this.code = code;
            this.cells = cells;
            this.hasEmptyVariant = hasEmptyVariant;
            this.presentName = presentName;
            this.emptyName = emptyName;
        }

        public String getCode() {
            return code;
        }

        public int[] getCells() {
            return cells;
        }

        public boolean isHasEmptyVariant() {
            return hasEmptyVariant;
        }

        public String getPresentName() {
            return presentName;
        }

        public String getEmptyName() {
            return emptyName;
        }
    }

    public static class EvaluatedArrow {
        private final String code;
        private final String state; // "CO" hoặc "TRONG"
        private final String label;
        private final boolean compensatedByName;

        public EvaluatedArrow(String code, String state, String label, boolean compensatedByName) {
            this.code = code;
            this.state = state;
            this.label = label;
            this.compensatedByName = compensatedByName;
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }

        public String getLabel() {
            return label;
        }

        public boolean isCompensatedByName() {
            return compensatedByName;
        }

        public String getTriggerKey() {
            return code + ":" + state;
        }
    }
}

