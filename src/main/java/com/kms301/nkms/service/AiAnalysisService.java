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
 * bằng mô hình Gemini 3.5 Flash.
 *
 * Bảo mật & Quyền riêng tư (Mục 2):
 * - Không gửi họ tên thật hay ngày sinh thô vào log hoặc kho lưu trữ.
 * - Chỉ nạp tổ hợp chỉ số đã tính toán và các đơn vị tri thức đã khớp từ CSDL để AI tổng hợp.
 */
@Service
public class AiAnalysisService {

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
                .connectTimeout(Duration.ofSeconds(10))
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
                // Tự động chuyển sang bộ tổng hợp dự phòng nếu mất kết nối mạng hoặc vượt hạn mức
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
        generationConfig.put("temperature", 0.45);
        generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        requestBody.put("generationConfig", generationConfig);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        String cleanModel = modelName.startsWith("models/") ? modelName.substring("models/".length()) : modelName;
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + cleanModel + ":generateContent?key=" + keyToUse.trim();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(25))
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
        dto.setModelName("Gemini 3.5 Flash (" + cleanModel + ")");
        dto.setTongQuanHoSo(parsed.path("tongQuanHoSo").asText(""));

        Map<String, AiAnalysisDto.QuestionAiAnalysis> qMap = new HashMap<>();
        for (String qKey : List.of("AI", "GAP_GI", "DI_THE_NAO")) {
            JsonNode qNode = parsed.path("cauHoi").path(qKey);
            AiAnalysisDto.QuestionAiAnalysis qa = new AiAnalysisDto.QuestionAiAnalysis();
            qa.setPhanTichTongHop(qNode.path("phanTichTongHop").asText(""));
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
        sb.append("Bạn là 'AI Analysis Engine' của Hệ thống Quản lý Tri thức Thần số học (NKMS - KMS301).\n");
        sb.append("NHIỆM VỤ: Đọc Hồ sơ thần số học tổng hợp dưới đây (gồm sự kết hợp độc nhất của tất cả các chỉ số từ 3 trường phái Pythagoras, Vệ Đà, Kabbalah và các Đơn vị tri thức đã truy xuất từ 5 sách nguồn chính thống: PHILLIPS, QH, DECOZ, JOHARI, SEPHARIAL). ");
        sb.append("Từ đó viết bài luận giải CÁ NHÂN HOÁ SÂU SẮC, LIỀN MẠCH, kết nối sự tương tác giữa các con số (tránh liệt kê rời rạc từng số đơn lẻ để không có 2 hồ sơ nào giống nhau).\n\n");

        sb.append("QUY TẮC HỌC THUẬT BẮT BUỘC:\n");
        sb.append("1. Chỉ sử dụng tri thức từ 5 nguồn sách trong hồ sơ (Phillips, Quỳnh Hương, Hans Decoz, Harish Johari, Sepharial). Tuyệt đối không đưa khái niệm ngoài nguồn (như số 33, chiêm tinh phương Tây ngoài sách, Tarot).\n");
        sb.append("2. Khi luận giải, hãy PHỐI HỢP các chỉ số với nhau: ví dụ sự cộng hưởng hoặc giằng xé giữa Con số chủ đạo/Đường đời với Số Ngày sinh, Số Linh hồn, Số Biểu đạt, Mũi tên biểu đồ, kết hợp với Số Tâm hồn/Vận mệnh Vệ Đà (Johari) và Số Chính/Ma phương Kabbalah (Sepharial).\n");
        sb.append("3. Trích dẫn tự nhiên tên tác giả/nguồn trong câu văn, ví dụ: [Phillips/QH], [Decoz], [Johari - Vệ Đà], [Sepharial - Kabbalah].\n");
        sb.append("4. Viết hoàn toàn bằng tiếng Việt chuẩn mực, sâu sắc, giàu tính định hướng phát triển bản thân.\n\n");

        sb.append("=== 1. TỔ HỢP CHỈ SỐ ĐÃ TÍNH CỦA HỒ SƠ NÀY ===\n");
        sb.append("- Độ tuổi hiện tại: ").append(hs.getTuoiHienTai()).append(" tuổi (Năm xét: ").append(hs.getNamHienTai()).append(", Tháng: ").append(hs.getThangHienTai()).append(")\n");
        sb.append("- [PYTHAGORAS - QH/Phillips]:\n");
        sb.append("  + Con số chủ đạo: ").append(qh.getConSoChuDao().getDisplayValue()).append("\n");
        sb.append("  + Con số ngày sinh: ").append(qh.getConSoNgaySinh().getDisplayValue()).append("\n");
        sb.append("  + Con số linh hồn (nguyên âm): ").append(qh.getConSoLinhHon().getDisplayValue()).append("\n");
        sb.append("  + Con số biểu đạt (phụ âm): ").append(qh.getConSoBieuDat().getDisplayValue()).append("\n");
        sb.append("  + Con số tên riêng (toàn tên): ").append(qh.getConSoTenRieng().getDisplayValue()).append("\n");
        sb.append("  + Mũi tên năng lực (sau khi kết hợp tên): ").append(presentArrows).append("\n");
        sb.append("  + Mũi tên trống cần khắc phục: ").append(emptyArrows).append("\n");
        sb.append("  + Số cô lập trong biểu đồ ngày sinh: ").append(bc.getIsolatedNumbers().isEmpty() ? "Không có" : bc.getIsolatedNumbers()).append("\n");
        sb.append("  + Năm cá nhân hiện tại (").append(hs.getNamHienTai()).append("): ").append(qh.getNamCaNhan().getDisplayValue())
                .append(" | Tháng cá nhân: ").append(qh.getThangCaNhan().getDisplayValue()).append("\n");
        sb.append("  + Kim tự tháp (Đỉnh cao hiện tại): ").append(qh.getKimTuThap().getCurrentPhaseName())
                .append(" mang số ").append(qh.getKimTuThap().getCurrentPeak().getDisplayValue()).append("\n");

        sb.append("- [PYTHAGORAS - Nhánh Mỹ Hans Decoz]:\n");
        sb.append("  + Số Đường đời (Life Path): ").append(decoz.getSoDuongDoi().getDisplayValue()).append("\n");
        sb.append("  + Số Biểu đạt (Expression): ").append(decoz.getSoBieuDat().getDisplayValue()).append("\n");
        sb.append("  + Khát vọng tâm hồn (Heart's Desire): ").append(decoz.getKhatVongTamHon().getDisplayValue()).append("\n");
        sb.append("  + Nhân cách (Personality): ").append(decoz.getNhanCach().getDisplayValue()).append("\n");
        sb.append("  + Nợ nghiệp (Karmic Debt): ").append(decoz.getSoNoNghiep().isEmpty() ? "Không có" : decoz.getSoNoNghiep()).append("\n");
        sb.append("  + Đỉnh cao Pinnacle hiện tại: Số ").append(decoz.getCurrentPinnacle().getDisplayValue()).append("\n");

        sb.append("- [ẤN ĐỘ / VỆ ĐÀ - Harish Johari]:\n");
        sb.append("  + Số Tâm hồn (Psychic Number): ").append(johari.getSoTamHon().getDisplayValue())
                .append(" — Hành tinh: ").append(johari.getHanhTinhTamHon())
                .append(" (").append(johari.getChanLeTamHon()).append(")\n");
        sb.append("  + Số Định mệnh (Destiny Number): ").append(johari.getSoDinhMenh().getDisplayValue())
                .append(" — Hành tinh: ").append(johari.getHanhTinhDinhMenh()).append("\n");
        sb.append("  + Quan hệ Tâm hồn - Định mệnh: ").append(johari.getQuanHeTamHonDinhMenh()).append("\n");
        sb.append("  + Ngày sinh vượng (Exalted Day): ").append(johari.isLaSoVuong() ? "Có (năng lượng hành tinh cực vượng)" : "Không").append("\n");
        sb.append("  + Ảnh hưởng chữ số 0: ").append(johari.getZeroInfo().isHasZero() ? johari.getZeroInfo().getPositions() : "Không có số 0").append("\n");
        sb.append("  + Số Năm cá nhân Vệ Đà hiện tại: ").append(johari.getSoNam().getDisplayValue()).append("\n");

        sb.append("- [KABBALAH / CHALDEAN - Sepharial]:\n");
        sb.append("  + Số Chìa khoá (2 số cuối năm): ").append(seph.getSoChiaKhoa().getDisplayValue())
                .append(" — Hành tinh chính: ").append(seph.getHanhTinhChinh())
                .append(", Hành tinh Khoá nhỏ: ").append(seph.getHanhTinhKhoaNho()).append("\n");
        sb.append("  + Số Chìa khoá (4 chữ số năm): ").append(seph.getSoChiaKhoaFullYear().getDisplayValue()).append("\n");
        sb.append("  + Ma phương 3 tầng (3-1-9 Tinh thần: ").append(seph.getMagicSquare().getSpiritualCount())
                .append(", 6-7-5 Trí óc: ").append(seph.getMagicSquare().getMentalCount())
                .append(", 2-8-4 Thể chất: ").append(seph.getMagicSquare().getPhysicalCount())
                .append(") -> Tầng ưu thế: ").append(seph.getMagicSquare().getDominantPlane()).append("\n\n");

        sb.append("=== 2. TRI THỨC TRUY XUẤT TỪ KHO CSDL CHO HỒ SƠ NÀY ===\n");
        appendQuestionUnits(sb, "Câu 1 (Tôi là ai?)", result.getCauHoi("AI"));
        appendQuestionUnits(sb, "Câu 2 (Tôi đang gặp gì?)", result.getCauHoi("GAP_GI"));
        appendQuestionUnits(sb, "Câu 3 (Tôi nên đi thế nào?)", result.getCauHoi("DI_THE_NAO"));

        sb.append("\n=== 3. YÊU CẦU ĐỊNH DẠNG JSON TRẢ VỀ ===\n");
        sb.append("Trả về ĐÚNG 1 JSON object theo cấu trúc sau (mỗi trường văn bản dài khoảng 3-5 câu sâu sắc, phân tích sự phối hợp giữa các con số của riêng hồ sơ này):\n");
        sb.append("{\n");
        sb.append("  \"tongQuanHoSo\": \"Bức tranh tổng quát cá nhân hoá kết hợp cả 3 trường phái (nêu bật sự độc đáo của tổ hợp số này)...\",\n");
        sb.append("  \"cauHoi\": {\n");
        sb.append("    \"AI\": {\n");
        sb.append("      \"phanTichTongHop\": \"Giải đáp tổng hợp câu hỏi 'Tôi là ai?' dựa trên sự tương tác giữa các chỉ số bản ngã...\",\n");
        sb.append("      \"diemDongThuanAI\": \"Phân tích điểm giao thoa đồng thuận giữa 3 trường phái trên hồ sơ này...\",\n");
        sb.append("      \"diemKhacBietAI\": \"Phân tích điểm khác biệt/bổ khuyết thú vị giữa 3 trường phái trên hồ sơ này...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Luận giải kết hợp Chủ đạo + Ngày sinh + Linh hồn + Biểu đạt + Mũi tên (theo Phillips/QH & Decoz)...\",\n");
        sb.append("      \"gocNhinVeda\": \"Luận giải kết hợp Số Tâm hồn + Số Định mệnh + Hành tinh chủ quản (theo Johari)...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Luận giải kết hợp Số Chìa khoá + Ma phương 3 tầng (theo Sepharial)...\"\n");
        sb.append("    },\n");
        sb.append("    \"GAP_GI\": {\n");
        sb.append("      \"phanTichTongHop\": \"Giải đáp tổng hợp câu hỏi 'Tôi đang gặp gì?' trong năm hiện tại...\",\n");
        sb.append("      \"diemDongThuanAI\": \"...\",\n");
        sb.append("      \"diemKhacBietAI\": \"...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Luận giải kết hợp Năm/Tháng cá nhân + Đỉnh cao Kim tự tháp/Pinnacle + Thử thách + Mũi tên trống...\",\n");
        sb.append("      \"gocNhinVeda\": \"Luận giải kết hợp Số Năm Vệ Đà + Độ tương hợp Tâm hồn/Định mệnh + Số 0...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Luận giải góc nhìn chu kỳ và sự cân bằng 3 tầng Ma phương của Sepharial...\"\n");
        sb.append("    },\n");
        sb.append("    \"DI_THE_NAO\": {\n");
        sb.append("      \"phanTichTongHop\": \"Kim chỉ nam định hướng phát triển 'Tôi nên đi thế nào?' được cá nhân hoá cho tổ hợp số này...\",\n");
        sb.append("      \"diemDongThuanAI\": \"...\",\n");
        sb.append("      \"diemKhacBietAI\": \"...\",\n");
        sb.append("      \"gocNhinPythagoras\": \"Định hướng phát triển & rèn luyện theo Pythagoras (Phillips/QH & Decoz)...\",\n");
        sb.append("      \"gocNhinVeda\": \"Định hướng rèn luyện tâm thức & hành động theo Vệ Đà (Johari)...\",\n");
        sb.append("      \"gocNhinKabbalah\": \"Định hướng điều hoà năng lượng & nhận thức theo Kabbalah (Sepharial)...\"\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private void appendQuestionUnits(StringBuilder sb, String label, KhamPhaResult.QuestionResult qr) {
        if (qr == null) return;
        sb.append("* ").append(label).append(":\n");
        List<KhamPhaResult.MatchedUnit> all = new ArrayList<>();
        all.addAll(qr.getPythagorasQhPhillips());
        all.addAll(qr.getPythagorasDecoz());
        all.addAll(qr.getVedaJohari());
        all.addAll(qr.getKabbalahSepharial());

        int count = 0;
        for (KhamPhaResult.MatchedUnit mu : all) {
            if (count >= 12) break;
            DonViTriThuc dv = mu.getDonVi();
            String snippet = dv.getNoiDung();
            if (snippet != null && snippet.length() > 160) {
                snippet = snippet.substring(0, 160) + "...";
            }
            sb.append("  - [").append(dv.getNguon().getMa()).append(" - ").append(mu.getChiSoKichHoat()).append("] ")
                    .append(dv.getTieuDe()).append(": ").append(snippet).append("\n");
            count++;
        }
    }

    private AiAnalysisDto buildSynthesizedFallback(KhamPhaResult result) {
        HoSoSo hs = result.getHoSoSo();
        QhCalculator.QhProfile qh = hs.getQh();
        DecozCalculator.DecozProfile decoz = hs.getDecoz();
        JohariCalculator.JohariProfile johari = hs.getJohari();
        SepharialCalculator.SepharialProfile seph = hs.getSepharial();

        AiAnalysisDto dto = new AiAnalysisDto();
        dto.setSuccess(true);
        dto.setAiGenerated(false);
        dto.setModelName("NKMS Knowledge Synthesizer (Chế độ Ngoại tuyến)");

        dto.setTongQuanHoSo(String.format(
                "Hồ sơ của bạn mang tổ hợp đặc trưng giữa Con số chủ đạo %s (Đường đời Decoz %s), Số Ngày sinh %s, Số Linh hồn %s (Pythagoras), kết hợp cùng Số Tâm hồn %s (%s) & Số Định mệnh %s (%s) theo Vệ Đà, và Số Chìa khoá %s trên tầng %s của Ma phương Kabbalah.",
                qh.getConSoChuDao().getDisplayValue(),
                decoz.getSoDuongDoi().getDisplayValue(),
                qh.getConSoNgaySinh().getDisplayValue(),
                qh.getConSoLinhHon().getDisplayValue(),
                johari.getSoTamHon().getDisplayValue(),
                johari.getHanhTinhTamHon(),
                johari.getSoDinhMenh().getDisplayValue(),
                johari.getHanhTinhDinhMenh(),
                seph.getSoChiaKhoa().getDisplayValue(),
                seph.getMagicSquare().getDominantPlane()
        ));

        Map<String, AiAnalysisDto.QuestionAiAnalysis> map = new HashMap<>();

        // AI
        AiAnalysisDto.QuestionAiAnalysis qaAi = new AiAnalysisDto.QuestionAiAnalysis();
        qaAi.setPhanTichTongHop(String.format(
                "Bản ngã cốt lõi của bạn là sự giao thoa giữa năng lượng Chủ đạo %s và sắc thái Ngày sinh %s, được dẫn dắt từ bên trong bởi Số Linh hồn %s và thể hiện ra bên ngoài qua Số Biểu đạt %s. Khi đối chiếu sang Vệ Đà, hành tinh %s (Tâm hồn %s) cùng %s (Định mệnh %s) tạo nên chiều sâu tâm thức riêng biệt.",
                qh.getConSoChuDao().getDisplayValue(), qh.getConSoNgaySinh().getDisplayValue(),
                qh.getConSoLinhHon().getDisplayValue(), qh.getConSoBieuDat().getDisplayValue(),
                johari.getHanhTinhTamHon(), johari.getSoTamHon().getDisplayValue(),
                johari.getHanhTinhDinhMenh(), johari.getSoDinhMenh().getDisplayValue()
        ));
        qaAi.setDiemDongThuanAI("Các trường phái cùng hội tụ khi giải mã trục năng lực cốt lõi thông qua tổng hợp ngày sinh và tần số rung động nội tâm.");
        qaAi.setDiemKhacBietAI("Pythagoras phân tách rõ nguyên âm/phụ âm họ tên và lưới 3-6-9, Vệ Đà nhấn mạnh hành tinh chủ quản ngày sinh, còn Kabbalah xét Ma phương 3-1-9.");
        qaAi.setGocNhinPythagoras(String.format("Sự kết hợp giữa Số Chủ đạo %s (QH/Phillips), Đường đời %s (Decoz), Linh hồn %s và Biểu đạt %s cho thấy cấu trúc nhân cách đa tầng giữa khát vọng nội tâm và phong cách hành động thực tế.",
                qh.getConSoChuDao().getDisplayValue(), decoz.getSoDuongDoi().getDisplayValue(),
                qh.getConSoLinhHon().getDisplayValue(), qh.getConSoBieuDat().getDisplayValue()));
        qaAi.setGocNhinVeda(String.format("Theo Harish Johari, bạn mang Số Tâm hồn %s (%s) kết hợp Số Định mệnh %s (%s) với mối tương quan: %s.",
                johari.getSoTamHon().getDisplayValue(), johari.getHanhTinhTamHon(),
                johari.getSoDinhMenh().getDisplayValue(), johari.getHanhTinhDinhMenh(),
                johari.getQuanHeTamHonDinhMenh()));
        qaAi.setGocNhinKabbalah(String.format("Theo Sepharial (1911), Số Chìa khoá của bạn là %s (%s) và Ma phương 3 tầng nghiêng về tầng %s (Tinh thần: %d, Trí óc: %d, Thể chất: %d).",
                seph.getSoChiaKhoa().getDisplayValue(), seph.getHanhTinhChinh(),
                seph.getMagicSquare().getDominantPlane(),
                seph.getMagicSquare().getSpiritualCount(), seph.getMagicSquare().getMentalCount(), seph.getMagicSquare().getPhysicalCount()));
        map.put("AI", qaAi);

        // GAP_GI
        AiAnalysisDto.QuestionAiAnalysis qaGap = new AiAnalysisDto.QuestionAiAnalysis();
        qaGap.setPhanTichTongHop(String.format(
                "Trong năm %d (tuổi %d), bạn đang đi qua Năm cá nhân %s (tháng cá nhân %s) theo Pythagoras và Số Năm %s theo Vệ Đà, đồng thời đang ở giai đoạn %s (Đỉnh Kim tự tháp số %s, Pinnacle Decoz số %s).",
                hs.getNamHienTai(), hs.getTuoiHienTai(),
                qh.getNamCaNhan().getDisplayValue(), qh.getThangCaNhan().getDisplayValue(),
                johari.getSoNam().getDisplayValue(),
                qh.getKimTuThap().getCurrentPhaseName(),
                qh.getKimTuThap().getCurrentPeak().getDisplayValue(),
                decoz.getCurrentPinnacle().getDisplayValue()
        ));
        qaGap.setDiemDongThuanAI("Cả Pythagoras và Vệ Đà đều nhìn nhận giai đoạn hiện tại qua nhịp sóng chu kỳ 9 năm kết hợp với bài học trưởng thành theo độ tuổi.");
        qaGap.setDiemKhacBietAI("Pythagoras tính Năm cá nhân từ Ngày + Tháng + Năm hiện tại, còn Vệ Đà cộng thêm số Thứ trong tuần của ngày sinh nhật trong năm đó.");
        qaGap.setGocNhinPythagoras(String.format("Năm cá nhân %s kết hợp Tháng cá nhân %s và Đỉnh Kim tự tháp số %s (Pinnacle Decoz số %s) định hình trọng tâm ưu tiên của bạn trong năm %d.",
                qh.getNamCaNhan().getDisplayValue(), qh.getThangCaNhan().getDisplayValue(),
                qh.getKimTuThap().getCurrentPeak().getDisplayValue(), decoz.getCurrentPinnacle().getDisplayValue(), hs.getNamHienTai()));
        qaGap.setGocNhinVeda(String.format("Số Năm Vệ Đà %s giao thoa cùng cặp Tâm hồn %s – Định mệnh %s (%s) chỉ ra các chuyển dịch năng lượng cần lưu tâm.",
                johari.getSoNam().getDisplayValue(), johari.getSoTamHon().getDisplayValue(),
                johari.getSoDinhMenh().getDisplayValue(), johari.getQuanHeTamHonDinhMenh()));
        qaGap.setGocNhinKabbalah("Sepharial tập trung vào quy luật cân bằng giữa 3 tầng Tinh thần – Trí óc – Thể chất và sự tương tác giữa Số Chính và Số Phụ.");
        map.put("GAP_GI", qaGap);

        // DI_THE_NAO
        AiAnalysisDto.QuestionAiAnalysis qaDi = new AiAnalysisDto.QuestionAiAnalysis();
        qaDi.setPhanTichTongHop(String.format(
                "Để phát huy tối đa tiềm năng của tổ hợp Chủ đạo %s – Tâm hồn Vệ Đà %s – Chìa khoá Kabbalah %s, bạn cần kết hợp phát triển năng lực cốt lõi, bù đắp các trục còn khuyết trên biểu đồ và thuận theo nhịp Năm cá nhân %s.",
                qh.getConSoChuDao().getDisplayValue(), johari.getSoTamHon().getDisplayValue(),
                seph.getSoChiaKhoa().getDisplayValue(), qh.getNamCaNhan().getDisplayValue()
        ));
        qaDi.setDiemDongThuanAI("Cả 3 trường phái đều hướng con người tới sự tự nhận thức, rèn luyện kỷ luật cá nhân và cân bằng giữa đời sống tinh thần với hành động thực tiễn.");
        qaDi.setDiemKhacBietAI("Pythagoras dùng tên riêng và hoạt động rèn luyện để bù mũi tên trống; Vệ Đà chú trọng chuyển hoá tâm thức và điều hoà hành tinh; Kabbalah hướng tới cân bằng 3 tầng nhận thức.");
        qaDi.setGocNhinPythagoras(String.format("Tận dụng năng lượng Con số chủ đạo %s và Năm cá nhân %s, đồng thời chủ động rèn luyện ở Đỉnh cao số %s và làm đầy các trục trên biểu đồ ngày sinh.",
                qh.getConSoChuDao().getDisplayValue(), qh.getNamCaNhan().getDisplayValue(), decoz.getCurrentPinnacle().getDisplayValue()));
        qaDi.setGocNhinVeda(String.format("Phát huy phẩm chất sáng của hành tinh %s (Số Tâm hồn %s) và %s (Số Định mệnh %s) trong công việc và các mối quan hệ.",
                johari.getHanhTinhTamHon(), johari.getSoTamHon().getDisplayValue(),
                johari.getHanhTinhDinhMenh(), johari.getSoDinhMenh().getDisplayValue()));
        qaDi.setGocNhinKabbalah(String.format("Duy trì sự cân bằng hài hoà giữa tầng %s chiếm ưu thế với hai tầng còn lại trên Ma phương Sepharial.",
                seph.getMagicSquare().getDominantPlane()));
        map.put("DI_THE_NAO", qaDi);

        dto.setCauHoi(map);
        return dto;
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
