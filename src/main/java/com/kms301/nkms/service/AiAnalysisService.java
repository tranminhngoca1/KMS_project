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
import com.kms301.nkms.model.DonViTriThuc;
import com.kms301.nkms.model.dto.AiAnalysisDto;
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
 * AI Analysis Engine (Tầng 4 trong kiến trúc NKMS):
 * Đọc Hồ sơ thần số học tổng hợp -> Truy xuất tri thức từ Kho -> Đối chiếu 3 trường phái -> Diễn giải cá nhân hoá
 * bằng mô hình Gemini 3.5 Flash với văn phong ấm áp, tâm lý, sâu sắc, gần gũi.
 */
@Service
public class AiAnalysisService {

    // Fallback key an toàn cho môi trường cloud (Render, Railway...) nếu biến môi trường chưa kịp gán
    private static final String DEFAULT_ENCODED_KEY = "QVEuQWI4Uk42SkxQRTBxMkFSYk1WOEtRc2JZdzRSM3UtZUFrTDd2UHFnUU5weS1NMl9tQ3c=";

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Cache ẩn danh theo SHA-256 của tổ hợp chỉ số (tối đa 200 hồ sơ trong RAM, không chứa thông tin cá nhân)
    private final Map<String, AiAnalysisDto> anonymousCache = new ConcurrentHashMap<>();

