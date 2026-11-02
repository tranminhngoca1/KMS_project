package com.kms301.nkms.model.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO chứa kết quả luận giải cá nhân hoá từ AI Analysis Engine (Gemini 3.5 Flash).
 * Thiết kế theo chuẩn phân tích tâm lý & khai vấn cuộc đời sâu sắc:
 * - Chân dung tính cách cốt lõi
 * - Năng lực bẩm sinh & thế mạnh vượt trội
 * - Điểm mù & mâu thuẫn nội tâm cần gỡ bỏ
 * - Định hướng phát triển & lời khuyên hành động thực tế
 */
public class AiAnalysisDto {

    private boolean success;
    private boolean aiGenerated;
    private String modelName;
    private String tongQuanHoSo;
    private Map<String, QuestionAiAnalysis> cauHoi = new HashMap<>();

    public AiAnalysisDto() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getTongQuanHoSo() {
        return tongQuanHoSo;
    }

    public void setTongQuanHoSo(String tongQuanHoSo) {
        this.tongQuanHoSo = tongQuanHoSo;
    }

    public Map<String, QuestionAiAnalysis> getCauHoi() {
        return cauHoi;
    }

    public void setCauHoi(Map<String, QuestionAiAnalysis> cauHoi) {
        this.cauHoi = cauHoi;
    }

