-- =====================================================================
-- V2__seed_nguon_va_nen_tang.sql
-- Seed Trường phái, Nguồn, Khái niệm, Đồng nghĩa, Bảng chữ cái, Công thức, Thẻ, Cặp thẻ đối lập, Xung đột
-- =====================================================================

-- 1. BA TRƯỜNG PHÁI
INSERT INTO truong_phai (id, ma, ten, mo_ta) VALUES
(1, 'PYTHAGORAS', 'Pythagoras phương Tây', 'Trường phái thần số học phương Tây gắn với Pythagoras, bao gồm nhánh David A. Phillips / Lê Đỗ Quỳnh Hương và nhánh Mỹ của Hans Decoz.'),
(2, 'VEDA', 'Ấn Độ / Vệ Đà (Johari)', 'Trường phái số học Ấn Độ kết hợp Tantra, Ayurveda và Chiêm tinh Vệ Đà theo Harish Johari (chỉ dùng các số đơn 1–9 và số kép 10–31, không có số bậc thầy).'),
(3, 'KABBALAH', 'Kabbalah / Chaldean (Sepharial)', 'Trường phái Kabbalah phương Tây đầu thế kỷ 20 theo Sepharial (W. G. Old), sử dụng biểu đồ Ma phương 3×3 ba tầng và Số chìa khoá.');

-- 2. NĂM NGUỒN TRI THỨC (Bỏ BPHS và Sefer Yetzirah theo quy định Mục 1)
INSERT INTO nguon (id, ma, tieu_de, tac_gia, nam_xuat_ban, truong_phai_id, nguon_goc_id, muc_tin_cay_chung, ghi_chu, file_tom_tat, file_pdf, so_trang_pdf) VALUES
(1, 'PHILLIPS', 'The Complete Book of Numerology', 'David A. Phillips', '1980/2005', 1, NULL, 'TRUNG_BINH', 'Bản gốc nhánh Pythagoras hiện đại (kế thừa Hettie Templeton). Bản PDF là bản chuyển đổi từ ebook (205 trang PDF, mục lục ghi số trang sách in đến tr.271).', 'tai-lieu/tom-tat/03-phillips-complete-book.md', 'tai-lieu/pdf/03-phillips-complete-book.pdf', 205),
(2, 'QH', 'Thay Đổi Cuộc Sống Với Nhân Số Học', 'Lê Đỗ Quỳnh Hương', '2020', 1, 1, 'TRUNG_BINH', 'Phái sinh từ PHILLIPS (~60% nội dung cốt lõi), Việt hoá thuật ngữ và bổ sung nghiên cứu thực nghiệm trên tên tiếng Việt (Phần 12). Khi tính đồng thuận, PHILLIPS và QH được tính chung một nguồn gốc.', 'tai-lieu/tom-tat/02-than-so-hoc-quynh-huong.md', 'tai-lieu/pdf/02-than-so-hoc-quynh-huong.pdf', 362),
(3, 'DECOZ', 'Numerology – Key to Your Inner Self', 'Hans Decoz & Tom Monte', '1994/2002', 1, NULL, 'TRUNG_BINH', 'Nhánh Pythagoras Mỹ: rút gọn từng phần ngày/tháng/năm và từng phần tên, giữ số bậc thầy 11/22, có hệ thống Số Nợ nghiệp (13, 14, 16, 19).', 'tai-lieu/tom-tat/04-decoz-key-to-inner-self.md', 'tai-lieu/pdf/04-decoz-key-to-inner-self.pdf', 292),
(4, 'JOHARI', 'Numerology – With Tantra, Ayurveda and Astrology', 'Harish Johari', '1990/2012', 2, NULL, 'THAP', 'Trường phái Ấn Độ / Vệ Đà. Bản PDF dàn trang lại (121 trang PDF; ghi chú trang PDF). Các nội dung về đá quý, thần chú, nhịn ăn gắn nhãn THAN_TRONG; bệnh lý Ayurveda gắn nhãn KHONG_DUNG.', 'tai-lieu/tom-tat/01-johari-numerology.md', 'tai-lieu/pdf/01-johari-numerology.pdf', 121),
(5, 'SEPHARIAL', 'The Kabala of Numbers', 'Sepharial (W. G. Old)', '1911', 3, NULL, 'THAP', 'Tác phẩm thuộc phạm vi công cộng (Public Domain, 1911). Các chương mang tính bói toán (Tarot theo năm, xổ số, đồ thất lạc, đọc ý nghĩ, giá cổ phiếu) gắn nhãn KHONG_DUNG và loại LICH_SU.', 'tai-lieu/tom-tat/08-sepharial-kabala-of-numbers.md', 'tai-lieu/pdf/08-sepharial-kabala-of-numbers.pdf', 181);

