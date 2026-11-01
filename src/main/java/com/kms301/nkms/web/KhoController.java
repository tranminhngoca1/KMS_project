package com.kms301.nkms.web;

import com.kms301.nkms.domain.*;
import com.kms301.nkms.service.KhoTriThucService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/kho")
public class KhoController {

    private final KhoTriThucService khoTriThucService;

    public KhoController(KhoTriThucService khoTriThucService) {
        this.khoTriThucService = khoTriThucService;
    }

    @GetMapping
    public String listKho(@RequestParam(name = "tuKhoa", required = false) String tuKhoa,
                          @RequestParam(name = "truongPhaiId", required = false) Long truongPhaiId,
                          @RequestParam(name = "nguonId", required = false) Long nguonId,
                          @RequestParam(name = "khaiNiemId", required = false) Long khaiNiemId,
                          @RequestParam(name = "cauHoi", required = false) String cauHoi,
                          @RequestParam(name = "loai", required = false) String loai,
                          @RequestParam(name = "mucTinCay", required = false) String mucTinCay,
                          @RequestParam(name = "nhanSuDung", required = false) String nhanSuDung,
                          @RequestParam(name = "trangThai", required = false) String trangThai,
                          @RequestParam(name = "theId", required = false) Long theId,
                          @RequestParam(name = "page", defaultValue = "0") int page,
                          @RequestParam(name = "size", defaultValue = "20") int size,
                          Model model) {
        model.addAttribute("activeNav", "kho");
        int validSize = (size == 10 || size == 20 || size == 50) ? size : 20;

        Page<DonViTriThuc> donViPage = khoTriThucService.searchDonVi(
                tuKhoa, truongPhaiId, nguonId, khaiNiemId,
                cauHoi, loai, mucTinCay, nhanSuDung, trangThai, theId,
                page, validSize
        );

        model.addAttribute("donViPage", donViPage);
        model.addAttribute("tuKhoa", tuKhoa != null ? tuKhoa : "");
        model.addAttribute("truongPhaiId", truongPhaiId);
        model.addAttribute("nguonId", nguonId);
        model.addAttribute("khaiNiemId", khaiNiemId);
        model.addAttribute("cauHoi", cauHoi != null ? cauHoi : "");
        model.addAttribute("loai", loai != null ? loai : "");
        model.addAttribute("mucTinCay", mucTinCay != null ? mucTinCay : "");
        model.addAttribute("nhanSuDung", nhanSuDung != null ? nhanSuDung : "");
        model.addAttribute("trangThai", trangThai != null ? trangThai : "");
        model.addAttribute("theId", theId);
        model.addAttribute("size", validSize);

        populateReferenceData(model);
        khoTriThucService.getUnitSystemInfo().ifPresent(b -> model.addAttribute("unitSystemInfo", b));
        return "kho-danh-sach";
    }

    @GetMapping("/{id}")
    public String detailKho(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeNav", "kho");
        DonViTriThuc donVi = khoTriThucService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị tri thức ID = " + id));

        model.addAttribute("donVi", donVi);
        model.addAttribute("quanHeList", khoTriThucService.findRelations(id));
        model.addAttribute("xungDotList", khoTriThucService.findConflictsForUnit(id));
        model.addAttribute("lichSuList", khoTriThucService.findVersionHistory(id));
        model.addAttribute("goiYQuanHe", khoTriThucService.findSuggestedRelatedUnits(donVi));
        return "kho-chi-tiet";
    }

    @GetMapping("/them")
    public String showCreateForm(Model model) {
        model.addAttribute("activeNav", "kho");
        DonViTriThuc donVi = new DonViTriThuc();
        donVi.setLoai("QUY_TAC");
        donVi.setCauHoi("AI");
        donVi.setMucTinCay("TRUNG_BINH");
        donVi.setNhanSuDung("DUNG");
        donVi.setTrangThai("DA_DUYET");

        model.addAttribute("donVi", donVi);
        model.addAttribute("isEdit", false);
        model.addAttribute("selectedTheIds", Set.of());
        populateReferenceData(model);
        return "kho-form";
    }

    @PostMapping("/them")
    public String submitCreate(@ModelAttribute("donVi") DonViTriThuc formUnit,
                               @RequestParam(name = "truongPhaiId") Long truongPhaiId,
                               @RequestParam(name = "nguonId") Long nguonId,
                               @RequestParam(name = "khaiNiemId", required = false) Long khaiNiemId,
                               @RequestParam(name = "theIds", required = false) List<Long> theIds,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        bindReferences(formUnit, truongPhaiId, nguonId, khaiNiemId);
        String username = auth != null ? auth.getName() : "admin";
        DonViTriThuc saved = khoTriThucService.createDonVi(formUnit, theIds, username);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã thêm mới đơn vị tri thức " + saved.getMa() + " (Phiên bản v1).");
        return "redirect:/kho/" + saved.getId();
    }

    @GetMapping("/{id}/sua")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeNav", "kho");
        DonViTriThuc donVi = khoTriThucService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn vị tri thức ID = " + id));