    public String getThe1(String maCauHoi) {
        if (cauHoi == null) return getDefaultLoadingText(maCauHoi, 1);
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getThe1() == null || qa.getThe1().isBlank()) {
            return getDefaultLoadingText(maCauHoi, 1);
        }
        return qa.getThe1();
    }

    public String getThe2(String maCauHoi) {
        if (cauHoi == null) return getDefaultLoadingText(maCauHoi, 2);
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getThe2() == null || qa.getThe2().isBlank()) {
            return getDefaultLoadingText(maCauHoi, 2);
        }
        return qa.getThe2();
    }

    public String getThe3(String maCauHoi) {
        if (cauHoi == null) return getDefaultLoadingText(maCauHoi, 3);
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getThe3() == null || qa.getThe3().isBlank()) {
            return getDefaultLoadingText(maCauHoi, 3);
        }
        return qa.getThe3();
    }

    public String getThe4(String maCauHoi) {
        if (cauHoi == null) return getDefaultLoadingText(maCauHoi, 4);
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getThe4() == null || qa.getThe4().isBlank()) {
            return getDefaultLoadingText(maCauHoi, 4);
        }
        return qa.getThe4();
    }

    // Backward-compatible aliases
    public String getChanDung(String maCauHoi) {
        return getThe1(maCauHoi);
    }

    public String getTheManh(String maCauHoi) {
        return getThe2(maCauHoi);
    }

    public String getVungToi(String maCauHoi) {
        return getThe3(maCauHoi);
    }

    public String getLoiKhuyen(String maCauHoi) {
        return getThe4(maCauHoi);
    }

    private String getDefaultLoadingText(String maCauHoi, int theIndex) {
        if ("GAP_GI".equalsIgnoreCase(maCauHoi)) {
            return switch (theIndex) {
                case 1 -> "Đang phân tích bối cảnh thời vận và năng lượng năm nay của bạn...";
                case 2 -> "Đang nắm bắt nhịp điệu tâm lý và biến chuyển ngắn hạn của bạn...";
                case 3 -> "Đang giải mã đại vận đỉnh cao và cột mốc trưởng thành của bạn...";
                default -> "Đang soi chiếu cạm bẫy tâm lý và thách thức giai đoạn cần lưu tâm...";
            };
        } else if ("DI_THE_NAO".equalsIgnoreCase(maCauHoi)) {
            return switch (theIndex) {
                case 1 -> "Đang định vị kim chỉ nam sống và tư duy dẫn đường cho bạn...";
                case 2 -> "Đang thiết lập kỷ luật hàng ngày và môi trường phù hợp cho bạn...";
                case 3 -> "Đang gợi mở giải pháp chuyển hóa điểm khuyết và buông bỏ rào cản...";
                default -> "Đang kiến tạo chiến lược dài hạn và đích đến viên mãn cho bạn...";
            };
        } else {
            return switch (theIndex) {
                case 1 -> "Đang phân tích bản ngã cốt lõi và con người thực sự của bạn...";
                case 2 -> "Đang khám phá thế giới nội tâm và khát vọng linh hồn của bạn...";
                case 3 -> "Đang nhận diện khí chất giao tiếp và phong cách biểu đạt của bạn...";
                default -> "Đang soi chiếu cấu trúc năng lượng và điểm khuyết bẩm sinh của bạn...";
            };
        }
    }

    public String getTongHop(String maCauHoi) {
        if (cauHoi == null) return "Đang phân tích và đúc kết thông điệp định hướng dành riêng cho bạn...";
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getPhanTichTongHop() == null || qa.getPhanTichTongHop().isBlank()) {
            return "Đang phân tích và đúc kết thông điệp định hướng dành riêng cho bạn...";
        }
        return qa.getPhanTichTongHop();
    }

    public static class QuestionAiAnalysis {
        private String phanTichTongHop = "";
        private String the1 = "";             // Thẻ 1 theo từng câu hỏi
        private String the2 = "";             // Thẻ 2 theo từng câu hỏi
        private String the3 = "";             // Thẻ 3 theo từng câu hỏi
        private String the4 = "";             // Thẻ 4 theo từng câu hỏi
        private String chanDung = "";         // Alias tương thích cũ
        private String theManh = "";          // Alias tương thích cũ
        private String vungToi = "";          // Alias tương thích cũ
        private String loiKhuyen = "";        // Alias tương thích cũ
        private String diemDongThuanAI = "";
        private String diemKhacBietAI = "";
        private String gocNhinPythagoras = "";
        private String gocNhinVeda = "";
        private String gocNhinKabbalah = "";

        public QuestionAiAnalysis() {
        }

        public String getPhanTichTongHop() {
            return phanTichTongHop;
        }

        public void setPhanTichTongHop(String phanTichTongHop) {
            this.phanTichTongHop = phanTichTongHop;
        }

        public String getThe1() {
            return (the1 != null && !the1.isBlank()) ? the1 : (chanDung != null ? chanDung : "");
        }

        public void setThe1(String the1) {
            this.the1 = the1;
            this.chanDung = the1;
        }

        public String getThe2() {
            return (the2 != null && !the2.isBlank()) ? the2 : (theManh != null ? theManh : "");
        }

        public void setThe2(String the2) {
            this.the2 = the2;
            this.theManh = the2;
        }

        public String getThe3() {
            return (the3 != null && !the3.isBlank()) ? the3 : (vungToi != null ? vungToi : "");
        }

        public void setThe3(String the3) {
            this.the3 = the3;
            this.vungToi = the3;
        }

        public String getThe4() {
            return (the4 != null && !the4.isBlank()) ? the4 : (loiKhuyen != null ? loiKhuyen : "");
        }

        public void setThe4(String the4) {
            this.the4 = the4;
            this.loiKhuyen = the4;
        }

        public String getChanDung() {
            return getThe1();
        }

        public void setChanDung(String chanDung) {
            setThe1(chanDung);
        }

        public String getTheManh() {
            return getThe2();
        }

        public void setTheManh(String theManh) {
            setThe2(theManh);
        }

        public String getVungToi() {
            return getThe3();
        }

        public void setVungToi(String vungToi) {
            setThe3(vungToi);
        }

        public String getLoiKhuyen() {
            return getThe4();
        }

        public void setLoiKhuyen(String loiKhuyen) {
            setThe4(loiKhuyen);
        }

        public String getDiemDongThuanAI() {
            return diemDongThuanAI;
        }

        public void setDiemDongThuanAI(String diemDongThuanAI) {
            this.diemDongThuanAI = diemDongThuanAI;
        }

        public String getDiemKhacBietAI() {
            return diemKhacBietAI;
        }

        public void setDiemKhacBietAI(String diemKhacBietAI) {
            this.diemKhacBietAI = diemKhacBietAI;
        }

        public String getGocNhinPythagoras() {
            return gocNhinPythagoras;
        }

        public void setGocNhinPythagoras(String gocNhinPythagoras) {
            this.gocNhinPythagoras = gocNhinPythagoras;
        }

        public String getGocNhinVeda() {
            return gocNhinVeda;
        }

        public void setGocNhinVeda(String gocNhinVeda) {
            this.gocNhinVeda = gocNhinVeda;
        }

        public String getGocNhinKabbalah() {
            return gocNhinKabbalah;
        }

        public void setGocNhinKabbalah(String gocNhinKabbalah) {
            this.gocNhinKabbalah = gocNhinKabbalah;
        }
    }
}
