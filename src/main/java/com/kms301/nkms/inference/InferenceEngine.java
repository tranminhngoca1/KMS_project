package com.kms301.nkms.inference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kms301.nkms.domain.*;
import com.kms301.nkms.engine.HoSoSo;
import com.kms301.nkms.inference.KhamPhaResult.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Bộ suy diễn & tổng hợp tri thức (Mục 9):
 * 1. Khớp quy tắc (chỉ lấy DA_DUYET và nhan_su_dung thuộc {DUNG, GOI_Y_VAN_HOA}).
 * 2. Áp dụng quan hệ NGOAI_LE_CUA / TINH_CHINH (ví dụ: phá thế mũi tên trống bằng biểu đồ tên).
 * 3. Tổng hợp "Các trường phái đồng ý" (gộp nguồn gốc PHILLIPS & QH làm 1 nguồn gốc để chống đồng thuận giả).
 * 4. Tổng hợp "Các trường phái khác nhau ở…" (3 loại: khác giá trị khái niệm tương đương, quan hệ MAU_THUAN / xung_dot, và cặp thẻ đối lập cap_the_doi_lap).
 */
@Component
public class InferenceEngine {

    private static final Set<String> ALLOWED_USAGE_LABELS = Set.of("DUNG", "GOI_Y_VAN_HOA");
    private static final List<String> QUESTIONS = List.of("AI", "GAP_GI", "DI_THE_NAO");
    private static final Map<String, String> QUESTION_TITLES = Map.of(
            "AI", "Tôi là ai?",
            "GAP_GI", "Tôi đang gặp gì?",
            "DI_THE_NAO", "Tôi nên đi thế nào?"
    );

    private final RuleEvaluator ruleEvaluator = new RuleEvaluator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KhamPhaResult evaluate(HoSoSo hoSoSo,
                                  List<DonViTriThuc> allUnits,
                                  List<QuanHeTriThuc> allRelations,
                                  List<XungDot> allConflicts,
                                  List<CapTheDoiLap> oppositeTagPairs) {
        Map<String, DonViTriThuc> unitByCode = new HashMap<>();
        for (DonViTriThuc u : allUnits) {
            if (u.getMa() != null) {
                unitByCode.put(u.getMa(), u);
            }
        }

        // 1. Lọc và đánh giá điều kiện JSON trên HoSoSo
        List<MatchedUnit> allMatched = new ArrayList<>();
        Map<Long, MatchedUnit> matchedById = new LinkedHashMap<>();

        for (DonViTriThuc unit : allUnits) {
            if (!"DA_DUYET".equalsIgnoreCase(unit.getTrangThai())) {
                continue;
            }
            if (unit.getNhanSuDung() == null || !ALLOWED_USAGE_LABELS.contains(unit.getNhanSuDung().toUpperCase())) {
                continue;
            }
            if (ruleEvaluator.matches(unit.getDieuKienJson(), hoSoSo)) {
                String indicator = ruleEvaluator.describeTriggeredIndicator(unit.getDieuKienJson());
                String steps = ruleEvaluator.describeCalculationSteps(unit.getDieuKienJson(), hoSoSo);
                RefinementInfo ref = checkRefinement(unit, hoSoSo, unitByCode, allRelations);
                MatchedUnit mu = new MatchedUnit(unit, indicator, ref.note, ref.refiningUnit, steps);
                allMatched.add(mu);
                if (unit.getId() != null) {
                    matchedById.put(unit.getId(), mu);
                }
            }
        }

        Map<String, XungDot> conflictByCode = new HashMap<>();
        if (allConflicts != null) {
            for (XungDot xd : allConflicts) {
                if (xd.getMa() != null) {
                    conflictByCode.put(xd.getMa(), xd);
                }
            }
        }

        // 2. Xây dựng kết quả cho từng câu hỏi (AI, GAP_GI, DI_THE_NAO)
        Map<String, QuestionResult> questionResults = new LinkedHashMap<>();
        for (String qCode : QUESTIONS) {
            List<MatchedUnit> unitsForQuestion = new ArrayList<>();
            for (MatchedUnit mu : allMatched) {
                String uQ = mu.getDonVi().getCauHoi();
                if (qCode.equalsIgnoreCase(uQ) || ("AI".equals(qCode) && "NEN_TANG".equalsIgnoreCase(uQ))) {
                    unitsForQuestion.add(mu);
                }
            }

            List<MatchedUnit> pyQhPhillips = new ArrayList<>();
            List<MatchedUnit> pyDecoz = new ArrayList<>();
            List<MatchedUnit> vedaMain = new ArrayList<>();
            List<MatchedUnit> vedaCultural = new ArrayList<>();
            List<MatchedUnit> kabbalah = new ArrayList<>();

            for (MatchedUnit mu : unitsForQuestion) {
                DonViTriThuc u = mu.getDonVi();
                String tpMa = u.getTruongPhai() != null ? u.getTruongPhai().getMa() : "";
                String nguonMa = u.getNguon() != null ? u.getNguon().getMa() : "";

                if ("PYTHAGORAS".equalsIgnoreCase(tpMa)) {
                    if ("DECOZ".equalsIgnoreCase(nguonMa)) {
                        pyDecoz.add(mu);
                    } else {
                        pyQhPhillips.add(mu);
                    }
                } else if ("VEDA".equalsIgnoreCase(tpMa)) {
                    if (mu.isGoiYVanHoa()) {
                        vedaCultural.add(mu);
                    } else {
                        vedaMain.add(mu);
                    }
                } else if ("KABBALAH".equalsIgnoreCase(tpMa)) {
                    kabbalah.add(mu);
                }
            }

            List<ConsensusItem> dongThuan = buildConsensus(unitsForQuestion);
            List<DifferenceItem> khacBiet = buildDifferences(
                    qCode, hoSoSo, unitsForQuestion, allMatched,
                    allRelations, allConflicts, conflictByCode, oppositeTagPairs);

            questionResults.put(qCode, new QuestionResult(
                    qCode,
                    QUESTION_TITLES.getOrDefault(qCode, qCode),
                    dongThuan,
                    khacBiet,
                    pyQhPhillips,
                    pyDecoz,
                    vedaMain,
                    vedaCultural,
                    kabbalah
            ));
        }

        return new KhamPhaResult(hoSoSo, questionResults);
    }