-- 3. KHÁI NIỆM (30 khái niệm cốt lõi)
INSERT INTO khai_niem (id, ma, ten_vi, ten_goc, truong_phai_id, mo_ta) VALUES
(1, 'CON_SO_CHU_DAO', 'Con số chủ đạo', 'Ruling Number', 1, 'Chỉ số quan trọng nhất trong nhánh QH/Phillips, tính bằng cách cộng gộp tất cả chữ số ngày, tháng, năm sinh rồi rút gọn về khoảng 2–11 hoặc 22/4 (không có số 1).'),
(2, 'CON_SO_NGAY_SINH', 'Con số ngày sinh', 'Day Number', 1, 'Chỉ số phản ánh năng lực tự nhiên từ ngày sinh trong tháng (1–11 giữ nguyên, 22 giữ 22/4, các ngày khác cộng chữ số).'),
(3, 'BIEU_DO_NGAY_SINH', 'Biểu đồ ngày sinh', 'Birth Chart', 1, 'Lưới 3×3 cố định (hàng dưới 1-4-7 Thể chất, hàng giữa 2-5-8 Tâm hồn, hàng trên 3-6-9 Trí não) điền các chữ số khác 0 của ngày tháng năm sinh.'),
(4, 'MUI_TEN_CA_TINH', 'Mũi tên cá tính', 'Arrows of Individuality', 1, '15 tổ hợp đường thẳng ngang, dọc, chéo trên biểu đồ ngày sinh (8 mũi tên có mặt và 7 mũi tên trống; bộ 1-2-3 không có bản trống).'),
(5, 'SO_CO_LAP', 'Số cô lập', 'Isolated Number', 1, 'Các chữ số ở 4 góc biểu đồ ngày sinh (1, 3, 7, 9) xuất hiện nhưng toàn bộ các ô liền kề đều trống.'),
(6, 'CON_SO_LINH_HON_QH', 'Con số linh hồn (QH)', 'Soul Urge Number', 1, 'Tổng giá trị các nguyên âm (A, E, I, O, U, Y) trong họ tên đầy đủ theo QH, rút gọn về 1–11.'),
(7, 'CON_SO_BIEU_DAT_QH', 'Con số biểu đạt (QH - Phụ âm)', 'Outer Expression Number', 1, 'Tổng giá trị các phụ âm trong họ tên đầy đủ theo QH, rút gọn về 1–11 hoặc 22/4. Lưu ý tránh nhầm với Expression của Decoz.'),
(8, 'CON_SO_TEN_RIENG_QH', 'Con số tên riêng (QH)', 'Complete Name Number', 1, 'Tổng của Con số linh hồn và Con số biểu đạt theo QH, rút gọn về 2–11 hoặc 22/4.'),
(9, 'NAM_THE_GIOI', 'Năm thế giới', 'Universal Year', 1, 'Tổng các chữ số của năm dương lịch cần xem rút gọn về 1–9.'),
(10, 'NAM_CA_NHAN', 'Năm cá nhân', 'Personal Year Number (PYN)', 1, 'Chỉ số chu kỳ 9 năm cho biết chủ đề phát triển của một năm dương lịch cụ thể.'),
(11, 'THANG_CA_NHAN', 'Tháng cá nhân', 'Personal Month Number', 1, 'Chỉ số chu kỳ tháng, tính từ Năm cá nhân cộng với tháng dương lịch hiện tại rút gọn về 1–9.'),
(12, 'KIM_TU_THAP_DINH', 'Kim tự tháp - 4 Đỉnh cao', 'The Pyramids / Peak Years', 1, 'Bốn đỉnh cao 9 năm trong giai đoạn trưởng thành 27 năm theo Phillips/QH (giữ 10 và 11 ở đỉnh 3 và 4).'),
(13, 'BA_GIAI_DOAN_DOI', 'Ba giai đoạn cuộc đời', 'Three Phases of Life', 1, 'Ba chu kỳ lớn của đời người theo Phillips/QH: Niên thiếu, Trưởng thành (27 năm Kim tự tháp) và Viên mãn.'),
(14, 'SO_DUONG_DOI_DECOZ', 'Số Đường đời (Decoz)', 'Life Path Number', 1, 'Chỉ số cốt lõi quan trọng nhất của Decoz: rút gọn riêng tháng, ngày, năm sinh (giữ 11, 22) rồi cộng 3 phần lại và rút gọn (1–9, 11, 22).'),
(15, 'SO_NO_NGHIEP', 'Số Nợ nghiệp (Decoz)', 'Karmic Debt Number', 1, 'Các tổng trung gian 13, 14, 16, 19 xuất hiện trong quá trình tính các chỉ số cốt lõi theo Decoz.'),
(16, 'SO_BIEU_DAT_DECOZ', 'Số Biểu đạt (Decoz - Toàn bộ tên)', 'Expression Number', 1, 'Tổng giá trị toàn bộ chữ cái (cả nguyên âm và phụ âm) tính theo từng từ trong tên rồi cộng lại theo Decoz (1–9, 11, 22).'),
(17, 'KHAT_VONG_TAM_HON_DECOZ', 'Khát vọng tâm hồn (Decoz)', 'Heart''s Desire Number', 1, 'Tổng giá trị các nguyên âm tính theo từng từ trong tên rồi cộng lại theo Decoz (1–9, 11, 22).'),
(18, 'NHAN_CACH_DECOZ', 'Nhân cách (Decoz - Phụ âm)', 'Personality Number', 1, 'Tổng giá trị các phụ âm tính theo từng từ trong tên rồi cộng lại theo Decoz (1–9, 11, 22).'),
(19, 'DINH_PINNACLE_DECOZ', 'Đỉnh Pinnacle (Decoz)', 'Pinnacle Cycle Number', 1, 'Bốn đỉnh chu kỳ của Decoz: giữ 11 và 22 ở mọi đỉnh, và rút gọn 10 về 1.'),
(20, 'SO_TAM_HON_JOHARI', 'Số tâm hồn (Johari)', 'Psychic Number', 2, 'Tổng các chữ số của ngày sinh rút gọn về 1–9 theo Johari, phản ánh cách bạn nhìn nhận bản thân (mạnh nhất trước tuổi 35).'),
(21, 'SO_DINH_MENH_JOHARI', 'Số định mệnh (Johari)', 'Destiny Number', 2, 'Tổng tất cả chữ số ngày, tháng, năm sinh rút gọn về 1–9 theo Johari, phản ánh cách thế giới nhìn nhận bạn và quả nghiệp (mạnh hơn sau tuổi 35).'),
(22, 'SO_TEN_JOHARI', 'Số tên (Johari)', 'Name Number', 2, 'Số tính từ tên thường gọi theo bảng Unit System của Johari, rút gọn về 1–9.'),
(23, 'SO_KEP_VA_SO_VUONG', 'Số kép & Số vượng (Johari)', 'Compound & Exalted Numbers', 2, 'Ảnh hưởng kết hợp của các ngày sinh từ 10 đến 31 và các ngày đặc biệt tạo nên Số vượng (Exalted Number).'),
(24, 'SO_KHONG', 'Số 0 trong ngày sinh', 'Zero in Birthdate', 2, 'Quan niệm về sự xuất hiện của chữ số 0 trong ngày, tháng, năm sinh giữa Johari và Phillips/QH.'),
(25, 'SO_NAM_JOHARI', 'Số năm dự báo (Johari)', 'Projected Year Number', 2, 'Công thức dự báo năm của Johari: tháng sinh + ngày sinh + 2 chữ số cuối năm cần xem + thứ của sinh nhật trong năm đó (CN=1..T7=7) rút về 1–9.'),
(26, 'HANH_TINH_CHU_QUAN', 'Hành tinh chủ quản của số', 'Ruling Planet', NULL, 'Sự tương ứng giữa các con số 1–9 và các hành tinh trong hệ thống Vệ Đà (Johari), Kabbalah (Sepharial) và Pythagoras (Phillips).'),
(27, 'BIEU_DO_MA_PHUONG', 'Biểu đồ Ma phương 3 tầng (Sepharial)', 'Square of Three / Table of Saturn', 3, 'Ma phương bậc 3 (4-9-2 / 3-5-7 / 8-1-6) và bảng 3 tầng Tinh thần (3-1-9), Trí óc (6-7-5), Thể chất (2-8-4), điền ngày tháng và 2 số cuối năm sinh.'),
(28, 'SO_CHIA_KHOA_SEPHARIAL', 'Số chìa khoá (Sepharial)', 'Key Number / Sum of Nativity', 3, 'Tổng các chữ số ngày, tháng, năm sinh rút gọn về 1–9 theo Sepharial (mặc định bỏ 2 chữ số thế kỷ).'),
(29, 'SO_BAC_THAY', 'Số bậc thầy', 'Master Numbers', 1, 'Các số đặc biệt (11, 22, và 10 trong QH) không bị rút gọn về một chữ số trong trường phái Pythagoras.'),
(30, 'BANG_CHU_CAI_QUY_DOI', 'Bảng quy đổi chữ cái ra số', 'Numerological Alphabet', NULL, 'Hệ thống gán giá trị số cho chữ cái (Pythagoras, Unit System của Johari, Bảng âm vị và Hebrew của Sepharial).');

