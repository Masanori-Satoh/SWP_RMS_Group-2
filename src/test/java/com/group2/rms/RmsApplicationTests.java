package com.group2.rms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Kiểm thử cơ bản: đảm bảo Spring Application Context khởi động được.
 * Chạy test này trong IntelliJ: Ctrl + Shift + F10 hoặc nút ▶️ cạnh class.
 */
@SpringBootTest
class RmsApplicationTests {

    @Test
    void contextLoads() {
        // Nếu không throw exception => Application Context khởi động thành công
    }
}
