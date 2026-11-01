package com.kms301.nkms.engine.pythagoras;

import com.kms301.nkms.engine.NameNormalizer;
import com.kms301.nkms.engine.NumberReducer;
import com.kms301.nkms.engine.StepResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Bộ máy tính toán trường phái PYTHAGORAS — nhánh DECOZ (Mục 6.2).
 */
public class DecozCalculator {

    private static final Set<Integer> KARMIC_DEBT_NUMBERS = Set.of(13, 14, 16, 19);

    /**
     * Số Đường đời (Life Path - DECOZ ch.2):
     * Rút gọn riêng tháng, ngày, năm bằng reduceKeepMaster, rồi cộng 3 phần -> reduceKeepMaster (1–9, 11, 22).
     */
    public StepResult calculateSoDuongDoi(LocalDate birthDate) {
        int m = birthDate.getMonthValue();
        int d = birthDate.getDayOfMonth();
        int y = birthDate.getYear();

        StepResult redMonth = NumberReducer.reduceKeepMaster(m, "Tháng " + m);
        StepResult redDay = NumberReducer.reduceKeepMaster(d, "Ngày " + d);
        int yearSum = NumberReducer.digitSum(y);
        StepResult redYear = NumberReducer.reduceKeepMaster(yearSum, "Năm " + NumberReducer.digitExpression(y));

        int total = redMonth.getNumericValue() + redDay.getNumericValue() + redYear.getNumericValue();
        String prefix = "[" + redMonth.getSteps() + "] + [" + redDay.getSteps() + "] + [" + redYear.getSteps() + "] → "
                + redMonth.getNumericValue() + "+" + redDay.getNumericValue() + "+" + redYear.getNumericValue();
        StepResult finalRes = NumberReducer.reduceKeepMaster(total, prefix);

        Set<Integer> allIntermediates = new LinkedHashSet<>();
        allIntermediates.add(d);
        allIntermediates.addAll(redMonth.getIntermediateSums());
        allIntermediates.addAll(redDay.getIntermediateSums());
        allIntermediates.addAll(redYear.getIntermediateSums());
        allIntermediates.addAll(finalRes.getIntermediateSums());

        return new StepResult(finalRes.getNumericValue(), finalRes.getDisplayValue(), finalRes.getSteps(), allIntermediates);
    }

    /**
     * Số Biểu đạt (Expression - DECOZ ch.3, tr.67–68):
     * Mỗi phần tên (họ, đệm, tên): cộng giá trị toàn bộ chữ -> reduceKeepMaster; cộng các phần -> reduceKeepMaster.
     */
    public StepResult calculateSoBieuDat(String rawName, Map<Character, Integer> alphabetMap) {
        return calculateNameMetric(rawName, alphabetMap, c -> true, "Toàn bộ chữ");
    }

    /**
     * Khát vọng tâm hồn (Heart's Desire - DECOZ tr.86–87):
     * Mỗi phần tên: cộng giá trị nguyên âm -> reduceKeepMaster; cộng các phần -> reduceKeepMaster.
     */
    public StepResult calculateKhatVongTamHon(String rawName, Map<Character, Integer> alphabetMap) {
        return calculateNameMetric(rawName, alphabetMap, NameNormalizer::isVowel, "Nguyên âm");
    }

    /**
     * Nhân cách (Personality - DECOZ ch.3, tr.102–103):
     * Mỗi phần tên: cộng giá trị phụ âm -> reduceKeepMaster; cộng các phần -> reduceKeepMaster.
     */
    public StepResult calculateNhanCach(String rawName, Map<Character, Integer> alphabetMap) {
        return calculateNameMetric(rawName, alphabetMap, NameNormalizer::isConsonant, "Phụ âm");
    }

