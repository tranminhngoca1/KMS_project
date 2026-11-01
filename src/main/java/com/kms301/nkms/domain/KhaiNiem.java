package com.kms301.nkms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "khai_niem")
public class KhaiNiem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ma;

    @Column(name = "ten_vi", nullable = false, length = 200)
    private String tenVi;

    @Column(name = "ten_goc", length = 200)
    private String tenGoc;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "truong_phai_id")
    private TruongPhai truongPhai;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", insertable = false, updatable = false)
    private LocalDateTime ngayCapNhat;

    public String getTenChuan() {
        return tenVi;
    }

    public String getNhom() {
        if (truongPhai != null) return truongPhai.getMa();
        return "LIEN_TRUONG_PHAI";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public String getTenVi() { return tenVi; }
    public void setTenVi(String tenVi) { this.tenVi = tenVi; }
    public String getTenGoc() { return tenGoc; }
    public void setTenGoc(String tenGoc) { this.tenGoc = tenGoc; }
    public TruongPhai getTruongPhai() { return truongPhai; }
    public void setTruongPhai(TruongPhai truongPhai) { this.truongPhai = truongPhai; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
}