    /**
     * Kiểm tra quan hệ NGOAI_LE_CUA / TINH_CHINH (đặc biệt là phá thế mũi tên trống nhờ biểu đồ tên).
     */
    private RefinementInfo checkRefinement(DonViTriThuc unit, HoSoSo hoSoSo,
                                           Map<String, DonViTriThuc> unitByCode,
                                           List<QuanHeTriThuc> allRelations) {
        if (unit.getDieuKienJson() == null) {
            return new RefinementInfo(null, null);
        }
        try {
            JsonNode node = objectMapper.readTree(unit.getDieuKienJson());
            if (node.has("chi_so") && "MUI_TEN".equals(node.get("chi_so").asText())
                    && node.has("trang_thai") && "TRONG".equalsIgnoreCase(node.get("trang_thai").asText())) {
                String bo = node.has("bo") ? node.get("bo").asText() : "";
                if (hoSoSo.getQh().getBirthChart().getArrows().isCompensatedByName(bo)) {
                    String phaTheCode = "PH-PHA-THE-" + bo.replace("-", "");
                    DonViTriThuc refUnit = unitByCode.get(phaTheCode);
                    if (refUnit == null && allRelations != null) {
                        for (QuanHeTriThuc rel : allRelations) {
                            if (("NGOAI_LE_CUA".equalsIgnoreCase(rel.getLoai()) || "TINH_CHINH".equalsIgnoreCase(rel.getLoai()))
                                    && rel.getDenDonVi() != null
                                    && Objects.equals(rel.getDenDonVi().getMa(), unit.getMa())) {
                                refUnit = rel.getTuDonVi();
                                break;
                            }
                        }
                    }
                    String note = "⚡ Phá thế (NGOAI_LE_CUA): Biểu đồ tên đã lấp đầy đủ 3 ô của trục trống " + bo
                            + ", giúp hoá giải và giảm nhẹ đáng kể điểm yếu của mũi tên trống này (QH Phần 12, tr.261–263).";
                    return new RefinementInfo(note, refUnit);
                }
            }
        } catch (Exception ignored) {
        }
        return new RefinementInfo(null, null);
    }

