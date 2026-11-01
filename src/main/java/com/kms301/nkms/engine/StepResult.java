package com.kms301.nkms.engine;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Kết quả của một phép tính thần số học, bao gồm giá trị số, chuỗi hiển thị (vd "22/4"),
 * chuỗi diễn giải từng bước trung gian, và tập các tổng trung gian (để phát hiện Nợ nghiệp 13, 14, 16, 19).
 */
public class StepResult {
    private final int numericValue;
    private final String displayValue;
    private final String steps;
    private final Set<Integer> intermediateSums;

    public StepResult(int numericValue, String displayValue, String steps, Set<Integer> intermediateSums) {
        this.numericValue = numericValue;
        this.displayValue = displayValue;
        this.steps = steps;
        this.intermediateSums = intermediateSums == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(intermediateSums));
    }

    public StepResult(int numericValue, String displayValue, String steps) {
        this(numericValue, displayValue, steps, Collections.emptySet());
    }

    public int getNumericValue() {
        return numericValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String getSteps() {
        return steps;
    }

    public Set<Integer> getIntermediateSums() {
        return intermediateSums;
    }

    @Override
    public String toString() {
        return displayValue + " (" + steps + ")";
    }
}

