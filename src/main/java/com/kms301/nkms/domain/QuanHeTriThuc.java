package com.kms301.nkms.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "quan_he_tri_thuc")
public class QuanHeTriThuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tu_don_vi_id", nullable = false)
    private DonViTriThuc tuDonVi;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "den_don_vi_id", nullable = false)
    private DonViTriThuc denDonVi;

    @Column(nullable = false, length = 40)
    private String loai; // PHAI_SINH, DONG_THUAN, MAU_THUAN, NGOAI_LE_CUA, TINH_CHINH, TUONG_DUONG

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DonViTriThuc getTuDonVi() { return tuDonVi; }
    public void setTuDonVi(DonViTriThuc tuDonVi) { this.tuDonVi = tuDonVi; }
    public DonViTriThuc getDenDonVi() { return denDonVi; }
    public void setDenDonVi(DonViTriThuc denDonVi) { this.denDonVi = denDonVi; }
    public String getLoai() { return loai; }
    public void setLoai(String loai) { this.loai = loai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}