    public AiAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
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
            } catch (Exception ignored) {
                // Tự động chuyển sang bộ tổng hợp dự phòng nếu mất kết nối mạng hoặc lỗi
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
        generationConfig.put("temperature", 0.65); // Tăng tính sáng tạo, truyền cảm và tự nhiên
        generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        requestBody.put("generationConfig", generationConfig);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        String cleanModel = modelName.startsWith("models/") ? modelName.substring("models/".length()) : modelName;
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + cleanModel + ":generateContent?key=" + keyToUse.trim();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }

        String rawText = parts.get(0).path("text").asText("");
        if (rawText.isBlank()) {
            return null;
        }

        JsonNode parsed = objectMapper.readTree(rawText);
        AiAnalysisDto dto = new AiAnalysisDto();
        dto.setSuccess(true);
        dto.setAiGenerated(true);
        dto.setModelName("Hệ Thống NKMS Đa Trường Phái");
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
        return dto;
    }

    private String buildPrompt(KhamPhaResult result) {
        HoSoSo hs = result.getHoSoSo();
        QhCalculator.QhProfile qh = hs.getQh();
        DecozCalculator.DecozProfile decoz = hs.getDecoz();
        JohariCalculator.JohariProfile johari = hs.getJohari();
        SepharialCalculator.SepharialProfile seph = hs.getSepharial();
        BirthChart bc = qh.getBirthChart();

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
        sb.append("Bạn là một Chuyên gia Khai vấn Tâm lý & Thần số học Đa trường phái (Master Life Coach & Numerology Counselor). ");
        sb.append("Nhiệm vụ của bạn là đọc Hồ sơ thần số học tổng hợp dưới đây và viết một bài luận giải cá nhân hoá THỰC SỰ SÂU SẮC, TRUYỀN CẢM, ẤM ÁP và GẦN GŨI (như một cuộc trò chuyện chân thành giữa chuyên gia tư vấn cuộc đời và người tìm kiếm định hướng).\n\n");

        sb.append("QUY TẮC BẮT BUỘC VỀ VĂN PHONG:\n");
        sb.append("1. TUYỆT ĐỐI KHÔNG dùng văn phong máy móc, toán học, ghép số cộc lốc (Tránh xa kiểu câu: 'Bản ngã của bạn là sự giao thoa năng lượng số A và số B...'). Người dùng bình thường đọc vào sẽ thấy vô nghĩa và máy móc.\n");
        sb.append("2. HÃY DỊCH NGHĨA CÁC CON SỐ THÀNH CÁ TÍNH ĐỜI THỰC: Hãy nói về cách họ suy nghĩ, tính cách, cảm xúc nội tâm, cách đối nhân xử thế, điểm mạnh thiên bẩm và những nỗi sợ thầm kín.\n");
        sb.append("3. KẾT NỐI TƯƠNG TÁC GIỮA CÁC CON SỐ: Ví dụ người có Con số chủ đạo là số X nhưng số Linh hồn là Y, số Tâm hồn Vệ Đà là Z thì họ thường trải qua sự giằng xé nội tâm như thế nào giữa vẻ ngoài và bên trong.\n");
        sb.append("4. NỘI DUNG PHẢI ĐẦY ĐỦ, CHI TIẾT (mỗi mục viết từ 2 - 4 đoạn văn sâu sắc, mạch lạc, có ví dụ cụ thể, tuyệt đối không viết ngắn cộc lốc).\n\n");

        sb.append("=== THÔNG TIN HỒ SƠ THẦN SỐ HỌC CỦA NGƯỜI NÀY ===\n");
        sb.append("- Tuổi hiện tại: ").append(hs.getTuoiHienTai()).append(" tuổi (Năm xét: ").append(hs.getNamHienTai()).append(")\n");
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
        sb.append("  + Tương quan Tâm hồn & Định mệnh: ").append(johari.getQuanHeTamHonDinhMenh()).append("\n");
        sb.append("  + Số Năm Vệ Đà: ").append(johari.getSoNam().getDisplayValue()).append("\n");

        sb.append("- [Kabbalah / Chaldean - Sepharial]:\n");
        sb.append("  + Số Chìa khoá: ").append(seph.getSoChiaKhoa().getDisplayValue())
                .append(" — Hành tinh: ").append(seph.getHanhTinhChinh()).append("\n");
        sb.append("  + Ma phương 3 tầng: Tầng ưu thế là ").append(seph.getMagicSquare().getDominantPlane()).append("\n\n");

        sb.append("=== YÊU CẦU ĐỊNH DẠNG JSON TRẢ VỀ ===\n");
        sb.append("Trả về ĐÚNG 1 JSON object (không bọc markdown, không dùng backticks ```json). Các đoạn văn phải viết dài, giàu cảm xúc và tâm lý:\n");
        sb.append("{\n");
        sb.append("  \"tongQuanHoSo\": \"Đoạn văn mở đầu khoảng 100-150 từ, đúc kết bức tranh tổng thể về năng lượng và khí chất của người này một cách đầy cuốn hút...\",\n");
        sb.append("  \"cauHoi\": {\n");
        sb.append("    \"AI\": {\n");
        sb.append("      \"phanTichTongHop\": \"Đoạn tóm tắt cốt lõi trả lời 'Tôi là ai?' dài 100 từ...\",\n");
        sb.append("      \"chanDung\": \"Chân dung con người thật: Phân tích sâu về tính cách, bản ngã sâu kín, cách họ tư duy và tương tác với các mối quan hệ (dài 150-200 từ)...\",\n");
        sb.append("      \"theManh\": \"Năng lực bẩm sinh & thế mạnh vượt trội: Những tài năng thiên bẩm, trực giác, phong cách làm việc độc đáo (dài 120-150 từ)...\",\n");
        sb.append("      \"vungToi\": \"Điểm mù & mâu thuẫn nội tâm: Những góc khuất cảm xúc, giằng xé tâm lý bên trong mà người này hay gặp phải (dài 120-150 từ)...\",\n");
        sb.append("      \"loiKhuyen\": \"Định hướng phát triển & môi trường lý tưởng: Lời khuyên nghề nghiệp và môi trường sống giúp họ thăng hoa (dài 120-150 từ)...\",\n");
        sb.append("      \"diemDongThuanAI\": \"Điểm chung mà cả Pythagoras và Vệ Đà cùng nhận định về con người này...\",\n");
        sb.append("      \"diemKhacBietAI\": \"Góc nhìn bổ khuyết giữa 3 trường phái trên bản ngã này...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Tóm lược góc nhìn phương Tây...\",\n");
        sb.append("      \"gocNhinVeda\": \"Tóm lược góc nhìn Vệ Đà...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Tóm lược góc nhìn Kabbalah...\"\n");
        sb.append("    },\n");
        sb.append("    \"GAP_GI\": {\n");
        sb.append("      \"phanTichTongHop\": \"Đoạn tóm tắt cốt lõi trả lời 'Tôi đang gặp gì?' dài 100 từ...\",\n");
        sb.append("      \"chanDung\": \"Bối cảnh năng lượng năm nay: Những biến động tâm lý, cảm xúc và xu hướng thời vận trong năm hiện tại (dài 150-200 từ)...\",\n");
        sb.append("      \"theManh\": \"Cơ hội và đòn bẩy: Những lĩnh vực thuận lợi, thời cơ mở rộng trong chu kỳ năm nay (dài 120-150 từ)...\",\n");
        sb.append("      \"vungToi\": \"Thử thách & cạm bẫy cần tránh: Những rủi ro về tinh thần, công việc hoặc mối quan hệ năm nay (dài 120-150 từ)...\",\n");
        sb.append("      \"loiKhuyen\": \"Bài học độ tuổi & lời khuyên hành động: Điều cuộc đời muốn bạn rèn luyện ở giai đoạn này (dài 120-150 từ)...\",\n");
        sb.append("      \"diemDongThuanAI\": \"Điểm tương đồng về chu kỳ thời vận giữa các trường phái...\",\n");
        sb.append("      \"diemKhacBietAI\": \"Khác biệt về nhịp chu kỳ thời vận giữa Pythagoras và Vệ Đà...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Tóm lược Năm cá nhân & Kim tự tháp Pythagoras...\",\n");
        sb.append("      \"gocNhinVeda\": \"Tóm lược Năm Vệ Đà...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Tóm lược chu kỳ Kabbalah...\"\n");
        sb.append("    },\n");
        sb.append("    \"DI_THE_NAO\": {\n");
        sb.append("      \"phanTichTongHop\": \"Đoạn tóm tắt cốt lõi trả lời 'Tôi nên đi thế nào?' dài 100 từ...\",\n");
        sb.append("      \"chanDung\": \"Kim chỉ nam sống: Thái độ sống và tư duy cốt lõi giúp bạn tìm thấy bình an và thành tựu (dài 150-200 từ)...\",\n");
        sb.append("      \"theManh\": \"Hành động ưu tiên: Những thói quen, kỹ năng cụ thể bạn cần bắt tay vào làm ngay (dài 120-150 từ)...\",\n");
        sb.append("      \"vungToi\": \"Thói quen cần từ bỏ: Những rào cản tâm lý hoặc hành vi tự huỷ hoại cần loại trừ (dài 120-150 từ)...\",\n");
        sb.append("      \"loiKhuyen\": \"Chiến lược dài hạn: Lời khuyên phát triển trọn vẹn tiềm năng cuộc đời (dài 120-150 từ)...\",\n");
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
        DecozCalculator.DecozProfile decoz = hs.getDecoz();
        JohariCalculator.JohariProfile johari = hs.getJohari();
        SepharialCalculator.SepharialProfile seph = hs.getSepharial();

        int chuDao = qh.getConSoChuDao().getNumericValue();
        int ngaySinhNum = qh.getConSoNgaySinh().getNumericValue();
        int namCaNhanNum = qh.getNamCaNhan().getNumericValue();

        AiAnalysisDto dto = new AiAnalysisDto();
        dto.setSuccess(true);
        dto.setAiGenerated(false);
        dto.setModelName("Bản Luận Giải Cá Nhân Hoá NKMS");

        dto.setTongQuanHoSo(String.format(
                "Bạn sở hữu một tổ hợp năng lượng vô cùng đặc biệt: Được dẫn dắt bởi Con số chủ đạo %s của tư duy và số Ngày sinh %s, kết hợp cùng năng lượng nội tâm sâu thẳm của hành tinh %s (Số Tâm hồn %s) và %s (Số Định mệnh %s). Bạn là người vừa có khát vọng cống hiến lớn lao, vừa có đời sống cảm xúc phong phú nhưng đôi lúc phải đối mặt với những giằng xé nội tâm sâu sắc.",
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
                "Bạn là sự hoà quyện giữa khí chất Con số chủ đạo %s và phong cách ngày sinh %s: Một người thông minh, nhạy bén, giàu lòng trắc ẩn nhưng luôn đòi hỏi cao ở chính mình và cuộc sống.",
                qh.getConSoChuDao().getDisplayValue(), qh.getConSoNgaySinh().getDisplayValue()
        ));
        qaAi.setChanDung(getPersonalityPortrait(chuDao, ngaySinhNum, johari.getHanhTinhTamHon()));
        qaAi.setTheManh(getStrengthsPortrait(chuDao, qh.getConSoLinhHon().getNumericValue()));
        qaAi.setVungToi(getBlindspotsPortrait(chuDao, johari.getSoTamHon().getNumericValue()));
        qaAi.setLoiKhuyen(getCareerAndLifeAdvice(chuDao));
        qaAi.setDiemDongThuanAI("Các trường phái cùng chỉ ra rằng bản ngã cốt lõi của bạn bắt nguồn từ sự kết nối giữa tư duy trí tuệ bẩm sinh và nhu cầu thấu cảm sâu sắc.");
        qaAi.setDiemKhacBietAI("Pythagoras giải mã cấu trúc tính cách qua họ tên và ngày sinh; Vệ Đà đào sâu vào năng lượng hành tinh tâm thức; Kabbalah tập trung vào quy luật cân bằng ma phương.");
        map.put("AI", qaAi);

        // CÂU HỎI 2: TÔI ĐANG GẶP GÌ?
        AiAnalysisDto.QuestionAiAnalysis qaGap = new AiAnalysisDto.QuestionAiAnalysis();
        qaGap.setPhanTichTongHop(String.format(
                "Năm nay (2026, tuổi %d), bạn đang bước vào Năm cá nhân số %s theo Pythagoras và chu kỳ số %s của Vệ Đà: Một năm mang tính bước ngoặt về nhận thức và tái định hình các mục tiêu cuộc đời.",
                hs.getTuoiHienTai(), qh.getNamCaNhan().getDisplayValue(), johari.getSoNam().getDisplayValue()
        ));
        qaGap.setChanDung(getYearEnergyPortrait(namCaNhanNum, hs.getTuoiHienTai()));
        qaGap.setTheManh(getYearOpportunitiesPortrait(namCaNhanNum));
        qaGap.setVungToi(getYearChallengesPortrait(namCaNhanNum));
        qaGap.setLoiKhuyen(String.format("Tại độ tuổi %d, giai đoạn %s (Đỉnh cao mang năng lượng số %s) đòi hỏi bạn cần kiên nhẫn, trau dồi nội lực thay vì hành động nóng vội.",
                hs.getTuoiHienTai(), qh.getKimTuThap().getCurrentPhaseName(), qh.getKimTuThap().getCurrentPeak().getDisplayValue()));
        qaGap.setDiemDongThuanAI("Cả Pythagoras và Vệ Đà đều nhận định đây là thời điểm bản lề trong chu kỳ 9 năm của bạn.");
        qaGap.setDiemKhacBietAI("Pythagoras tính theo chu kỳ năm tự nhiên; Vệ Đà tính thêm ảnh hưởng của ngày trong tuần sinh nhật.");
        map.put("GAP_GI", qaGap);

        // CÂU HỎI 3: TÔI NÊN ĐI THẾ NÀO?
        AiAnalysisDto.QuestionAiAnalysis qaDi = new AiAnalysisDto.QuestionAiAnalysis();
        qaDi.setPhanTichTongHop(String.format(
                "Kim chỉ nam tối thượng dành cho bạn là học cách cân bằng giữa nhiệt huyết bên ngoài và sự tĩnh lặng bên trong, thuận theo nhịp sóng Năm cá nhân %s để gặt hái thành tựu bền vững.",
                qh.getNamCaNhan().getDisplayValue()
        ));
        qaDi.setChanDung("Thái độ sống quan trọng nhất: Hãy học cách lắng nghe trực giác, chấp nhận những điểm chưa hoàn hảo của bản thân và kiên định với giá trị cốt lõi.");
        qaDi.setTheManh("Hành động cụ thể: Thiết lập kỷ luật hàng ngày, tập trung hoàn thành dứt điểm từng mục tiêu và chủ động mở rộng các mối quan hệ chân thành.");
        qaDi.setVungToi("Rào cản cần vượt qua: Tránh thói quen trì hoãn do cầu toàn quá mức, bớt lo âu về tương lai và đừng ngại từ chối những điều không phù hợp với định hướng dài hạn.");
        qaDi.setLoiKhuyen("Chiến lược dài hạn: Kết hợp tư duy chiến lược của Pythagoras với sự bình thản tâm thức của Vệ Đà để làm chủ cuộc đời mình.");
        qaDi.setDiemDongThuanAI("Cả 3 trường phái đều khuyên bạn hướng vào việc tự tu dưỡng, rèn luyện thói quen tự giác và sống có mục đích rõ ràng.");
        qaDi.setDiemKhacBietAI("Pythagoras chú trọng rèn luyện bù mũi tên trống; Vệ Đà khuyên chuyển hoá năng lượng hành tinh; Kabbalah hướng tới cân bằng 3 tầng thể xác - trí tuệ - tinh thần.");
        map.put("DI_THE_NAO", qaDi);

        dto.setCauHoi(map);
        return dto;
    }

    private String getPersonalityPortrait(int chuDao, int ngaySinh, String hanhTinh) {
        return switch (chuDao) {
            case 3 -> "Bạn là người sở hữu tư duy cực kỳ nhạy bén, hoạt ngôn và có khiếu hài hước tự nhiên. Ở bạn toát ra một nguồn năng lượng sáng tạo, ấm áp và thu hút người khác trong giao tiếp. Bạn thích những điều mới mẻ, ghét sự rập khuôn tẻ nhạt. Tuy nhiên, đằng sau vẻ ngoài vui vẻ, hóm hỉnh là một tâm hồn rất tinh tế, dễ bị tổn thương bởi những lời phê bình thiếu xây dựng.";
            case 4 -> "Bạn là mẫu người thực tế, cẩn trọng, có tinh thần trách nhiệm và tính kỷ luật rất cao. Bạn luôn coi trọng sự an toàn, chuẩn mực và làm việc có quy trình rõ ràng. Người khác luôn cảm thấy an tâm và tin tưởng khi đồng hành cùng bạn bởi bạn luôn giữ chữ tín và làm đến cùng những gì đã hứa.";
            case 5 -> "Bạn là cánh chim tự do của cuộc đời, yêu thích khám phá, trải nghiệm và luôn khao khát thoát khỏi những ràng buộc cứng nhắc. Bạn thích nghi rất nhanh với sự thay đổi, có tài ngoại giao và năng lượng dồi dào. Bạn sống hết mình cho giây phút hiện tại và ghét cảm giác bị kiểm soát hay nhàm chán.";
            case 6 -> "Bạn mang trái tim ấm áp của một người chăm sóc, giàu lòng trắc ẩn và luôn hướng về gia đình cũng như những người thân yêu. Bạn có gu thẩm mỹ tinh tế, yêu cái đẹp và luôn nỗ lực tạo dựng sự hoà thuận xung quanh. Tuy nhiên, bạn thường có xu hướng ôm đồm lo lắng thay cho người khác khiến bản thân dễ mệt mỏi.";
            case 7 -> "Bạn là một người thích chiêm nghiệm, sở hữu trí tuệ triết học sâu sắc và trực giác vô cùng mạnh mẽ. Bạn không dễ tin vào những điều bề nổi mà luôn muốn tự mình tìm hiểu, trải nghiệm và chứng minh chân lý. Bạn cần những khoảng lặng riêng tư để tái tạo năng lượng và thường có những bài học cuộc đời thông qua trải nghiệm cá nhân sâu sắc.";
            case 8 -> "Bạn sở hữu tố chất lãnh đạo bẩm sinh, tư duy tài chính sắc bén và khát vọng thành công vượt bậc. Bạn hành động dứt khoát, tự tin và có năng lực điều hành xuất sắc. Bạn coi trọng sự độc lập, công bằng và luôn mong muốn xây dựng một nền tảng vững chắc cho tương lai.";
            case 9 -> "Bạn là người giàu lý tưởng nhân đạo, luôn hướng về cộng đồng và mong muốn đem lại giá trị tốt đẹp cho xã hội. Bạn bao dung, chân thành và có sức hút tự nhiên từ sự vị tha. Tuy nhiên, đôi lúc bạn quá đặt niềm tin vào người khác hoặc ôm đồm những trách nhiệm vượt quá khả năng.";
            case 10, 1 -> "Bạn mang tố chất của một người tiên phong: Độc lập, quyết đoán, tự tin và có tinh thần tự chủ tuyệt vời. Bạn thích đứng mũi chịu sào, dám nghĩ dám làm và không ngại đối mặt với thử thách mới. Bạn có khả năng truyền cảm hứng dẫn dắt đội ngũ rất mạnh mẽ.";
            case 11 -> "Bạn là một linh hồn có trực giác tâm linh cực kỳ nhạy bén, sống chân thành và luôn hướng tới sự hòa hợp sâu sắc. Bạn có khả năng thấu hiểu cảm xúc của người khác mà không cần họ phải nói ra. Tiềm năng tinh thần của bạn rất lớn nhưng đòi hỏi bạn phải học cách làm chủ cảm xúc của chính mình.";
            case 22 -> "Bạn mang năng lượng của 'Người kiến tạo bậc thầy': Vừa có tầm nhìn vĩ mô xa rộng, vừa có năng lực thực thi thực tế siêu việt. Bạn có tiềm năng tạo nên những công trình hoặc dự án để lại dấu ấn lớn, miễn là bạn giữ vững niềm tin và không nản lòng trước khó khăn bước đầu.";
            default -> "Bạn là người sở hữu nội lực phong phú, tính cách đa chiều và có khả năng dung hoà tuyệt vời giữa lý trí và cảm xúc.";
        };
    }

    private String getStrengthsPortrait(int chuDao, int linhHon) {
        return switch (chuDao) {
            case 3 -> "Thế mạnh vượt trội của bạn là khả năng biểu đạt ngôn từ xuất sắc, tư duy sáng tạo linh hoạt và tài biến hoá các ý tưởng trừu tượng thành câu chuyện sinh động. Bạn dễ dàng truyền cảm hứng, thuyết phục và tạo bầu không khí tích cực cho tập thể.";
            case 4 -> "Năng lực bẩm sinh của bạn là khả năng phân tích tỉ mỉ, tổ chức công việc khoa học và sự kiên trì bền bỉ đến đáng nể. Bạn rất giỏi trong việc xây dựng hệ thống, quản lý vận hành và xử lý các vấn đề đòi hỏi tính chi tiết cao.";
            case 5 -> "Điểm mạnh nổi bật là sự nhanh nhạy trước thời cuộc, tài xoay xở linh hoạt trong tình huống bất ngờ và năng lực kết nối đa dạng các mối quan hệ xã hội. Bạn luôn là người tìm ra hướng đi mới khi người khác bế tắc.";
            case 6 -> "Bạn sở hữu năng khiếu thẩm mỹ tự nhiên, khả năng chữa lành cảm xúc và tài năng xây dựng văn hoá gắn kết tập thể. Bạn rất giỏi trong việc lắng nghe, thấu hiểu và xoa dịu những căng thẳng xung quanh.";
            case 7 -> "Thế mạnh bẩm sinh là năng lực nghiên cứu chuyên sâu, khả năng quan sát thấu suốt bản chất sự việc và trực giác phán đoán cực kỳ chuẩn xác. Bạn có năng khiếu tự học và trở thành chuyên gia trong lĩnh vực mình đam mê.";
            case 8 -> "Năng lực lãnh đạo quyết đoán, trực giác kinh doanh sắc sảo và khả năng quản lý tài chính - nguồn lực hiệu quả là những điểm mạnh giúp bạn luôn nổi bật ở các vị trí điều hành hoặc kinh doanh độc lập.";
            case 9 -> "Sức mạnh lớn nhất của bạn là tầm nhìn nhân văn rộng mở, khả năng quy tụ lòng người và năng khiếu giáo dục, tư vấn hoặc hoạt động xã hội. Lời nói của bạn luôn mang trọng lượng và sự tin cậy.";
            default -> "Bạn có khả năng tự nhận thức cao, tư duy linh hoạt và năng lực thích ứng tuyệt vời trong môi trường làm việc đa văn hoá.";
        };
    }

    private String getBlindspotsPortrait(int chuDao, int tamHon) {
        return switch (chuDao) {
            case 3 -> "Cạm bẫy lớn nhất là sự mất kiên nhẫn khi làm những việc lặp đi lặp lại và dễ để cảm xúc chi phối quyết định. Đôi khi bạn nói nhiều hơn lắng nghe hoặc phân tán năng lượng vào quá nhiều dự án cùng lúc mà không hoàn thành trọn vẹn.";
            case 4 -> "Điểm mù là sự bảo thủ, ngại đổi mới và quá cầu toàn vào chi tiết khiến bản thân bị căng thẳng. Bạn dễ bỏ lỡ các cơ hội mang tính đột phá chỉ vì chúng chưa có lộ trình an toàn 100%.";
            case 5 -> "Mâu thuẫn nội tâm nằm ở sự thèm muốn tự do quá mức dẫn đến dễ cả thèm chóng chán, thiếu sự bền chí dài hạn. Bạn dễ bị cám dỗ bởi những trải nghiệm mới lạ mà xao nhãng mục tiêu trọng tâm.";
            case 6 -> "Góc khuất là sự bao bọc thái quá và hay lo lắng những chuyện chưa xảy ra. Bạn thường đặt kỳ vọng quá cao vào người khác để rồi dễ thất vọng hoặc tự biến mình thành 'nạn nhân' trong các mối quan hệ.";
            case 7 -> "Điểm mù là xu hướng tự cô lập bản thân, đa nghi và khó bộc bạch cảm xúc thật ra bên ngoài. Sự cầu toàn tri thức đôi khi khiến bạn trở nên xa cách hoặc phán xét người khác theo tiêu chuẩn khắt khe của mình.";
            case 8 -> "Cạm bẫy là tính độc đoán, kiểm soát và dễ bị cuốn vào vòng xoáy công việc/vật chất mà lãng quên sức khoẻ và cảm xúc của những người thân cận.";
            default -> "Cần cảnh giác với sự giằng xé giữa khát vọng cá nhân và áp lực từ kỳ vọng của môi trường xung quanh.";
        };
    }

    private String getCareerAndLifeAdvice(int chuDao) {
        return switch (chuDao) {
            case 3 -> "Môi trường phát triển lý tưởng: Các lĩnh vực truyền thông, marketing, sáng tạo nội dung, nghệ thuật, giảng dạy, tư vấn hoặc quan hệ công chúng. Hãy rèn luyện thói quen viết nhật ký mục tiêu và kiên trì theo đuổi một dự án đến cùng trước khi bắt đầu cái mới.";
            case 4 -> "Môi trường phù hợp: Kỹ thuật, công nghệ thông tin, quản trị vận hành, tài chính kế toán, luật pháp hoặc kiến trúc. Lời khuyên: Hãy học cách mở lòng với những phương pháp mới và cho phép bản thân nghỉ ngơi nhiều hơn.";
            case 5 -> "Môi trường lý tưởng: Kinh doanh tự do, du lịch, xuất nhập khẩu, báo chí, tổ chức sự kiện hoặc các công việc thường xuyên di chuyển. Lời khuyên: Học cách đặt ra kỷ luật thép cho bản thân để tự do của bạn trở nên có giá trị bền vững.";
            case 6 -> "Môi trường phù hợp: Giáo dục, y tế, tâm lý, thiết kế thời trang/nội thất, dịch vụ khách hàng hoặc quản trị nhân sự. Lời khuyên: Hãy học cách yêu thương chính mình trước tiên và học cách nói 'không' với những yêu cầu vô lý.";
            case 7 -> "Môi trường lý tưởng: Nghiên cứu khoa học, phân tích dữ liệu, giảng dạy đại học, triết học, tâm lý học hoặc các ngành công nghệ cao. Lời khuyên: Hãy thực hành chia sẻ những tri thức bạn tích lũy được ra cộng đồng thay vì giữ kín cho riêng mình.";
            case 8 -> "Môi trường phù hợp: Quản trị doanh nghiệp, đầu tư tài chính, bất động sản, luật kinh tế hoặc các vị trí quản lý cấp cao. Lời khuyên: Hãy cân bằng giữa mục tiêu tài chính với đời sống tinh thần và lắng nghe ý kiến cộng sự.";
            default -> "Hãy kiên định với thế mạnh cốt lõi của bạn và tìm kiếm một môi trường cho phép bạn tự chủ phát huy năng lực.";
        };
    }

    private String getYearEnergyPortrait(int namCaNhan, int tuoi) {
        return switch (namCaNhan) {
            case 1 -> "Năm nay là Năm cá nhân số 1 — Năm của sự khởi đầu mới! Năng lượng thôi thúc bạn bắt tay vào các dự án mới, tự tin bước ra khỏi vùng an toàn và khẳng định bản thân.";
            case 2 -> "Năm nay là Năm cá nhân số 2 — Năm của sự kết nối và kiên nhẫn. Nhịp sống chậm lại để bạn lắng nghe trực giác, hàn gắn các mối quan hệ và chuẩn bị nội lực vững vàng.";
            case 3 -> "Năm nay là Năm cá nhân số 3 — Năm của sự sáng tạo và mở rộng giao tiếp! Bạn sẽ có nhiều cơ hội để học hỏi kỹ năng mới, mở rộng vòng kết nối và lan tỏa năng lượng tích cực.";
            case 4 -> "Năm nay là Năm cá nhân số 4 — Năm của củng cố nền tảng và kỷ luật thực tế. Đây là thời điểm bạn cần tập trung dọn dẹp các tồn đọng, chăm sóc sức khoẻ và xây dựng nền tảng vững chắc.";
            case 5 -> "Năm nay là Năm cá nhân số 5 — Năm của những chuyển dịch và cơ hội bứt phá! Nhiều cánh cửa mới sẽ mở ra, đòi hỏi bạn phải dũng cảm thích nghi và dám thay đổi.";
            case 6 -> "Năm nay là Năm cá nhân số 6 — Năm của gia đình, tình yêu và trách nhiệm. Mối quan tâm của bạn hướng nhiều về những người thân yêu và làm đẹp không gian sống.";
            case 7 -> "Năm nay là Năm cá nhân số 7 — Năm của sự tĩnh lặng và chiêm nghiệm nội tâm. Một năm lý tưởng để học hỏi chuyên sâu, thanh lọc tâm trí và nâng cấp nhận thức bản thân.";
            case 8 -> "Năm nay là Năm cá nhân số 8 — Năm của gặt hái thành quả và khẳng định vị thế tài chính! Những nỗ lực của các năm trước bắt đầu đơm hoa kết trái nếu bạn hành động công bằng và quyết đoán.";
            case 9 -> "Năm nay là Năm cá nhân số 9 — Năm của sự khép lại một chu kỳ để chuẩn bị cho chu kỳ mới. Hãy mạnh dạn buông bỏ những điều không còn phù hợp để đón nhận khởi đầu mới rực rỡ hơn.";
            default -> "Năm nay là thời điểm quan trọng để bạn tái định vị bản thân và tích luỹ thêm các giá trị bền vững.";
        };
    }

    private String getYearOpportunitiesPortrait(int namCaNhan) {
        return switch (namCaNhan) {
            case 1, 3, 5, 8 -> "Thời cơ vàng để bạn chủ động hành động, ra mắt sản phẩm mới, tìm kiếm đối tác chiến lược hoặc mở rộng quy mô công việc. Đừng ngần ngại đề xuất những ý tưởng táo bạo.";
            case 2, 4, 6, 7 -> "Đòn bẩy tốt nhất năm nay nằm ở việc hoàn thiện chiều sâu chuyên môn, nâng cấp các kỹ năng mềm và xây dựng mạng lưới quan hệ bền vững, chất lượng cao.";
            default -> "Cơ hội đến từ sự chủ động học hỏi và thích ứng linh hoạt trước những thay đổi của môi trường.";
        };
    }

    private String getYearChallengesPortrait(int namCaNhan) {
        return switch (namCaNhan) {
            case 1, 5 -> "Tránh sự nóng vội, bốc đồng hoặc đưa ra các quyết định liều lĩnh khi chưa nghiên cứu kỹ rủi ro.";
            case 2, 6 -> "Cần tránh sự cả nể, lo lắng thái quá hoặc để cảm xúc tiêu cực của người khác ảnh hưởng đến tâm trạng của mình.";
            case 4, 7 -> "Tránh suy nghĩ bi quan, khép kín hoặc tự tạo áp lực cầu toàn quá mức khiến bản thân kiệt sức.";
            case 8 -> "Cảnh giác với cái tôi quá lớn hoặc sự phung phí tài chính không có kế hoạch kiểm soát.";
            case 9 -> "Đừng níu kéo những điều đã qua; sự chần chừ buông bỏ sẽ chỉ làm bạn lãng phí thời gian quý báu.";
            default -> "Hãy duy trì sự tỉnh táo và bình tĩnh trước mọi biến động ngoại cảnh.";
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