-- 4. ĐỒNG NGHĨA & THUẬT NGỮ DỄ NHẦM LẪN (bảng dong_nghia)
INSERT INTO dong_nghia (id, khai_niem_id, ten, loai, ghi_chu) VALUES
(1, 1, 'Ruling Number', 'DONG_NGHIA', 'Thuật ngữ gốc tiếng Anh của David A. Phillips (Ch.7), được Lê Đỗ Quỳnh Hương dịch là Con số chủ đạo.'),
(2, 1, 'Số Đường đời (Life Path) / Số Định mệnh (Destiny)', 'DONG_NGHIA', 'Cùng đóng vai trò chỉ số cốt lõi từ ngày tháng năm sinh nhưng khác quy tắc rút gọn giữa QH, Decoz và Johari (Xung đột #1).'),
(3, 4, 'Mũi tên Hoài nghi (Arrow of Scepticism)', 'TEN_CU', 'Tên cũ của mũi tên trống 3-5-7 trong các ấn bản trước của Phillips và trong sách Quỳnh Hương (Xung đột #14).'),
(4, 4, 'Arrow of the Enquirer (Mũi tên Người tìm hiểu)', 'TEN_MOI', 'Tên mới của mũi tên trống 3-5-7 trong ấn bản cập nhật của David A. Phillips nhằm mang sắc thái tích cực hơn (Xung đột #14).'),
(5, 6, 'Heart''s Desire Number (Khát vọng tâm hồn)', 'DONG_NGHIA', 'Thuật ngữ của Hans Decoz cho chỉ số nguyên âm họ tên (QH gọi là Con số linh hồn).'),
(6, 7, 'Số Biểu đạt (Expression Number - Decoz)', 'TRUNG_TEN_KHAC_NGHIA', 'CẢNH BÁO XUNG ĐỘT #7: "Con số biểu đạt" của QH chỉ tính PHỤ ÂM, còn "Expression Number" (thường dịch là Số Biểu đạt) của Decoz tính TOÀN BỘ chữ cái họ tên.'),
(7, 7, 'Personality Number (Nhân cách - Decoz)', 'DONG_NGHIA', 'Cùng được tính từ tổng các phụ âm trong họ tên (QH gọi là Con số biểu đạt, Decoz gọi là Nhân cách / Personality).'),
(8, 8, 'Expression Number (Số Biểu đạt - Decoz)', 'DONG_NGHIA', 'Cùng phản ánh tổng năng lượng của cả nguyên âm và phụ âm họ tên (QH gọi là Con số tên riêng, Decoz gọi là Expression Number).'),
(9, 12, 'Pinnacle Cycle (Đỉnh - Decoz)', 'DONG_NGHIA', 'Cùng cấu trúc 4 đỉnh cao 9 năm nhưng QH giữ 10/11 ở đỉnh 3–4 còn Decoz giữ 11/22 ở mọi đỉnh và rút 10 về 1 (Xung đột #9).'),
(10, 20, 'Con số linh hồn (Pythagoras)', 'TRUNG_TEN_KHAC_NGHIA', 'Psychic Number của Johari dịch là "Số tâm hồn" nhưng tính từ NGÀY SINH, khác hoàn toàn với "Con số linh hồn" tính từ NGUYÊN ÂM TÊN của Pythagoras.');

