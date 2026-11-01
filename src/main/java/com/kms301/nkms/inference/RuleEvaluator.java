package com.kms301.nkms.inference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kms301.nkms.engine.HoSoSo;

/**
 * Bộ đánh giá điều kiện JSON (dieu_kien_json) của Đơn vị tri thức trên Hồ sơ số (HoSoSo).
 * Hỗ trợ điều kiện đơn và điều kiện tổ hợp {"tat_ca": [...]} / {"bat_ky": [...]}.
 */
public class RuleEvaluator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public boolean matches(String dieuKienJson, HoSoSo hoSo) {
        if (dieuKienJson == null || dieuKienJson.isBlank() || hoSo == null) {
            return false;
        }
        try {
            JsonNode root = MAPPER.readTree(dieuKienJson);
            return evaluateNode(root, hoSo);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateNode(JsonNode node, HoSoSo hoSo) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.has("tat_ca") && node.get("tat_ca").isArray()) {
            JsonNode arr = node.get("tat_ca");
            if (arr.isEmpty()) return false;
            for (JsonNode child : arr) {
                if (!evaluateNode(child, hoSo)) {
                    return false;
                }
            }
            return true;
        }
        if (node.has("bat_ky") && node.get("bat_ky").isArray()) {
            JsonNode arr = node.get("bat_ky");
            for (JsonNode child : arr) {
                if (evaluateNode(child, hoSo)) {
                    return true;
                }
            }
            return false;
        }

        String chiSo = textVal(node, "chi_so");
        if (chiSo == null) {
            return false;
        }

