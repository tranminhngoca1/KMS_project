# Hệ thống Quản lý Tri thức Thần số học (NKMS — KMS301)

Dự án học phần **KMS301** — Hệ thống Quản lý Tri thức (Knowledge Management System) đa trường phái về Thần số học.
Hệ thống thu nhận, mã hoá, lưu trữ, truy vết nguồn gốc (provenance), quản trị xung đột tri thức, quản trị chất lượng (nhãn tin cậy, nhãn sử dụng), quản trị phiên bản và ứng dụng suy diễn đối chiếu song song trên **3 trường phái**:
1. **Pythagoras phương Tây (`PYTHAGORAS`)**: David A. Phillips (*The Complete Book of Numerology*, 1980/2005), Lê Đỗ Quỳnh Hương (*Thay Đổi Cuộc Sống Với Nhân Số Học* — phái sinh ~60% từ Phillips), và biến thể nhánh Mỹ của Hans Decoz (*Numerology – Key to Your Inner Self*, 1994/2002).
2. **Ấn Độ / Vệ Đà (`VEDA`)**: Harish Johari (*Numerology – With Tantra, Ayurveda and Astrology*, 1990/2012).
3. **Kabbalah / Chaldean (`KABBALAH`)**: Sepharial – W. G. Old (*The Kabala of Numbers*, 1911).

> **Lưu ý phạm vi nguồn (Mục 1)**: Hệ thống **không sử dụng** *Brihat Parashara Hora Shastra (BPHS, tập 1 & 2)* và *Sefer Yetzirah* vì đây là chiêm tinh Vệ Đà (Jyotish) và triết lý vũ trụ luận Kabbalah, không phải hệ thống thần số học tra cứu theo họ tên và ngày sinh.

---

## 1. Công nghệ & Kiến trúc

- **Ngôn ngữ & Framework**: Java 17, Spring Boot 3.2.10 (Spring Web, Spring Data JPA, Validation, Spring Security).
- **Giao diện (View)**: Thymeleaf + HTML5/CSS3/JS thuần (giao diện tiếng Việt, responsive trên máy tính và điện thoại).
- **Cơ sở dữ liệu**: MySQL 8 / TiDB Cloud (`utf8mb4` / `utf8mb4_unicode_ci`), quản lý schema và dữ liệu bằng **Flyway** (`V1__schema.sql` → `V7__seed_quan_he_va_xung_dot.sql`).
- **Kiểm thử tự động**: JUnit 5 (`NumerologyEngineTest` và `InferenceEngineTest`).

### Cấu trúc dự án theo mô hình MVC (`com.kms301.nkms`)
```text
com.kms301.nkms
 ├─ controller/      [CONTROLLER] Điều hướng HTTP requests & trả về Views
 │   ├─ KhamPhaController     (Khám phá 3 trường phái, 3 câu hỏi)
 │   ├─ KhoController          (Quản trị kho tri thức, duyệt, thêm, sửa)
 │   └─ TrangPhuController     (Nguồn, xung đột, khái niệm, thống kê)
 ├─ model/           [MODEL] Thực thể dữ liệu & đối tượng truyền nhận
 │   ├─ entity/                (13 JPA Entities: TruongPhai, Nguon, DonViTriThuc, QuanHeTriThuc, XungDot, ...)
 │   └─ dto/                   (KhamPhaRequest - form DTO)
 ├─ service/         [SERVICE] Xử lý nghiệp vụ logic (KhamPhaService, KhoTriThucService, ThongKeService)
 ├─ repository/      [REPOSITORY/DAO] Tầng truy xuất dữ liệu Spring Data JPA
 ├─ engine/          [CALCULATION ENGINE] Bộ máy tính toán Java thuần (Pythagoras, Vệ Đà, Kabbalah)
 ├─ inference/       [KNOWLEDGE REASONING] Bộ máy suy luận tri thức, khớp luật & tổng hợp Đồng thuận / Khác biệt
 ├─ config/          [CONFIG] Cấu hình bảo mật Spring Security & hệ thống
 └─ resources/
     ├─ templates/   [VIEW] Giao diện Thymeleaf HTML (kham-pha, kho-*, nguon, xung-dot...)
     ├─ static/      [ASSETS] CSS, JS, hình ảnh
     └─ db/migration [DATABASE] Kịch bản Flyway V1 -> V7
```

---

