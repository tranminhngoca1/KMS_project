package com.kms301.nkms.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object (DTO) đại diện cho form dữ liệu Khám phá (Model trong mô hình MVC).
 * Tuân thủ bảo mật và quyền riêng tư (Mục 2 Prompt): DTO chỉ tồn tại trong vòng đời request,
 * không lưu trữ họ tên hay ngày sinh người dùng xuống CSDL.
 */
public class KhamPhaRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Ngày sinh không được để trống")
    @Pattern(regexp = "^\\d{2}/\\d{2}/\\d{4}$", message = "Ngày sinh phải theo định dạng dd/MM/yyyy")
    private String ngaySinh;

    public KhamPhaRequest() {
    }

    public KhamPhaRequest(String hoTen, String ngaySinh) {
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(String ngaySinh) {
        this.ngaySinh = ngaySinh;
    }
}