    /**
     * Mục 9.2: Tổng hợp "Các trường phái đồng ý".
     * - Gom thẻ (the) của các đơn vị đã kích hoạt.
     * - Gộp nguồn gốc: PHILLIPS và QH tính là 1 nguồn gốc duy nhất ("PHILLIPS_ROOT") để tránh "đồng thuận giả".
     * - Điểm đồng thuận = thẻ xuất hiện ở >= 2 trường phái khác nhau.
     * - Sắp xếp theo số trường phái giảm dần, rồi theo số nguồn độc lập giảm dần.
     */
    private List<ConsensusItem> buildConsensus(List<MatchedUnit> unitsForQuestion) {
        Map<String, The> tagByCode = new LinkedHashMap<>();
        Map<String, List<MatchedUnit>> unitsByTag = new LinkedHashMap<>();

        for (MatchedUnit mu : unitsForQuestion) {
            if (mu.getDonVi().getDanhSachThe() == null) continue;
            for (The the : mu.getDonVi().getDanhSachThe()) {
                tagByCode.putIfAbsent(the.getMa(), the);
                unitsByTag.computeIfAbsent(the.getMa(), k -> new ArrayList<>()).add(mu);
            }
        }

        List<ConsensusItem> items = new ArrayList<>();
        for (Map.Entry<String, List<MatchedUnit>> entry : unitsByTag.entrySet()) {
            The the = tagByCode.get(entry.getKey());
            List<MatchedUnit> supporting = entry.getValue();

            // Gom theo trường phái và gộp nguồn gốc (PHILLIPS + QH -> 1 nguồn gốc)
            Map<String, Set<String>> indicatorsBySchool = new LinkedHashMap<>();
            Set<String> distinctRootSources = new LinkedHashSet<>();
            List<MatchedUnit> deduplicatedEvidence = new ArrayList<>();
            Set<String> seenRootSourcePerTag = new HashSet<>();

            for (MatchedUnit mu : supporting) {
                DonViTriThuc u = mu.getDonVi();
                String schoolCode = u.getTruongPhai() != null ? u.getTruongPhai().getMa() : "UNKNOWN";
                indicatorsBySchool.computeIfAbsent(schoolCode, k -> new LinkedHashSet<>())
                        .add(mu.getChiSoKichHoat());

                String rootSourceKey = resolveRootSourceKey(u.getNguon());
                distinctRootSources.add(rootSourceKey);

                String schoolRootCombo = schoolCode + ":" + rootSourceKey + ":" + mu.getChiSoKichHoat();
                if (seenRootSourcePerTag.add(schoolRootCombo)) {
                    deduplicatedEvidence.add(mu);
                }
            }

            if (indicatorsBySchool.size() >= 2) {
                List<String> schoolPhrases = new ArrayList<>();
                for (Map.Entry<String, Set<String>> sp : indicatorsBySchool.entrySet()) {
                    String schoolName = formatSchoolName(sp.getKey());
                    String joinedIndicators = String.join(", ", sp.getValue());
                    schoolPhrases.add(schoolName + " (" + joinedIndicators + ")");
                }
                String summary = the.getTen() + " — " + String.join(" và ", schoolPhrases) + " đều nhắc tới.";
                items.add(new ConsensusItem(
                        the,
                        indicatorsBySchool.size(),
                        distinctRootSources.size(),
                        summary,
                        deduplicatedEvidence
                ));
            }
        }

        items.sort(Comparator
                .comparingInt(ConsensusItem::getSoTruongPhai).reversed()
                .thenComparing(Comparator.comparingInt(ConsensusItem::getSoNguonGocDocLap).reversed())
                .thenComparing(c -> c.getThe().getTen()));

        return items;
    }

    /**
     * Quy đổi nguồn về nguồn gốc để PHILLIPS và QH luôn tính là 1 nguồn duy nhất (Mục 9.2).
     */
    public String resolveRootSourceKey(Nguon nguon) {
        if (nguon == null) return "UNKNOWN";
        if (nguon.getNguonGoc() != null && nguon.getNguonGoc().getMa() != null) {
            return nguon.getNguonGoc().getMa().toUpperCase();
        }
        String ma = nguon.getMa() != null ? nguon.getMa().toUpperCase() : "UNKNOWN";
        if ("QH".equals(ma) || "PHILLIPS".equals(ma)) {
            return "PHILLIPS";
        }
        return ma;
    }

