package com.kms301.nkms.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "bang_chu_cai_gia_tri")
public class BangChuCaiGiaTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bang_chu_cai_id", nullable = false)
    private BangChuCai bangChuCai;

    @Column(name = "ky_tu", nullable = false, length = 20)
    private String kyTu;

    @Column(name = "gia_tri", nullable = false)
    private Integer giaTri;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BangChuCai getBangChuCai() { return bangChuCai; }
    public void setBangChuCai(BangChuCai bangChuCai) { this.bangChuCai = bangChuCai; }
    public String getKyTu() { return kyTu; }
    public void setKyTu(String kyTu) { this.kyTu = kyTu; }
    public String getChuCai() { return kyTu; }
    public void setChuCai(String chuCai) { this.kyTu = chuCai; }
    public Integer getGiaTri() { return giaTri; }
    public void setGiaTri(Integer giaTri) { this.giaTri = giaTri; }
}
