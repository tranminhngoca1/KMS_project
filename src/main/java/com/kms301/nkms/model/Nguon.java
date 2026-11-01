package com.kms301.nkms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguon")
public class Nguon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ma;

    @Column(name = "tieu_de", nullable = false, length = 255)
    private String tieuDe;

    @Column(name = "tac_gia", nullable = false, length = 200)
    private String tacGia;

    @Column(name = "nam_xuat_ban", length = 50)
    private String namXuatBan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truong_phai_id", nullable = false)
    private TruongPhai truongPhai;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguon_goc_id")
    private Nguon nguonGoc;

    @Column(name = "muc_tin_cay_chung", nullable = false, length = 30)
    private String mucTinCayChung;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "file_tom_tat")
    private String fileTomTat;

    @Column(name = "file_pdf")
    private String filePdf;

    @Column(name = "so_trang_pdf")
    private Integer soTrangPdf;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    /**
     * Trả về mã nguồn gốc để tránh đếm trùng PHILLIPS và QH khi tính đồng thuận (Mục 9, 10).
     */
    public String getRootSourceCode() {
        if (nguonGoc != null) {
            return nguonGoc.getMa();
        }
        return ma;
    }

    public String getTenSach() {
        return tieuDe;
    }

    public String getTyLePhaiSinh() {
        return nguonGoc != null ? "~60% nội dung" : "";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public String getTieuDe() { return tieuDe; }
    public void setTieuDe(String tieuDe) { this.tieuDe = tieuDe; }
    public String getTacGia() { return tacGia; }
    public void setTacGia(String tacGia) { this.tacGia = tacGia; }
    public String getNamXuatBan() { return namXuatBan; }
    public void setNamXuatBan(String namXuatBan) { this.namXuatBan = namXuatBan; }
    public TruongPhai getTruongPhai() { return truongPhai; }
    public void setTruongPhai(TruongPhai truongPhai) { this.truongPhai = truongPhai; }
    public Nguon getNguonGoc() { return nguonGoc; }
    public void setNguonGoc(Nguon nguonGoc) { this.nguonGoc = nguonGoc; }
    public String getMucTinCayChung() { return mucTinCayChung; }
    public void setMucTinCayChung(String mucTinCayChung) { this.mucTinCayChung = mucTinCayChung; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public String getFileTomTat() { return fileTomTat; }
    public void setFileTomTat(String fileTomTat) { this.fileTomTat = fileTomTat; }
    public String getFilePdf() { return filePdf; }
    public void setFilePdf(String filePdf) { this.filePdf = filePdf; }
    public Integer getSoTrangPdf() { return soTrangPdf; }
    public void setSoTrangPdf(Integer soTrangPdf) { this.soTrangPdf = soTrangPdf; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
}
