package com.kms301.nkms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "the")
public class The {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String ma;

    @Column(nullable = false, length = 150)
    private String ten;

    @Column(nullable = false, length = 40)
    private String nhom; // TINH_CACH, NGHE_NGHIEP, THOI_DIEM, QUAN_HE

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMa() { return ma; }
    public void setMa(String ma) { this.ma = ma; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
    public String getNhom() { return nhom; }
    public void setNhom(String nhom) { this.nhom = nhom; }
}