        return switch (chiSo) {
            case "QH_CHU_DAO" -> matchStepDisplayOrNumeric(hoSo.getQh().getConSoChuDao().getDisplayValue(),
                    hoSo.getQh().getConSoChuDao().getNumericValue(), node.get("bang"));
            case "QH_NGAY_SINH" -> matchStepDisplayOrNumeric(hoSo.getQh().getConSoNgaySinh().getDisplayValue(),
                    hoSo.getQh().getConSoNgaySinh().getNumericValue(), node.get("bang"));
            case "BIEU_DO_DEM" -> {
                int so = intVal(node, "so", -1);
                if (so < 1 || so > 9) yield false;
                int actualCount = hoSo.getQh().getBirthChart().getBirthCount(so);
                if (node.has("bang")) {
                    yield actualCount == intVal(node, "bang", -1);
                }
                if (node.has("tu")) {
                    int min = intVal(node, "tu", 0);
                    int max = node.has("den") ? intVal(node, "den", 99) : 99;
                    yield actualCount >= min && actualCount <= max;
                }
                yield false;
            }
            case "MUI_TEN" -> {
                String bo = textVal(node, "bo");
                String trangThai = textVal(node, "trang_thai");
                if (bo == null || trangThai == null) yield false;
                yield hoSo.getQh().getBirthChart().getArrows().hasArrow(bo, trangThai);
            }
            case "SO_CO_LAP" -> {
                int so = intVal(node, "so", -1);
                yield hoSo.getQh().getBirthChart().getIsolatedNumbers().contains(so);
            }
            case "QH_LINH_HON" -> matchStepDisplayOrNumeric(hoSo.getQh().getConSoLinhHon().getDisplayValue(),
                    hoSo.getQh().getConSoLinhHon().getNumericValue(), node.get("bang"));
            case "QH_BIEU_DAT" -> matchStepDisplayOrNumeric(hoSo.getQh().getConSoBieuDat().getDisplayValue(),
                    hoSo.getQh().getConSoBieuDat().getNumericValue(), node.get("bang"));
            case "QH_TEN_RIENG" -> matchStepDisplayOrNumeric(hoSo.getQh().getConSoTenRieng().getDisplayValue(),
                    hoSo.getQh().getConSoTenRieng().getNumericValue(), node.get("bang"));
            case "QH_NAM_CA_NHAN" -> matchStepDisplayOrNumeric(hoSo.getQh().getNamCaNhan().getDisplayValue(),
                    hoSo.getQh().getNamCaNhan().getNumericValue(), node.get("bang"));
            case "QH_THANG_CA_NHAN" -> matchStepDisplayOrNumeric(hoSo.getQh().getThangCaNhan().getDisplayValue(),
                    hoSo.getQh().getThangCaNhan().getNumericValue(), node.get("bang"));
            case "QH_DINH_HIEN_TAI" -> matchStepDisplayOrNumeric(
                    hoSo.getQh().getKimTuThap().getCurrentPeak().getDisplayValue(),
                    hoSo.getQh().getKimTuThap().getCurrentPeak().getNumericValue(),
                    node.get("bang"));
            case "QH_GIAI_DOAN" -> {
                String expected = textVal(node, "bang");
                yield expected != null && expected.equalsIgnoreCase(hoSo.getQh().getKimTuThap().getCurrentPhaseCode());
            }
            case "CO_SO_0" -> {
                boolean expected = !node.has("co") || node.get("co").asBoolean(true);
                yield hoSo.getJohari().getZeroInfo().isHasZero() == expected;
            }
            case "DECOZ_DUONG_DOI" -> matchStepDisplayOrNumeric(hoSo.getDecoz().getSoDuongDoi().getDisplayValue(),
                    hoSo.getDecoz().getSoDuongDoi().getNumericValue(), node.get("bang"));
            case "DECOZ_NO_NGHIEP" -> {
                int debt = node.has("chua") ? intVal(node, "chua", -1) : intVal(node, "bang", -1);
                yield hoSo.getDecoz().getSoNoNghiep().contains(debt);
            }
            case "DECOZ_BIEU_DAT" -> matchStepDisplayOrNumeric(hoSo.getDecoz().getSoBieuDat().getDisplayValue(),
                    hoSo.getDecoz().getSoBieuDat().getNumericValue(), node.get("bang"));
            case "DECOZ_KHAT_VONG" -> matchStepDisplayOrNumeric(hoSo.getDecoz().getKhatVongTamHon().getDisplayValue(),
                    hoSo.getDecoz().getKhatVongTamHon().getNumericValue(), node.get("bang"));
            case "DECOZ_NHAN_CACH" -> matchStepDisplayOrNumeric(hoSo.getDecoz().getNhanCach().getDisplayValue(),
                    hoSo.getDecoz().getNhanCach().getNumericValue(), node.get("bang"));
            case "DECOZ_NAM_CA_NHAN" -> matchStepDisplayOrNumeric(hoSo.getDecoz().getNamCaNhan().getDisplayValue(),
                    hoSo.getDecoz().getNamCaNhan().getNumericValue(), node.get("bang"));
            case "DECOZ_DINH_HIEN_TAI" -> matchStepDisplayOrNumeric(
                    hoSo.getDecoz().getCurrentPinnacle().getDisplayValue(),
                    hoSo.getDecoz().getCurrentPinnacle().getNumericValue(),
                    node.get("bang"));
            case "JOHARI_TAM_HON" -> matchStepDisplayOrNumeric(hoSo.getJohari().getSoTamHon().getDisplayValue(),
                    hoSo.getJohari().getSoTamHon().getNumericValue(), node.get("bang"));
            case "JOHARI_DINH_MENH" -> matchStepDisplayOrNumeric(hoSo.getJohari().getSoDinhMenh().getDisplayValue(),
                    hoSo.getJohari().getSoDinhMenh().getNumericValue(), node.get("bang"));
            case "JOHARI_SO_TEN" -> {
                if (!hoSo.getJohari().getSoTen().isActive() || hoSo.getJohari().getSoTen().getStepResult() == null) {
                    yield false;
                }
                yield matchStepDisplayOrNumeric(
                        hoSo.getJohari().getSoTen().getStepResult().getDisplayValue(),
                        hoSo.getJohari().getSoTen().getStepResult().getNumericValue(),
                        node.get("bang"));
            }
            case "JOHARI_SO_KEP" -> {
                Integer soKep = hoSo.getJohari().getSoKepNgaySinh();
                if (soKep == null) yield false;
                yield soKep == intVal(node, "bang", -1);
            }
            case "JOHARI_SO_NAM" -> matchStepDisplayOrNumeric(hoSo.getJohari().getSoNam().getDisplayValue(),
                    hoSo.getJohari().getSoNam().getNumericValue(), node.get("bang"));
            case "LUON_KICH_HOAT" -> !node.has("bang") || node.get("bang").asBoolean(true);
            case "SEPHARIAL_CHIA_KHOA" -> matchStepDisplayOrNumeric(hoSo.getSepharial().getSoChiaKhoa().getDisplayValue(),
                    hoSo.getSepharial().getSoChiaKhoa().getNumericValue(), node.get("bang"));
            case "SEPHARIAL_TANG_UU_THE" -> {
                String expected = textVal(node, "bang");
                yield expected != null && expected.equalsIgnoreCase(hoSo.getSepharial().getMagicSquare().getDominantPlane());
            }
            default -> false;
        };
    }

    /**
     * Trả về nhãn chỉ số ngắn gọn để hiển thị trong câu tổng hợp Đồng thuận / Khác biệt
     * (vd: "Số chủ đạo 8", "Số định mệnh 8", "Mũi tên 1-5-9", "Số chìa khoá 7").
     */
    public String describeTriggeredIndicator(String dieuKienJson) {
        if (dieuKienJson == null || dieuKienJson.isBlank()) {
            return "Tri thức nền";
        }
        try {
            JsonNode node = MAPPER.readTree(dieuKienJson);
            return describeNode(node);
        } catch (Exception e) {
            return "Quy tắc";
        }
    }

    private String describeNode(JsonNode node) {
        if (node == null || node.isNull()) return "Quy tắc";
        if (node.has("tat_ca") && node.get("tat_ca").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode child : node.get("tat_ca")) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append(describeNode(child));
            }
            return sb.toString();
        }
        String chiSo = textVal(node, "chi_so");
        if (chiSo == null) return "Quy tắc";
        String bang = textVal(node, "bang");
        return switch (chiSo) {
            case "QH_CHU_DAO" -> "Số chủ đạo " + bang;
            case "QH_NGAY_SINH" -> "Số ngày sinh " + bang;
            case "BIEU_DO_DEM" -> {
                int so = intVal(node, "so", 0);
                if (node.has("bang")) {
                    int cnt = intVal(node, "bang", 0);
                    yield cnt == 0 ? ("Biểu đồ thiếu số " + so) : ("Biểu đồ có " + cnt + " số " + so);
                }
                yield "Biểu đồ có ≥" + intVal(node, "tu", 3) + " số " + so;
            }
            case "MUI_TEN" -> {
                String bo = textVal(node, "bo");
                String tt = textVal(node, "trang_thai");
                yield "TRONG".equalsIgnoreCase(tt) ? ("Mũi tên trống " + bo) : ("Mũi tên " + bo);
            }
            case "SO_CO_LAP" -> "Số " + intVal(node, "so", 0) + " cô lập";
            case "QH_LINH_HON" -> "Số linh hồn " + bang;
            case "QH_BIEU_DAT" -> "Số biểu đạt " + bang;
            case "QH_TEN_RIENG" -> "Số tên riêng " + bang;
            case "QH_NAM_CA_NHAN" -> "Năm cá nhân " + bang;
            case "QH_THANG_CA_NHAN" -> "Tháng cá nhân " + bang;
            case "QH_DINH_HIEN_TAI" -> "Đỉnh kim tự tháp " + bang;
            case "QH_GIAI_DOAN" -> "Giai đoạn " + bang;
            case "CO_SO_0" -> "Chữ số 0 trong ngày sinh";
            case "DECOZ_DUONG_DOI" -> "Số đường đời (Decoz) " + bang;
            case "DECOZ_NO_NGHIEP" -> "Số nợ nghiệp " + (node.has("chua") ? intVal(node, "chua", 0) : bang);
            case "DECOZ_BIEU_DAT" -> "Số biểu đạt (Decoz) " + bang;
            case "DECOZ_KHAT_VONG" -> "Khát vọng tâm hồn " + bang;
            case "DECOZ_NHAN_CACH" -> "Số nhân cách " + bang;
            case "DECOZ_NAM_CA_NHAN" -> "Năm cá nhân (Decoz) " + bang;
            case "DECOZ_DINH_HIEN_TAI" -> "Đỉnh Pinnacle " + bang;
            case "JOHARI_TAM_HON" -> "Số tâm hồn " + bang;
            case "JOHARI_DINH_MENH" -> "Số định mệnh " + bang;
            case "JOHARI_SO_TEN" -> "Số tên (Johari) " + bang;
            case "JOHARI_SO_KEP" -> "Ngày sinh kép " + bang;
            case "JOHARI_SO_NAM" -> "Số năm (Johari) " + bang;
            case "SEPHARIAL_CHIA_KHOA" -> "Số chìa khoá " + bang;
            case "SEPHARIAL_TANG_UU_THE" -> "Tầng ưu thế " + bang;
            default -> chiSo;
        };
    }

    /**
     * Trả về chuỗi diễn giải các bước tính toán trung gian từ Hồ sơ số (HoSoSo)
     * tương ứng với điều kiện kích hoạt của đơn vị tri thức (phục vụ Lớp 2 — Modal Thanh tra).
     */
    public String describeCalculationSteps(String dieuKienJson, HoSoSo hoSo) {
        if (dieuKienJson == null || dieuKienJson.isBlank() || hoSo == null) {
            return "Tri thức nền tảng được kích hoạt để đối chiếu phương pháp luận.";
        }
        try {
            JsonNode node = MAPPER.readTree(dieuKienJson);
            return describeStepsNode(node, hoSo);
        } catch (Exception e) {
            return "Khớp điều kiện: " + dieuKienJson;
        }
    }

    private String describeStepsNode(JsonNode node, HoSoSo hoSo) {
        if (node == null || node.isNull()) return "";
        if (node.has("tat_ca") && node.get("tat_ca").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode child : node.get("tat_ca")) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(describeStepsNode(child, hoSo));
            }
            return sb.toString();
        }
        if (node.has("bat_ky") && node.get("bat_ky").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode child : node.get("bat_ky")) {
                if (evaluateNode(child, hoSo)) {
                    if (sb.length() > 0) sb.append(" | ");
                    sb.append(describeStepsNode(child, hoSo));
                }
            }
            return sb.toString();
        }
        String chiSo = textVal(node, "chi_so");
        if (chiSo == null) return "";
        return switch (chiSo) {
            case "QH_CHU_DAO" -> "Con số chủ đạo (QH/Phillips): " + hoSo.getQh().getConSoChuDao().getSteps();
            case "QH_NGAY_SINH" -> "Con số ngày sinh (QH/Phillips): " + hoSo.getQh().getConSoNgaySinh().getSteps();
            case "BIEU_DO_DEM" -> {
                int so = intVal(node, "so", 0);
                int cnt = hoSo.getQh().getBirthChart().getBirthCount(so);
                int nameCnt = hoSo.getQh().getBirthChart().getNameCount(so);
                yield "Biểu đồ ngày sinh 3×3 (QH/Phillips): Số " + so + " xuất hiện " + cnt
                        + " lần trong ngày sinh" + (nameCnt > 0 ? (" (và +" + nameCnt + " lần từ họ tên)") : "");
            }
            case "MUI_TEN" -> {
                String bo = textVal(node, "bo");
                String tt = textVal(node, "trang_thai");
                boolean comp = hoSo.getQh().getBirthChart().getArrows().isCompensatedByName(bo);
                yield "Trục " + bo + " trên Biểu đồ 3×3: trạng thái " + tt
                        + (comp ? " (Đã được chữ số từ họ tên lấp đầy / phá thế)" : "");
            }
            case "SO_CO_LAP" -> {
                int so = intVal(node, "so", 0);
                yield "Số " + so + " nằm ở góc Biểu đồ ngày sinh nhưng các ô kề quanh nó đều trống.";
            }
            case "QH_LINH_HON" -> "Con số linh hồn (QH — tổng nguyên âm): " + hoSo.getQh().getConSoLinhHon().getSteps();
            case "QH_BIEU_DAT" -> "Con số biểu đạt (QH — tổng phụ âm): " + hoSo.getQh().getConSoBieuDat().getSteps();
            case "QH_TEN_RIENG" -> "Con số tên riêng (QH): " + hoSo.getQh().getConSoTenRieng().getSteps();
            case "QH_NAM_CA_NHAN" -> "Năm cá nhân " + hoSo.getNamHienTai() + " (QH/Phillips): " + hoSo.getQh().getNamCaNhan().getSteps();
            case "QH_THANG_CA_NHAN" -> "Tháng cá nhân " + hoSo.getThangHienTai() + "/" + hoSo.getNamHienTai() + " (QH/Phillips): " + hoSo.getQh().getThangCaNhan().getSteps();
            case "QH_DINH_HIEN_TAI" -> "Kim tự tháp 4 đỉnh (QH/Phillips): " + hoSo.getQh().getKimTuThap().getAgeFormulaSteps()
                    + " → Đỉnh hiện tại (Đỉnh " + hoSo.getQh().getKimTuThap().getCurrentPeak().getIndex() + "): "
                    + hoSo.getQh().getKimTuThap().getCurrentPeak().getSteps();
            case "QH_GIAI_DOAN" -> "Giai đoạn cuộc đời (QH/Phillips): " + hoSo.getQh().getKimTuThap().getCurrentPhaseName()
                    + " (" + hoSo.getQh().getKimTuThap().getAgeFormulaSteps() + ")";
            case "CO_SO_0" -> "Phân tích chữ số 0 trong ngày sinh: "
                    + (hoSo.getJohari().getZeroInfo().isHasZero()
                    ? ("Có số 0 tại " + hoSo.getJohari().getZeroInfo().getPositions() + " (Mức ưu tiên Johari: " + hoSo.getJohari().getZeroInfo().getHighestLevel() + ")")
                    : "Không có chữ số 0");
            case "DECOZ_DUONG_DOI" -> "Số đường đời — Life Path (Decoz, cộng 3 phần rút gọn): " + hoSo.getDecoz().getSoDuongDoi().getSteps();
            case "DECOZ_NO_NGHIEP" -> "Số nợ nghiệp — Karmic Debt (Decoz): Phát hiện tổng trung gian thuộc {13, 14, 16, 19} → " + hoSo.getDecoz().getSoNoNghiep();
            case "DECOZ_BIEU_DAT" -> "Số biểu đạt — Expression (Decoz, cộng từng từ tên): " + hoSo.getDecoz().getSoBieuDat().getSteps();
            case "DECOZ_KHAT_VONG" -> "Khát vọng tâm hồn — Heart's Desire (Decoz): " + hoSo.getDecoz().getKhatVongTamHon().getSteps();
            case "DECOZ_NHAN_CACH" -> "Số nhân cách — Personality (Decoz): " + hoSo.getDecoz().getNhanCach().getSteps();
            case "DECOZ_NAM_CA_NHAN" -> "Năm cá nhân " + hoSo.getNamHienTai() + " (Decoz): " + hoSo.getDecoz().getNamCaNhan().getSteps();
            case "DECOZ_DINH_HIEN_TAI" -> "Đỉnh Pinnacle hiện tại (Decoz): Đỉnh " + hoSo.getDecoz().getCurrentPinnacle().getIndex()
                    + " = " + hoSo.getDecoz().getCurrentPinnacle().getSteps();
            case "JOHARI_TAM_HON" -> "Số tâm hồn — Psychic Number (Johari, rút gọn ngày sinh 1–9): " + hoSo.getJohari().getSoTamHon().getSteps()
                    + " • Hành tinh: " + hoSo.getJohari().getHanhTinhTamHon();
            case "JOHARI_DINH_MENH" -> "Số định mệnh — Destiny Number (Johari, tổng chữ số ngày+tháng+năm → 1–9): "
                    + hoSo.getJohari().getSoDinhMenh().getSteps() + " • Hành tinh: " + hoSo.getJohari().getHanhTinhDinhMenh();
            case "JOHARI_SO_TEN" -> "Số tên — Name Number (Johari Unit System): " + hoSo.getJohari().getSoTen().getDisplayMessage();
            case "JOHARI_SO_KEP" -> "Ngày sinh có 2 chữ số (Double-digit Compound Number — Johari): Ngày " + hoSo.getJohari().getSoKepNgaySinh();
            case "JOHARI_SO_NAM" -> "Số năm " + hoSo.getNamHienTai() + " (Johari, cộng thứ sinh nhật trong tuần): " + hoSo.getJohari().getSoNam().getSteps();
            case "SEPHARIAL_CHIA_KHOA" -> "Số chìa khoá — Key Number (Sepharial Ch.IV, bỏ 2 chữ số thế kỷ): "
                    + hoSo.getSepharial().getSoChiaKhoa().getSteps()
                    + " (Nếu giữ 4 chữ số năm: " + hoSo.getSepharial().getSoChiaKhoaFullYear().getSteps() + ")";
            case "SEPHARIAL_TANG_UU_THE" -> "Ma phương 3 tầng (Sepharial Ch.IV): " + hoSo.getSepharial().getMagicSquare().getSteps();
            default -> "Điều kiện kích hoạt: " + chiSo;
        };
    }

    private boolean matchStepDisplayOrNumeric(String displayVal, int numericVal, JsonNode expectedNode) {
        if (expectedNode == null || expectedNode.isNull()) return false;
        String expStr = expectedNode.asText().trim();
        if (expStr.equalsIgnoreCase(displayVal)) {
            return true;
        }
        if ("22/4".equals(displayVal) && ("22".equals(expStr) || "22/4".equals(expStr))) {
            return true;
        }
        try {
            int expInt = Integer.parseInt(expStr);
            if (expInt == numericVal) {
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private String textVal(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private int intVal(JsonNode node, String field, int defaultVal) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return defaultVal;
        }
        return node.get(field).asInt(defaultVal);
    }
}