    /**
     * Mục 9.3: Tổng hợp "Các trường phái khác nhau ở…" theo 3 loại (a, b, c).
     */
    private List<DifferenceItem> buildDifferences(String qCode,
                                                  HoSoSo hoSoSo,
                                                  List<MatchedUnit> unitsForQuestion,
                                                  List<MatchedUnit> allMatched,
                                                  List<QuanHeTriThuc> allRelations,
                                                  List<XungDot> allConflicts,
                                                  Map<String, XungDot> conflictByCode,
                                                  List<CapTheDoiLap> oppositeTagPairs) {
        List<DifferenceItem> diffs = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();

        // --- LOẠI (a): Khác giá trị cho cùng khái niệm tương đương ---
        if ("AI".equals(qCode)) {
            String qhChuDao = hoSoSo.getQh().getConSoChuDao().getDisplayValue();
            String decozDuongDoi = hoSoSo.getDecoz().getSoDuongDoi().getDisplayValue();
            String johariDinhMenh = hoSoSo.getJohari().getSoDinhMenh().getDisplayValue();
            String sephChiaKhoa = hoSoSo.getSepharial().getSoChiaKhoa().getDisplayValue();

            // Kiểm tra khác biệt giữa Con số chủ đạo (QH/Phillips), Số đường đời (Decoz), Số định mệnh (Johari)
            boolean sameMainNumber = qhChuDao.equals(decozDuongDoi) && qhChuDao.equals(johariDinhMenh);
            if (!sameMainNumber) {
                XungDot xd1 = conflictByCode.get("XD-01-CHU-DAO-DUONG-DOI-DINH-MENH");
                XungDot xd2 = conflictByCode.get("XD-02-SO-BAC-THAY");
                boolean isMasterDiff = "10".equals(qhChuDao) || "11".equals(qhChuDao) || "22/4".equals(qhChuDao)
                        || "11".equals(decozDuongDoi) || "22".equals(decozDuongDoi);
                XungDot chosenXd = (isMasterDiff && xd2 != null && xd1 == null) ? xd2 : xd1;

                String moTa = String.format(
                        "Với cùng ngày sinh %02d/%02d/%04d: Pythagoras (QH/Phillips) ra Con số chủ đạo = %s; "
                                + "Pythagoras nhánh Mỹ (Decoz) ra Số đường đời = %s; "
                                + "Vệ Đà (Johari) ra Số định mệnh = %s; "
                                + "Kabbalah (Sepharial) ra Số chìa khoá = %s. "
                                + "Nguyên nhân: khác biệt về tập giá trị đích (QH giữ số 10 và viết 22/4; Decoz rút 10→1 và giữ 11, 22; Johari rút triệt để về 1–9) hoặc cách cộng từng thành phần ngày/tháng/năm.",
                        hoSoSo.getNgaySinh().getDayOfMonth(),
                        hoSoSo.getNgaySinh().getMonthValue(),
                        hoSoSo.getNgaySinh().getYear(),
                        qhChuDao, decozDuongDoi, johariDinhMenh, sephChiaKhoa
                );
                List<MatchedUnit> evidence = collectUnitsByConcepts(unitsForQuestion,
                        Set.of("CON_SO_CHU_DAO", "SO_NGAY_SINH_CHU_DAO"));
                diffs.add(new DifferenceItem(
                        "KHAC_GIA_TRI",
                        "Khác giá trị chỉ số tổng ngày sinh (Chủ đạo / Đường đời / Định mệnh / Chìa khoá)",
                        moTa,
                        chosenXd != null ? chosenXd.getMa() : "XD-01-CHU-DAO-DUONG-DOI-DINH-MENH",
                        chosenXd != null ? chosenXd.getId() : null,
                        evidence
                ));
                addedKeys.add("XD-01-CHU-DAO-DUONG-DOI-DINH-MENH");
            }

            // Kiểm tra khác biệt Số chìa khoá Sepharial (bỏ 2 chữ số thế kỷ của năm sinh)
            String sephFullYear = hoSoSo.getSepharial().getSoChiaKhoaFullYear().getDisplayValue();
            if (!sephChiaKhoa.equals(johariDinhMenh) || !sephChiaKhoa.equals(sephFullYear)) {
                XungDot xd12 = conflictByCode.get("XD-12-SO-CHIA-KHOA-SEPHARIAL");
                String moTa = String.format(
                        "Kabbalah (Sepharial Ch.IV, tr.43) bỏ 2 chữ số thế kỷ của năm sinh nên Số chìa khoá = %s (nếu giữ đủ 4 chữ số năm sẽ là %s, và Số định mệnh Johari là %s).",
                        sephChiaKhoa, sephFullYear, johariDinhMenh
                );
                diffs.add(new DifferenceItem(
                        "KHAC_GIA_TRI",
                        "Cách tính Số chìa khoá (Sepharial) bỏ chữ số thế kỷ so với Vệ Đà & Pythagoras",
                        moTa,
                        xd12 != null ? xd12.getMa() : "XD-12-SO-CHIA-KHOA-SEPHARIAL",
                        xd12 != null ? xd12.getId() : null,
                        collectUnitsByConcepts(unitsForQuestion, Set.of("SO_CHIA_KHOA", "CON_SO_CHU_DAO"))
                ));
                addedKeys.add("XD-12-SO-CHIA-KHOA-SEPHARIAL");
            }

            // Khác biệt cách quy đổi và cộng Tên giữa 3 trường phái
            String qhTen = hoSoSo.getQh().getConSoTenRieng().getDisplayValue();
            String decozBieuDat = hoSoSo.getDecoz().getSoBieuDat().getDisplayValue();
            XungDot xd6 = conflictByCode.get("XD-06-BANG-CHU-CAI");
            String moTaTen = String.format(
                    "Với họ tên \"%s\": Pythagoras (QH) tính Con số tên riêng = %s (Linh hồn %s + Biểu đạt %s); "
                            + "Decoz cộng rút gọn từng từ rồi cộng lại ra Số biểu đạt (Expression) = %s, Khát vọng tâm hồn = %s, Nhân cách = %s; "
                            + "Vệ Đà (Johari) dùng bảng Unit System (%s); "
                            + "Kabbalah (Sepharial) dùng bảng âm vị tiếng Anh/Hebrew nên không áp dụng cho tên tiếng Việt.",
                    hoSoSo.getParsedName().getNormalizedName(),
                    qhTen,
                    hoSoSo.getQh().getConSoLinhHon().getDisplayValue(),
                    hoSoSo.getQh().getConSoBieuDat().getDisplayValue(),
                    decozBieuDat,
                    hoSoSo.getDecoz().getKhatVongTamHon().getDisplayValue(),
                    hoSoSo.getDecoz().getNhanCach().getDisplayValue(),
                    hoSoSo.getJohari().getSoTen().getDisplayMessage()
            );
            diffs.add(new DifferenceItem(
                    "KHAC_GIA_TRI",
                    "Khác biệt hệ thống bảng chữ cái và công thức tính Số tên giữa 3 trường phái",
                    moTaTen,
                    xd6 != null ? xd6.getMa() : "XD-06-BANG-CHU-CAI",
                    xd6 != null ? xd6.getId() : null,
                    collectUnitsByConcepts(unitsForQuestion, Set.of("SO_TEN_BIET_BIEU_DAT", "SO_LINH_HON_NOI_TAM", "SO_NHAN_CACH_ BIEU_DAT_PHU_AM"))
            ));
            addedKeys.add("XD-06-BANG-CHU-CAI");
        } else if ("GAP_GI".equals(qCode)) {
            String qhPyn = hoSoSo.getQh().getNamCaNhan().getDisplayValue();
            String decozPyn = hoSoSo.getDecoz().getNamCaNhan().getDisplayValue();
            String johariYn = hoSoSo.getJohari().getSoNam().getDisplayValue();
            XungDot xd4 = conflictByCode.get("XD-04-NAM-CA-NHAN");

            String moTaNam = String.format(
                    "Trong năm %d: Pythagoras (QH/Phillips) tính Năm cá nhân = %s (cộng Thế giới năm %s); "
                            + "Decoz tính Năm cá nhân = %s; "
                            + "Vệ Đà (Johari) cộng thêm số thứ tự ngày trong tuần của sinh nhật năm %d nên ra Số năm = %s; "
                            + "Kabbalah (Sepharial) không có công thức Năm cá nhân thuần số học (chỉ có Tarot bói toán đã gắn nhãn KHONG_DUNG).",
                    hoSoSo.getNamHienTai(), qhPyn, hoSoSo.getQh().getNamTheGioi().getDisplayValue(),
                    decozPyn, hoSoSo.getNamHienTai(), johariYn
            );
            diffs.add(new DifferenceItem(
                    "KHAC_GIA_TRI",
                    "Khác công thức Năm cá nhân (Pythagoras) và Số năm có cộng thứ sinh nhật (Vệ Đà)",
                    moTaNam,
                    xd4 != null ? xd4.getMa() : "XD-04-NAM-CA-NHAN",
                    xd4 != null ? xd4.getId() : null,
                    collectUnitsByConcepts(unitsForQuestion, Set.of("NAM_CA_NHAN"))
            ));
            addedKeys.add("XD-04-NAM-CA-NHAN");

            String qhPeak = hoSoSo.getQh().getKimTuThap().getCurrentPeak().getDisplayValue();
            String decozPeak = hoSoSo.getDecoz().getCurrentPinnacle().getDisplayValue();
            if (!qhPeak.equals(decozPeak)) {
                XungDot xd9 = conflictByCode.get("XD-09-DINH-KIM-TU-THAP-PINNACLE");
                String moTaPeak = String.format(
                        "Ở tuổi %d (Đỉnh %d): Kim tự tháp QH/Phillips giữ số 10, 11 ở đỉnh 3–4 nên ra Đỉnh = %s, trong khi Pinnacle của Decoz giữ 11, 22 ở mọi đỉnh và rút 10→1 nên ra Đỉnh = %s.",
                        hoSoSo.getTuoiHienTai(),
                        hoSoSo.getQh().getKimTuThap().getCurrentPeak().getIndex(),
                        qhPeak, decozPeak
                );
                diffs.add(new DifferenceItem(
                        "KHAC_GIA_TRI",
                        "Khác giá trị Đỉnh Kim tự tháp (QH/Phillips) và Đỉnh Pinnacle (Decoz)",
                        moTaPeak,
                        xd9 != null ? xd9.getMa() : "XD-09-DINH-KIM-TU-THAP-PINNACLE",
                        xd9 != null ? xd9.getId() : null,
                        collectUnitsByConcepts(unitsForQuestion, Set.of("KIM_TU_THAP_DINH"))
                ));
                addedKeys.add("XD-09-DINH-KIM-TU-THAP-PINNACLE");
            }
        } else if ("DI_THE_NAO".equals(qCode)) {
            int keyNum = hoSoSo.getSepharial().getSoChiaKhoa().getNumericValue();
            XungDot xd12 = conflictByCode.get("XD-12-SO-CHIA-KHOA-SEPHARIAL");
            String moTaNghe = (keyNum == 3 || keyNum == 5 || keyNum == 9)
                    ? String.format("Số chìa khoá của bạn là %d — nằm trong 3 con số (3, 5, 9) được Sepharial định hướng nghề nghiệp trực tiếp ở Chương XIV (tr.139), trong khi Pythagoras và Vệ Đà có lời khuyên định hướng cho tất cả các số.", keyNum)
                    : String.format("Số chìa khoá của bạn theo Sepharial là %d, nhưng Chương XIV của Sepharial (tr.139) chỉ lập bảng nghề nghiệp cho 3 số (3: thần học/triết học, 5: thương mại, 9: điều hành/quản trị). Vì vậy định hướng hành động ở cột Kabbalah dựa trên lời khuyên tổng hợp theo mặt phẳng biểu đồ.", keyNum);
            diffs.add(new DifferenceItem(
                    "KHAC_GIA_TRI",
                    "Phạm vi định hướng nghề nghiệp và hành động giữa 3 trường phái",
                    moTaNghe,
                    xd12 != null ? xd12.getMa() : "XD-12-SO-CHIA-KHOA-SEPHARIAL",
                    xd12 != null ? xd12.getId() : null,
                    collectUnitsByConcepts(unitsForQuestion, Set.of("SO_CHIA_KHOA", "MA_PHUONG_3_TANG"))
            ));
        }

        // --- LOẠI (b): Hai đơn vị được kích hoạt có quan hệ MAU_THUAN hoặc cùng thuộc một bản ghi xung_dot ---
        Map<Long, MatchedUnit> activeInTabMap = new LinkedHashMap<>();
        for (MatchedUnit mu : unitsForQuestion) {
            if (mu.getDonVi().getId() != null) {
                activeInTabMap.put(mu.getDonVi().getId(), mu);
            }
        }
        // Riêng câu hỏi AI xét thêm các đơn vị NEN_TANG đã kích hoạt (vd số 0)
        if ("AI".equals(qCode)) {
            for (MatchedUnit mu : allMatched) {
                if ("NEN_TANG".equalsIgnoreCase(mu.getDonVi().getCauHoi()) && mu.getDonVi().getId() != null) {
                    activeInTabMap.put(mu.getDonVi().getId(), mu);
                }
            }
        }

        if (allRelations != null) {
            for (QuanHeTriThuc rel : allRelations) {
                if (!"MAU_THUAN".equalsIgnoreCase(rel.getLoai())) continue;
                Long idA = rel.getTuDonVi() != null ? rel.getTuDonVi().getId() : null;
                Long idB = rel.getDenDonVi() != null ? rel.getDenDonVi().getId() : null;
                if (idA != null && idB != null && activeInTabMap.containsKey(idA) && activeInTabMap.containsKey(idB)) {
                    MatchedUnit muA = activeInTabMap.get(idA);
                    MatchedUnit muB = activeInTabMap.get(idB);
                    String pairKey = "REL:" + Math.min(idA, idB) + ":" + Math.max(idA, idB);
                    if (addedKeys.add(pairKey)) {
                        // Tìm xem cặp này có thuộc bản ghi xung_dot nào không
                        XungDot linkedXd = findConflictForUnits(allConflicts, idA, idB);
                        if (linkedXd != null) {
                            addedKeys.add(linkedXd.getMa());
                        }
                        diffs.add(new DifferenceItem(
                                "MAU_THUAN_TRUC_TIEP",
                                "Mâu thuẫn quan điểm trực tiếp: " + muA.getDonVi().getTieuDe() + " ↔ " + muB.getDonVi().getTieuDe(),
                                rel.getGhiChu() != null ? rel.getGhiChu() : (linkedXd != null ? linkedXd.getMoTa() : "Hai đơn vị tri thức có quan điểm trái ngược nhau."),
                                linkedXd != null ? linkedXd.getMa() : null,
                                linkedXd != null ? linkedXd.getId() : null,
                                List.of(muA, muB)
                        ));
                    }
                }
            }
        }

        if (allConflicts != null) {
            for (XungDot xd : allConflicts) {
                if (addedKeys.contains(xd.getMa())) continue;
                List<MatchedUnit> matchedInConflict = new ArrayList<>();
                Set<String> schoolsInConflict = new HashSet<>();
                Set<String> sourcesInConflict = new HashSet<>();
                for (DonViTriThuc d : xd.getDanhSachDonVi()) {
                    if (d.getId() != null && activeInTabMap.containsKey(d.getId())) {
                        MatchedUnit mu = activeInTabMap.get(d.getId());
                        matchedInConflict.add(mu);
                        if (d.getTruongPhai() != null) schoolsInConflict.add(d.getTruongPhai().getMa());
                        if (d.getNguon() != null) sourcesInConflict.add(d.getNguon().getMa());
                    }
                }
                if (matchedInConflict.size() >= 2 && (schoolsInConflict.size() >= 2 || sourcesInConflict.size() >= 2)) {
                    addedKeys.add(xd.getMa());
                    diffs.add(new DifferenceItem(
                            "MAU_THUAN_TRUC_TIEP",
                            "Xung đột tri thức (" + xd.getMa() + ")",
                            xd.getMoTa() + " → Cách xử lý KMS: " + xd.getCachXuLy(),
                            xd.getMa(),
                            xd.getId(),
                            matchedInConflict
                    ));
                }
            }
        }

        // --- LOẠI (c): Hai đơn vị ở hai trường phái khác nhau mang cặp thẻ đối lập trong cap_the_doi_lap ---
        if (oppositeTagPairs != null) {
            for (CapTheDoiLap cap : oppositeTagPairs) {
                The theA = cap.getTheA();
                The theB = cap.getTheB();
                if (theA == null || theB == null) continue;

                List<MatchedUnit> holdersA = findUnitsWithTag(unitsForQuestion, theA.getMa());
                List<MatchedUnit> holdersB = findUnitsWithTag(unitsForQuestion, theB.getMa());

                for (MatchedUnit muA : holdersA) {
                    for (MatchedUnit muB : holdersB) {
                        String spA = muA.getDonVi().getTruongPhai() != null ? muA.getDonVi().getTruongPhai().getMa() : "";
                        String spB = muB.getDonVi().getTruongPhai() != null ? muB.getDonVi().getTruongPhai().getMa() : "";
                        if (!spA.equals(spB)) {
                            String oppKey = "OPP:" + theA.getMa() + ":" + theB.getMa() + ":" + spA + ":" + spB;
                            if (addedKeys.add(oppKey)) {
                                String title = String.format("Góc nhìn đối lập: \"%s\" (%s) ↔ \"%s\" (%s)",
                                        theA.getTen(), formatSchoolName(spA),
                                        theB.getTen(), formatSchoolName(spB));
                                String desc = String.format("%s (%s) nhấn mạnh khía cạnh \"%s\", trong khi %s (%s) lại chỉ ra xu hướng \"%s\". (%s)",
                                        formatSchoolName(spA), muA.getChiSoKichHoat(), theA.getTen(),
                                        formatSchoolName(spB), muB.getChiSoKichHoat(), theB.getTen(),
                                        cap.getMoTa() != null ? cap.getMoTa() : "Cặp thẻ đối lập");
                                diffs.add(new DifferenceItem(
                                        "THE_DOI_LAP",
                                        title,
                                        desc,
                                        null,
                                        null,
                                        List.of(muA, muB)
                                ));
                            }
                            break;
                        }
                    }
                }
            }
        }

        return diffs;
    }

