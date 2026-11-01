package com.kms301.nkms.inference;

import com.kms301.nkms.model.*;
import com.kms301.nkms.engine.HoSoSo;
import com.kms301.nkms.engine.NameNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InferenceEngineTest {

    private InferenceEngine engine;
    private TruongPhai tpPythagoras;
    private TruongPhai tpVeda;
    private TruongPhai tpKabbalah;
    private Nguon nguonPhillips;
    private Nguon nguonQh;
    private Nguon nguonDecoz;
    private Nguon nguonJohari;
    private Nguon nguonSepharial;
    private The theLanhDao;
    private The theDocLap;
    private The theHopTac;

    @BeforeEach
    void setUp() {
        engine = new InferenceEngine();

        tpPythagoras = new TruongPhai();
        tpPythagoras.setId(1L);
        tpPythagoras.setMa("PYTHAGORAS");
        tpPythagoras.setTen("Pythagoras phương Tây");

        tpVeda = new TruongPhai();
        tpVeda.setId(2L);
        tpVeda.setMa("VEDA");
        tpVeda.setTen("Ấn Độ / Vệ Đà");

        tpKabbalah = new TruongPhai();
        tpKabbalah.setId(3L);
        tpKabbalah.setMa("KABBALAH");
        tpKabbalah.setTen("Kabbalah / Chaldean");

        nguonPhillips = new Nguon();
        nguonPhillips.setId(1L);
        nguonPhillips.setMa("PHILLIPS");
        nguonPhillips.setTruongPhai(tpPythagoras);

        nguonQh = new Nguon();
        nguonQh.setId(2L);
        nguonQh.setMa("QH");
        nguonQh.setTruongPhai(tpPythagoras);
        nguonQh.setNguonGoc(nguonPhillips);

        nguonDecoz = new Nguon();
        nguonDecoz.setId(3L);
        nguonDecoz.setMa("DECOZ");
        nguonDecoz.setTruongPhai(tpPythagoras);

        nguonJohari = new Nguon();
        nguonJohari.setId(4L);
        nguonJohari.setMa("JOHARI");
        nguonJohari.setTruongPhai(tpVeda);

        nguonSepharial = new Nguon();
        nguonSepharial.setId(5L);
        nguonSepharial.setMa("SEPHARIAL");
        nguonSepharial.setTruongPhai(tpKabbalah);

        theLanhDao = new The();
        theLanhDao.setId(1L);
        theLanhDao.setMa("LANH_DAO");
        theLanhDao.setTen("Lãnh đạo & Dẫn dắt");
        theLanhDao.setNhom("TINH_CACH");

        theDocLap = new The();
        theDocLap.setId(2L);
        theDocLap.setMa("DOC_LAP");
        theDocLap.setTen("Độc lập & Tự chủ");
        theDocLap.setNhom("TINH_CACH");

        theHopTac = new The();
        theHopTac.setId(3L);
        theHopTac.setMa("HOP_TAC");
        theHopTac.setTen("Hợp tác & Hoà giải");
        theHopTac.setNhom("TINH_CACH");
    }

    @Test
    @DisplayName("Chỉ kích hoạt đơn vị DA_DUYET và nhãn DUNG/GOI_Y_VAN_HOA; loại trừ THAN_TRONG và KHONG_DUNG")
    void testFilterByStatusAndUsageLabel() {
        HoSoSo hoSo = HoSoSo.build("NGUYEN VAN AN", LocalDate.of(1985, 1, 4), 2026, 10,
                NameNormalizer.getDefaultPythagorasMap(), null, false);

        DonViTriThuc uApproved = createUnit(1L, "U-OK", tpPythagoras, nguonQh, "AI",
                "{\"chi_so\":\"QH_CHU_DAO\",\"bang\":\"10\"}", "DA_DUYET", "DUNG", Set.of(theLanhDao));
        DonViTriThuc uCultural = createUnit(2L, "U-CULT", tpVeda, nguonJohari, "DI_THE_NAO",
                "{\"chi_so\":\"JOHARI_TAM_HON\",\"bang\":4}", "DA_DUYET", "GOI_Y_VAN_HOA", Set.of());
        DonViTriThuc uThanTrong = createUnit(3L, "U-WARN", tpVeda, nguonJohari, "AI",
                "{\"chi_so\":\"JOHARI_TAM_HON\",\"bang\":4}", "DA_DUYET", "THAN_TRONG", Set.of(theLanhDao));
        DonViTriThuc uKhongDung = createUnit(4L, "U-NO", tpKabbalah, nguonSepharial, "GAP_GI",
                "{\"chi_so\":\"LUON_KICH_HOAT\",\"bang\":true}", "DA_DUYET", "KHONG_DUNG", Set.of());
        DonViTriThuc uDraft = createUnit(5L, "U-DRAFT", tpPythagoras, nguonDecoz, "AI",
                "{\"chi_so\":\"DECOZ_DUONG_DOI\",\"bang\":\"1\"}", "NHAP", "DUNG", Set.of(theLanhDao));

        KhamPhaResult res = engine.evaluate(hoSo, List.of(uApproved, uCultural, uThanTrong, uKhongDung, uDraft),
                List.of(), List.of(), List.of());

        assertEquals(1, res.getCauHoi("AI").getPythagorasQhPhillips().size());
        assertEquals(0, res.getCauHoi("AI").getVedaJohari().size(), "THAN_TRONG phải bị loại khỏi Khám phá");
        assertEquals(0, res.getCauHoi("AI").getPythagorasDecoz().size(), "NHAP phải bị loại khỏi Khám phá");
        assertEquals(1, res.getCauHoi("DI_THE_NAO").getVedaJohariGoiYVanHoa().size(), "GOI_Y_VAN_HOA được phép hiển thị riêng");
        assertEquals(0, res.getCauHoi("GAP_GI").getKabbalahSepharial().size(), "KHONG_DUNG phải bị loại khỏi Khám phá");
    }

    @Test
    @DisplayName("Gộp nguồn gốc PHILLIPS & QH làm 1 nguồn và nhận diện đồng thuận liên trường phái")
    void testConsensusDeduplicatesPhillipsAndQh() {
        HoSoSo hoSo = HoSoSo.build("NGUYEN VAN AN", LocalDate.of(1985, 1, 4), 2026, 10,
                NameNormalizer.getDefaultPythagorasMap(), null, false);

        DonViTriThuc uPhillips = createUnit(1L, "PH-CD-10", tpPythagoras, nguonPhillips, "AI",
                "{\"chi_so\":\"QH_CHU_DAO\",\"bang\":\"10\"}", "DA_DUYET", "DUNG", Set.of(theLanhDao, theDocLap));
        DonViTriThuc uQh = createUnit(2L, "QH-CD-10", tpPythagoras, nguonQh, "AI",
                "{\"chi_so\":\"QH_CHU_DAO\",\"bang\":\"10\"}", "DA_DUYET", "DUNG", Set.of(theLanhDao, theDocLap));

        // Khi chỉ có PHILLIPS và QH (cùng trường phái Pythagoras), KHÔNG được tính là đồng thuận liên trường phái
        KhamPhaResult onlyPy = engine.evaluate(hoSo, List.of(uPhillips, uQh), List.of(), List.of(), List.of());
        assertTrue(onlyPy.getCauHoi("AI").getDongThuan().isEmpty(),
                "Chỉ PHILLIPS và QH không được tạo đồng thuận liên trường phái");

        // Khi có thêm JOHARI (trường phái VEDA) cũng nhắc tới theLanhDao -> tạo đồng thuận 2 trường phái, 2 nguồn gốc độc lập
        DonViTriThuc uJohari = createUnit(3L, "JH-DM-1", tpVeda, nguonJohari, "AI",
                "{\"chi_so\":\"JOHARI_DINH_MENH\",\"bang\":1}", "DA_DUYET", "DUNG", Set.of(theLanhDao));

        KhamPhaResult crossSchool = engine.evaluate(hoSo, List.of(uPhillips, uQh, uJohari), List.of(), List.of(), List.of());
        List<KhamPhaResult.ConsensusItem> consensus = crossSchool.getCauHoi("AI").getDongThuan();
        assertEquals(1, consensus.size());
        KhamPhaResult.ConsensusItem item = consensus.get(0);
        assertEquals("LANH_DAO", item.getThe().getMa());
        assertEquals(2, item.getSoTruongPhai());
        assertEquals(2, item.getSoNguonGocDocLap(), "PHILLIPS + QH phải gộp thành 1 nguồn gốc, cộng JOHARI là 2");
        assertTrue(item.getCauTomTat().contains("Pythagoras") && item.getCauTomTat().contains("Vệ Đà"));
    }

    @Test
    @DisplayName("Phát hiện đủ 3 loại Khác biệt (a: khác giá trị, b: mâu thuẫn số 0, c: cặp thẻ đối lập) và Phá thế mũi tên trống")
    void testDifferencesAndPhaThe() {
        // Ngày sinh 04/01/1985: QH Chủ đạo = 10, Decoz Đường đời = 1, Johari Định mệnh = 1 (Khác biệt loại a)
        // Ngày sinh 19/08/2000: Có chữ số 0 trong năm 2000 -> kích hoạt mâu thuẫn số 0 giữa Phillips và Johari (Khác biệt loại b)
        HoSoSo hoSo = HoSoSo.build("NGUYEN VAN AN", LocalDate.of(1985, 1, 4), 2026, 10,
                NameNormalizer.getDefaultPythagorasMap(), null, false);
        HoSoSo hoSoZero = HoSoSo.build("PHAM MINH DUC", LocalDate.of(2000, 8, 19), 2026, 10,
                NameNormalizer.getDefaultPythagorasMap(), null, false);

        DonViTriThuc uPhZero = createUnit(10L, "PH-SO-0-BO-QUA", tpPythagoras, nguonPhillips, "NEN_TANG",
                "{\"chi_so\":\"CO_SO_0\",\"co\":true}", "DA_DUYET", "DUNG", Set.of());
        DonViTriThuc uJhZero = createUnit(11L, "JH-SO-0-NGAY-SINH", tpVeda, nguonJohari, "AI",
                "{\"chi_so\":\"CO_SO_0\",\"co\":true}", "DA_DUYET", "DUNG", Set.of());

        QuanHeTriThuc relMauThuan = new QuanHeTriThuc();
        relMauThuan.setId(100L);
        relMauThuan.setTuDonVi(uPhZero);
        relMauThuan.setDenDonVi(uJhZero);
        relMauThuan.setLoai("MAU_THUAN");
        relMauThuan.setGhiChu("Phillips bỏ số 0 khỏi biểu đồ; Johari coi số 0 làm phức tạp số đứng trước.");

        DonViTriThuc uDocLap = createUnit(12L, "QH-CD-10", tpPythagoras, nguonQh, "AI",
                "{\"chi_so\":\"QH_CHU_DAO\",\"bang\":\"10\"}", "DA_DUYET", "DUNG", Set.of(theDocLap));
        DonViTriThuc uHopTac = createUnit(13L, "SE-CK-9", tpKabbalah, nguonSepharial, "AI",
                "{\"chi_so\":\"SEPHARIAL_CHIA_KHOA\",\"bang\":9}", "DA_DUYET", "DUNG", Set.of(theHopTac));

        CapTheDoiLap capDoiLap = new CapTheDoiLap();
        capDoiLap.setId(1L);
        capDoiLap.setTheA(theDocLap);
        capDoiLap.setTheB(theHopTac);
        capDoiLap.setMoTa("Xu hướng tự quyết độc lập đối lập với nhu cầu hoà hợp tập thể.");

        KhamPhaResult res = engine.evaluate(hoSo,
                List.of(uPhZero, uJhZero, uDocLap, uHopTac),
                List.of(relMauThuan),
                List.of(),
                List.of(capDoiLap));

        List<KhamPhaResult.DifferenceItem> diffs = res.getCauHoi("AI").getKhacBiet();
        assertTrue(diffs.stream().anyMatch(d -> "KHAC_GIA_TRI".equals(d.getLoai())), "Phải phát hiện khác biệt giá trị loại (a)");
        assertTrue(diffs.stream().anyMatch(d -> "THE_DOI_LAP".equals(d.getLoai())), "Phải phát hiện cặp thẻ đối lập loại (c)");

        KhamPhaResult resZero = engine.evaluate(hoSoZero,
                List.of(uPhZero, uJhZero),
                List.of(relMauThuan),
                List.of(),
                List.of());
        List<KhamPhaResult.DifferenceItem> diffsZero = resZero.getCauHoi("AI").getKhacBiet();
        assertTrue(diffsZero.stream().anyMatch(d -> "MAU_THUAN_TRUC_TIEP".equals(d.getLoai())), "Phải phát hiện mâu thuẫn số 0 loại (b) với ngày sinh 19/08/2000");

        // Kiểm tra phá thế mũi tên trống 2-4-6 / 3-6-9:
        // Sinh 14/02/2000 -> chỉ có chữ số {1, 2, 4} -> trống hoàn toàn trục 3-6-9!
        // Tên "CUONG R" -> C(3), U(3), O(6), N(5), G(7), R(9) có đủ {3, 6, 9} lấp đầy trục 3-6-9!
        HoSoSo hoSoPhaThe = HoSoSo.build("CUONG R", LocalDate.of(2000, 2, 14), 2026, 10,
                NameNormalizer.getDefaultPythagorasMap(), null, false);
        DonViTriThuc uEmpty369 = createUnit(20L, "PH-MT-369-TRONG", tpPythagoras, nguonQh, "AI",
                "{\"chi_so\":\"MUI_TEN\",\"bo\":\"3-6-9\",\"trang_thai\":\"TRONG\"}", "DA_DUYET", "DUNG", Set.of());
        KhamPhaResult resPhaThe = engine.evaluate(hoSoPhaThe, List.of(uEmpty369), List.of(), List.of(), List.of());
        assertEquals(1, resPhaThe.getCauHoi("AI").getPythagorasQhPhillips().size());
        KhamPhaResult.MatchedUnit muEmpty = resPhaThe.getCauHoi("AI").getPythagorasQhPhillips().get(0);
        assertNotNull(muEmpty.getChuThichTinhChinh(), "Mũi tên trống 3-6-9 được tên lấp đầy phải có chú thích Phá thế");
        assertTrue(muEmpty.getChuThichTinhChinh().contains("Phá thế"));
    }

    private DonViTriThuc createUnit(Long id, String ma, TruongPhai tp, Nguon nguon, String cauHoi,
                                    String dieuKienJson, String trangThai, String nhanSuDung, Set<The> tags) {
        DonViTriThuc u = new DonViTriThuc();
        u.setId(id);
        u.setMa(ma);
        u.setTieuDe("Tiêu đề " + ma);
        u.setLoai("QUY_TAC");
        u.setTruongPhai(tp);
        u.setNguon(nguon);
        u.setCauHoi(cauHoi);
        u.setDieuKienJson(dieuKienJson);
        u.setNoiDung("Nội dung diễn giải " + ma);
        u.setChuong("Ch.1");
        u.setTrangTu(10);
        u.setTrangDen(12);
        u.setMucTinCay("CAO");
        u.setNhanSuDung(nhanSuDung);
        u.setTrangThai(trangThai);
        u.setDanhSachThe(tags);
        return u;
    }
}
