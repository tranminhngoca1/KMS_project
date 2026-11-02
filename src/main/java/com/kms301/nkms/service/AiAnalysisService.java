package com.kms301.nkms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kms301.nkms.engine.HoSoSo;
import com.kms301.nkms.engine.kabbalah.SepharialCalculator;
import com.kms301.nkms.engine.pythagoras.Arrows;
import com.kms301.nkms.engine.pythagoras.BirthChart;
import com.kms301.nkms.engine.pythagoras.DecozCalculator;
import com.kms301.nkms.engine.pythagoras.QhCalculator;
import com.kms301.nkms.engine.veda.JohariCalculator;
import com.kms301.nkms.inference.KhamPhaResult;
import com.kms301.nkms.model.dto.AiAnalysisDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI Analysis Engine (Tầng 4 trong kiến trúc KMS):
 * Đọc Hồ sơ thần số học tổng hợp -> Đối chiếu 3 trường phái (Pythagoras, Vệ Đà, Kabbalah) ->
 * Luận giải cá nhân hoá chuyên sâu với vai trò Chuyên gia Khai vấn Cuộc đời (Life Coach),
 * văn phong ấm áp, thấu cảm, gần gũi, bám sát các chỉ số đã cho, tuyệt đối không dùng tri thức ngoài.
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    // Fallback key an toàn cho môi trường cloud nếu biến môi trường chưa kịp gán
    private static final String DEFAULT_ENCODED_KEY = "QVEuQWI4Uk42SkxQRTBxMkFSYk1WOEtRc2JZdzRSM3UtZUFrTDd2UHFnUU5weS1NMl9tQ3c=";

    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite",
            "gemini-flash-lite-latest",
            "gemini-3.5-flash"
    );

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3-flash-preview}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Cache ẩn danh theo SHA-256 của tổ hợp chỉ số (tối đa 200 hồ sơ trong RAM, không chứa thông tin cá nhân)
    private final Map<String, AiAnalysisDto> anonymousCache = new ConcurrentHashMap<>();

    public AiAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public AiAnalysisDto analyzeProfile(KhamPhaResult result, boolean forceRefresh) {
        String fingerprint = buildAnonymousFingerprint(result.getHoSoSo());
        if (!forceRefresh && anonymousCache.containsKey(fingerprint)) {
            return anonymousCache.get(fingerprint);
        }

        String effectiveKey = getEffectiveApiKey();
        if (effectiveKey != null && !effectiveKey.isBlank()) {
            try {
                AiAnalysisDto aiDto = callGeminiApi(result, effectiveKey);
                if (aiDto != null && aiDto.isSuccess()) {
                    if (anonymousCache.size() > 200) {
                        anonymousCache.clear();
                    }
                    anonymousCache.put(fingerprint, aiDto);
                    return aiDto;
                }
            } catch (Exception ex) {
                log.warn("Gemini API call failed, falling back to synthesized interpretation: {}", ex.getMessage());
            }
        }

        return buildSynthesizedFallback(result);
    }

    private String getEffectiveApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        try {
            java.nio.file.Path keyFile = java.nio.file.Path.of("gemini-key.txt");
            if (java.nio.file.Files.exists(keyFile)) {
                String key = java.nio.file.Files.readString(keyFile, StandardCharsets.UTF_8).trim();
                if (!key.isBlank()) {
                    return key;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            return new String(Base64.getDecoder().decode(DEFAULT_ENCODED_KEY), StandardCharsets.UTF_8).trim();
        } catch (Exception ignored) {
        }
        return null;
    }

    private AiAnalysisDto callGeminiApi(KhamPhaResult result, String keyToUse) throws Exception {
        String prompt = buildPrompt(result);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 8192);
        requestBody.put("generationConfig", generationConfig);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        List<String> modelsToTry = new ArrayList<>();
        if (modelName != null && !modelName.isBlank()) {
            String clean = modelName.startsWith("models/") ? modelName.substring("models/".length()) : modelName.trim();
            modelsToTry.add(clean);
        }
        for (String m : FALLBACK_MODELS) {
            if (!modelsToTry.contains(m)) {
                modelsToTry.add(m);
            }
        }

        for (String currentModel : modelsToTry) {
            try {
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + currentModel + ":generateContent?key=" + keyToUse.trim();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .timeout(Duration.ofSeconds(45))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    log.warn("Gemini model {} returned status {}. Trying next candidate...", currentModel, response.statusCode());
                    continue;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (!candidates.isArray() || candidates.isEmpty()) {
                    continue;
                }

                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (!parts.isArray() || parts.isEmpty()) {
                    continue;
                }

                String rawText = parts.get(0).path("text").asText("");
                if (rawText.isBlank()) {
                    continue;
                }

                JsonNode parsed = objectMapper.readTree(rawText);
                AiAnalysisDto dto = new AiAnalysisDto();
                dto.setSuccess(true);
                dto.setAiGenerated(true);
                dto.setModelName("Hệ Thống KMS Đa Trường Phái");
                dto.setTongQuanHoSo(parsed.path("tongQuanHoSo").asText(""));

                Map<String, AiAnalysisDto.QuestionAiAnalysis> qMap = new HashMap<>();
                for (String qKey : List.of("AI", "GAP_GI", "DI_THE_NAO")) {
                    JsonNode qNode = parsed.path("cauHoi").path(qKey);
                    AiAnalysisDto.QuestionAiAnalysis qa = new AiAnalysisDto.QuestionAiAnalysis();
                    qa.setPhanTichTongHop(qNode.path("phanTichTongHop").asText(""));
                    qa.setChanDung(qNode.path("chanDung").asText(""));
                    qa.setTheManh(qNode.path("theManh").asText(""));
                    qa.setVungToi(qNode.path("vungToi").asText(""));
                    qa.setLoiKhuyen(qNode.path("loiKhuyen").asText(""));
                    qa.setDiemDongThuanAI(qNode.path("diemDongThuanAI").asText(""));
                    qa.setDiemKhacBietAI(qNode.path("diemKhacBietAI").asText(""));
                    qa.setGocNhinPythagoras(qNode.path("gocNhinPythagoras").asText(""));
                    qa.setGocNhinVeda(qNode.path("gocNhinVeda").asText(""));
                    qa.setGocNhinKabbalah(qNode.path("gocNhinKabbalah").asText(""));
                    qMap.put(qKey, qa);
                }
                dto.setCauHoi(qMap);

                if (dto.getCauHoi().containsKey("AI") && !dto.getChanDung("AI").isBlank()) {
                    return dto;
                }
            } catch (Exception ex) {
                log.warn("Error calling model {}: {}. Attempting fallback...", currentModel, ex.getMessage());
            }
        }
        return null;
    }

    private String buildPrompt(KhamPhaResult result) {
        HoSoSo hs = result.getHoSoSo();
        QhCalculator.QhProfile qh = hs.getQh();
        DecozCalculator.DecozProfile decoz = hs.getDecoz();
        JohariCalculator.JohariProfile johari = hs.getJohari();
        SepharialCalculator.SepharialProfile seph = hs.getSepharial();
        BirthChart bc = qh.getBirthChart();

        String originalName = (hs.getParsedName() != null && !hs.getParsedName().getOriginalName().isBlank())
                ? hs.getParsedName().getOriginalName() : "Bạn";
        String tenGoi = (hs.getParsedName() != null && !hs.getParsedName().getTenGoi().isBlank())
                ? hs.getParsedName().getTenGoi() : "bạn";

        String presentArrows = bc.getArrows().getEvaluatedArrows().stream()
                .filter(a -> "CO".equalsIgnoreCase(a.getState()))
                .map(Arrows.EvaluatedArrow::getLabel)
                .collect(Collectors.joining(", "));
        if (presentArrows.isEmpty()) presentArrows = "Không có mũi tên đầy";

        String emptyArrows = bc.getArrows().getEvaluatedArrows().stream()
                .filter(a -> "TRONG".equalsIgnoreCase(a.getState()))
                .map(a -> a.getLabel() + (a.isCompensatedByName() ? " (đã được bù bởi tên riêng)" : ""))
                .collect(Collectors.joining(", "));
        if (emptyArrows.isEmpty()) emptyArrows = "Không có mũi tên trống";

        StringBuilder sb = new StringBuilder();
        sb.append("VAI TRÒ VÀ GIỌNG VĂN:\n");
        sb.append("Bạn là một Chuyên gia Khai vấn Tâm thức và Định hướng Cuộc đời (Master Life Coach và Numerology Counselor). ");
        sb.append("Nhiệm vụ của bạn là đọc Hồ sơ thần số học tổng hợp dưới đây của '").append(originalName).append("' và viết một bài luận giải cá nhân hoá THỰC SỰ SÂU SẮC, TRUYỀN CẢM, ẤM ÁP và GẦN GŨI (như một cuộc trò chuyện 1-1 chân thành, giàu cảm xúc giữa một người thầy, người bạn tri kỷ thấu hiểu tâm lý và người tìm kiếm định hướng).\n\n");

        sb.append("QUY TẮC BẮT BUỘC VỀ PHẠM VI TRI THỨC VÀ VĂN PHONG:\n");
        sb.append("1. TUYỆT ĐỐI CHỈ SỬ DỤNG TRI THỨC ĐÃ CUNG CẤP TRONG HỆ THỐNG (3 trường phái: Pythagoras [David Phillips / Lê Đỗ Quỳnh Hương và Hans Decoz], Vệ Đà [Harish Johari], Kabbalah [Sepharial]).\n");
        sb.append("2. NGHIÊM CẤM đưa vào bất kỳ tri thức ngoài nào như: 12 cung hoàng đạo Tây phương (Bạch Dương, Kim Ngưu, Song Tử...), bài Tarot, phong thuỷ ngũ hành, tử vi tướng số, hay bói toán mê tín dân gian. Mọi nhận định, lời khuyên phải quy chiếu chính xác từ các con số, hành tinh và biểu đồ trong hồ sơ đã cho.\n");
        sb.append("3. VĂN PHONG GẦN GŨI, ẤM ÁP, CÁ NHÂN HOÁ TRIỆT ĐỂ: Xưng hô 'tôi' và 'bạn', gọi tên người này thân mật là '").append(tenGoi).append("'. Lời văn tự nhiên, tâm lý, đồng cảm sâu sắc. Tuyệt đối KHÔNG dùng văn phong máy móc, toán học, ghép số cộc lốc hay liệt kê công thức khô khan.\n");
        sb.append("4. DỊCH NGHĨA CÁC CON SỐ THÀNH CÁ TÍNH ĐỜI THỰC: Hãy nói về cách họ cảm nhận, suy nghĩ, những đấu tranh thầm kín giữa vẻ ngoài và nội tâm, cách đối nhân xử thế, điểm mạnh thiên bẩm và những nỗi sợ thầm kín.\n");
        sb.append("5. ĐAN CÀI SỰ PHỐI HỢP VÀ TƯƠNG TÁC GIỮA CÁC CON SỐ: Ví dụ người có Con số chủ đạo là X nhưng ngày sinh là Y, số Linh hồn là Z, số Tâm hồn Vệ Đà mang năng lượng hành tinh W thì họ thường trải qua sự giằng xé nội tâm ra sao, tầng ưu thế ma phương Kabbalah tác động thế nào.\n");
        sb.append("6. NỘI DUNG PHẢI ĐẦY ĐỦ, CHI TIẾT (mỗi mục viết từ 2 - 3 đoạn văn mạch lạc, phân tích sâu, có ví dụ và liên hệ thực tế, độ dài 200 - 350 từ mỗi mục, tuyệt đối không viết ngắn cộc lốc dưới 150 từ).\n\n");

        sb.append("=== THÔNG TIN HỒ SƠ THẦN SỐ HỌC ===\n");
        sb.append("- Họ và tên: ").append(originalName).append("\n");
        sb.append("- Tuổi hiện tại: ").append(hs.getTuoiHienTai()).append(" tuổi (Năm xét: ").append(hs.getNamHienTai()).append(", Tháng xét: ").append(hs.getThangHienTai()).append(")\n");
        sb.append("- [Pythagoras - QH/Phillips]:\n");
        sb.append("  + Con số chủ đạo: ").append(qh.getConSoChuDao().getDisplayValue()).append("\n");
        sb.append("  + Con số ngày sinh: ").append(qh.getConSoNgaySinh().getDisplayValue()).append("\n");
        sb.append("  + Con số linh hồn (khát vọng nội tâm): ").append(qh.getConSoLinhHon().getDisplayValue()).append("\n");
        sb.append("  + Con số biểu đạt (phong cách bên ngoài): ").append(qh.getConSoBieuDat().getDisplayValue()).append("\n");
        sb.append("  + Con số tên riêng (sứ mệnh): ").append(qh.getConSoTenRieng().getDisplayValue()).append("\n");
        sb.append("  + Mũi tên năng lực: ").append(presentArrows).append("\n");
        sb.append("  + Mũi tên trống: ").append(emptyArrows).append("\n");
        sb.append("  + Năm cá nhân hiện tại (").append(hs.getNamHienTai()).append("): ").append(qh.getNamCaNhan().getDisplayValue())
                .append(" | Tháng cá nhân: ").append(qh.getThangCaNhan().getDisplayValue()).append("\n");
        sb.append("  + Đỉnh cao Kim tự tháp hiện tại: ").append(qh.getKimTuThap().getCurrentPhaseName())
                .append(" mang số ").append(qh.getKimTuThap().getCurrentPeak().getDisplayValue()).append("\n");

        sb.append("- [Pythagoras - Nhánh Mỹ Hans Decoz]:\n");
        sb.append("  + Số Đường đời: ").append(decoz.getSoDuongDoi().getDisplayValue()).append("\n");
        sb.append("  + Số Biểu đạt: ").append(decoz.getSoBieuDat().getDisplayValue()).append("\n");
        sb.append("  + Khát vọng tâm hồn: ").append(decoz.getKhatVongTamHon().getDisplayValue()).append("\n");
        sb.append("  + Nợ nghiệp: ").append(decoz.getSoNoNghiep().isEmpty() ? "Không có" : decoz.getSoNoNghiep()).append("\n");
        sb.append("  + Đỉnh Pinnacle hiện tại: Số ").append(decoz.getCurrentPinnacle().getDisplayValue()).append("\n");

        sb.append("- [Ấn Độ / Vệ Đà - Harish Johari]:\n");
        sb.append("  + Số Tâm hồn (Psychic): ").append(johari.getSoTamHon().getDisplayValue())
                .append(" — Hành tinh: ").append(johari.getHanhTinhTamHon()).append("\n");
        sb.append("  + Số Định mệnh (Destiny): ").append(johari.getSoDinhMenh().getDisplayValue())
                .append(" — Hành tinh: ").append(johari.getHanhTinhDinhMenh()).append("\n");
        sb.append("  + Tương quan Tâm hồn và Định mệnh: ").append(johari.getQuanHeTamHonDinhMenh()).append("\n");
        sb.append("  + Số Năm Vệ Đà: ").append(johari.getSoNam().getDisplayValue()).append("\n");

        sb.append("- [Kabbalah / Chaldean - Sepharial]:\n");
        sb.append("  + Số Chìa khoá: ").append(seph.getSoChiaKhoa().getDisplayValue())
                .append(" — Hành tinh: ").append(seph.getHanhTinhChinh()).append("\n");
        sb.append("  + Ma phương 3 tầng: Tầng ưu thế là ").append(seph.getMagicSquare().getDominantPlane()).append("\n\n");

        sb.append("=== YÊU CẦU ĐỊNH DẠNG JSON TRẢ VỀ ===\n");
        sb.append("Trả về ĐÚNG 1 JSON object (không bọc markdown, không dùng ```json). Từng mục phải viết rất chi tiết, sâu sắc, chia thành 2-3 đoạn văn rõ ràng:\n");
        sb.append("{\n");
        sb.append("  \"tongQuanHoSo\": \"Đoạn văn mở đầu 130-180 từ ấm áp, xúc động, bao quát khí chất và tiềm năng cuộc đời của ").append(tenGoi).append("...\",\n");
        sb.append("  \"cauHoi\": {\n");
        sb.append("    \"AI\": {\n");
        sb.append("      \"phanTichTongHop\": \"Thông điệp đúc kết trả lời 'Tôi là ai?' dài 100-140 từ...\",\n");
        sb.append("      \"chanDung\": \"Bản ngã cốt lõi & chân dung tâm lý: 250-320 từ phân tích chiều sâu tính cách, sự giao hoà giữa số chủ đạo, ngày sinh và hành tinh Vệ Đà...\",\n");
        sb.append("      \"theManh\": \"Năng lực bẩm sinh & thế mạnh vượt trội: 200-280 từ chỉ rõ các tài năng thiên bẩm, trực giác, mũi tên biểu đồ và tầng ưu thế ma phương...\",\n");
        sb.append("      \"vungToi\": \"Điểm mù & mâu thuẫn nội tâm cần gỡ bỏ: 200-280 từ về góc khuất cảm xúc, giằng xé tâm lý bên trong, nỗi sợ thầm kín...\",\n");
        sb.append("      \"loiKhuyen\": \"Định hướng phát triển & môi trường lý tưởng: 200-280 từ về nghề nghiệp và môi trường sống giúp họ thăng hoa trọn vẹn...\",\n");
        sb.append("      \"diemDongThuanAI\": \"Điểm chung mà cả Pythagoras và Vệ Đà cùng nhận định về con người này...\",\n");
        sb.append("      \"diemKhacBietAI\": \"Góc nhìn bổ khuyết giữa 3 trường phái trên bản ngã này...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Tóm lược góc nhìn phương Tây...\",\n");
        sb.append("      \"gocNhinVeda\": \"Tóm lược góc nhìn Vệ Đà...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Tóm lược góc nhìn Kabbalah...\"\n");
        sb.append("    },\n");
        sb.append("    \"GAP_GI\": {\n");
        sb.append("      \"phanTichTongHop\": \"Thông điệp đúc kết trả lời 'Tôi đang gặp gì?' dài 100-140 từ...\",\n");
        sb.append("      \"chanDung\": \"Bối cảnh thời vận & năng lượng năm nay: 250-320 từ phân tích biến động tâm lý, cảm xúc và xu hướng năm cá nhân kết hợp nhịp số năm Vệ Đà...\",\n");
        sb.append("      \"theManh\": \"Cơ hội và đòn bẩy thuận lợi: 200-280 từ chỉ rõ thời cơ mở rộng, những lĩnh vực may mắn trong chu kỳ năm nay...\",\n");
        sb.append("      \"vungToi\": \"Cạm bẫy & thử thách cần tránh: 200-280 từ về những rủi ro tâm lý, cám dỗ nôn nóng hoặc trì trệ trong năm nay...\",\n");
        sb.append("      \"loiKhuyen\": \"Bài học độ tuổi & đỉnh cao kim tự tháp: 200-280 từ về bài học cuộc đời muốn bạn rèn luyện ở độ tuổi ").append(hs.getTuoiHienTai()).append(" này...\",\n");
        sb.append("      \"diemDongThuanAI\": \"Điểm tương đồng về chu kỳ thời vận giữa các trường phái...\",\n");
        sb.append("      \"diemKhacBietAI\": \"Khác biệt về nhịp chu kỳ thời vận giữa Pythagoras và Vệ Đà...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Tóm lược Năm cá nhân & Kim tự tháp Pythagoras...\",\n");
        sb.append("      \"gocNhinVeda\": \"Tóm lược Năm Vệ Đà...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Tóm lược chu kỳ Kabbalah...\"\n");
        sb.append("    },\n");
        sb.append("    \"DI_THE_NAO\": {\n");
        sb.append("      \"phanTichTongHop\": \"Thông điệp đúc kết trả lời 'Tôi nên đi thế nào?' dài 100-140 từ...\",\n");
        sb.append("      \"chanDung\": \"Kim chỉ nam sống & tư duy cốt lõi: 250-320 từ về thái độ sống giúp ").append(tenGoi).append(" luôn an nhiên, vững vàng...\",\n");
        sb.append("      \"theManh\": \"Hành động ưu tiên & kỷ luật bản thân: 200-280 từ về những thói quen, kỹ năng cụ thể cần bắt tay rèn luyện ngay...\",\n");
        sb.append("      \"vungToi\": \"Rào cản tâm lý & thói quen cần buông bỏ: 200-280 từ về những xiềng xích tư duy, thói quen trì hoãn cần loại trừ...\",\n");
        sb.append("      \"loiKhuyen\": \"Chiến lược dài hạn & khai phóng tiềm năng: 200-280 từ về lộ trình phát triển trọn vẹn kết hợp tinh hoa 3 trường phái...\",\n");
        sb.append("      \"diemDongThuanAI\": \"Điểm chung về phương pháp tu dưỡng...\",\n");
        sb.append("      \"diemKhacBietAI\": \"Khác biệt trong công cụ cải thiện năng lượng giữa các trường phái...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Lời khuyên rèn luyện Pythagoras...\",\n");
        sb.append("      \"gocNhinVeda\": \"Lời khuyên tâm thức Vệ Đà...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Lời khuyên cân bằng Kabbalah...\"\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private AiAnalysisDto buildSynthesizedFallback(KhamPhaResult result) {
        HoSoSo hs = result.getHoSoSo();
        QhCalculator.QhProfile qh = hs.getQh();
        JohariCalculator.JohariProfile johari = hs.getJohari();
        SepharialCalculator.SepharialProfile seph = hs.getSepharial();

        int chuDao = qh.getConSoChuDao().getNumericValue();
        int ngaySinhNum = qh.getConSoNgaySinh().getNumericValue();
        int namCaNhanNum = qh.getNamCaNhan().getNumericValue();
        String tenGoi = (hs.getParsedName() != null && !hs.getParsedName().getTenGoi().isBlank())
                ? hs.getParsedName().getTenGoi() : "bạn";

        AiAnalysisDto dto = new AiAnalysisDto();
        dto.setSuccess(true);
        dto.setAiGenerated(false);
        dto.setModelName("Hệ Thống KMS Đa Trường Phái");

        dto.setTongQuanHoSo(String.format(
                "Chào %s, ở tuổi %d, khi nhìn sâu vào hồ sơ thần số học đa trường phái của bạn, tôi thấy một bản hòa ca năng lượng vô cùng đặc biệt. Bạn mang trong mình ngọn lửa hành động của Con số chủ đạo %s, phong cách độc đáo từ số Ngày sinh %s, được nuôi dưỡng bởi năng lượng tâm thức sâu thẳm của hành tinh %s (Số Tâm hồn %s) và %s (Số Định mệnh %s). Sự kết hợp này mang lại cho bạn một sức mạnh nội tại bền bỉ, vừa nhạy bén trong tư duy thực tế, vừa chứa đựng những khoảng lặng tâm hồn giàu lòng trắc ẩn.",
                tenGoi, hs.getTuoiHienTai(),
                qh.getConSoChuDao().getDisplayValue(),
                qh.getConSoNgaySinh().getDisplayValue(),
                johari.getHanhTinhTamHon(),
                johari.getSoTamHon().getDisplayValue(),
                johari.getHanhTinhDinhMenh(),
                johari.getSoDinhMenh().getDisplayValue()
        ));

        Map<String, AiAnalysisDto.QuestionAiAnalysis> map = new HashMap<>();

        // CÂU HỎI 1: TÔI LÀ AI?
        AiAnalysisDto.QuestionAiAnalysis qaAi = new AiAnalysisDto.QuestionAiAnalysis();
        qaAi.setPhanTichTongHop(String.format(
                "Bạn là sự giao thoa kỳ diệu giữa khí chất tự chủ của Con số chủ đạo %s và phong cách ngày sinh %s, dưới sự soi chiếu của hành tinh %s: Một người có trực giác nhạy bén, giàu khát vọng cống hiến nhưng luôn đòi hỏi sự chân thành tuyệt đối ở chính mình và cuộc sống.",
                qh.getConSoChuDao().getDisplayValue(), qh.getConSoNgaySinh().getDisplayValue(), johari.getHanhTinhTamHon()
        ));
        qaAi.setChanDung(getPersonalityPortrait(chuDao, ngaySinhNum, johari.getHanhTinhTamHon(), tenGoi));
        qaAi.setTheManh(getStrengthsPortrait(chuDao, qh.getConSoLinhHon().getNumericValue(), seph.getMagicSquare().getDominantPlane()));
        qaAi.setVungToi(getBlindspotsPortrait(chuDao, johari.getSoTamHon().getNumericValue()));
        qaAi.setLoiKhuyen(getCareerAndLifeAdvice(chuDao, tenGoi));
        qaAi.setDiemDongThuanAI("Cả 3 trường phái Pythagoras, Vệ Đà và Kabbalah cùng đồng thuận rằng bản ngã cốt lõi của bạn bắt nguồn từ sự kết nối giữa tư duy trí tuệ bẩm sinh và nhu cầu khẳng định giá trị độc bản.");
        qaAi.setDiemKhacBietAI("Pythagoras giải mã cấu trúc tính cách qua họ tên và ngày sinh; Vệ Đà đào sâu vào năng lượng hành tinh tâm thức; Kabbalah tập trung vào quy luật cân bằng ma phương 3 tầng.");
        map.put("AI", qaAi);

        // CÂU HỎI 2: TÔI ĐANG GẶP GÌ?
        AiAnalysisDto.QuestionAiAnalysis qaGap = new AiAnalysisDto.QuestionAiAnalysis();
        qaGap.setPhanTichTongHop(String.format(
                "Năm nay (%d, tuổi %d), bạn đang bước vào Năm cá nhân số %s theo Pythagoras và chu kỳ số %s của Vệ Đà: Một thời điểm bản lề để bạn thanh lọc năng lượng, củng cố nội lực và chuẩn bị cho những bước tiến lớn.",
                hs.getNamHienTai(), hs.getTuoiHienTai(), qh.getNamCaNhan().getDisplayValue(), johari.getSoNam().getDisplayValue()
        ));
        qaGap.setChanDung(getYearEnergyPortrait(namCaNhanNum, hs.getTuoiHienTai(), tenGoi));
        qaGap.setTheManh(getYearOpportunitiesPortrait(namCaNhanNum));
        qaGap.setVungToi(getYearChallengesPortrait(namCaNhanNum));
        qaGap.setLoiKhuyen(String.format("Ở tuổi %d, giai đoạn %s (Đỉnh cao mang năng lượng số %s) nhắc nhở %s cần giữ vững tâm thế kiên định, lấy việc rèn luyện nội lực làm gốc rễ thay vì nóng vội tìm kiếm thành quả bề nổi.",
                hs.getTuoiHienTai(), qh.getKimTuThap().getCurrentPhaseName(), qh.getKimTuThap().getCurrentPeak().getDisplayValue(), tenGoi));
        qaGap.setDiemDongThuanAI("Pythagoras và Vệ Đà đều nhận định đây là năm có tính chất bản lề trong chu kỳ 9 năm của bạn, tạo tiền đề quyết định cho những năm tiếp theo.");
        qaGap.setDiemKhacBietAI("Pythagoras tính theo chu kỳ năm tự nhiên; Vệ Đà bổ sung thêm ảnh hưởng chuyển dịch của hành tinh chủ quản theo ngày sinh.");
        map.put("GAP_GI", qaGap);

        // CÂU HỎI 3: TÔI NÊN ĐI THẾ NÀO?
        AiAnalysisDto.QuestionAiAnalysis qaDi = new AiAnalysisDto.QuestionAiAnalysis();
        qaDi.setPhanTichTongHop(String.format(
                "Kim chỉ nam tối thượng dành cho %s là học cách dung hoà giữa hành động quyết đoán bên ngoài và sự tĩnh lặng bên trong, nương theo sóng nhịp Năm cá nhân %s để gặt hái thành công bền vững.",
                tenGoi, qh.getNamCaNhan().getDisplayValue()
        ));
        qaDi.setChanDung(String.format(
                "Thái độ sống then chốt: Hãy học cách lắng nghe trực giác và trân trọng những trải nghiệm mà cuộc đời mang lại. Đừng để nỗi sợ thất bại cản bước chân tiên phong của bạn, nhưng cũng đừng quên chăm sóc khu vườn tâm hồn sau những ngày làm việc miệt mài.",
                tenGoi));
        qaDi.setTheManh("Hành động cụ thể: Thiết lập kỷ luật cá nhân hàng ngày, tập trung hoàn thành dứt điểm từng mục tiêu trọng tâm và chủ động mở rộng những mối quan hệ có chiều sâu giá trị.");
        qaDi.setVungToi("Rào cản cần vượt qua: Tránh thói quen trì hoãn do cầu toàn quá mức, bớt lo âu về những điều chưa tới và học cách từ chối những việc làm hao tổn năng lượng mà không phục vụ mục tiêu dài hạn.");
        qaDi.setLoiKhuyen("Chiến lược dài hạn: Kết hợp tư duy hệ thống của Pythagoras với sự bình an tâm thức của Vệ Đà và sự cân bằng ma phương Kabbalah để kiến tạo một cuộc đời trọn vẹn và an lạc.");
        qaDi.setDiemDongThuanAI("Cả 3 trường phái đều khuyến nghị bạn hướng vào việc tự tu dưỡng, rèn luyện sự tự giác và kiên trì với những giá trị cốt lõi.");
        qaDi.setDiemKhacBietAI("Pythagoras chú trọng rèn luyện bù mũi tên trống; Vệ Đà khuyên chuyển hoá năng lượng hành tinh; Kabbalah hướng tới sự điều phối hài hoà 3 tầng thể xác - trí tuệ - tinh thần.");
        map.put("DI_THE_NAO", qaDi);

        dto.setCauHoi(map);
        return dto;
    }

    private String getPersonalityPortrait(int chuDao, int ngaySinh, String hanhTinh, String tenGoi) {
        String base = switch (chuDao) {
            case 3 -> String.format(
                    "Bạn sở hữu tư duy cực kỳ linh hoạt, hoạt ngôn và có khiếu hài hước tự nhiên đầy cuốn hút. Ở bạn luôn toát ra một nguồn năng lượng sáng tạo, ấm áp và khả năng truyền cảm hứng tuyệt vời trong giao tiếp.\n\n" +
                    "Tuy nhiên, đằng sau vẻ ngoài hóm hỉnh và sôi nổi ấy lại là một thế giới nội tâm rất tinh tế, chịu sự ảnh hưởng sâu sắc từ hành tinh %s. Bạn nhạy cảm với những biến chuyển cảm xúc xung quanh và dễ cảm thấy cô đơn nếu không tìm được người thực sự thấu hiểu chiều sâu suy nghĩ của mình.",
                    hanhTinh);
            case 4 -> String.format(
                    "Bạn là mẫu người thực tế, cẩn trọng, có tinh thần trách nhiệm và tính kỷ luật rất đáng nể. Bạn luôn coi trọng sự chuẩn mực, an toàn và chỉ thực sự yên tâm khi công việc được tổ chức theo quy trình rõ ràng.\n\n" +
                    "Sự bảo trợ của hành tinh %s mang lại cho bạn sự kiên trì phi thường và khả năng chịu áp lực cao. Trong mắt bạn bè và đồng nghiệp, bạn luôn là điểm tựa đáng tin cậy nhất trong những lúc giông bão.",
                    hanhTinh);
            case 5 -> String.format(
                    "Bạn là cánh chim tự do của cuộc đời, mang tâm hồn phiêu lưu, khao khát khám phá và luôn muốn phá vỡ những giới hạn rập khuôn tẻ nhạt. Bạn thích nghi cực nhanh với sự thay đổi và có tài ngoại giao bẩm sinh.\n\n" +
                    "Dưới ảnh hưởng của hành tinh %s, bạn sống trọn vẹn cho từng khoảnh khắc hiện tại. Bạn ghét cảm giác bị trói buộc và luôn tìm kiếm những chân trời mới để làm phong phú thêm vốn sống của mình.",
                    hanhTinh);
            case 6 -> String.format(
                    "Bạn mang trái tim ấm áp của một người chăm sóc, giàu lòng trắc ẩn và luôn đặt tình cảm gia đình cũng như những người thân yêu lên vị trí ưu tiên hàng đầu. Bạn có gu thẩm mỹ tinh tế và tình yêu sâu sắc với cái đẹp.\n\n" +
                    "Năng lượng từ hành tinh %s thôi thúc bạn tạo dựng sự hòa thuận và bình yên ở mọi nơi bạn đặt chân đến. Bạn sẵn sàng hy sinh lợi ích cá nhân vì sự ấm êm của tập thể.",
                    hanhTinh);
            case 7 -> String.format(
                    "Bạn là một linh hồn chiêm nghiệm, sở hữu trí tuệ triết học sâu sắc và trực giác vô cùng nhạy bén. Bạn không bao giờ vội tin vào những điều bề nổi mà luôn khao khát tự mình trải nghiệm để tìm ra bản chất chân lý.\n\n" +
                    "Sự dẫn dắt của hành tinh %s tạo nên cho bạn một phong thái điềm tĩnh, độc lập. Bạn cần những khoảng không gian tĩnh lặng riêng tư để tái tạo năng lượng và đúc kết những bài học cuộc đời quý giá.",
                    hanhTinh);
            case 8 -> String.format(
                    "Bạn mang tố chất của một nhà lãnh đạo bẩm sinh, sở hữu tư duy chiến lược sắc bén và khát vọng thành công vượt bậc. Bạn hành động dứt khoát, tự tin và có năng lực điều hành xuất sắc.\n\n" +
                    "Hành tinh %s mang lại cho bạn bản lĩnh kiên cường trước thử thách. Bạn coi trọng sự công bằng, tính độc lập tự chủ và luôn nỗ lực xây dựng một nền tảng tài chính - sự nghiệp vững chắc cho tương lai.",
                    hanhTinh);
            case 9 -> String.format(
                    "Bạn là người giàu lý tưởng nhân đạo, luôn hướng về cộng đồng và khao khát đem lại những giá trị tốt đẹp cho cuộc đời. Bạn bao dung, chân thành và có sức hút tự nhiên từ sự vị tha.\n\n" +
                    "Dưới ánh sáng của hành tinh %s, bạn luôn mong muốn cống hiến và nâng đỡ người khác. Bạn nhìn cuộc sống bằng lăng kính nhân văn và có khả năng đánh thức lòng tốt trong lòng mọi người.",
                    hanhTinh);
            case 10, 1 -> String.format(
                    "Bạn mang tố chất của một người tiên phong: Độc lập, quyết đoán, tự tin và có khả năng thích ứng siêu việt trong mọi hoàn cảnh sống. Bạn dám nghĩ dám làm và không ngại đối mặt với những thử thách mới.\n\n" +
                    "Hành tinh %s ban tặng cho bạn nguồn sinh lực dồi dào và ý chí dẫn dắt mạnh mẽ. Bạn không thích đi theo lối mòn mà luôn muốn tạo dựng dấu ấn riêng biệt trong cuộc đời.",
                    hanhTinh);
            case 11 -> String.format(
                    "Bạn là một linh hồn có trực giác tâm linh cực kỳ nhạy bén, sống chân thành và luôn hướng tới sự hòa hợp sâu sắc. Bạn có khả năng cảm nhận được tâm tư, tình cảm của người khác mà không cần họ phải cất lời.\n\n" +
                    "Sự kết hợp với hành tinh %s mang đến cho bạn tiềm năng tinh thần to lớn, giúp bạn trở thành người dẫn đường cảm xúc và truyền cảm hứng chữa lành cho những người xung quanh.",
                    hanhTinh);
            case 22 -> String.format(
                    "Bạn mang năng lượng của 'Người kiến tạo bậc thầy': Vừa sở hữu tầm nhìn chiến lược vĩ mô, vừa có năng lực thực thi thực tế siêu việt. Bạn khao khát để lại những công trình, dự án có giá trị lâu dài cho xã hội.\n\n" +
                    "Hành tinh %s tiếp thêm cho bạn sự bền bỉ sắt đá để biến những giấc mơ táo bạo nhất thành hiện thực cụ thể trên mặt đất.",
                    hanhTinh);
            default -> String.format(
                    "Bạn là người sở hữu nội lực phong phú, tính cách đa chiều và có khả năng dung hòa tuyệt vời giữa lý trí và cảm xúc dưới sự bảo trợ của hành tinh %s.",
                    hanhTinh);
        };
        return base;
    }

    private String getStrengthsPortrait(int chuDao, int linhHon, String dominantPlane) {
        return switch (chuDao) {
            case 3 -> String.format(
                    "Thế mạnh vượt trội của bạn là tài năng biểu đạt ngôn từ xuất sắc, tư duy sáng tạo linh hoạt và năng khiếu kết nối mọi người một cách tự nhiên. Bạn có biệt tài biến những ý tưởng trừu tượng thành những câu chuyện sinh động và lôi cuốn.\n\n" +
                    "Đặc biệt, với tầng ưu thế %s trong ma phương Kabbalah, bạn có khả năng nắm bắt tâm lý đám đông rất nhanh, dễ dàng thuyết phục và truyền lửa nhiệt huyết cho tập thể.",
                    dominantPlane);
            case 4 -> String.format(
                    "Năng lực bẩm sinh của bạn là khả năng phân tích chi tiết, tổ chức công việc khoa học và sự kiên trì bền bỉ hiếm có. Bạn rất giỏi trong việc thiết lập hệ thống, quản lý vận hành và kiểm soát rủi ro.\n\n" +
                    "Sự kết hợp với tầng ưu thế %s giúp bạn luôn nhìn thấu các mắt xích công việc và đưa ra những giải pháp thực tế, vững chắc nhất cho mọi bài toán khó.",
                    dominantPlane);
            case 5 -> String.format(
                    "Điểm mạnh nổi bật là sự nhanh nhạy trước thời cuộc, tài ứng biến linh hoạt trong mọi tình huống bất ngờ và năng lực kết nối đa dạng các mối quan hệ xã hội. Bạn luôn là người tìm ra hướng đi mới khi mọi thứ tưởng chừng bế tắc.\n\n" +
                    "Tầng ưu thế %s của Kabbalah tiếp thêm cho bạn trực giác sắc bén và sự nhạy cảm thương mại tuyệt vời.",
                    dominantPlane);
            case 6 -> String.format(
                    "Bạn sở hữu năng khiếu thẩm mỹ bẩm sinh, khả năng chữa lành cảm xúc và tài năng xây dựng văn hóa gắn kết tập thể. Bạn rất giỏi trong việc lắng nghe, cảm thông và xoa dịu những bất hòa xung quanh.\n\n" +
                    "Tầng ưu thế %s giúp bạn luôn biết cách biến không gian sống và làm việc thành nơi tràn ngập sự ấm áp, truyền cảm hứng cho mọi người.",
                    dominantPlane);
            case 7 -> String.format(
                    "Thế mạnh thiên bẩm là năng lực nghiên cứu chuyên sâu, khả năng quan sát thấu suốt bản chất quy luật và trực giác phán đoán chuẩn xác. Bạn có năng khiếu tự học vượt bậc và khả năng trở thành chuyên gia trong lĩnh vực mình theo đuổi.\n\n" +
                    "Nhờ tầng ưu thế %s, bạn luôn đào sâu đến tận gốc rễ của tri thức và đưa ra những góc nhìn độc lập, sáng suốt.",
                    dominantPlane);
            case 8 -> String.format(
                    "Năng lực lãnh đạo quyết đoán, trực giác tài chính - kinh doanh sắc sảo và khả năng tối ưu hóa nguồn lực là những vũ khí lợi hại giúp bạn luôn nổi bật ở các vị trí điều hành hoặc kinh doanh độc lập.\n\n" +
                    "Tầng ưu thế %s tiếp thêm cho bạn sự tỉnh táo và tầm nhìn bao quát để chớp lấy những cơ hội lớn.",
                    dominantPlane);
            case 9 -> String.format(
                    "Sức mạnh lớn nhất của bạn là tầm nhìn nhân văn rộng mở, khả năng quy tụ lòng người và năng khiếu giảng dạy, cố vấn hoặc hoạt động xã hội. Tiếng nói của bạn luôn mang trọng lượng và sự tin cậy tự nhiên.\n\n" +
                    "Cùng với tầng ưu thế %s, bạn có khả năng truyền cảm hứng và đánh thức tiềm năng to lớn trong từng con người.",
                    dominantPlane);
            default -> String.format(
                    "Bạn có khả năng tự nhận thức cao, tư duy độc lập và năng lực thích ứng linh hoạt trong nhiều môi trường khác nhau nhờ sự hỗ trợ đắc lực từ tầng ưu thế %s.",
                    dominantPlane);
        };
    }

    private String getBlindspotsPortrait(int chuDao, int tamHon) {
        return switch (chuDao) {
            case 3 -> "Điểm mù lớn nhất là sự mất kiên nhẫn khi phải làm những công việc lặp đi lặp lại và dễ để cảm xúc tức thời chi phối các quyết định quan trọng.\n\n" +
                    "Đôi khi bạn nói nhiều hơn lắng nghe hoặc phân tán năng lượng vào quá nhiều dự án cùng lúc khiến mọi việc dở dang. Cần học cách kiểm soát sự bốc đồng và kiên trì với một mục tiêu duy nhất đến khi có kết quả.";
            case 4 -> "Góc khuất tâm lý là sự cầu toàn quá mức, bảo thủ và ngại thay đổi khiến bản thân dễ rơi vào trạng thái căng thẳng, kiệt sức.\n\n" +
                    "Bạn có xu hướng xem xét vấn đề quá chi tiết đến mức bỏ lỡ những cơ hội mang tính đột phá chỉ vì chúng chưa có lộ trình an toàn 100%. Hãy học cách thả lỏng và cho phép những điều bất ngờ diễn ra.";
            case 5 -> "Mâu thuẫn nội tâm nằm ở sự thèm muốn tự do quá mức, dẫn đến trạng thái 'cả thèm chóng chán' và thiếu sự bền chí trong dài hạn.\n\n" +
                    "Bạn dễ bị thu hút bởi những trải nghiệm mới lạ mà xao nhãng trách nhiệm cốt lõi. Cần học cách tự đặt ra kỷ luật cho chính mình để sự tự do mang lại thành quả thực chất.";
            case 6 -> "Cạm bẫy cảm xúc là sự bao bọc thái quá và hay ôm đồm lo lắng những chuyện chưa xảy ra của người khác.\n\n" +
                    "Bạn thường đặt kỳ vọng quá cao vào sự hồi đáp của người thân để rồi dễ rơi vào cảm giác thất vọng hoặc tự biến mình thành 'nạn nhân' trong các mối quan hệ. Hãy học cách thiết lập ranh giới cảm xúc lành mạnh.";
            case 7 -> "Điểm mù là xu hướng tự cô lập bản thân, đa nghi và khó bộc bạch cảm xúc thật ra bên ngoài.\n\n" +
                    "Sự khắt khe về tri thức đôi khi khiến bạn trở nên xa cách hoặc phán xét người khác theo tiêu chuẩn quá cao của mình. Hãy mở lòng sẻ chia và đón nhận sự ấm áp giản dị từ cuộc sống đời thường.";
            case 8 -> "Cạm bẫy là cái tôi quá lớn, tính độc đoán và xu hướng bị cuốn vào vòng xoáy công việc/vật chất mà lãng quên sức khoẻ và cảm xúc của những người thân cận.\n\n" +
                    "Hãy học cách lắng nghe phản hồi từ cộng sự và dành thời gian nuôi dưỡng đời sống tinh thần để giữ được sự cân bằng bền vững.";
            default -> "Cần cảnh giác với sự giằng xé giữa khát vọng khẳng định bản thân và áp lực từ kỳ vọng của những người xung quanh. Hãy học cách kiên định với giá trị cốt lõi của chính mình.";
        };
    }

    private String getCareerAndLifeAdvice(int chuDao, String tenGoi) {
        return switch (chuDao) {
            case 3 -> String.format(
                    "Môi trường phát triển lý tưởng: Các lĩnh vực truyền thông, marketing, sáng tạo nội dung, nghệ thuật, giảng dạy, tư vấn hoặc quan hệ công chúng – nơi cho phép bạn tự do tỏa sáng và kết nối.\n\n" +
                    "Lời khuyên khai vấn: Hãy rèn luyện thói quen ghi chép mục tiêu cụ thể mỗi ngày và kiên quyết hoàn thành dứt điểm từng dự án trước khi bắt tay vào một ý tưởng mới, %s nhé.",
                    tenGoi);
            case 4 -> String.format(
                    "Môi trường phù hợp: Kỹ thuật, công nghệ thông tin, quản trị vận hành, tài chính kế toán, luật pháp, y tế hoặc kiến trúc – nơi đòi hỏi sự chuẩn mực, trật tự và tính chính xác cao.\n\n" +
                    "Lời khuyên khai vấn: Hãy học cách đón nhận những phương pháp làm việc mới mẻ và cho phép bản thân có những khoảng nghỉ ngơi tái tạo năng lượng thay vì gồng gánh mọi việc một mình.",
                    tenGoi);
            case 5 -> String.format(
                    "Môi trường lý tưởng: Kinh doanh tự do, du lịch, xuất nhập khẩu, truyền thông sự kiện, đối ngoại hoặc các công việc đòi hỏi sự di chuyển và thích ứng liên tục.\n\n" +
                    "Lời khuyên khai vấn: Hãy tự đặt cho mình những quy tắc kỷ luật sắt đá. Khi tự do đi đôi với kỷ luật tự giác, bạn sẽ tạo ra những bước nhảy vọt phi thường.",
                    tenGoi);
            case 6 -> String.format(
                    "Môi trường phù hợp: Giáo dục, tâm lý trị liệu, chăm sóc sức khỏe, thiết kế thẩm mỹ, quản trị nhân sự hoặc các hoạt động dịch vụ cộng đồng.\n\n" +
                    "Lời khuyên khai vấn: Hãy học cách yêu thương và chăm sóc chính bản thân mình trước tiên. Bạn chỉ có thể nâng đỡ người khác một cách trọn vẹn khi chiếc cốc năng lượng của bạn luôn đong đầy.",
                    tenGoi);
            case 7 -> String.format(
                    "Môi trường lý tưởng: Nghiên cứu khoa học, phân tích dữ liệu, giảng dạy chuyên sâu, triết học, công nghệ cao hoặc các lĩnh vực khai vấn tâm lý.\n\n" +
                    "Lời khuyên khai vấn: Hãy thực hành chia sẻ những tri thức và chiêm nghiệm quý báu mà bạn tích lũy được ra cộng đồng thay vì giữ kín cho riêng mình.",
                    tenGoi);
            case 8 -> String.format(
                    "Môi trường phù hợp: Quản trị doanh nghiệp, đầu tư tài chính, bất động sản, luật kinh tế hoặc các vị trí điều hành chiến lược cấp cao.\n\n" +
                    "Lời khuyên khai vấn: Hãy luôn gắn liền mục tiêu tài chính với đạo đức và giá trị phụng sự cộng đồng. Sự thịnh vượng bền vững nhất luôn bắt nguồn từ sự công bằng và lòng trắc ẩn.",
                    tenGoi);
            default -> String.format(
                    "Hãy kiên định với thế mạnh độc bản của bạn, chọn môi trường làm việc khuyến khích sự tự chủ và trao quyền, %s nhé.",
                    tenGoi);
        };
    }

    private String getYearEnergyPortrait(int namCaNhan, int tuoi, String tenGoi) {
        return switch (namCaNhan) {
            case 1 -> String.format(
                    "Năm nay là Năm cá nhân số 1 – Năm của sự khởi đầu mới đầy rực rỡ! Đây là thời điểm vũ trụ mở ra một chu kỳ 9 năm hoàn toàn mới cho bạn ở tuổi %d.\n\n" +
                    "Năng lượng thôi thúc bạn bước ra khỏi vùng an toàn, tự tin đưa ra những quyết định độc lập và bắt tay vào hiện thực hóa những hoài bão lớn.",
                    tuoi);
            case 2 -> String.format(
                    "Năm nay là Năm cá nhân số 2 – Năm của sự lắng dịu, kết nối và kiên nhẫn. Nhịp sống chậm lại một nhịp để bạn lắng nghe trực giác và hàn gắn các mối quan hệ sâu sắc.\n\n" +
                    "Đây là thời điểm để bạn chuẩn bị nội lực vững vàng, học cách hợp tác hòa hợp thay vì đối đầu trực diện.",
                    tuoi);
            case 3 -> String.format(
                    "Năm nay là Năm cá nhân số 3 – Năm của sự bừng nở sáng tạo và mở rộng giao tiếp! Năng lượng tràn ngập sự tươi vui, thôi thúc bạn học hỏi kỹ năng mới và lan tỏa niềm cảm hứng tích cực.\n\n" +
                    "Hãy chủ động mở rộng mạng lưới quan hệ và thể hiện tài năng của mình một cách tự tin nhất.",
                    tuoi);
            case 4 -> String.format(
                    "Năm nay là Năm cá nhân số 4 – Năm của củng cố nền móng và kỷ luật thực tế. Đây là giai đoạn bạn cần dọn dẹp các tồn đọng, chăm sóc sức khỏe và xây dựng cấu trúc công việc vững chắc.\n\n" +
                    "Kiên trì và tỉ mỉ trong từng việc nhỏ sẽ giúp bạn đặt nền móng không thể lay chuyển cho tương lai.",
                    tuoi);
            case 5 -> String.format(
                    "Năm nay là Năm cá nhân số 5 – Năm của những chuyển dịch lớn và cơ hội bứt phá ngoạn mục! Nhiều cánh cửa mới sẽ bất ngờ mở ra, mang đến những trải nghiệm phong phú.\n\n" +
                    "Hãy dũng cảm thích nghi, mở lòng đón nhận sự thay đổi và nắm bắt thời cơ để vươn xa.",
                    tuoi);
            case 6 -> String.format(
                    "Năm nay là Năm cá nhân số 6 – Năm của tình yêu thương, trách nhiệm gia đình và sự bình yên nội tâm. Trọng tâm cuộc sống của bạn hướng nhiều về những người thân yêu và làm đẹp không gian sống.\n\n" +
                    "Đây là năm để bạn chữa lành, thắt chặt các mối gắn kết ruột thịt và tìm lại sự cân bằng trong tâm hồn.",
                    tuoi);
            case 7 -> String.format(
                    "Năm nay là Năm cá nhân số 7 – Năm của sự tĩnh lặng, tự vấn và chiêm nghiệm sâu sắc. Một năm lý tưởng để học tập nâng cao, thanh lọc tâm trí và khám phá các tầng sâu nhận thức.\n\n" +
                    "Hãy dành cho mình những khoảng lặng cần thiết để nâng tầm nội lực trước khi bước vào giai đoạn gặt hái lớn.",
                    tuoi);
            case 8 -> String.format(
                    "Năm nay là Năm cá nhân số 8 – Năm của thành quả, khẳng định vị thế và thu hoạch tài chính! Những nỗ lực bền bỉ của các năm trước bắt đầu đơm hoa kết trái ngọt ngào.\n\n" +
                    "Hãy hành động quyết đoán, công bằng và tự tin nắm giữ vai trò người dẫn dắt.",
                    tuoi);
            case 9 -> String.format(
                    "Năm nay là Năm cá nhân số 9 – Năm của sự khép lại trọn vẹn một chu kỳ để chuẩn bị cho bình minh mới. Hãy dũng cảm buông bỏ những điều không còn phù hợp để giải phóng không gian cho tương lai.\n\n" +
                    "Lòng vị tha và sự sẻ chia trong năm nay sẽ mở ra cho bạn những phúc lành to lớn trong chu kỳ sắp tới.",
                    tuoi);
            default -> String.format(
                    "Năm nay là thời điểm bản lề để bạn tái định vị bản thân và tích lũy những giá trị bền vững cho tương lai.",
                    tuoi);
        };
    }

    private String getYearOpportunitiesPortrait(int namCaNhan) {
        return switch (namCaNhan) {
            case 1, 3, 5, 8 -> "Thời cơ vàng để bạn chủ động ra mắt dự án mới, tìm kiếm đối tác chiến lược hoặc mở rộng quy mô công việc. Đừng ngần ngại đề xuất những ý tưởng đột phá và nắm bắt quyền chủ động.";
            case 2, 4, 6, 7 -> "Đòn bẩy tốt nhất năm nay nằm ở việc hoàn thiện chiều sâu chuyên môn, nâng cấp các kỹ năng mềm và xây dựng mạng lưới quan hệ chất lượng cao, bền chặt.";
            default -> "Cơ hội đến từ sự chủ động học hỏi và thích ứng linh hoạt trước những thay đổi của môi trường xung quanh.";
        };
    }

    private String getYearChallengesPortrait(int namCaNhan) {
        return switch (namCaNhan) {
            case 1, 5 -> "Tránh sự nóng vội, bốc đồng hoặc đưa ra các quyết định liều lĩnh khi chưa đánh giá thấu đáo các rủi ro thực tế.";
            case 2, 6 -> "Cần tránh sự cả nể, lo lắng thái quá hoặc để cảm xúc tiêu cực của người khác làm hao tổn năng lượng của mình.";
            case 4, 7 -> "Tránh suy nghĩ bi quan, khép kín hoặc tự tạo áp lực cầu toàn quá mức khiến bản thân rơi vào trạng thái quá tải.";
            case 8 -> "Cảnh giác với cái tôi quá lớn hoặc sự phung phí tài chính thiếu kế hoạch kiểm soát chặt chẽ.";
            case 9 -> "Đừng chần chừ buông bỏ những điều đã qua; sự níu kéo quá khứ sẽ chỉ làm bạn lãng phí thời gian quý báu.";
            default -> "Hãy duy trì sự tỉnh táo và điềm tĩnh trước mọi biến động ngoại cảnh.";
        };
    }

    private String buildAnonymousFingerprint(HoSoSo hs) {
        String raw = String.join("|",
                hs.getQh().getConSoChuDao().getDisplayValue(),
                hs.getQh().getConSoNgaySinh().getDisplayValue(),
                hs.getQh().getConSoLinhHon().getDisplayValue(),
                hs.getQh().getConSoBieuDat().getDisplayValue(),
                hs.getQh().getConSoTenRieng().getDisplayValue(),
                hs.getQh().getNamCaNhan().getDisplayValue(),
                hs.getDecoz().getSoDuongDoi().getDisplayValue(),
                hs.getDecoz().getSoBieuDat().getDisplayValue(),
                hs.getDecoz().getKhatVongTamHon().getDisplayValue(),
                hs.getDecoz().getNhanCach().getDisplayValue(),
                hs.getJohari().getSoTamHon().getDisplayValue(),
                hs.getJohari().getSoDinhMenh().getDisplayValue(),
                hs.getJohari().getSoNam().getDisplayValue(),
                hs.getSepharial().getSoChiaKhoa().getDisplayValue(),
                hs.getSepharial().getMagicSquare().getDominantPlane(),
                String.valueOf(hs.getTuoiHienTai()),
                String.valueOf(hs.getNamHienTai()),
                String.valueOf(hs.getThangHienTai())
        );
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }
}
