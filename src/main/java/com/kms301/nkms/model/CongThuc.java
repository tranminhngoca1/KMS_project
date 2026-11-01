package com.kms301.nkms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cong_thuc")
public class CongThuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ma;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "khai_niem_id", nullable = false)
    private KhaiNiem khaiNiem;

    @Column(name = "mo_ta_cac_buoc", nullable = false, columnDefinition = "TEXT")
    private String moTaCacBuoc;

    @Column(name = "ma_ham_java", length = 150)
    private String maHamJava;

    @Column(name = "tap_gia_tri", length = 150)
    private String tapGiaTri;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguon_id", nullable = false)
    private Nguon nguon;

    @Column(length = 100)
    private String chuong;

    @Column(name = "trang_tu")
    private Integer trangTu;

    @Column(name = "trang_den")
    private Integer trangDen;

    @Column(name = "trang_thai", nullable = false, length = 50)
    private String trangThai;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public KhaiNiem getKhaiNiem() { return khaiNiem; }
    public void setKhaiNiem(KhaiNiem khaiNiem) { this.khaiNiem = khaiNiem; }
    public String getMoTaCacBuoc() { return moTaCacBuoc; }
    public void setMoTaCacBuoc(String moTaCacBuoc) { this.moTaCacBuoc = moTaCacBuoc; }
    public String getMaHamJava() { return maHamJava; }
    public void setMaHamJava(String maHamJava) { this.maHamJava = maHamJava; }
    public String getTapGiaTri() { return tapGiaTri; }
    public void setTapGiaTri(String tapGiaTri) { this.tapGiaTri = tapGiaTri; }
    public Nguon getNguon() { return nguon; }
    public void setNguon(Nguon nguon) { this.nguon = nguon; }
    public String getChuong() { return chuong; }
    public void setChuong(String chuong) { this.chuong = chuong; }
    public Integer getTrangTu() { return trangTu; }
    public void setTrangTu(Integer trangTu) { this.trangTu = trangTu; }
    public Integer getTrangDen() { return trangDen; }
    public void setTrangDen(Integer trangDen) { this.trangDen = trangDen; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}