    private StepResult calculateNameMetric(String rawName, Map<Character, Integer> alphabetMap,
                                           Predicate<Character> charFilter, String label) {
        Map<Character, Integer> map = (alphabetMap == null || alphabetMap.isEmpty())
                ? NameNormalizer.getDefaultPythagorasMap()
                : alphabetMap;
        List<String> words = NameNormalizer.splitWords(rawName);
        if (words.isEmpty()) {
            return new StepResult(0, "0", "Không có dữ liệu tên");
        }

        Set<Integer> allIntermediates = new LinkedHashSet<>();
        List<Integer> wordReducedValues = new ArrayList<>();
        StringBuilder wordSteps = new StringBuilder(label).append(" theo từng từ: ");

        for (String word : words) {
            int wordSum = 0;
            StringBuilder letterExpr = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (charFilter.test(c)) {
                    int val = map.getOrDefault(c, 0);
                    if (val > 0) {
                        if (letterExpr.length() > 0) {
                            letterExpr.append("+");
                        }
                        letterExpr.append(val);
                        wordSum += val;
                    }
                }
            }
            if (wordSum > 0) {
                StepResult wordRed = NumberReducer.reduceKeepMaster(wordSum, word + "(" + letterExpr + ")");
                allIntermediates.addAll(wordRed.getIntermediateSums());
                wordReducedValues.add(wordRed.getNumericValue());
                if (wordSteps.length() > (label + " theo từng từ: ").length()) {
                    wordSteps.append("; ");
                }
                wordSteps.append(wordRed.getSteps());
            }
        }

        if (wordReducedValues.isEmpty()) {
            return new StepResult(0, "0", "Không có chữ cái phù hợp");
        }

        int sumOfParts = 0;
        StringBuilder sumExpr = new StringBuilder();
        for (int val : wordReducedValues) {
            if (sumExpr.length() > 0) {
                sumExpr.append("+");
            }
            sumExpr.append(val);
            sumOfParts += val;
        }

