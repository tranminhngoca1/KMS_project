package com.kms301.nkms.engine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Kiểm tra tính hợp lệ của ngày sinh dương lịch (Mục 3.1).
 * Phải hợp lệ (kể cả năm nhuận) và nằm trong khoảng 01/01/1900 đến hôm nay.
 */
public final class DateValidator {

    private static final DateTimeFormatter STRICT_FORMATTER = DateTimeFormatter
            .ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);

    private DateValidator() {
    }

    public static LocalDate parseAndValidate(String ngaySinhStr) {
        return parseAndValidate(ngaySinhStr, LocalDate.now());
    }

    public static LocalDate parseAndValidate(String ngaySinhStr, LocalDate today) {
        if (ngaySinhStr == null || ngaySinhStr.isBlank()) {
            throw new IllegalArgumentException("Ngày sinh không được để trống (định dạng dd/MM/yyyy).");
        }
        String trimmed = ngaySinhStr.trim();
        LocalDate date;
        try {
            date = LocalDate.parse(trimmed, STRICT_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ (vui lòng nhập đúng ngày dương lịch dd/MM/yyyy, lưu ý tháng và năm nhuận): " + trimmed);
        }
        if (date.isBefore(MIN_DATE)) {
            throw new IllegalArgumentException("Ngày sinh phải từ 01/01/1900 trở về sau.");
        }
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("Ngày sinh không được vượt quá ngày hiện tại.");
        }
        return date;
    }

    public static ValidationResult validate(String ngaySinhStr) {
        try {
            LocalDate date = parseAndValidate(ngaySinhStr);
            return new ValidationResult(true, date, null);
        } catch (IllegalArgumentException e) {
            return new ValidationResult(false, null, e.getMessage());
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final LocalDate date;
        private final String errorMessage;

        public ValidationResult(boolean valid, LocalDate date, String errorMessage) {
            this.valid = valid;
            this.date = date;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public LocalDate getDate() { return date; }
        public String getErrorMessage() { return errorMessage; }
    }
}