-- 5. BẢNG CHỮ CÁI (4 hệ thống theo Mục 5)
INSERT INTO bang_chu_cai (id, ma, truong_phai_id, nguon_id, trang_thai, ghi_chu) VALUES
(1, 'PYTHAGORAS_ALPHABET', 1, 1, 'KICH_HOAT', 'Bảng chữ cái Pythagoras chuẩn (A,J,S=1; B,K,T=2; C,L,U=3; D,M,V=4; E,N,W=5; F,O,X=6; G,P,Y=7; H,Q,Z=8; I,R=9) dùng cho QH, PHILLIPS và DECOZ.'),
(2, 'JOHARI_UNIT_SYSTEM', 2, 4, 'CHUA_CO_DU_LIEU', 'CẦN XÁC MINH — Trong bản PDF Johari (trang PDF 8, mục Name Number), bảng Unit System là ảnh bị mất, không trích xuất được. Không tự điền bằng bảng Chaldean/Cheiro trên mạng. Quản trị viên nhập từ sách in tại trang Kho để kích hoạt.'),
(3, 'SEPHARIAL_PHONETIC', 3, 5, 'KHONG_DUNG', 'Bảng âm vị (phonetic) tiếng Anh của Sepharial (Ch.IV, tr.38). Nhóm quyết định không dùng vì âm vị tiếng Việt khác âm vị tiếng Anh; lưu làm tri thức lịch sử.'),
(4, 'SEPHARIAL_HEBREW', 3, 5, 'KHONG_DUNG', 'Bảng chữ cái Hebrew cổ của Sepharial (Ch.III–IV). Không áp dụng cho chữ Quốc ngữ tiếng Việt; lưu làm tri thức lịch sử.');

-- Giá trị 26 chữ cái A-Z cho bảng Pythagoras (bang_chu_cai_id = 1)
INSERT INTO bang_chu_cai_gia_tri (bang_chu_cai_id, ky_tu, gia_tri) VALUES
(1, 'A', 1), (1, 'B', 2), (1, 'C', 3), (1, 'D', 4), (1, 'E', 5), (1, 'F', 6), (1, 'G', 7), (1, 'H', 8), (1, 'I', 9),
(1, 'J', 1), (1, 'K', 2), (1, 'L', 3), (1, 'M', 4), (1, 'N', 5), (1, 'O', 6), (1, 'P', 7), (1, 'Q', 8), (1, 'R', 9),
(1, 'S', 1), (1, 'T', 2), (1, 'U', 3), (1, 'V', 4), (1, 'W', 5), (1, 'X', 6), (1, 'Y', 7), (1, 'Z', 8);