## 2. Hướng dẫn chạy ứng dụng

### Yêu cầu môi trường
- **JDK 17** trở lên.
- Dự án đã tích hợp sẵn **Maven Wrapper** (`.\mvnw.cmd` trên Windows / `./mvnw` trên macOS & Linux).
- Kết nối CSDL đã được cấu hình sẵn trong `src/main/resources/application.yml` trỏ tới database `nkms` trên TiDB Cloud (MySQL 8 compatible).

### Chạy bộ kiểm thử tự động (JUnit 5)
```powershell
.\mvnw.cmd test
```

### Khởi chạy ứng dụng Web (Spring Boot)
```powershell
.\mvnw.cmd spring-boot:run
```
Sau khi khởi động thành công, mở trình duyệt tại:
- **Trang Khám phá (công khai)**: `http://localhost:8080/kham-pha`
- **Kho tri thức**: `http://localhost:8080/kho`
- **Nguồn sách & Quan hệ phái sinh**: `http://localhost:8080/nguon`
- **Hồ sơ 14 Xung đột tri thức**: `http://localhost:8080/xung-dot`
- **Từ điển Khái niệm, Đồng nghĩa & Công thức**: `http://localhost:8080/khai-niem`
- **Thống kê KMS**: `http://localhost:8080/thong-ke`
- **Giới thiệu phương pháp KMS**: `http://localhost:8080/gioi-thieu`

---

## 3. Tài khoản Quản trị (Admin)

Trang Khám phá (`/kham-pha`) và việc xem/tra cứu Kho tri thức (`/kho`, `/kho/{id}`) hoàn toàn công khai, không cần đăng nhập.
Các thao tác **Thêm mới**, **Chỉnh sửa (nâng phiên bản)**, **Duyệt (`NHAP -> DA_DUYET`)**, **Lưu trữ (`-> LUU_TRU`)** và **Nhập bảng chữ cái Unit System** yêu cầu đăng nhập tại `http://localhost:8080/dang-nhap`:

- **Tài khoản**: `admin`
- **Mật khẩu**: `admin123`
*(Có thể thay đổi trong `src/main/resources/application.yml` tại mục `nkms.admin.username` và `nkms.admin.password`).*

---

## 4. Hướng dẫn thêm & quản trị tri thức mới

1. Đăng nhập với tài khoản quản trị (`admin` / `admin123`).
2. Truy cập **Kho tri thức** (`/kho`) → bấm nút **➕ Thêm đơn vị tri thức** (`/kho/them`).
3. Điền đầy đủ thông tin:
   - **Mã đơn vị** (ví dụ: `PH-CD-10`), **Tiêu đề**, **Trường phái**, **Nguồn sách**, **Khái niệm chuẩn**.
   - **Vị trí trích dẫn (Provenance)**: Chương (`Ch.7`, `§6`...) và số trang (`trang_tu`, `trang_den`). Nếu chưa tra được số trang, để trống để hệ thống hiển thị minh bạch `tr.?`.
   - **Điều kiện kích hoạt (`dieu_kien_json`)**: Ví dụ `{"chi_so":"QH_CHU_DAO","bang":"10"}` hoặc điều kiện tổ hợp `{"tat_ca":[...]}`.
   - **Mức tin cậy (`CAO`, `TRUNG_BINH`, `THAP`)** và **Nhãn sử dụng (`DUNG`, `GOI_Y_VAN_HOA`, `THAN_TRONG`, `KHONG_DUNG`)**.
     - *Lưu ý*: Các đơn vị gắn nhãn `THAN_TRONG` hoặc `KHONG_DUNG` chỉ lưu trong Kho để nghiên cứu quản trị tri thức, **tuyệt đối không bao giờ hiển thị ở trang Khám phá**.
4. Khi chỉnh sửa một đơn vị hiện có (`/kho/{id}/sua`), hệ thống yêu cầu nhập **Lý do chỉnh sửa**, tự động tăng `phien_ban` (`v1 -> v2...`) và lưu bản chụp nội dung cũ vào bảng `lich_su_phien_ban`.
5. Để kích hoạt tính **Số tên (Name Number)** theo trường phái Vệ Đà của Harish Johari: truy cập `/kho/bang-chu-cai/unit-system`, nhập 26 giá trị số (1–9) cho các chữ cái A–Z từ sách in và bấm Lưu. Hệ thống sẽ tự động chuyển trạng thái bảng `UNIT_SYSTEM` từ `CHUA_CO_DU_LIEU` sang `KICH_HOAT`.

