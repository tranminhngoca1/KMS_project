-- =====================================================================
-- V1__schema.sql — Lược đồ cơ sở dữ liệu Hệ thống Quản lý Tri thức Thần số học (NKMS)
-- MySQL 8 / TiDB compatible, charset utf8mb4, collation utf8mb4_unicode_ci
-- =====================================================================

CREATE TABLE IF NOT EXISTS truong_phai (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    ten VARCHAR(150) NOT NULL,
    mo_ta TEXT,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS nguon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(50) NOT NULL UNIQUE,
    tieu_de VARCHAR(255) NOT NULL,
    tac_gia VARCHAR(200) NOT NULL,
    nam_xuat_ban VARCHAR(50),
    truong_phai_id BIGINT NOT NULL,
    nguon_goc_id BIGINT NULL,
    muc_tin_cay_chung ENUM('CAO','TRUNG_BINH','THAP') NOT NULL DEFAULT 'TRUNG_BINH',
    ghi_chu TEXT,
    file_tom_tat VARCHAR(255),
    file_pdf VARCHAR(255),
    so_trang_pdf INT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_nguon_truong_phai FOREIGN KEY (truong_phai_id) REFERENCES truong_phai(id),
    CONSTRAINT fk_nguon_goc FOREIGN KEY (nguon_goc_id) REFERENCES nguon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS khai_niem (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(100) NOT NULL UNIQUE,
    ten_vi VARCHAR(200) NOT NULL,
    ten_goc VARCHAR(200),
    truong_phai_id BIGINT NULL,
    mo_ta TEXT,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_khai_niem_truong_phai FOREIGN KEY (truong_phai_id) REFERENCES truong_phai(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dong_nghia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    khai_niem_id BIGINT NOT NULL,
    ten VARCHAR(200) NOT NULL,
    loai ENUM('DONG_NGHIA','TEN_CU','TEN_MOI','TRUNG_TEN_KHAC_NGHIA') NOT NULL,
    ghi_chu TEXT,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dong_nghia_khai_niem FOREIGN KEY (khai_niem_id) REFERENCES khai_niem(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cong_thuc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(100) NOT NULL UNIQUE,
    khai_niem_id BIGINT NOT NULL,
    mo_ta_cac_buoc TEXT NOT NULL,
    ma_ham_java VARCHAR(150),
    tap_gia_tri VARCHAR(150),
    nguon_id BIGINT NOT NULL,
    chuong VARCHAR(100),
    trang_tu INT NULL,
    trang_den INT NULL,
    trang_thai VARCHAR(50) NOT NULL DEFAULT 'KICH_HOAT',
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cong_thuc_khai_niem FOREIGN KEY (khai_niem_id) REFERENCES khai_niem(id),
    CONSTRAINT fk_cong_thuc_nguon FOREIGN KEY (nguon_id) REFERENCES nguon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bang_chu_cai (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(100) NOT NULL UNIQUE,
    truong_phai_id BIGINT NOT NULL,
    nguon_id BIGINT NOT NULL,
    trang_thai ENUM('KICH_HOAT','CHUA_CO_DU_LIEU','KHONG_DUNG') NOT NULL,
    ghi_chu TEXT,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bcc_truong_phai FOREIGN KEY (truong_phai_id) REFERENCES truong_phai(id),
    CONSTRAINT fk_bcc_nguon FOREIGN KEY (nguon_id) REFERENCES nguon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bang_chu_cai_gia_tri (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bang_chu_cai_id BIGINT NOT NULL,
    ky_tu VARCHAR(20) NOT NULL,
    gia_tri INT NOT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bccgt_bcc FOREIGN KEY (bang_chu_cai_id) REFERENCES bang_chu_cai(id),
    UNIQUE KEY uk_bcc_ky_tu (bang_chu_cai_id, ky_tu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS don_vi_tri_thuc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(100) NOT NULL UNIQUE,
    tieu_de VARCHAR(300) NOT NULL,
    loai ENUM('QUY_TAC','MO_TA','CONG_THUC','BANG_TRA','CA_THUC_TE','LICH_SU') NOT NULL,
    khai_niem_id BIGINT NULL,
    truong_phai_id BIGINT NOT NULL,
    gia_tri_kich_hoat VARCHAR(100) NULL,
    dieu_kien_json JSON NULL,
    noi_dung TEXT NOT NULL,
    cau_hoi ENUM('AI','GAP_GI','DI_THE_NAO','NEN_TANG') NOT NULL,
    nguon_id BIGINT NOT NULL,
    chuong VARCHAR(100) NULL,
    trang_tu INT NULL,
    trang_den INT NULL,
    muc_tin_cay ENUM('CAO','TRUNG_BINH','THAP') NOT NULL DEFAULT 'TRUNG_BINH',
    nhan_su_dung ENUM('DUNG','GOI_Y_VAN_HOA','THAN_TRONG','KHONG_DUNG') NOT NULL DEFAULT 'DUNG',
    ly_do_nhan TEXT NULL,
    phien_ban INT NOT NULL DEFAULT 1,
    trang_thai ENUM('NHAP','DA_DUYET','LUU_TRU') NOT NULL DEFAULT 'DA_DUYET',
    nguoi_tao VARCHAR(100) NOT NULL DEFAULT 'system',
    nguoi_duyet VARCHAR(100) NULL,
    ghi_chu VARCHAR(255) NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dvtt_khai_niem FOREIGN KEY (khai_niem_id) REFERENCES khai_niem(id),
    CONSTRAINT fk_dvtt_truong_phai FOREIGN KEY (truong_phai_id) REFERENCES truong_phai(id),
    CONSTRAINT fk_dvtt_nguon FOREIGN KEY (nguon_id) REFERENCES nguon(id),
    INDEX idx_dvtt_truong_phai (truong_phai_id),
    INDEX idx_dvtt_nguon (nguon_id),
    INDEX idx_dvtt_khai_niem (khai_niem_id),
    INDEX idx_dvtt_cau_hoi (cau_hoi),
    INDEX idx_dvtt_nhan_su_dung (nhan_su_dung),
    INDEX idx_dvtt_trang_thai (trang_thai),
    INDEX idx_dvtt_kich_hoat (gia_tri_kich_hoat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS the (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(100) NOT NULL UNIQUE,
    ten VARCHAR(150) NOT NULL,
    nhom ENUM('TINH_CACH','NGHE_NGHIEP','THOI_DIEM','QUAN_HE') NOT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS don_vi_the (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    don_vi_id BIGINT NOT NULL,
    the_id BIGINT NOT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dvt_don_vi FOREIGN KEY (don_vi_id) REFERENCES don_vi_tri_thuc(id) ON DELETE CASCADE,
    CONSTRAINT fk_dvt_the FOREIGN KEY (the_id) REFERENCES the(id) ON DELETE CASCADE,
    UNIQUE KEY uk_don_vi_the (don_vi_id, the_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cap_the_doi_lap (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    the_a_id BIGINT NOT NULL,
    the_b_id BIGINT NOT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ctdl_a FOREIGN KEY (the_a_id) REFERENCES the(id) ON DELETE CASCADE,
    CONSTRAINT fk_ctdl_b FOREIGN KEY (the_b_id) REFERENCES the(id) ON DELETE CASCADE,
    UNIQUE KEY uk_cap_the_doi_lap (the_a_id, the_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS quan_he_tri_thuc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tu_don_vi_id BIGINT NOT NULL,
    den_don_vi_id BIGINT NOT NULL,
    loai ENUM('PHAI_SINH','DONG_THUAN','MAU_THUAN','NGOAI_LE_CUA','TINH_CHINH','TUONG_DUONG') NOT NULL,
    ghi_chu TEXT,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_qhtt_tu FOREIGN KEY (tu_don_vi_id) REFERENCES don_vi_tri_thuc(id) ON DELETE CASCADE,
    CONSTRAINT fk_qhtt_den FOREIGN KEY (den_don_vi_id) REFERENCES don_vi_tri_thuc(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS xung_dot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(100) NOT NULL UNIQUE,
    khai_niem_id BIGINT NULL,
    mo_ta TEXT NOT NULL,
    cach_xu_ly TEXT NOT NULL,
    trang_thai ENUM('DA_XU_LY','CHO_XAC_MINH') NOT NULL DEFAULT 'DA_XU_LY',
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_xd_khai_niem FOREIGN KEY (khai_niem_id) REFERENCES khai_niem(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS xung_dot_don_vi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    xung_dot_id BIGINT NOT NULL,
    don_vi_id BIGINT NOT NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_xddv_xung_dot FOREIGN KEY (xung_dot_id) REFERENCES xung_dot(id) ON DELETE CASCADE,
    CONSTRAINT fk_xddv_don_vi FOREIGN KEY (don_vi_id) REFERENCES don_vi_tri_thuc(id) ON DELETE CASCADE,
    UNIQUE KEY uk_xung_dot_don_vi (xung_dot_id, don_vi_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lich_su_phien_ban (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    don_vi_id BIGINT NOT NULL,
    phien_ban INT NOT NULL,
    noi_dung_cu_json JSON NOT NULL,
    nguoi_sua VARCHAR(100) NOT NULL,
    ly_do TEXT NOT NULL,
    thoi_diem DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lspb_don_vi FOREIGN KEY (don_vi_id) REFERENCES don_vi_tri_thuc(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