-- 6. CÔNG THỨC TÍNH TOÁN (bảng cong_thuc - Mức tin cậy thủ tục CAO)
INSERT INTO cong_thuc (id, ma, khai_niem_id, mo_ta_cac_buoc, ma_ham_java, tap_gia_tri, nguon_id, chuong, trang_tu, trang_den, trang_thai) VALUES
(1, 'CT_QH_CHU_DAO', 1, 'Cộng tất cả các chữ số của ngày, tháng, năm sinh dương lịch; lặp cộng chữ số cho đến khi tổng nằm trong khoảng 2–11 hoặc bằng 22 (hiển thị 22/4).', 'QhCalculator#calculateConSoChuDao', '2–11, 22/4 (không có 1)', 2, 'Phần 5', 22, 62, 'KICH_HOAT'),
(2, 'CT_QH_NGAY_SINH', 2, 'Ngày sinh từ 1 đến 11 giữ nguyên; ngày 22 giữ là 22/4; các ngày khác cộng hai chữ số của ngày sinh.', 'QhCalculator#calculateConSoNgaySinh', '1–11, 22/4', 2, 'Phần 8', 225, 253, 'KICH_HOAT'),
(3, 'CT_QH_BIEU_DO', 3, 'Điền các chữ số khác 0 của ngày, tháng, năm sinh (năm đủ 4 chữ số) vào lưới 3×3: hàng dưới 1-4-7, hàng giữa 2-5-8, hàng trên 3-6-9. Xác định 15 mũi tên và các số góc cô lập (1, 3, 7, 9).', 'QhCalculator#calculateBirthChart', 'Tần suất 1–9, 15 mũi tên, số cô lập', 1, 'Ch.5–6', 13, 79, 'KICH_HOAT'),
(4, 'CT_QH_LINH_HON', 6, 'Chuẩn hoá họ tên tiếng Việt không dấu, lấy tổng giá trị các nguyên âm (A, E, I, O, U, Y) rồi rút gọn đến khi <= 11.', 'QhCalculator#calculateConSoLinhHon', '1–11', 2, 'Phần 12', 310, 335, 'KICH_HOAT'),
(5, 'CT_QH_BIEU_DAT', 7, 'Lấy tổng giá trị các phụ âm trong họ tên đã chuẩn hoá rồi rút gọn đến khi <= 11 (giữ 22/4).', 'QhCalculator#calculateConSoBieuDat', '1–11, 22/4', 2, 'Phần 12', 336, 350, 'KICH_HOAT'),
(6, 'CT_QH_TEN_RIENG', 8, 'Cộng Con số linh hồn và Con số biểu đạt (nếu 22/4 thì lấy 4) rồi rút gọn theo quy tắc reduceQH.', 'QhCalculator#calculateConSoTenRieng', '2–11, 22/4', 2, 'Phần 12', 351, 358, 'KICH_HOAT'),
(7, 'CT_QH_NAM_CA_NHAN', 10, 'Năm thế giới (rút gọn năm hiện tại về 1–9) + tổng chữ số tháng sinh + tổng chữ số ngày sinh -> rút gọn về 1–9.', 'QhCalculator#calculateNamCaNhan', '1–9', 2, 'Phần 9', 254, 278, 'KICH_HOAT'),
(8, 'CT_QH_THANG_CA_NHAN', 11, 'Năm cá nhân (PYN) + reduceTo9(tháng dương lịch hiện tại) -> reduceTo9.', 'QhCalculator#calculateThangCaNhan', '1–9', 3, 'Ch.6', 188, 189, 'KICH_HOAT'),
(9, 'CT_QH_KIM_TU_THAP', 12, 'Chân đế: tháng, ngày, năm rút về 1–9. Đỉnh 1 = tháng+ngày (1–9); Đỉnh 2 = ngày+năm (1–9); Đỉnh 3 = Đ1+Đ2 (giữ 10, 11); Đỉnh 4 = tháng+năm (giữ 10, 11). Tuổi Đỉnh 1 = 36 − Con số chủ đạo (22/4 lấy 4). Mỗi đỉnh sau cộng 9 năm.', 'QhCalculator#calculateKimTuThap', 'Đỉnh 1–2: 1–9; Đỉnh 3–4: 1–11', 2, 'Phần 11', 284, 309, 'KICH_HOAT'),
(10, 'CT_DECOZ_DUONG_DOI', 14, 'Rút gọn riêng tháng, ngày, năm sinh bằng reduceKeepMaster (giữ 11, 22), sau đó cộng 3 phần lại và áp dụng reduceKeepMaster.', 'DecozCalculator#calculateSoDuongDoi', '1–9, 11, 22', 3, 'Ch.2', 20, 34, 'KICH_HOAT'),
(11, 'CT_DECOZ_BIEU_DAT', 16, 'Với mỗi từ trong họ tên: cộng tất cả chữ cái -> reduceKeepMaster; sau đó cộng kết quả các từ -> reduceKeepMaster.', 'DecozCalculator#calculateSoBieuDat', '1–9, 11, 22', 3, 'Ch.3', 66, 80, 'KICH_HOAT'),
(12, 'CT_DECOZ_KHAT_VONG', 17, 'Với mỗi từ trong họ tên: cộng các nguyên âm -> reduceKeepMaster; sau đó cộng kết quả các từ -> reduceKeepMaster.', 'DecozCalculator#calculateKhatVongTamHon', '1–9, 11, 22', 3, 'Ch.3', 85, 97, 'KICH_HOAT'),
(13, 'CT_DECOZ_NHAN_CACH', 18, 'Với mỗi từ trong họ tên: cộng các phụ âm -> reduceKeepMaster; sau đó cộng kết quả các từ -> reduceKeepMaster.', 'DecozCalculator#calculateNhanCach', '1–9, 11, 22', 3, 'Ch.3', 102, 111, 'KICH_HOAT'),
(14, 'CT_DECOZ_NAM_CA_NHAN', 10, 'reduceTo9(tháng sinh) + reduceTo9(ngày sinh) + reduceTo9(năm hiện tại) -> reduceTo9 (rút gọn cả số bậc thầy).', 'DecozCalculator#calculateNamCaNhan', '1–9', 3, 'Ch.6', 179, 187, 'KICH_HOAT'),
(15, 'CT_DECOZ_PINNACLE', 19, 'Như Kim tự tháp QH nhưng giữ 11 và 22 ở mọi đỉnh (1, 2, 3, 4) và rút gọn 10 về 1.', 'DecozCalculator#calculatePinnacles', '1–9, 11, 22', 3, 'Ch.6', 218, 230, 'KICH_HOAT'),
(16, 'CT_JOHARI_TAM_HON', 20, 'Cộng các chữ số của ngày sinh dương lịch và rút gọn về 1–9 (không giữ 11, 22).', 'JohariCalculator#calculateSoTamHon', '1–9', 4, '§1', 7, 7, 'KICH_HOAT'),
(17, 'CT_JOHARI_DINH_MENH', 21, 'Cộng tất cả các chữ số của ngày, tháng, năm sinh và rút gọn về 1–9 (không giữ 11, 22).', 'JohariCalculator#calculateSoDinhMenh', '1–9', 4, '§1', 7, 8, 'KICH_HOAT'),
(18, 'CT_JOHARI_SO_TEN', 22, 'Quy đổi chữ cái theo bảng Unit System của Johari rồi rút gọn về 1–9. Hiện tạm tắt do ảnh bảng trong PDF bị mất.', 'JohariCalculator#calculateSoTen', '1–9', 4, '§1', 8, 9, 'CHUA_CO_DU_LIEU'),
(19, 'CT_JOHARI_SO_NAM', 25, 'tháng sinh + ngày sinh + 2 chữ số cuối của năm cần xem + số thứ tự ngày trong tuần của sinh nhật năm đó (CN=1, T2=2, ..., T7=7) -> reduceTo9.', 'JohariCalculator#calculateSoNam', '1–9', 4, '§14', 106, 106, 'KICH_HOAT'),
(20, 'CT_SEPHARIAL_MA_PHUONG', 27, 'Lấy các chữ số của ngày sinh, tháng sinh và 2 chữ số cuối của năm sinh (bỏ thế kỷ) điền vào Ma phương 3×3 và bảng 3 tầng Tinh thần (3-1-9), Trí óc (6-7-5), Thể chất (2-8-4).', 'SepharialCalculator#calculateMagicSquare', 'Tần suất 1–9 trên 3 tầng', 5, 'Ch.IV', 41, 53, 'KICH_HOAT'),
(21, 'CT_SEPHARIAL_CHIA_KHOA', 28, 'Tổng các chữ số của ngày, tháng và năm sinh (mặc định bỏ 2 chữ số thế kỷ theo Ch.IV) -> reduceTo9.', 'SepharialCalculator#calculateSoChiaKhoa', '1–9', 5, 'Ch.XIV', 138, 142, 'KICH_HOAT');

