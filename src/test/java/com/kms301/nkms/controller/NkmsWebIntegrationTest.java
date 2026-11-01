package com.kms301.nkms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NkmsWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Tất cả các trang giao diện (/kham-pha, /kho, /nguon, /xung-dot, /khai-niem, /thong-ke, /gioi-thieu) trả về HTTP 200")
    void testAllPublicPagesRenderSuccessfully() throws Exception {
        mockMvc.perform(get("/kham-pha"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Khám Phá Bản Thể Qua Thần Số Học")));

        mockMvc.perform(get("/kho"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kho Tri thức Thần số học")));

        mockMvc.perform(get("/kho/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Truy vết Nguồn gốc")));

        mockMvc.perform(get("/nguon"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PHILLIPS")))
                .andExpect(content().string(containsString("BPHS")))
                .andExpect(content().string(containsString("Sefer Yetzirah")));

        mockMvc.perform(get("/xung-dot"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("XD-01-CHU-DAO-DUONG-DOI-DINH-MENH")));

        mockMvc.perform(get("/khai-niem"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CON_SO_CHU_DAO")));

        mockMvc.perform(get("/thong-ke"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tổng số Đơn vị Tri thức")));

        mockMvc.perform(get("/gioi-thieu"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Độ tin cậy Thủ tục")));

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dang-nhap"));
    }

    @Test
    @DisplayName("Kiểm thử 4 ngày sinh minh hoạ xung đột ở trang Khám phá (04/01/1985, 11/06/1985, 02/01/1990, 19/08/2000)")
    void testFourConflictBirthdatesOnKhamPha() throws Exception {
        String[][] testCases = {
                {"Nguyễn Văn An", "04/01/1985"},
                {"Trần Thị Bình", "11/06/1985"},
                {"Lê Hoàng Cường", "02/01/1990"},
                {"Phạm Minh Đức", "19/08/2000"}
        };

        for (String[] tc : testCases) {
            MvcResult res = mockMvc.perform(post("/kham-pha")
                            .param("hoTen", tc[0])
                            .param("ngaySinh", tc[1]))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Khám Phá Bản Thể Qua Thần Số Học")))
                    .andExpect(content().string(containsString("AI Life Coach")))
                    .andExpect(content().string(containsString("Bản Ngã Cốt Lõi &amp; Chân Dung Tâm Lý")))
                    .andExpect(content().string(containsString("Thế Mạnh Vượt Trội &amp; Năng Lực Bẩm Sinh")))
                    .andExpect(content().string(containsString("Điểm Mù &amp; Mâu Thuẫn Cần Gỡ Bỏ")))
                    .andExpect(content().string(containsString("Định Hướng Hành Động &amp; Lời Khuyên Khai Vấn")))
                    .andReturn();

            String html = res.getResponse().getContentAsString();
            assertTrue(html.contains("/kho"), "Trang Khám phá phải có liên kết tới Kho tri thức (/kho)");
        }
    }

    @Test
    @DisplayName("Kiểm thử AI Analysis Engine (/api/ai-analysis) trả về luận giải cá nhân hoá cho cả 3 câu hỏi")
    void testAiAnalysisEndpoint() throws Exception {
        mockMvc.perform(post("/api/ai-analysis")
                        .param("hoTen", "Nguyễn Văn An")
                        .param("ngaySinh", "04/01/1985"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tongQuanHoSo").isNotEmpty())
                .andExpect(jsonPath("$.cauHoi.AI.phanTichTongHop").isNotEmpty())
                .andExpect(jsonPath("$.cauHoi.GAP_GI.phanTichTongHop").isNotEmpty())
                .andExpect(jsonPath("$.cauHoi.DI_THE_NAO.phanTichTongHop").isNotEmpty());
    }
}
