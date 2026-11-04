package com.group2.rms.career.controller;

import com.group2.rms.admin.service.PolicyDocument;
import com.group2.rms.admin.service.PolicyDocumentStorage;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * CONTROLLER TEST (@WebMvcTest + SecurityConfig thật): /terms, /privacy-policy công khai, trả PDF inline.
 */
@WebMvcTest(controllers = PolicyDocumentController.class)
@Import({SecurityConfig.class, DatabaseUserDetailsService.class})
class PolicyDocumentControllerTests {

    private static final byte[] PDF = "%PDF-1.4 terms".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PolicyDocumentStorage storage;
    @MockitoBean
    private UserRepository users;

    @Test
    @DisplayName("Khách chưa đăng nhập mở /terms -> 200, PDF hiển thị trong tab (inline), không về trang đăng nhập")
    void terms_shouldServePdfInline_whenAnonymous() throws Exception {
        when(storage.read(PolicyDocument.TERMS)).thenReturn(new ByteArrayResource(PDF));

        mvc.perform(get("/terms"))
                .andExpect(status().isOk())
                // app ép thêm charset=UTF-8 cho mọi response (server.servlet.encoding.force); trình duyệt vẫn mở PDF
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    @DisplayName("Khách mở /privacy-policy -> Trả đúng file Chính sách bảo mật")
    void privacyPolicy_shouldServePrivacyFile_whenAnonymous() throws Exception {
        byte[] privacy = "%PDF-1.4 privacy".getBytes(StandardCharsets.US_ASCII);
        when(storage.read(PolicyDocument.PRIVACY)).thenReturn(new ByteArrayResource(privacy));

        mvc.perform(get("/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(privacy));
    }
}