-- 7. BỘ THẺ TỪ VỰNG CHUNG (bảng the - 32 thẻ tính cách, 15 thẻ nghề nghiệp, 6 thẻ thời điểm/quan hệ)
INSERT INTO the (id, ma, ten, nhom) VALUES
-- Nhóm TINH_CACH (1..32)
(1, 'LANH_DAO', 'Lãnh đạo & Tiên phong', 'TINH_CACH'),
(2, 'DOC_LAP', 'Độc lập & Tự chủ', 'TINH_CACH'),
(3, 'SANG_TAO', 'Sáng tạo & Biểu đạt', 'TINH_CACH'),
(4, 'PHAN_TICH', 'Phân tích & Nghiên cứu', 'TINH_CACH'),
(5, 'NHAY_CAM', 'Nhạy cảm & Thấu cảm', 'TINH_CACH'),
(6, 'THUC_TE', 'Thực tế & Thực tiễn', 'TINH_CACH'),
(7, 'TU_DO', 'Tự do & Phiêu lưu', 'TINH_CACH'),
(8, 'CHAM_SOC', 'Chăm sóc & Gia đình', 'TINH_CACH'),
(9, 'LY_TUONG', 'Lý tưởng & Hoài bão', 'TINH_CACH'),
(10, 'HOP_TAC', 'Hợp tác & Hoà giải', 'TINH_CACH'),
(11, 'TRUC_GIAC', 'Trực giác & Tâm linh', 'TINH_CACH'),
(12, 'KY_LUAT', 'Kỷ luật & Trật tự', 'TINH_CACH'),
(13, 'THAM_VONG', 'Tham vọng & Thành tựu', 'TINH_CACH'),
(14, 'KIEN_DINH', 'Kiên định & Bền bỉ', 'TINH_CACH'),
(15, 'LINH_HOAT', 'Linh hoạt & Thích ứng', 'TINH_CACH'),
(16, 'GIAO_TIEP', 'Giao tiếp & Truyền cảm hứng', 'TINH_CACH'),
(17, 'HUONG_NOI', 'Hướng nội & Chiêm nghiệm', 'TINH_CACH'),
(18, 'HUONG_NGOAI', 'Hướng ngoại & Xã giao', 'TINH_CACH'),
(19, 'NHAN_DAO', 'Nhân đạo & Phụng sự cộng đồng', 'TINH_CACH'),
(20, 'TRACH_NHIEM', 'Trách nhiệm & Đáng tin cậy', 'TINH_CACH'),
(21, 'TRIEN_KHAI', 'Tổ chức & Triển khai', 'TINH_CACH'),
(22, 'HOA_GIAI', 'Ngoại giao & Kết nối', 'TINH_CACH'),
(23, 'TU_DUY_LOGIC', 'Tư duy logic & Học thuật', 'TINH_CACH'),
(24, 'NGHE_THUAT', 'Cảm thụ nghệ thuật & Thẩm mỹ', 'TINH_CACH'),
(25, 'TAM_LINH', 'Chiều sâu triết học & Tâm linh', 'TINH_CACH'),
(26, 'QUYET_DOAN', 'Quyết đoán & Hành động', 'TINH_CACH'),
(27, 'THAN_TRONG_TC', 'Thận trọng & Cẩn밀', 'TINH_CACH'),
(28, 'LAC_QUAN', 'Lạc quan & Hài hước', 'TINH_CACH'),
(29, 'KHAM_PHA', 'Ham học hỏi & Khám phá', 'TINH_CACH'),
(30, 'TO_CHUC', 'Quản trị & Hệ thống', 'TINH_CACH'),
(31, 'KIEN_NHAN', 'Kiên nhẫn & Điềm tĩnh', 'TINH_CACH'),
(32, 'THICH_NGHI', 'Đổi mới & Cải cách', 'TINH_CACH'),
-- Nhóm NGHE_NGHIEP (33..47)
(33, 'KINH_DOANH_QUAN_LY', 'Kinh doanh & Quản lý điều hành', 'NGHE_NGHIEP'),
(34, 'TAI_CHINH_NGAN_HANG', 'Tài chính, Ngân hàng & Đầu tư', 'NGHE_NGHIEP'),
(35, 'KHOA_HOC_NGHIEN_CUU', 'Khoa học & Nghiên cứu chuyên sâu', 'NGHE_NGHIEP'),
(36, 'CNTT', 'Công nghệ thông tin & Kỹ thuật số', 'NGHE_NGHIEP'),
(37, 'GIAO_DUC', 'Giáo dục, Đào tạo & Cố vấn', 'NGHE_NGHIEP'),
(38, 'Y_TE_CHAM_SOC', 'Y tế, Trị liệu & Chăm sóc', 'NGHE_NGHIEP'),
(39, 'NGHE_THUAT_BIEU_DIEN', 'Nghệ thuật, Âm nhạc & Biểu diễn', 'NGHE_NGHIEP'),
(40, 'THIET_KE', 'Thiết kế, Kiến trúc & Mỹ thuật', 'NGHE_NGHIEP'),
(41, 'LUAT_CHINH_TRI', 'Luật pháp, Hành chính & Chính trị', 'NGHE_NGHIEP'),
(42, 'CONG_TAC_XA_HOI', 'Công tác xã hội & Phi lợi nhuận', 'NGHE_NGHIEP'),
(43, 'KY_THUAT_THU_CONG', 'Kỹ thuật, Xây dựng & Sản xuất', 'NGHE_NGHIEP'),
(44, 'TRUYEN_THONG_VIET', 'Truyền thông, Báo chí & Viết lách', 'NGHE_NGHIEP'),
(45, 'DU_LICH_DICH_VU', 'Du lịch, Thương mại & Dịch vụ', 'NGHE_NGHIEP'),
(46, 'THE_THAO', 'Thể thao & Vận động', 'NGHE_NGHIEP'),
(47, 'TON_GIAO_TAM_LINH', 'Triết học, Văn hoá & Tâm linh', 'NGHE_NGHIEP'),
-- Nhóm THOI_DIEM & QUAN_HE (48..53)
(48, 'KHOI_DAU_MOI', 'Khởi đầu mới & Hành động', 'THOI_DIEM'),
(49, 'CUNG_CO_NOI_LUC', 'Củng cố nội lực & Nghỉ ngơi', 'THOI_DIEM'),
(50, 'THAY_DOI_CHUYEN_DICH', 'Thay đổi, Tự do & Dịch chuyển', 'THOI_DIEM'),
(51, 'THU_HOACH_THANH_TUU', 'Thu hoạch tài chính & Thành tựu', 'THOI_DIEM'),
(52, 'HOP_TAC_GAN_KET', 'Hợp tác, Gia đình & Gắn kết', 'THOI_DIEM'),
(53, 'HOAN_THANH_CHU_KY', 'Hoàn tất chu kỳ & Buông bỏ', 'THOI_DIEM');