    private List<MatchedUnit> collectUnitsByConcepts(List<MatchedUnit> units, Set<String> conceptCodes) {
        List<MatchedUnit> res = new ArrayList<>();
        for (MatchedUnit mu : units) {
            KhaiNiem kn = mu.getDonVi().getKhaiNiem();
            if (kn != null && kn.getMa() != null && conceptCodes.contains(kn.getMa())) {
                res.add(mu);
            }
        }
        return res;
    }

    private List<MatchedUnit> findUnitsWithTag(List<MatchedUnit> units, String tagCode) {
        List<MatchedUnit> res = new ArrayList<>();
        for (MatchedUnit mu : units) {
            if (mu.getDonVi().getDanhSachThe() == null) continue;
            for (The t : mu.getDonVi().getDanhSachThe()) {
                if (tagCode.equalsIgnoreCase(t.getMa())) {
                    res.add(mu);
                    break;
                }
            }
        }
        return res;
    }

    private XungDot findConflictForUnits(List<XungDot> conflicts, Long idA, Long idB) {
        if (conflicts == null) return null;
        for (XungDot xd : conflicts) {
            boolean hasA = false;
            boolean hasB = false;
            for (DonViTriThuc u : xd.getDanhSachDonVi()) {
                if (Objects.equals(u.getId(), idA)) hasA = true;
                if (Objects.equals(u.getId(), idB)) hasB = true;
            }
            if (hasA && hasB) return xd;
        }
        return null;
    }

    public static String formatSchoolName(String schoolCode) {
        if (schoolCode == null) return "Trường phái";
        return switch (schoolCode.toUpperCase()) {
            case "PYTHAGORAS" -> "Pythagoras";
            case "VEDA" -> "Vệ Đà (Johari)";
            case "KABBALAH" -> "Kabbalah (Sepharial)";
            default -> schoolCode;
        };
    }

    private static class RefinementInfo {
        final String note;
        final DonViTriThuc refiningUnit;

        RefinementInfo(String note, DonViTriThuc refiningUnit) {
            this.note = note;
            this.refiningUnit = refiningUnit;
        }
    }
}