---

## 5. Danh sách các mục "CẦN XÁC MINH" & Kết quả đối chiếu PDF gốc

| STT | Hạng mục | Kết quả đối chiếu PDF gốc (`tai-lieu/pdf/`) | Trạng thái trong hệ thống |
|---|---|---|---|
| 1 | **Bảng chữ cái "Unit System" của Harish Johari** (Mục 5) | Trong file `01-johari-numerology.pdf` tại Chương 1, mục "Name Number" (trang 10 sách / trang 8 PDF), hình ảnh bảng quy đổi chữ cái sang số bị khuyết trong bản scan PDF. Hệ thống tuân thủ nghiêm ngặt quy tắc không tự lấy bảng Chaldean/Cheiro từ internet để điền thay. | **Đang mở (`CHUA_CO_DU_LIEU`)**: Ô Số tên Johari ở Khám phá hiển thị *"Chưa có dữ liệu bảng chữ cái từ nguồn"*. Đã cung cấp form quản trị 26 chữ cái A–Z tại `/kho/bang-chu-cai/unit-system` để nhập từ bản sách in và tự động kích hoạt. |
| 2 | **Số chìa khoá (Key Number) ở Chương XIV của Sepharial** (Mục 6.4 & Xung đột #12) | Đã tra cứu `08-sepharial-kabala-of-numbers.pdf`: Chương IV (tr.43) quy định rõ bỏ 2 chữ số thế kỷ của năm sinh khi lập biểu đồ và tính tổng; tuy nhiên Chương XIV (tr.140 / PDF tr.151) chỉ viết *"adding together the day, month, and year of birth"* mà không nói rõ có bỏ chữ số thế kỷ hay không. | **Đang mở (`CHO_XAC_MINH` — Xung đột `XD-12-SEPHARIAL-CHIA-KHOA`)**: Bộ máy mặc định áp dụng quy tắc bỏ 2 chữ số thế kỷ của Ch.IV để nhất quán, đồng thời hiển thị cả giá trị nếu giữ đủ 4 chữ số năm ngay trên giao diện Khám phá. |
| 3 | **Bảng nghề nghiệp theo Số chìa khoá của Sepharial** (Mục 6.4 & Xung đột #13) | Đã tra cứu `08-sepharial-kabala-of-numbers.pdf` Chương XIV (tr.139 / PDF tr.150): tác giả chỉ nêu ví dụ định hướng nghề nghiệp cho 3 con số là **3** (thần học/triết học), **5** (thương mại) và **9** (điều hành/quản trị), không lập bảng đủ cho các số 1, 2, 4, 6, 7, 8. | **Đã xác minh & Ghi nhận giới hạn nguồn (`XD-13-SEPHARIAL-NGHE-NGHIEP`)**: Chỉ tạo quy tắc nghề nghiệp Ch.XIV cho số 3, 5, 9; các số còn lại hiển thị ghi chú rõ ràng rằng nguồn gốc không lập bảng đầy đủ. |
| 4 | **Công thức Số Nhân cách (Personality Number) của Hans Decoz** (Mục 6.2 & 8.1) | Đã tra cứu `04-decoz-key-to-inner-self.pdf` Chương 3 (trang in 102–111 / PDF tr.118–127): xác minh công thức chuẩn ở trang 103 là cộng phụ âm từng phần tên với `reduceKeepMaster` rồi cộng các kết quả lại với `reduceKeepMaster`. | **Đã xác minh xong từ PDF**: Đã cài đặt trong `DecozCalculator` và bổ sung đầy đủ các đơn vị tri thức Nhân cách (1–9, 11, 22). |
| 5 | **Công thức Tháng cá nhân (Personal Month)** (Mục 6.1 & 6.2) | Đã đối chiếu `03-phillips-complete-book.pdf` Chương 9 ("Monthly Variations", trang in 127–168 / PDF tr.107–129) và `04-decoz-key-to-inner-self.pdf` Chương 6 (trang in 188–192 / PDF tr.204–208). | **Đã xác minh xong từ PDF**: Đã cài đặt công thức `reduceTo9(Năm cá nhân + Tháng hiện tại)` kèm trích dẫn trang đầy đủ. |

