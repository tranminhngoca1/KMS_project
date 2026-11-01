package com.kms301.nkms.controller;

import com.kms301.nkms.engine.DateValidator;
import com.kms301.nkms.engine.NameNormalizer;
import com.kms301.nkms.inference.KhamPhaResult;
import com.kms301.nkms.model.dto.AiAnalysisDto;
import com.kms301.nkms.service.AiAnalysisService;
import com.kms301.nkms.service.KhamPhaService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller cho trang Khám phá (/kham-pha) và API luận giải cá nhân hoá bằng AI Analysis Engine (/api/ai-analysis).
 * Quyền riêng tư (Mục 2): Không lưu họ tên và ngày sinh người dùng nhập ở Khám phá; không ghi log cá nhân.
 */
@Controller
public class KhamPhaController {

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final KhamPhaService khamPhaService;
    private final AiAnalysisService aiAnalysisService;

    public KhamPhaController(KhamPhaService khamPhaService, AiAnalysisService aiAnalysisService) {
        this.khamPhaService = khamPhaService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/kham-pha";
    }

    @GetMapping("/kham-pha")
    public String showKhamPha(@RequestParam(name = "hoTen", required = false) String hoTen,
                              @RequestParam(name = "ngaySinh", required = false) String ngaySinh,
                              Model model) {
        model.addAttribute("activeNav", "kham-pha");
        if (hoTen != null && !hoTen.isBlank() && ngaySinh != null && !ngaySinh.isBlank()) {
            return processKhamPha(hoTen, ngaySinh, model);
        }
        model.addAttribute("hoTen", "");
        model.addAttribute("ngaySinh", "");
        return "kham-pha";
    }

    @PostMapping("/kham-pha")
    public String submitKhamPha(@RequestParam(name = "hoTen", required = false) String hoTen,
                                @RequestParam(name = "ngaySinh", required = false) String ngaySinh,
                                Model model) {
        model.addAttribute("activeNav", "kham-pha");
        return processKhamPha(hoTen, ngaySinh, model);
    }

    private String processKhamPha(String hoTen, String ngaySinhRaw, Model model) {
        model.addAttribute("hoTen", hoTen != null ? hoTen.trim() : "");
        model.addAttribute("ngaySinh", ngaySinhRaw != null ? ngaySinhRaw.trim() : "");

        String normalizedName = NameNormalizer.normalize(hoTen);
        if (normalizedName.isBlank()) {
            model.addAttribute("loiNhapLieu", "Vui lòng nhập họ và tên hợp lệ (chứa ít nhất một chữ cái A–Z).");
            return "kham-pha";
        }

        String formattedDate = normalizeDateInput(ngaySinhRaw);
        DateValidator.ValidationResult dateVal = DateValidator.validate(formattedDate);
        if (!dateVal.isValid()) {
            model.addAttribute("loiNhapLieu", dateVal.getErrorMessage());
            return "kham-pha";
        }

        LocalDate birthDate = dateVal.getDate();
        model.addAttribute("ngaySinh", birthDate.format(DD_MM_YYYY));

        KhamPhaResult ketQua = khamPhaService.khamPha(hoTen, birthDate);
        model.addAttribute("ketQua", ketQua);
        return "kham-pha";
    }

    @PostMapping("/api/ai-analysis")
    @ResponseBody
    public ResponseEntity<AiAnalysisDto> analyzeWithAi(
            @RequestParam(name = "hoTen") String hoTen,
            @RequestParam(name = "ngaySinh") String ngaySinhRaw,
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        String normalizedName = NameNormalizer.normalize(hoTen);
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String formattedDate = normalizeDateInput(ngaySinhRaw);
        DateValidator.ValidationResult dateVal = DateValidator.validate(formattedDate);
        if (!dateVal.isValid()) {
            return ResponseEntity.badRequest().build();
        }
        KhamPhaResult ketQua = khamPhaService.khamPha(hoTen, dateVal.getDate());
        AiAnalysisDto analysis = aiAnalysisService.analyzeProfile(ketQua, force);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Chấp nhận cả định dạng dd/MM/yyyy chuẩn và yyyy-MM-dd (nếu trình duyệt gửi từ date picker).
     */
    private String normalizeDateInput(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            String[] parts = trimmed.split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        }
        return trimmed;
    }
}

