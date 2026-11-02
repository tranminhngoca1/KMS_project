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
 * văn phong ấm áp, thấu cảm, gần gũi, súc tích và mạch lạc.
 *
 * Lưu ý quan trọng:
 * - Backend kiểm soát nghiêm ngặt chỉ dựa trên tri thức đã cấp, không tự thêm tri thức ngoài (zodiac, tarot, bói toán).
 * - Luận giải tập trung hoàn toàn vào con người người dùng, KHÔNG chèn tên sách, tác giả hay trường phái vào văn bản.
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
        sb.append("Bạn là Chuyên gia Khai vấn Tâm lý & Cuộc đời (Life Coach). Bạn đang đọc hồ sơ của '").append(originalName).append("' (").append(hs.getTuoiHienTai()).append(" tuổi).\n");
        sb.append("Hãy viết bài luận giải cá nhân hoá ẤM ÁP, GẦN GŨI, THẤU CẢM NHƯNG CÔ ĐỌNG, SÚC TÍCH (tránh viết dài lê thê, mỗi thẻ 1-2 đoạn ngắn gọn khoảng 80 - 110 từ).\n\n");

        sb.append("QUY TẮC BẮT BUỘC:\n");
        sb.append("1. TUYỆT ĐỐI KHÔNG nhắc đến tên trường phái, tên sách hay tác giả trong bài viết (TUYỆT ĐỐI KHÔNG viết: 'theo Pythagoras', 'theo Harish Johari', 'theo Vệ Đà', 'theo Kabbalah', 'theo David Phillips'... Người đọc không quan tâm nguồn sách, họ chỉ muốn nghe về chính con người họ).\n");
        sb.append("2. BACKEND ĐÃ CUNG CẤP ĐỦ DỮ LIỆU: Chỉ dựa vào dữ liệu trong hồ sơ dưới đây để phân tích, TUYỆT ĐỐI KHÔNG tự bịa thêm các tri thức ngoài như 12 cung hoàng đạo, Tarot, bói toán hay tử vi.\n");
        sb.append("3. VĂN PHONG TỰ NHIÊN, GẦN GŨI: Xưng 'tôi' - 'bạn' (hoặc gọi thân mật '").append(tenGoi).append("'). Lời văn chân thành, ấm áp, chạm đến cảm xúc nhưng CÔ ĐỌNG, MẠCH LẠC.\n");
        sb.append("4. ĐỘ DÀI VỪA PHẢI, DỄ ĐỌC: Mỗi mục chỉ viết từ 80 - 110 từ (1 đến 2 đoạn văn ngắn, súc tích, đi thẳng vào trọng tâm, không lan man).\n\n");

        sb.append("=== DỮ LIỆU HỒ SƠ ĐÃ TÍNH TOÁN ===\n");
        sb.append("- Tên: ").append(originalName).append(" (").append(hs.getTuoiHienTai()).append(" tuổi, Năm xét: ").append(hs.getNamHienTai()).append(", Tháng xét: ").append(hs.getThangHienTai()).append(")\n");
        sb.append("- Con số chủ đạo: ").append(qh.getConSoChuDao().getDisplayValue()).append(" (Đường đời: ").append(decoz.getSoDuongDoi().getDisplayValue()).append(")\n");
        sb.append("- Phong cách ngày sinh: ").append(qh.getConSoNgaySinh().getDisplayValue()).append("\n");
        sb.append("- Khát vọng nội tâm: ").append(qh.getConSoLinhHon().getDisplayValue()).append(" (Khát vọng tâm hồn: ").append(decoz.getKhatVongTamHon().getDisplayValue()).append(")\n");
        sb.append("- Phong cách biểu đạt: ").append(qh.getConSoBieuDat().getDisplayValue()).append("\n");
        sb.append("- Mũi tên năng lực: ").append(presentArrows).append(" | Mũi tên trống: ").append(emptyArrows).append("\n");
        sb.append("- Chu kỳ năm hiện tại: Năm cá nhân số ").append(qh.getNamCaNhan().getDisplayValue())
                .append(", Tháng cá nhân: ").append(qh.getThangCaNhan().getDisplayValue())
                .append(", Số năm Vệ Đà: ").append(johari.getSoNam().getDisplayValue()).append("\n");
        sb.append("- Giai đoạn đỉnh cao hiện tại: ").append(qh.getKimTuThap().getCurrentPhaseName())
                .append(" mang số ").append(qh.getKimTuThap().getCurrentPeak().getDisplayValue()).append("\n");
        sb.append("- Năng lượng tâm hồn sâu thẳm: Số ").append(johari.getSoTamHon().getDisplayValue())
                .append(" (Hành tinh: ").append(johari.getHanhTinhTamHon()).append("), Số định mệnh: ")
                .append(johari.getSoDinhMenh().getDisplayValue()).append(" (Hành tinh: ").append(johari.getHanhTinhDinhMenh()).append(")\n");
        sb.append("- Năng lượng Chìa khoá: Số ").append(seph.getSoChiaKhoa().getDisplayValue())
                .append(" (Hành tinh: ").append(seph.getHanhTinhChinh()).append("), Tầng ưu thế: ")
                .append(seph.getMagicSquare().getDominantPlane()).append("\n\n");

        sb.append("=== YÊU CẦU ĐỊNH DẠNG JSON TRẢ VỀ ===\n");
        sb.append("Trả về ĐÚNG 1 JSON object (không bọc markdown, không dùng ```json). Từng mục viết súc tích, độ dài 80-110 từ:\n");
        sb.append("{\n");
        sb.append("  \"tongQuanHoSo\": \"Đoạn văn ngắn 70-90 từ ấm áp, súc tích đúc kết thần thái của ").append(tenGoi).append("...\",\n");
        sb.append("  \"cauHoi\": {\n");
        sb.append("    \"AI\": {\n");
        sb.append("      \"phanTichTongHop\": \"50-70 từ tóm tắt cốt lõi Tôi là ai...\",\n");
        sb.append("      \"chanDung\": \"80-110 từ về bản ngã cốt lõi, sự kết hợp giữa vẻ ngoài và chiều sâu nội tâm...\",\n");
        sb.append("      \"theManh\": \"80-110 từ về thế mạnh thực tế, khả năng hành động và biểu đạt...\",\n");
        sb.append("      \"vungToi\": \"80-110 từ về điểm mù cầu toàn, dễ cô độc, áp lực nội tâm...\",\n");
        sb.append("      \"loiKhuyen\": \"80-110 từ về môi trường phát triển và lời khuyên thiết thực...\"\n");
        sb.append("    },\n");
        sb.append("    \"GAP_GI\": {\n");
        sb.append("      \"phanTichTongHop\": \"50-70 từ tóm tắt thời vận năm nay...\",\n");
        sb.append("      \"chanDung\": \"80-110 từ về năng lượng năm nay và biến chuyển tâm lý...\",\n");
        sb.append("      \"theManh\": \"80-110 từ về cơ hội và đòn bẩy thuận lợi trong năm nay...\",\n");
        sb.append("      \"vungToi\": \"80-110 từ về cạm bẫy tâm lý và điều cần tránh trong năm nay...\",\n");
        sb.append("      \"loiKhuyen\": \"80-110 từ về bài học giai đoạn tuổi ").append(hs.getTuoiHienTai()).append("...\"\n");
        sb.append("    },\n");
        sb.append("    \"DI_THE_NAO\": {\n");
        sb.append("      \"phanTichTongHop\": \"50-70 từ tóm tắt kim chỉ nam...\",\n");
        sb.append("      \"chanDung\": \"80-110 từ về kim chỉ nam giữ tâm thế an nhiên, vững vàng...\",\n");
        sb.append("      \"theManh\": \"80-110 từ về hành động cụ thể, kỷ luật tự thân cần bắt đầu...\",\n");
        sb.append("      \"vungToi\": \"80-110 từ về thói quen cần buông bỏ để nhẹ gánh...\",\n");
        sb.append("      \"loiKhuyen\": \"80-110 từ về chiến lược sống trọn vẹn và an lạc...\"\n");
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
        int namCaNhanNum = qh.getNamCaNhan().getNumericValue();
        String tenGoi = (hs.getParsedName() != null && !hs.getParsedName().getTenGoi().isBlank())
                ? hs.getParsedName().getTenGoi() : "bạn";

        AiAnalysisDto dto = new AiAnalysisDto();
        dto.setSuccess(true);
        dto.setAiGenerated(false);
        dto.setModelName("Hệ Thống KMS Đa Trường Phái");

        dto.setTongQuanHoSo(String.format(
                "Chào %s, ở tuổi %d, tôi thấy một chân dung đầy bản lĩnh: Bạn là người có tư chất tự chủ, nhạy bén trong hành động nhưng luôn nuôi dưỡng một thế giới nội tâm sâu sắc. Bạn sở hữu sự kiên định để hiện thực hóa các mục tiêu, đồng thời có khả năng thấu cảm và lắng nghe người khác. Đây là giai đoạn bạn bắt đầu tích lũy được sự vững vàng và chín muồi trong nhận thức cuộc sống.",
                tenGoi, hs.getTuoiHienTai()
        ));

        Map<String, AiAnalysisDto.QuestionAiAnalysis> map = new HashMap<>();

        // CÂU HỎI 1: TÔI LÀ AI?
        AiAnalysisDto.QuestionAiAnalysis qaAi = new AiAnalysisDto.QuestionAiAnalysis();
        qaAi.setPhanTichTongHop(String.format(
                "Bạn mang tố chất của một người độc lập, kiên định và giàu trực giác. Bạn hành động quyết đoán nhưng luôn đòi hỏi sự chân thành và chiều sâu trong mọi việc.",
                tenGoi
        ));
        qaAi.setChanDung(getPersonalityPortrait(chuDao, tenGoi));
        qaAi.setTheManh(getStrengthsPortrait(chuDao));
        qaAi.setVungToi(getBlindspotsPortrait(chuDao));
        qaAi.setLoiKhuyen(getCareerAndLifeAdvice(chuDao, tenGoi));
        map.put("AI", qaAi);

        // CÂU HỎI 2: TÔI ĐANG GẶP GÌ?
        AiAnalysisDto.QuestionAiAnalysis qaGap = new AiAnalysisDto.QuestionAiAnalysis();
        qaGap.setPhanTichTongHop(String.format(
                "Năm nay (%d, tuổi %d), bạn đang bước vào chu kỳ số %s: Thời điểm quan trọng để bạn điều chỉnh nhịp sống, củng cố nội lực và chuẩn bị cho những bước tiến lớn.",
                hs.getNamHienTai(), hs.getTuoiHienTai(), qh.getNamCaNhan().getDisplayValue()
        ));
        qaGap.setChanDung(getYearEnergyPortrait(namCaNhanNum, hs.getTuoiHienTai(), tenGoi));
        qaGap.setTheManh(getYearOpportunitiesPortrait(namCaNhanNum));
        qaGap.setVungToi(getYearChallengesPortrait(namCaNhanNum));
        qaGap.setLoiKhuyen(String.format("Ở tuổi %d, giai đoạn này nhắc nhở %s cần giữ sự điềm tĩnh, lấy việc rèn luyện sự vững vàng làm gốc rễ thay vì nóng vội tìm kiếm thành quả trước mắt.",
                hs.getTuoiHienTai(), tenGoi));
        map.put("GAP_GI", qaGap);

        // CÂU HỎI 3: TÔI NÊN ĐI THẾ NÀO?
        AiAnalysisDto.QuestionAiAnalysis qaDi = new AiAnalysisDto.QuestionAiAnalysis();
        qaDi.setPhanTichTongHop(String.format(
                "Kim chỉ nam then chốt dành cho %s là học cách cân bằng giữa hành động thực tế và sự bình yên nội tâm, kiên định với những giá trị cốt lõi.",
                tenGoi
        ));
        qaDi.setChanDung(String.format(
                "Thái độ sống quan trọng nhất là học cách tin tưởng vào trực giác và chấp nhận những điều chưa hoàn hảo. Đừng để nỗi sợ thất bại cản bước, nhưng hãy biết dành thời gian tĩnh lặng để tái tạo năng lượng.",
                tenGoi));
        qaDi.setTheManh("Hãy thiết lập kỷ luật cá nhân hàng ngày, tập trung hoàn thành dứt điểm từng mục tiêu trọng tâm và chủ động nuôi dưỡng các mối quan hệ có chiều sâu.");
        qaDi.setVungToi("Hãy buông bỏ thói quen trì hoãn do quá cầu toàn, bớt lo âu về những điều chưa xảy ra và học cách từ chối những việc làm tiêu hao năng lượng.");
        qaDi.setLoiKhuyen("Chiến lược dài hạn: Hãy kết hợp sự quyết đoán trong hành động với sự điềm tĩnh trong tâm thức để làm chủ cuộc đời mình một cách an nhiên.");
        map.put("DI_THE_NAO", qaDi);

        dto.setCauHoi(map);
        return dto;
    }

    private String getPersonalityPortrait(int chuDao, String tenGoi) {
        return switch (chuDao) {
            case 3 -> String.format(
                    "%s là người sở hữu tư duy sáng tạo linh hoạt, hoạt ngôn và có khiếu hài hước tự nhiên. Bạn luôn mang lại bầu không khí vui vẻ và tràn đầy năng lượng cho người xung quanh.\n\n" +
                    "Tuy nhiên, đằng sau vẻ ngoài sôi nổi là một nội tâm tinh tế, nhạy cảm với cảm xúc và luôn khao khát tìm kiếm sự thấu hiểu chân thành.",
                    tenGoi);
            case 4 -> String.format(
                    "%s là mẫu người thực tế, cẩn trọng, có tinh thần trách nhiệm và tính kỷ luật cao. Bạn coi trọng sự chuẩn mực, an toàn và luôn làm việc có quy trình rõ ràng.\n\n" +
                    "Bạn luôn là điểm tựa đáng tin cậy nhất cho gia đình và bạn bè trong những lúc khó khăn nhờ sự kiên định và bền bỉ phi thường.",
                    tenGoi);
            case 5 -> String.format(
                    "%s là người yêu thích tự do, thích khám phá và ghét sự rập khuôn tẻ nhạt. Bạn có khả năng thích nghi rất nhanh với sự thay đổi và sở hữu tài ngoại giao tự nhiên.\n\n" +
                    "Bạn luôn tìm kiếm những trải nghiệm mới để làm phong phú tâm hồn, nhưng đôi lúc cảm thấy khó khăn khi phải gắn bó với những cam kết cố định.",
                    tenGoi);
            case 6 -> String.format(
                    "%s mang trái tim ấm áp, giàu lòng trắc ẩn và luôn hướng về gia đình. Bạn yêu cái đẹp, sự hài hòa và sẵn sàng chăm sóc, nâng đỡ những người xung quanh.\n\n" +
                    "Tuy nhiên, bạn có xu hướng lo lắng thái quá và ôm đồm trách nhiệm, dễ khiến bản thân bị quá tải cảm xúc.",
                    tenGoi);
            case 7 -> String.format(
                    "%s là người thích chiêm nghiệm, sở hữu trí tuệ sâu sắc và trực giác nhạy bén. Bạn không vội tin vào những điều bề nổi mà luôn muốn tự mình tìm hiểu đến tận cùng bản chất sự việc.\n\n" +
                    "Bạn cần những khoảng không gian tĩnh lặng riêng tư để suy ngẫm và tái tạo năng lượng sau những giờ làm việc căng thẳng.",
                    tenGoi);
            case 8 -> String.format(
                    "%s mang tố chất của một người dẫn đầu độc lập, tư duy sắc bén và khát vọng thành công rõ ràng. Bạn hành động dứt khoát, tự tin và có năng lực quản lý xuất sắc.\n\n" +
                    "Bạn coi trọng sự công bằng và tính tự chủ, luôn nỗ lực xây dựng nền tảng vững chắc cho tương lai của mình.",
                    tenGoi);
            case 9 -> String.format(
                    "%s là người giàu lý tưởng nhân đạo, luôn hướng về cộng đồng và mong muốn đem lại giá trị tốt đẹp cho cuộc sống. Bạn bao dung, chân thành và có sức hút từ sự vị tha.\n\n" +
                    "Bạn nhìn cuộc đời bằng góc nhìn nhân văn, nhưng đôi khi đặt kỳ vọng quá cao vào người khác để rồi dễ cảm thấy thất vọng.",
                    tenGoi);
            case 10, 1 -> String.format(
                    "%s mang tư chất của một người tiên phong: Độc lập, quyết đoán, tự tin và có khả năng thích ứng linh hoạt trong mọi hoàn cảnh. Bạn không ngại đối mặt với thử thách mới.\n\n" +
                    "Bạn luôn muốn tạo dựng con đường riêng của mình, hành động dứt khoát và có khả năng truyền cảm hứng dẫn dắt đội ngũ.",
                    tenGoi);
            case 11 -> String.format(
                    "%s là người có trực giác rất nhạy bén, sống chân thành và hướng tới sự hòa hợp sâu sắc. Bạn có khả năng cảm nhận tâm tư của người khác một cách tự nhiên.\n\n" +
                    "Tiềm năng tinh thần của bạn rất lớn, nhưng bạn cần học cách giữ vững ranh giới cảm xúc để không bị ảnh hưởng bởi năng lượng tiêu cực xung quanh.",
                    tenGoi);
            case 22 -> String.format(
                    "%s là người vừa có tầm nhìn xa rộng, vừa có năng lực thực thi thực tế siêu việt. Bạn khao khát xây dựng những dự án, công trình mang lại giá trị lâu dài cho xã hội.\n\n" +
                    "Sự bền bỉ và ý chí mạnh mẽ giúp bạn biến những ý tưởng táo bạo thành hiện thực vững chắc.",
                    tenGoi);
            default -> String.format(
                    "%s là người sở hữu nội lực phong phú, tính cách đa chiều và có khả năng dung hòa tuyệt vời giữa lý trí và cảm xúc.",
                    tenGoi);
        };
    }

    private String getStrengthsPortrait(int chuDao) {
        return switch (chuDao) {
            case 3 -> "Thế mạnh vượt trội của bạn là khả năng biểu đạt ngôn từ sinh động, tư duy sáng tạo linh hoạt và năng khiếu kết nối mọi người một cách tự nhiên. Bạn dễ dàng truyền cảm hứng và tạo bầu không khí tích cực cho tập thể.";
            case 4 -> "Năng lực bẩm sinh của bạn là khả năng phân tích tỉ mỉ, tổ chức công việc khoa học và sự kiên trì bền bỉ. Bạn rất giỏi trong việc thiết lập hệ thống, quản lý vận hành và xử lý các vấn đề đòi hỏi tính chi tiết cao.";
            case 5 -> "Điểm mạnh nổi bật là sự nhanh nhạy trước thời cuộc, tài xoay xở linh hoạt trong tình huống bất ngờ và năng lực kết nối đa dạng các mối quan hệ xã hội. Bạn luôn tìm ra hướng đi mới khi mọi thứ bế tắc.";
            case 6 -> "Bạn sở hữu năng khiếu thẩm mỹ tự nhiên, khả năng gắn kết tập thể và sự đồng cảm sâu sắc. Bạn rất giỏi trong việc lắng nghe, thấu hiểu và xoa dịu những căng thẳng xung quanh.";
            case 7 -> "Thế mạnh thiên bẩm là năng lực quan sát thấu suốt bản chất sự việc, trực giác phán đoán chuẩn xác và khả năng tự học chuyên sâu. Bạn có tố chất trở thành chuyên gia trong lĩnh vực mình đam mê.";
            case 8 -> "Năng lực lãnh đạo quyết đoán, trực giác kinh doanh sắc sảo và khả năng tối ưu hóa nguồn lực giúp bạn luôn nổi bật ở các vị trí điều hành hoặc kinh doanh độc lập.";
            case 9 -> "Sức mạnh lớn nhất của bạn là tầm nhìn rộng mở, khả năng quy tụ lòng người và năng khiếu giảng dạy, cố vấn hoặc hoạt động xã hội. Lời nói của bạn luôn mang trọng lượng và sự tin cậy.";
            default -> "Bạn có khả năng tự nhận thức cao, tư duy độc lập và năng lực thích ứng linh hoạt trong môi trường làm việc đa dạng.";
        };
    }

    private String getBlindspotsPortrait(int chuDao) {
        return switch (chuDao) {
            case 3 -> "Điểm mù lớn nhất là sự mất kiên nhẫn khi phải làm những việc lặp đi lặp lại và dễ để cảm xúc chi phối quyết định. Đôi khi bạn phân tán năng lượng vào quá nhiều việc cùng lúc mà không hoàn thành trọn vẹn.";
            case 4 -> "Góc khuất tâm lý là sự quá cầu toàn, ngại đổi mới và có xu hướng lo lắng thái quá. Bạn dễ bỏ lỡ các cơ hội đột phá chỉ vì chúng chưa có lộ trình an toàn 100%.";
            case 5 -> "Mâu thuẫn nội tâm nằm ở sự thèm muốn tự do quá mức, dẫn đến trạng thái dễ cả thèm chóng chán. Bạn dễ bị thu hút bởi những điều mới lạ mà xao nhãng mục tiêu trọng tâm dài hạn.";
            case 6 -> "Cạm bẫy cảm xúc là sự bao bọc thái quá và hay lo lắng thay cho người khác. Bạn thường đặt kỳ vọng quá cao vào sự hồi đáp để rồi dễ cảm thấy thất vọng hoặc kiệt sức.";
            case 7 -> "Điểm mù là xu hướng tự cô lập bản thân, khép kín và khó bộc bạch cảm xúc thật. Sự khắt khe đôi khi khiến bạn trở nên xa cách hoặc phán xét người khác theo tiêu chuẩn quá cao của mình.";
            case 8 -> "Cạm bẫy là tính độc đoán, kiểm soát và dễ bị cuốn vào guồng quay công việc mà lãng quên sức khoẻ cũng như cảm xúc của những người thân cận.";
            default -> "Cần cảnh giác với sự giằng xé giữa khát vọng cá nhân và áp lực từ kỳ vọng của những người xung quanh.";
        };
    }

    private String getCareerAndLifeAdvice(int chuDao, String tenGoi) {
        return switch (chuDao) {
            case 3 -> "Môi trường lý tưởng: Truyền thông, sáng tạo nội dung, nghệ thuật, giảng dạy hoặc tư vấn. Lời khuyên: Hãy rèn luyện thói quen kiên trì theo đuổi từng mục tiêu đến cùng trước khi bắt đầu cái mới.";
            case 4 -> "Môi trường phù hợp: Quản trị vận hành, kỹ thuật, tài chính kế toán, luật pháp hoặc kiến trúc. Lời khuyên: Hãy học cách mở lòng đón nhận phương pháp mới và cho phép bản thân nghỉ ngơi nhiều hơn.";
            case 5 -> "Môi trường lý tưởng: Kinh doanh tự do, du lịch, truyền thông sự kiện hoặc các công việc di chuyển. Lời khuyên: Hãy xây dựng tính kỷ luật tự giác để sự tự do mang lại thành quả thực chất.";
            case 6 -> "Môi trường phù hợp: Giáo dục, y tế, tâm lý, thiết kế hoặc quản trị nhân sự. Lời khuyên: Hãy học cách yêu thương chính mình trước tiên và biết từ chối những yêu cầu vượt quá khả năng.";
            case 7 -> "Môi trường lý tưởng: Nghiên cứu, phân tích dữ liệu, giảng dạy đại học hoặc công nghệ cao. Lời khuyên: Hãy chia sẻ tri thức bạn tích lũy được ra cộng đồng thay vì giữ kín cho riêng mình.";
            case 8 -> "Môi trường phù hợp: Quản trị doanh nghiệp, đầu tư tài chính, bất động sản hoặc điều hành cấp cao. Lời khuyên: Hãy luôn gắn mục tiêu tài chính với đời sống tinh thần và lắng nghe ý kiến cộng sự.";
            default -> "Hãy kiên định với thế mạnh độc bản của bạn và tìm kiếm một môi trường cho phép bạn tự chủ phát huy năng lực.";
        };
    }

    private String getYearEnergyPortrait(int namCaNhan, int tuoi, String tenGoi) {
        return switch (namCaNhan) {
            case 1 -> String.format(
                    "Năm nay là Năm số 1 – Năm của sự khởi đầu mới! Năng lượng thôi thúc bạn bước ra khỏi vùng an toàn ở tuổi %d, tự tin bắt tay vào các dự án mới và khẳng định dấu ấn cá nhân.",
                    tuoi);
            case 2 -> String.format(
                    "Năm nay là Năm số 2 – Năm của sự kết nối và kiên nhẫn. Nhịp sống chậm lại để bạn lắng nghe trực giác, hàn gắn các mối quan hệ và chuẩn bị nội lực vững vàng.",
                    tuoi);
            case 3 -> String.format(
                    "Năm nay là Năm số 3 – Năm của sự sáng tạo và mở rộng giao tiếp! Bạn sẽ có nhiều cơ hội để học hỏi kỹ năng mới, mở rộng vòng kết nối và lan tỏa năng lượng tích cực.",
                    tuoi);
            case 4 -> String.format(
                    "Năm nay là Năm số 4 – Năm của củng cố nền tảng và kỷ luật thực tế. Đây là thời điểm bạn cần tập trung dọn dẹp các tồn đọng, chăm sóc sức khoẻ và xây dựng nền tảng vững chắc.",
                    tuoi);
            case 5 -> String.format(
                    "Năm nay là Năm số 5 – Năm của những chuyển dịch và cơ hội bứt phá! Nhiều cánh cửa mới sẽ mở ra, đòi hỏi bạn phải dũng cảm thích nghi và dám thay đổi.",
                    tuoi);
            case 6 -> String.format(
                    "Năm nay là Năm số 6 – Năm của gia đình, tình yêu và trách nhiệm. Trọng tâm cuộc sống hướng nhiều về những người thân yêu, làm đẹp không gian sống và tìm kiếm sự bình yên.",
                    tuoi);
            case 7 -> String.format(
                    "Năm nay là Năm số 7 – Năm của sự tĩnh lặng và chiêm nghiệm nội tâm. Một năm lý tưởng để học hỏi chuyên sâu, thanh lọc tâm trí và nâng cấp nhận thức bản thân.",
                    tuoi);
            case 8 -> String.format(
                    "Năm nay là Năm số 8 – Năm của gặt hái thành quả và khẳng định vị thế tài chính! Những nỗ lực trước đó bắt đầu đơm hoa kết trái nếu bạn hành động công bằng và quyết đoán.",
                    tuoi);
            case 9 -> String.format(
                    "Năm nay là Năm số 9 – Năm của sự khép lại một chu kỳ để chuẩn bị cho khởi đầu mới. Hãy mạnh dạn buông bỏ những điều không còn phù hợp để đón nhận những cơ hội mới.",
                    tuoi);
            default -> String.format(
                    "Năm nay là thời điểm quan trọng để bạn tái định vị bản thân và tích luỹ thêm các giá trị bền vững.",
                    tuoi);
        };
    }

    private String getYearOpportunitiesPortrait(int namCaNhan) {
        return switch (namCaNhan) {
            case 1, 3, 5, 8 -> "Thời cơ vàng để bạn chủ động hành động, ra mắt ý tưởng mới, tìm kiếm đối tác hoặc mở rộng quy mô công việc. Đừng ngần ngại nắm bắt quyền chủ động.";
            case 2, 4, 6, 7 -> "Đòn bẩy tốt nhất năm nay nằm ở việc hoàn thiện chuyên môn, nâng cấp kỹ năng mềm và xây dựng mạng lưới quan hệ chất lượng, bền vững.";
            default -> "Cơ hội đến từ sự chủ động học hỏi và thích ứng linh hoạt trước những thay đổi xung quanh.";
        };
    }

    private String getYearChallengesPortrait(int namCaNhan) {
        return switch (namCaNhan) {
            case 1, 5 -> "Tránh sự nóng vội, bốc đồng hoặc đưa ra các quyết định liều lĩnh khi chưa nghiên cứu kỹ rủi ro.";
            case 2, 6 -> "Cần tránh sự cả nể, lo lắng thái quá hoặc để cảm xúc tiêu cực của người khác ảnh hưởng đến mình.";
            case 4, 7 -> "Tránh suy nghĩ bi quan, khép kín hoặc tự tạo áp lực cầu toàn quá mức khiến bản thân kiệt sức.";
            case 8 -> "Cảnh giác với cái tôi quá lớn hoặc sự chi tiêu tài chính thiếu kế hoạch kiểm soát chặt chẽ.";
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
