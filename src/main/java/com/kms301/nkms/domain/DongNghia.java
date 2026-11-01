package com.kms301.nkms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dong_nghia")
public class DongNghia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "khai_niem_id", nullable = false)
    private KhaiNiem khaiNiem;

    @Column(nullable = false, length = 200)
    private String ten;

    @Column(nullable = false, length = 50)
    private String loai; // DONG_NGHIA, TEN_CU, TEN_MOI, TRUNG_TEN_KHAC_NGHIA

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public KhaiNiem getKhaiNiem() { return khaiNiem; }
    public void setKhaiNiem(KhaiNiem khaiNiem) { this.khaiNiem = khaiNiem; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}

