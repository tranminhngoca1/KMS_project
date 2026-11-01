package com.kms301.nkms.engine;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Chuẩn hoá họ và tên tiếng Việt (Mục 3.2):
 * - Cắt khoảng trắng thừa, chuyển chữ in hoa.
 * - Bỏ dấu tiếng Việt (Unicode NFD), Đ/đ -> D.
 * - Bỏ mọi ký tự không phải A-Z và khoảng trắng.
 * - Tách từ: từ đầu là họ, từ cuối là tên gọi, các từ giữa là tên đệm.
 * - Nguyên âm: A, E, I, O, U, Y (luôn là nguyên âm trong tiếng Việt theo QH, áp dụng thống nhất cho cả DECOZ).
 */
public final class NameNormalizer {

    private static final Set<Character> VOWELS = Set.of('A', 'E', 'I', 'O', 'U', 'Y');

    private static final Map<Character, Integer> DEFAULT_PYTHAGORAS_MAP;

    static {
        Map<Character, Integer> map = new LinkedHashMap<>();
        String[] groups = {
                "AJS", // 1
                "BKT", // 2
                "CLU", // 3
                "DMV", // 4
                "ENW", // 5
                "FOX", // 6
                "GPY", // 7
                "HQZ", // 8
                "IR"   // 9
        };
        for (int i = 0; i < groups.length; i++) {
            int value = i + 1;
            for (char c : groups[i].toCharArray()) {
                map.put(c, value);
            }
        }
        DEFAULT_PYTHAGORAS_MAP = Collections.unmodifiableMap(map);
    }

    private NameNormalizer() {
    }

    public static String normalize(String rawName) {
        if (rawName == null) {
            return "";
        }
        String upper = rawName.trim().toUpperCase(Locale.ROOT);
        upper = upper.replace('Đ', 'D').replace('đ', 'D');
        String decomposed = Normalizer.normalize(upper, Normalizer.Form.NFD);
        String withoutDiacritics = decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lettersAndSpaces = withoutDiacritics.replaceAll("[^A-Z\\s]", " ");
        return lettersAndSpaces.trim().replaceAll("\\s+", " ");
    }

    public static List<String> splitWords(String normalizedName) {
        String norm = normalize(normalizedName);
        if (norm.isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = norm.split(" ");
        List<String> list = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (!token.isBlank()) {
                list.add(token);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static ParsedName parse(String rawName) {
        String norm = normalize(rawName);
        List<String> words = splitWords(norm);
        String ho = "";
        String tenGoi = "";
        List<String> tenDem = new ArrayList<>();
        if (words.size() == 1) {
            tenGoi = words.get(0);
        } else if (words.size() >= 2) {
            ho = words.get(0);
            tenGoi = words.get(words.size() - 1);
            for (int i = 1; i < words.size() - 1; i++) {
                tenDem.add(words.get(i));
            }
        }
        return new ParsedName(rawName, norm, words, ho, Collections.unmodifiableList(tenDem), tenGoi);
    }

    public static boolean isVowel(char c) {
        char upper = Character.toUpperCase(c);
        return VOWELS.contains(upper);
    }

    public static boolean isConsonant(char c) {
        char upper = Character.toUpperCase(c);
        return upper >= 'A' && upper <= 'Z' && !VOWELS.contains(upper);
    }

    public static Map<Character, Integer> getDefaultPythagorasMap() {
        return DEFAULT_PYTHAGORAS_MAP;
    }

    public static int getPythagorasValue(char c) {
        return DEFAULT_PYTHAGORAS_MAP.getOrDefault(Character.toUpperCase(c), 0);
    }

    public static class ParsedName {
        private final String originalName;
        private final String normalizedName;
        private final List<String> words;
        private final String ho;
        private final List<String> tenDem;
        private final String tenGoi;

        public ParsedName(String originalName, String normalizedName, List<String> words,
                          String ho, List<String> tenDem, String tenGoi) {
            this.originalName = originalName;
            this.normalizedName = normalizedName;
            this.words = words;
            this.ho = ho;
            this.tenDem = tenDem;
            this.tenGoi = tenGoi;
        }

        public String getOriginalName() {
            return originalName;
        }

        public String getNormalizedName() {
            return normalizedName;
        }

        public List<String> getWords() {
            return words;
        }

        public String getHo() {
            return ho;
        }

        public List<String> getTenDem() {
            return tenDem;
        }

        public String getTenGoi() {
            return tenGoi;
        }

        public String getLastName() {
            return ho;
        }

        public String getMiddleName() {
            return tenDem == null ? "" : String.join(" ", tenDem);
        }

        public String getFirstName() {
            return tenGoi;
        }
    }
}

