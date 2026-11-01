package com.kms301.nkms.web;

import com.kms301.nkms.domain.XungDot;
import com.kms301.nkms.service.ThongKeService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TrangPhuController {

    private final ThongKeService thongKeService;

    public TrangPhuController(ThongKeService thongKeService) {
        this.thongKeService = thongKeService;
    }

    @GetMapping("/nguon")
    public String showNguon(Model model) {
        model.addAttribute("activeNav", "nguon");
        model.addAttribute("dsNguon", thongKeService.getAllNguon());
        model.addAttribute("soDonViTheoNguon", thongKeService.getUnitCountByNguon());
        model.addAttribute("dsBangChuCai", thongKeService.getAllBangChuCai());
        return "nguon";
    }

    @GetMapping("/xung-dot")
    public String showXungDot(@RequestParam(name = "page", defaultValue = "0") int page,
                              @RequestParam(name = "size", defaultValue = "20") int size,
                              Model model) {
        model.addAttribute("activeNav", "xung-dot");
        int validSize = (size == 10 || size == 20 || size == 50) ? size : 20;
        Page<XungDot> xungDotPage = thongKeService.getXungDotPage(page, validSize);
        model.addAttribute("xungDotPage", xungDotPage);
        model.addAttribute("size", validSize);
        return "xung-dot";
    }

    @GetMapping("/khai-niem")
    public String showKhaiNiem(Model model) {
        model.addAttribute("activeNav", "khai-niem");
        model.addAttribute("dsKhaiNiem", thongKeService.getAllKhaiNiem());
        model.addAttribute("dongNghiaMap", thongKeService.getDongNghiaByKhaiNiem());
        model.addAttribute("dsCongThuc", thongKeService.getAllCongThuc());
        return "khai-niem";
    }

    @GetMapping("/thong-ke")
    public String showThongKe(Model model) {
        model.addAttribute("activeNav", "thong-ke");
        model.addAllAttributes(thongKeService.buildThongKeData());
        return "thong-ke";
    }

    @GetMapping("/gioi-thieu")
    public String showGioiThieu(Model model) {
        model.addAttribute("activeNav", "gioi-thieu");
        return "gioi-thieu";
    }

    @GetMapping("/dang-nhap")
    public String showDangNhap(Model model) {
        model.addAttribute("activeNav", "dang-nhap");
        return "dang-nhap";
    }
}