-- 8. CẶP THẺ ĐỐI LẬP (bảng cap_the_doi_lap)
INSERT INTO cap_the_doi_lap (id, the_a_id, the_b_id) VALUES
(1, 17, 18), -- HUONG_NOI <-> HUONG_NGOAI
(2, 2, 10),  -- DOC_LAP <-> HOP_TAC
(3, 12, 7),  -- KY_LUAT <-> TU_DO
(4, 6, 9),   -- THUC_TE <-> LY_TUONG
(5, 26, 27), -- QUYET_DOAN <-> THAN_TRONG_TC
(6, 48, 49); -- KHOI_DAU_MOI <-> CUNG_CO_NOI_LUC

-- 9. BẢNG XUNG ĐỘT TRI THỨC (14 xung đột bắt buộc ở Mục 7)
INSERT INTO xung_dot (id, ma, khai_niem_id, mo_ta, cach_xu_ly, trang_thai) VALUES
(1, 'XD-01-CHU-DAO-DUONG-DOI-DINH-MENH', 1, 'Các bên: QH-PHILLIPS · DECOZ · JOHARI. Mâu thuẫn: Cùng tính chỉ số cốt lõi từ ngày tháng năm sinh nhưng QH/Phillips cộng gộp toàn bộ chữ số (tập giá trị 2–11, 22/4, không có số 1); Decoz rút gọn riêng từng phần tháng, ngày, năm và giữ 11/22 (tập giá trị 1–9, 11, 22); Johari cộng gộp và luôn rút gọn về 1–9 (không giữ 11, 22). Ví dụ sinh 04/01/1985: QH ra 10, Decoz ra 1, Johari ra 1; sinh 11/06/1985: QH ra 4, Decoz ra 22, Johari ra 4.', 'Tính cả 3 công thức độc lập, gắn nhãn rõ tên chỉ số của từng trường phái, hiển thị song song và tự động cảnh báo ở khối "Các trường phái khác nhau" khi kết quả lệch nhau.', 'DA_XU_LY'),
(2, 'XD-02-SO-BAC-THAY', 29, 'Các bên: QH · DECOZ · JOHARI · SEPHARIAL. Mâu thuẫn: QH/Phillips giữ 11 và 22/4, đồng thời coi 10 là con số đặc biệt riêng (2–11, 22/4); Decoz chỉ giữ 11 và 22 (10 rút về 1); Johari và Sepharial hoàn toàn không có số bậc thầy (11 -> 2, 22 -> 4).', 'Áp dụng đúng quy tắc rút gọn của từng trường phái (reduceQH, reduceKeepMaster, reduceTo9); tuyệt đối không dùng số 33 vì không có trong 5 nguồn.', 'DA_XU_LY'),
(3, 'XD-03-SO-0', 24, 'Các bên: QH/PHILLIPS · JOHARI. Mâu thuẫn: Phillips/QH coi số 0 là biểu tượng huyền học của tiềm năng vô hạn và thiên hướng tâm linh; ngược lại, Johari (§1, tr.PDF 5) coi số 0 xuất hiện trong ngày/tháng/năm sinh là yếu tố bất lợi về vật chất, khiến đương số phải vất vả hơn (mức ảnh hưởng: ngày > tháng > năm).', 'Hiển thị cả hai góc nhìn đối lập trong mục "Các trường phái khác nhau" khi ngày/tháng/năm sinh có số 0, đồng thời diễn đạt lại lời của Johari theo hướng trung lập.', 'DA_XU_LY'),
(4, 'XD-04-NAM-CA-NHAN', 10, 'Các bên: QH · DECOZ · JOHARI. Mâu thuẫn: QH/Phillips lấy Năm thế giới + chữ số tháng + chữ số ngày và nhấn mạnh "khúc giao thời 2–5 tháng" cuối năm trước; Decoz rút gọn riêng tháng, ngày, năm về 1–9 (rút gọn cả số bậc thầy) tính từ 1/1 đến 31/12; Johari cộng tháng + ngày + 2 số cuối của năm + thứ trong tuần của sinh nhật năm đó (CN=1..T7=7).', 'Trong cột Pythagoras hiển thị Năm cá nhân QH làm chính kèm biến thể Decoz bên dưới; cột Vệ Đà hiển thị Số năm Johari riêng.', 'DA_XU_LY'),
(5, 'XD-05-HANH-TINH-CUA-SO', 26, 'Các bên: JOHARI · SEPHARIAL (2 bảng) · PHILLIPS ch.12. Mâu thuẫn: Các nguồn gán hành tinh khác nhau cho cùng một con số. Ví dụ số 3: Johari gán Mộc tinh, Sepharial (bảng chính) gán Hoả tinh; số 4: Johari gán Rahu, Sepharial (bảng chính) gán Thuỷ tinh, Sepharial (Khoá nhỏ) gán Mặt Trời; số 7: Johari gán Ketu, Sepharial (bảng chính) gán Thổ tinh, Sepharial (Khoá nhỏ) gán Mặt Trăng.', 'Mỗi cột trường phái hiển thị đúng bảng hành tinh của nguồn đó và ghi rõ trích dẫn nguồn; tại cột Kabbalah ghi chú rõ sách có 2 bảng hành tinh.', 'DA_XU_LY'),
(6, 'XD-06-BANG-CHU-CAI', 30, 'Các bên: Pythagoras · Unit System (Johari) · Âm vị (Sepharial) · Hebrew (Sepharial). Mâu thuẫn: 4 hệ thống quy đổi chữ cái ra số hoàn toàn khác nhau giữa các trường phái.', 'Chỉ kích hoạt bảng Pythagoras cho nhánh QH/Decoz; bảng Unit System của Johari để trạng thái CHUA_CO_DU_LIEU chờ nhập từ sách in; bảng âm vị và Hebrew của Sepharial gắn trạng thái KHONG_DUNG (lưu làm tri thức lịch sử).', 'DA_XU_LY'),
(7, 'XD-07-THUAT-NGU-BIEU-DAT', 7, 'Các bên: QH · DECOZ. Mâu thuẫn: "Con số biểu đạt" của Quỳnh Hương là tổng các PHỤ ÂM trong tên; trong khi "Expression Number" (thường dịch là Số Biểu đạt) của Decoz là tổng TOÀN BỘ chữ cái trong tên (phụ âm trong Decoz được gọi là Personality / Nhân cách).', 'Lưu quan hệ TRUNG_TEN_KHAC_NGHIA trong bảng đồng nghĩa; trên giao diện luôn ghi rõ công thức bên cạnh tên ("Con số biểu đạt QH - Phụ âm" và "Số Biểu đạt Decoz - Toàn bộ tên").', 'DA_XU_LY'),
(8, 'XD-08-BIEU-DO-NGAY-SINH', 3, 'Các bên: QH/PHILLIPS · SEPHARIAL. Mâu thuẫn: QH/Phillips dùng lưới cố định 1-4-7 / 2-5-8 / 3-6-9 và lấy đủ 4 chữ số năm sinh; Sepharial dùng Ma phương 4-9-2 / 3-5-7 / 8-1-6 chuyển thành 3 tầng (3-1-9 / 6-7-5 / 2-8-4), bỏ 2 chữ số thế kỷ của năm sinh và tính ngày từ 12h trưa.', 'Tính và vẽ cả hai biểu đồ độc lập ở hai cột Pythagoras và Kabbalah, kèm chú thích quy tắc lập biểu đồ của từng trường phái.', 'DA_XU_LY'),
(9, 'XD-09-DINH-KIM-TU-THAP-PINNACLE', 12, 'Các bên: QH · DECOZ. Mâu thuẫn: Kim tự tháp của QH/Phillips chỉ giữ số 10 và 11 ở Đỉnh 3 và Đỉnh 4 (Đỉnh 1–2 luôn rút về 1–9); Pinnacle của Decoz giữ số bậc thầy 11 và 22 ở tất cả 4 đỉnh, nhưng rút gọn 10 về 1.', 'Hiển thị Kim tự tháp QH/Phillips làm chính trong cột Pythagoras và hiển thị 4 Đỉnh Pinnacle của Decoz trong phần biến thể cách tính.', 'DA_XU_LY'),
(10, 'XD-10-TINH-TOT-XAU-CUA-SO', 21, 'Các bên: JOHARI · DECOZ/QH. Mâu thuẫn: Johari (§1, tr.PDF 10) nhận định thẳng một số Số định mệnh (2, 3, 4, 6, 8) là "không tốt / nhiều khó khăn"; trong khi Pythagoras (Phillips/QH/Decoz) khẳng định mọi con số đều bình đẳng, không có số tốt hay xấu tuyệt đối.', 'Diễn đạt lại lời của Johari thành "thử thách cần lưu ý và rèn luyện", gắn nhãn DUNG cho bản đã diễn đạt lại, và lưu nguyên văn góc nhìn gốc vào bảng lịch sử phiên bản (lich_su_phien_ban).', 'DA_XU_LY'),
(11, 'XD-11-CHU-Y-NGUYEN-AM', 6, 'Các bên: QH · DECOZ. Mâu thuẫn: Trong tiếng Việt, Quỳnh Hương (Phần 12) quy định chữ Y luôn là nguyên âm; trong tiếng Anh, Decoz xét chữ Y là nguyên âm hay phụ âm tuỳ thuộc âm phát ra trong từ.', 'Áp dụng thống nhất quy tắc của QH (A, E, I, O, U, Y luôn là nguyên âm) cho cả QH và Decoz khi tính tên người dùng tiếng Việt, và ghi rõ quyết định thiết kế này.', 'DA_XU_LY'),
(12, 'XD-12-SO-CHIA-KHOA-SEPHARIAL', 28, 'Các bên: SEPHARIAL nội bộ (Ch.IV vs Ch.XIV). Mâu thuẫn: Ở Chương IV (tr.43), Sepharial nói rõ "bỏ 2 chữ số thế kỷ" khi tính tổng chữ số ngày sinh; nhưng ở Chương XIV (tr.140) chỉ viết ngắn gọn "cộng ngày, tháng và năm sinh" rồi chuyển sang ví dụ giờ/phút hành tinh mà không nói rõ năm có bỏ thế kỷ hay không.', 'Mặc định tính Số chìa khoá theo quy tắc bỏ 2 chữ số thế kỷ để thống nhất với Biểu đồ Ma phương Chương IV, đồng thời hiển thị cả kết quả tính đủ 4 chữ số năm trong phần Cách tính và để trạng thái CHO_XAC_MINH.', 'CHO_XAC_MINH'),
(13, 'XD-13-NGUON-GOC-MUI-TEN', 4, 'Các bên: QH · PHILLIPS. Mâu thuẫn: Nhiều tài liệu đại chúng gán "Mũi tên cá tính" cho Pythagoras cổ đại, nhưng chính David A. Phillips (Ch.1, tr.1; Ch.6, tr.51) ghi nhận hệ thống Mũi tên và Biểu đồ hiện đại được thầy của ông là bà Hettie Templeton xây dựng và phổ biến vào thế kỷ 20.', 'Ghi nhận đúng truy vết nguồn gốc (provenance): Hettie Templeton (thế kỷ 20) truyền dạy cho David A. Phillips, sau đó Lê Đỗ Quỳnh Hương Việt hoá.', 'DA_XU_LY'),
(14, 'XD-14-TEN-MUI-TEN-3-5-7-TRONG', 4, 'Các bên: PHILLIPS bản mới · QH. Mâu thuẫn: Mũi tên trống 3-5-7 trong bản dịch của Quỳnh Hương (dựa trên bản cũ) gọi là "Mũi tên Hoài nghi" (Arrow of Scepticism), còn trong bản Phillips cập nhật đổi tên thành "Arrow of the Enquirer" (Mũi tên Người tìm hiểu / Ham học hỏi).', 'Lưu cả hai tên vào bảng đồng nghĩa (TEN_CU và TEN_MOI) và hiển thị song song "Mũi tên Hoài nghi / Người tìm hiểu (Enquirer - trống 3-5-7)" để người đọc không hiểu tiêu cực.', 'DA_XU_LY');
