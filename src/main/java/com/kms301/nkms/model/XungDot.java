package com.kms301.nkms.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "xung_dot")
public class XungDot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ma;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "khai_niem_id")
    private KhaiNiem khaiNiem;

    @Column(name = "mo_ta", nullable = false, columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "cach_xu_ly", nullable = false, columnDefinition = "TEXT")
    private String cachXuLy;

    @Column(name = "trang_thai", nullable = false, length = 40)
    private String trangThai; // DA_XU_LY, CHO_XAC_MINH

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "xung_dot_don_vi",
            joinColumns = @JoinColumn(name = "xung_dot_id"),
            inverseJoinColumns = @JoinColumn(name = "don_vi_id")
    )
    private Set<DonViTriThuc> danhSachDonVi = new LinkedHashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public KhaiNiem getKhaiNiem() { return khaiNiem; }
    public void setKhaiNiem(KhaiNiem khaiNiem) { this.khaiNiem = khaiNiem; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public String getCachXuLy() { return cachXuLy; }
    public void setCachXuLy(String cachXuLy) { this.cachXuLy = cachXuLy; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public Set<DonViTriThuc> getDanhSachDonVi() { return danhSachDonVi; }
    public void setDanhSachDonVi(Set<DonViTriThuc> danhSachDonVi) { this.danhSachDonVi = danhSachDonVi; }
}