        Set<Long> selectedTheIds = new HashSet<>();
        if (donVi.getDanhSachThe() != null) {
            for (The t : donVi.getDanhSachThe()) {
                selectedTheIds.add(t.getId());
            }
        }

        model.addAttribute("donVi", donVi);
        model.addAttribute("isEdit", true);
        model.addAttribute("selectedTheIds", selectedTheIds);
        model.addAttribute("goiYQuanHe", khoTriThucService.findSuggestedRelatedUnits(donVi));
        populateReferenceData(model);
        return "kho-form";
    }

    @PostMapping("/{id}/sua")
    public String submitEdit(@PathVariable("id") Long id,
                             @ModelAttribute("donVi") DonViTriThuc formUnit,
                             @RequestParam(name = "truongPhaiId") Long truongPhaiId,
                             @RequestParam(name = "nguonId") Long nguonId,
                             @RequestParam(name = "khaiNiemId", required = false) Long khaiNiemId,
                             @RequestParam(name = "theIds", required = false) List<Long> theIds,
                             @RequestParam(name = "lyDoSua", required = false) String lyDoSua,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        bindReferences(formUnit, truongPhaiId, nguonId, khaiNiemId);
        String username = auth != null ? auth.getName() : "admin";
        DonViTriThuc updated = khoTriThucService.updateDonVi(id, formUnit, theIds, lyDoSua, username);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã cập nhật đơn vị tri thức " + updated.getMa() + " lên phiên bản v" + updated.getPhienBan() + " và ghi nhận lịch sử phiên bản.");
        return "redirect:/kho/" + updated.getId();
    }

    @PostMapping("/{id}/duyet")
    public String approveUnit(@PathVariable("id") Long id,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        String username = auth != null ? auth.getName() : "admin";
        khoTriThucService.approveDonVi(id, username);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã duyệt đơn vị tri thức (chuyển trạng thái NHAP → DA_DUYET).");
        return "redirect:/kho/" + id;
    }

    @PostMapping("/{id}/luu-tru")
    public String archiveUnit(@PathVariable("id") Long id,
                              @RequestParam(name = "lyDo", required = false) String lyDo,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        String username = auth != null ? auth.getName() : "admin";
        khoTriThucService.archiveDonVi(id, username, lyDo);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã chuyển đơn vị tri thức sang trạng thái Lưu trữ (LUU_TRU — xoá mềm).");
        return "redirect:/kho/" + id;
    }

    @GetMapping("/bang-chu-cai/unit-system")
    public String showUnitSystemForm(Model model) {
        model.addAttribute("activeNav", "kho");
        List<String> letters = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) {
            letters.add(String.valueOf(c));
        }
        model.addAttribute("letters", letters);
        model.addAttribute("currentValues", khoTriThucService.getUnitSystemMapForView());
        khoTriThucService.getUnitSystemInfo().ifPresent(b -> model.addAttribute("unitSystemInfo", b));
        return "unit-system-form";
    }

    @PostMapping("/bang-chu-cai/unit-system")
    public String saveUnitSystemForm(@RequestParam Map<String, String> allParams,
                                     RedirectAttributes redirectAttributes) {
        Map<String, Integer> letterValues = new LinkedHashMap<>();
        try {
            for (char c = 'A'; c <= 'Z'; c++) {
                String key = "letter_" + c;
                String raw = allParams.get(key);
                if (raw == null || raw.isBlank()) {
                    throw new IllegalArgumentException("Thiếu giá trị cho chữ cái " + c);
                }
                letterValues.put(String.valueOf(c), Integer.parseInt(raw.trim()));
            }
            khoTriThucService.saveUnitSystemAlphabet(letterValues);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                    "Đã lưu 26 chữ cái cho bảng Unit System của Johari và tự động chuyển trạng thái sang KICH_HOAT!");
            return "redirect:/kho";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("thongBaoLoi", "Lỗi nhập bảng chữ cái: " + e.getMessage());
            return "redirect:/kho/bang-chu-cai/unit-system";
        }
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("dsTruongPhai", khoTriThucService.getAllTruongPhai());
        model.addAttribute("dsNguon", khoTriThucService.getAllNguon());
        model.addAttribute("dsKhaiNiem", khoTriThucService.getAllKhaiNiem());
        model.addAttribute("dsThe", khoTriThucService.getAllThe());
    }

    private void bindReferences(DonViTriThuc formUnit, Long truongPhaiId, Long nguonId, Long khaiNiemId) {
        khoTriThucService.getAllTruongPhai().stream()
                .filter(tp -> Objects.equals(tp.getId(), truongPhaiId))
                .findFirst().ifPresent(formUnit::setTruongPhai);
        khoTriThucService.getAllNguon().stream()
                .filter(ng -> Objects.equals(ng.getId(), nguonId))
                .findFirst().ifPresent(formUnit::setNguon);
        if (khaiNiemId != null) {
            khoTriThucService.getAllKhaiNiem().stream()
                    .filter(kn -> Objects.equals(kn.getId(), khaiNiemId))
                    .findFirst().ifPresent(formUnit::setKhaiNiem);
        } else {
            formUnit.setKhaiNiem(null);
        }
    }
}

