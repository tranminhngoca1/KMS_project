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

    public String getChanDung(String maCauHoi) {
        if (cauHoi == null) return "Đang phân tích chân dung tâm lý và bản ngã cốt lõi của bạn...";
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getChanDung() == null || qa.getChanDung().isBlank()) {
            return "Đang phân tích chân dung tâm lý và bản ngã cốt lõi của bạn...";
        }
        return qa.getChanDung();
    }

    public String getTheManh(String maCauHoi) {
        if (cauHoi == null) return "Đang nhận diện các thế mạnh và tài năng thiên bẩm của bạn...";
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getTheManh() == null || qa.getTheManh().isBlank()) {
            return "Đang nhận diện các thế mạnh và tài năng thiên bẩm của bạn...";
        }
        return qa.getTheManh();
    }

    public String getVungToi(String maCauHoi) {
        if (cauHoi == null) return "Đang soi chiếu các điểm mù và thách thức nội tâm cần chuyển hoá...";
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getVungToi() == null || qa.getVungToi().isBlank()) {
            return "Đang soi chiếu các điểm mù và thách thức nội tâm cần chuyển hoá...";
        }
        return qa.getVungToi();
    }

    public String getLoiKhuyen(String maCauHoi) {
        if (cauHoi == null) return "Đang đúc kết lời khuyên và chiến lược phát triển cá nhân hoá dành cho bạn...";
        QuestionAiAnalysis qa = cauHoi.get(maCauHoi);
        if (qa == null || qa.getLoiKhuyen() == null || qa.getLoiKhuyen().isBlank()) {
            return "Đang đúc kết lời khuyên và chiến lược phát triển cá nhân hoá dành cho bạn...";
        }
        return qa.getLoiKhuyen();
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
        private String chanDung = "";         // Bản ngã & chân dung cốt lõi
        private String theManh = "";          // Thế mạnh vượt trội & năng khiếu
        private String vungToi = "";          // Điểm mù & mâu thuẫn nội tâm
        private String loiKhuyen = "";        // Lời khuyên định hướng & hành động
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

        public String getChanDung() {
            return chanDung;
        }

        public void setChanDung(String chanDung) {
            this.chanDung = chanDung;
        }

        public String getTheManh() {
            return theManh;
        }

        public void setTheManh(String theManh) {
            this.theManh = theManh;
        }

        public String getVungToi() {
            return vungToi;
        }

        public void setVungToi(String vungToi) {
            this.vungToi = vungToi;
        }

        public String getLoiKhuyen() {
            return loiKhuyen;
        }

        public void setLoiKhuyen(String loiKhuyen) {
            this.loiKhuyen = loiKhuyen;
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
