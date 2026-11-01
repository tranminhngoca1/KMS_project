package com.kms301.nkms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bang_chu_cai")
public class BangChuCai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ma;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truong_phai_id", nullable = false)
    private TruongPhai truongPhai;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguon_id", nullable = false)
    private Nguon nguon;

    @Column(name = "trang_thai", nullable = false, length = 40)
    private String trangThai; // KICH_HOAT, CHUA_CO_DU_LIEU, KHONG_DUNG

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public TruongPhai getTruongPhai() { return truongPhai; }
    public void setTruongPhai(TruongPhai truongPhai) { this.truongPhai = truongPhai; }
    public Nguon getNguon() { return nguon; }
    public void setNguon(Nguon nguon) { this.nguon = nguon; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}

