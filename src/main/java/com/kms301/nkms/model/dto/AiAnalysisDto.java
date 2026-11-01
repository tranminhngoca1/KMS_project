package com.kms301.nkms.model.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO chứa kết quả luận giải cá nhân hoá từ AI Analysis Engine (Gemini 3.5 Flash).
 * Đọc Hồ sơ thần số học tổng hợp -> Truy xuất tri thức -> Đối chiếu 3 trường phái -> Diễn giải cá nhân hoá.
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

    public static class QuestionAiAnalysis {
        private String phanTichTongHop;
        private String diemDongThuanAI;
        private String diemKhacBietAI;
        private String gocNhinPythagoras;
        private String gocNhinVeda;
        private String gocNhinKabbalah;

        public QuestionAiAnalysis() {
        }

        public String getPhanTichTongHop() {
            return phanTichTongHop;
        }

        public void setPhanTichTongHop(String phanTichTongHop) {
            this.phanTichTongHop = phanTichTongHop;
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