        StepResult finalRes = NumberReducer.reduceKeepMaster(sumOfParts, wordSteps + " → Tổng các từ: " + sumExpr);
        allIntermediates.addAll(finalRes.getIntermediateSums());
        return new StepResult(finalRes.getNumericValue(), finalRes.getDisplayValue(), finalRes.getSteps(), allIntermediates);
    }

    /**
     * Phát hiện cờ Số Nợ nghiệp (Karmic Debt ∈ {13, 14, 16, 19} - DECOZ ch.1, tr.15–18).
     */
    public Set<Integer> detectKarmicDebts(LocalDate birthDate, StepResult duongDoi,
                                          StepResult bieuDat, StepResult khatVong, StepResult nhanCach) {
        Set<Integer> debts = new LinkedHashSet<>();
        int day = birthDate.getDayOfMonth();
        if (KARMIC_DEBT_NUMBERS.contains(day)) {
            debts.add(day);
        }
        collectDebts(duongDoi, debts);
        collectDebts(bieuDat, debts);
        collectDebts(khatVong, debts);
        collectDebts(nhanCach, debts);
        return Collections.unmodifiableSet(debts);
    }

    public Map<Integer, List<String>> detectKarmicDebtSources(LocalDate birthDate, StepResult duongDoi,
                                                              StepResult bieuDat, StepResult khatVong, StepResult nhanCach) {
        Map<Integer, List<String>> map = new LinkedHashMap<>();
        int day = birthDate.getDayOfMonth();
        if (KARMIC_DEBT_NUMBERS.contains(day)) {
            map.computeIfAbsent(day, k -> new ArrayList<>()).add("Ngày sinh (" + day + ")");
        }
        collectDebtSource(duongDoi, "Số Đường đời", map);
        collectDebtSource(bieuDat, "Số Biểu đạt (Expression)", map);
        collectDebtSource(khatVong, "Khát vọng tâm hồn (Heart's Desire)", map);
        collectDebtSource(nhanCach, "Nhân cách (Personality)", map);
        return map;
    }

    private void collectDebts(StepResult res, Set<Integer> target) {
        if (res == null) return;
        for (int val : res.getIntermediateSums()) {
            if (KARMIC_DEBT_NUMBERS.contains(val)) {
                target.add(val);
            }
        }
    }

    private void collectDebtSource(StepResult res, String sourceName, Map<Integer, List<String>> map) {
        if (res == null) return;
        for (int val : res.getIntermediateSums()) {
            if (KARMIC_DEBT_NUMBERS.contains(val)) {
                List<String> list = map.computeIfAbsent(val, k -> new ArrayList<>());
                if (!list.contains(sourceName)) {
                    list.add(sourceName);
                }
            }
        }
    }

    /**
     * Năm cá nhân (DECOZ tr.179):
     * reduceTo9(tháng) + reduceTo9(ngày) + reduceTo9(năm hiện tại) -> reduceTo9 (rút gọn cả số bậc thầy).
     */
    public StepResult calculateNamCaNhan(LocalDate birthDate, int currentYear) {
        int m = NumberReducer.reduceTo9(birthDate.getMonthValue()).getNumericValue();
        int d = NumberReducer.reduceTo9(birthDate.getDayOfMonth()).getNumericValue();
        int y = NumberReducer.reduceTo9(NumberReducer.digitSum(currentYear)).getNumericValue();
        int total = m + d + y;
        String prefix = "Tháng (" + m + ") + Ngày (" + d + ") + Năm " + currentYear + " (" + y + ") = "
                + m + "+" + d + "+" + y;
        return NumberReducer.reduceTo9(total, prefix);
    }

    /**
     * Tháng cá nhân (DECOZ tr.188):
     * PYN + reduceTo9(tháng hiện tại) -> reduceTo9.
     */
    public StepResult calculateThangCaNhan(int pyn, int currentMonth) {
        int m = NumberReducer.reduceTo9(currentMonth).getNumericValue();
        int total = pyn + m;
        String prefix = "Năm cá nhân (" + pyn + ") + Tháng (" + m + ") = " + pyn + "+" + m;
        return NumberReducer.reduceTo9(total, prefix);
    }

    /**
     * Đỉnh (Pinnacle - DECOZ tr.218–219):
     * Như QH nhưng giữ 11 và 22 ở mọi đỉnh, và rút 10 về 1.
     */
    public List<QhCalculator.PeakInfo> calculatePinnacles(LocalDate birthDate, StepResult soDuongDoi) {
        int m = NumberReducer.reduceTo9(birthDate.getMonthValue()).getNumericValue();
        int d = NumberReducer.reduceTo9(birthDate.getDayOfMonth()).getNumericValue();
        int y = NumberReducer.reduceTo9(NumberReducer.digitSum(birthDate.getYear())).getNumericValue();

        StepResult p1 = NumberReducer.reduceKeepMaster(m + d, "Tháng (" + m + ") + Ngày (" + d + ")");
        StepResult p2 = NumberReducer.reduceKeepMaster(d + y, "Ngày (" + d + ") + Năm (" + y + ")");
        StepResult p3 = NumberReducer.reduceKeepMaster(p1.getNumericValue() + p2.getNumericValue(),
                "Đỉnh 1 (" + p1.getDisplayValue() + ") + Đỉnh 2 (" + p2.getDisplayValue() + ")");
        StepResult p4 = NumberReducer.reduceKeepMaster(m + y, "Tháng (" + m + ") + Năm (" + y + ")");

        int lpSingle = NumberReducer.reduceTo9(soDuongDoi.getNumericValue()).getNumericValue();
        int ageP1 = 36 - lpSingle;
        int ageP2 = ageP1 + 9;
        int ageP3 = ageP2 + 9;
        int ageP4 = ageP3 + 9;

        return List.of(
                new QhCalculator.PeakInfo(1, p1.getNumericValue(), p1.getDisplayValue(), ageP1, p1.getSteps()),
                new QhCalculator.PeakInfo(2, p2.getNumericValue(), p2.getDisplayValue(), ageP2, p2.getSteps()),
                new QhCalculator.PeakInfo(3, p3.getNumericValue(), p3.getDisplayValue(), ageP3, p3.getSteps()),
                new QhCalculator.PeakInfo(4, p4.getNumericValue(), p4.getDisplayValue(), ageP4, p4.getSteps())
        );
    }

    public DecozProfile calculateAll(LocalDate birthDate, String fullName, int targetYear, int targetMonth,
                                     Map<Character, Integer> alphabetMap) {
        StepResult duongDoi = calculateSoDuongDoi(birthDate);
        StepResult bieuDat = calculateSoBieuDat(fullName, alphabetMap);
        StepResult khatVong = calculateKhatVongTamHon(fullName, alphabetMap);
        StepResult nhanCach = calculateNhanCach(fullName, alphabetMap);
        Set<Integer> noNghiep = detectKarmicDebts(birthDate, duongDoi, bieuDat, khatVong, nhanCach);
        Map<Integer, List<String>> noNghiepNguon = detectKarmicDebtSources(birthDate, duongDoi, bieuDat, khatVong, nhanCach);
        StepResult namCaNhan = calculateNamCaNhan(birthDate, targetYear);
        StepResult thangCaNhan = calculateThangCaNhan(namCaNhan.getNumericValue(), targetMonth);
        List<QhCalculator.PeakInfo> pinnacles = calculatePinnacles(birthDate, duongDoi);

        int currentAge = Math.max(0, targetYear - birthDate.getYear());
        QhCalculator.PeakInfo currentPinnacle;
        if (currentAge <= pinnacles.get(0).getAgeReached()) {
            currentPinnacle = pinnacles.get(0);
        } else if (currentAge <= pinnacles.get(1).getAgeReached()) {
            currentPinnacle = pinnacles.get(1);
        } else if (currentAge <= pinnacles.get(2).getAgeReached()) {
            currentPinnacle = pinnacles.get(2);
        } else {
            currentPinnacle = pinnacles.get(3);
        }

        return new DecozProfile(duongDoi, bieuDat, khatVong, nhanCach, noNghiep, noNghiepNguon,
                namCaNhan, thangCaNhan, pinnacles, currentPinnacle);
    }

    public static class DecozProfile {
        private final StepResult soDuongDoi;
        private final StepResult soBieuDat;
        private final StepResult khatVongTamHon;
        private final StepResult nhanCach;
        private final Set<Integer> soNoNghiep;
        private final Map<Integer, List<String>> noNghiepNguon;
        private final StepResult namCaNhan;
        private final StepResult thangCaNhan;
        private final List<QhCalculator.PeakInfo> pinnacles;
        private final QhCalculator.PeakInfo currentPinnacle;

        public DecozProfile(StepResult soDuongDoi, StepResult soBieuDat, StepResult khatVongTamHon,
                            StepResult nhanCach, Set<Integer> soNoNghiep, Map<Integer, List<String>> noNghiepNguon,
                            StepResult namCaNhan, StepResult thangCaNhan,
                            List<QhCalculator.PeakInfo> pinnacles, QhCalculator.PeakInfo currentPinnacle) {
            this.soDuongDoi = soDuongDoi;
            this.soBieuDat = soBieuDat;
            this.khatVongTamHon = khatVongTamHon;
            this.nhanCach = nhanCach;
            this.soNoNghiep = soNoNghiep;
            this.noNghiepNguon = noNghiepNguon;
            this.namCaNhan = namCaNhan;
            this.thangCaNhan = thangCaNhan;
            this.pinnacles = pinnacles;
            this.currentPinnacle = currentPinnacle;
        }

        public StepResult getSoDuongDoi() {
            return soDuongDoi;
        }

        public StepResult getSoBieuDat() {
            return soBieuDat;
        }

        public StepResult getKhatVongTamHon() {
            return khatVongTamHon;
        }

        public StepResult getNhanCach() {
            return nhanCach;
        }

        public Set<Integer> getSoNoNghiep() {
            return soNoNghiep;
        }

        public Map<Integer, List<String>> getNoNghiepNguon() {
            return noNghiepNguon;
        }

        public StepResult getNamCaNhan() {
            return namCaNhan;
        }

        public StepResult getThangCaNhan() {
            return thangCaNhan;
        }

        public List<QhCalculator.PeakInfo> getPinnacles() {
            return pinnacles;
        }

        public QhCalculator.PeakInfo getCurrentPinnacle() {
            return currentPinnacle;
        }
    }
}

