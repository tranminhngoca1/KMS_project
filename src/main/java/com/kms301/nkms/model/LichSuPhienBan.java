package com.kms301.nkms.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_phien_ban")
public class LichSuPhienBan {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "don_vi_id", nullable = false)
    private DonViTriThuc donVi;

    @Column(name = "phien_ban", nullable = false)
    private Integer phienBan;

    @Column(name = "noi_dung_cu_json", nullable = false, columnDefinition = "JSON")
    private String noiDungCuJson = "{}";

    @Column(name = "nguoi_sua", nullable = false, length = 100)
    private String nguoiSua;

    @Column(name = "ly_do", nullable = false, columnDefinition = "TEXT")
    private String lyDo;

    @Column(name = "thoi_diem", insertable = false, updatable = false)
    private LocalDateTime thoiDiem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DonViTriThuc getDonVi() { return donVi; }
    public void setDonVi(DonViTriThuc donVi) { this.donVi = donVi; }
    public DonViTriThuc getDonViTriThuc() { return donVi; }
    public void setDonViTriThuc(DonViTriThuc donVi) { this.donVi = donVi; }
    public Integer getPhienBan() { return phienBan; }
    public void setPhienBan(Integer phienBan) { this.phienBan = phienBan; }
    public String getNoiDungCuJson() { return noiDungCuJson; }
    public void setNoiDungCuJson(String noiDungCuJson) { this.noiDungCuJson = noiDungCuJson; }
    public String getNguoiSua() { return nguoiSua; }
    public void setNguoiSua(String nguoiSua) { this.nguoiSua = nguoiSua; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    public LocalDateTime getThoiDiem() { return thoiDiem; }
    public void setThoiDiem(LocalDateTime thoiDiem) { this.thoiDiem = thoiDiem; }

    public void setSnapshot(String noiDungCu, String nhanCu, String mucTinCayCu) {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("noi_dung_cu", noiDungCu != null ? noiDungCu : "");
            node.put("nhan_su_dung_cu", nhanCu != null ? nhanCu : "");
            node.put("muc_tin_cay_cu", mucTinCayCu != null ? mucTinCayCu : "");
            this.noiDungCuJson = MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            this.noiDungCuJson = "{}";
        }
    }

    public String getNoiDungCu() {
        return readJsonField("noi_dung_cu", "tom_tat_truoc_khi_sua", noiDungCuJson);
    }

    public String getNhanCu() {
        return readJsonField("nhan_su_dung_cu", "nhan_cu", "DUNG");
    }

    public String getMucTinCayCu() {
        return readJsonField("muc_tin_cay_cu", "tin_cay_cu", "TRUNG_BINH");
    }

    private String readJsonField(String key1, String key2, String fallback) {
        if (noiDungCuJson == null || noiDungCuJson.isBlank()) return fallback;
        try {
            JsonNode node = MAPPER.readTree(noiDungCuJson);
            if (node.has(key1) && !node.get(key1).isNull()) return node.get(key1).asText();
            if (node.has(key2) && !node.get(key2).isNull()) return node.get(key2).asText();
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
