package com.kms301.nkms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "don_vi_tri_thuc")
public class DonViTriThuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ma;

    @Column(name = "tieu_de", nullable = false, length = 300)
    private String tieuDe;

    @Column(nullable = false, length = 40)
    private String loai; // QUY_TAC, MO_TA, CONG_THUC, BANG_TRA, CA_THUC_TE, LICH_SU

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "khai_niem_id")
    private KhaiNiem khaiNiem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truong_phai_id", nullable = false)
    private TruongPhai truongPhai;

    @Column(name = "gia_tri_kich_hoat", length = 100)
    private String giaTriKichHoat;

    @Column(name = "dieu_kien_json", columnDefinition = "JSON")
    private String dieuKienJson;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "cau_hoi", nullable = false, length = 40)
    private String cauHoi; // AI, GAP_GI, DI_THE_NAO, NEN_TANG

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguon_id", nullable = false)
    private Nguon nguon;

    @Column(length = 100)
    private String chuong;

    @Column(name = "trang_tu")
    private Integer trangTu;

    @Column(name = "trang_den")
    private Integer trangDen;

    @Column(name = "muc_tin_cay", nullable = false, length = 40)
    private String mucTinCay; // CAO, TRUNG_BINH, THAP

    @Column(name = "nhan_su_dung", nullable = false, length = 40)
    private String nhanSuDung; // DUNG, GOI_Y_VAN_HOA, THAN_TRONG, KHONG_DUNG

    @Column(name = "ly_do_nhan", columnDefinition = "TEXT")
    private String lyDoNhan;

    @Column(name = "phien_ban", nullable = false)
    private Integer phienBan = 1;

    @Column(name = "trang_thai", nullable = false, length = 40)
    private String trangThai = "DA_DUYET"; // NHAP, DA_DUYET, LUU_TRU

    @Column(name = "nguoi_tao", nullable = false, length = 100)
    private String nguoiTao = "system";

    @Column(name = "nguoi_duyet", length = 100)
    private String nguoiDuyet;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "don_vi_the",
            joinColumns = @JoinColumn(name = "don_vi_id"),
            inverseJoinColumns = @JoinColumn(name = "the_id")
    )
    private Set<The> danhSachThe = new LinkedHashSet<>();

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    /**
     * Định dạng trích dẫn chuẩn theo Mục 8.2: vd "Phillips, Ch.7, tr.85" hoặc "Johari, §6, tr.?"
     */
    public String getTrichDanHienThi() {
        String tenNguon = nguon != null ? nguon.getMa() : "?";
        if ("PHILLIPS".equals(tenNguon)) tenNguon = "Phillips";
        else if ("JOHARI".equals(tenNguon)) tenNguon = "Johari";
        else if ("DECOZ".equals(tenNguon)) tenNguon = "Decoz";
        else if ("SEPHARIAL".equals(tenNguon)) tenNguon = "Sepharial";

        StringBuilder sb = new StringBuilder(tenNguon);
        if (chuong != null && !chuong.isBlank()) {
            sb.append(", ").append(chuong);
        }
        if (trangTu != null) {
            sb.append(", tr.").append(trangTu);
            if (trangDen != null && !trangDen.equals(trangTu)) {
                sb.append("–").append(trangDen);
            }
            if (ghiChu != null && ghiChu.toLowerCase().contains("pdf")) {
                sb.append(" (PDF)");
            }
        } else {
            sb.append(", tr.?");
        }
        return sb.toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public String getTieuDe() { return tieuDe; }
    public void setTieuDe(String tieuDe) { this.tieuDe = tieuDe; }
    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }
    public KhaiNiem getKhaiNiem() { return khaiNiem; }
    public void setKhaiNiem(KhaiNiem khaiNiem) { this.khaiNiem = khaiNiem; }
    public TruongPhai getTruongPhai() { return truongPhai; }
    public void setTruongPhai(TruongPhai truongPhai) { this.truongPhai = truongPhai; }
    public String getGiaTriKichHoat() { return giaTriKichHoat; }
    public void setGiaTriKichHoat(String giaTriKichHoat) { this.giaTriKichHoat = giaTriKichHoat; }
    public String getDieuKienJson() { return dieuKienJson; }
    public void setDieuKienJson(String dieuKienJson) { this.dieuKienJson = dieuKienJson; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public String getCauHoi() { return cauHoi; }
    public void setCauHoi(String cauHoi) { this.cauHoi = cauHoi; }
    public Nguon getNguon() { return nguon; }
    public void setNguon(Nguon nguon) { this.nguon = nguon; }
    public String getChuong() { return chuong; }
    public void setChuong(String chuong) { this.chuong = chuong; }
    public Integer getTrangTu() { return trangTu; }
    public void setTrangTu(Integer trangTu) { this.trangTu = trangTu; }
    public Integer getTrangDen() { return trangDen; }
    public void setTrangDen(Integer trangDen) { this.trangDen = trangDen; }
    public String getMucTinCay() { return mucTinCay; }
    public void setMucTinCay(String mucTinCay) { this.mucTinCay = mucTinCay; }
    public String getNhanSuDung() { return nhanSuDung; }
    public void setNhanSuDung(String nhanSuDung) { this.nhanSuDung = nhanSuDung; }
    public String getLyDoNhan() { return lyDoNhan; }
    public void setLyDoNhan(String lyDoNhan) { this.lyDoNhan = lyDoNhan; }
    public Integer getPhienBan() { return phienBan; }
    public void setPhienBan(Integer phienBan) { this.phienBan = phienBan; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getNguoiTao() { return nguoiTao; }
    public void setNguoiTao(String nguoiTao) { this.nguoiTao = nguoiTao; }
    public String getNguoiDuyet() { return nguoiDuyet; }
    public void setNguoiDuyet(String nguoiDuyet) { this.nguoiDuyet = nguoiDuyet; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public Set<The> getDanhSachThe() { return danhSachThe; }
    public void setDanhSachThe(Set<The> danhSachThe) { this.danhSachThe = danhSachThe; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
}

