package com.group2.rms.controller.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller này dùng để TEST kết nối trực tiếp xuống MS SQL Server.
 * Trả về chữ thuần (hoặc JSON) thay vì trả về giao diện HTML.
 */
@RestController
@RequestMapping("/test-db")
public class TestDbController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping(produces = "text/html;charset=UTF-8")
    public String testDatabaseConnection() {
        String template = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Database Status</title>
                <style>
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
                    body {
                        font-family: 'Inter', sans-serif;
                        background: linear-gradient(135deg, #f0fdf4 0%%, #e0f2fe 100%%);
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        margin: 0;
                    }
                    .card {
                        background: rgba(255, 255, 255, 0.95);
                        backdrop-filter: blur(10px);
                        padding: 40px;
                        border-radius: 20px;
                        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
                        text-align: center;
                        max-width: 450px;
                        width: 90%%;
                        animation: fadeIn 0.5s ease-out;
                    }
                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(-20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    .icon { font-size: 60px; margin-bottom: 10px; }
                    h2 { margin: 0 0 10px; color: #1f2937; font-weight: 700; }
                    .success-text { color: #16a34a; }
                    .error-text { color: #dc2626; }
                    .info-box {
                        background: #f9fafb;
                        border-left: 4px solid %s;
                        padding: 15px;
                        border-radius: 8px;
                        margin-top: 20px;
                        font-size: 14px;
                        color: #4b5563;
                        text-align: left;
                        line-height: 1.6;
                        word-break: break-word;
                    }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="icon">%s</div>
                    <h2 class="%s">%s</h2>
                    <div class="info-box">%s</div>
                </div>
            </body>
            </html>
            """;

        try {
            String sqlQuery = "SELECT CAST(SERVERPROPERTY('ProductVersion') AS VARCHAR) AS version, "
                            + "CAST(SERVERPROPERTY('Edition') AS VARCHAR) AS edition, "
                            + "net_transport, local_tcp_port "
                            + "FROM sys.dm_exec_connections WHERE session_id = @@SPID";
            java.util.Map<String, Object> row = jdbcTemplate.queryForMap(sqlQuery);
            
            String version = (String) row.get("version");
            String edition = (String) row.get("edition");
            String transport = (String) row.get("net_transport");
            Object portObj = row.get("local_tcp_port");
            String port = portObj != null ? portObj.toString() : "N/A";

            String dbInfo = "📌 <b>Phiên bản SQL:</b> " + version + " (" + edition + ")<br>"
                          + "🔌 <b>Giao thức:</b> " + transport + "<br>"
                          + "🚪 <b>Port kết nối:</b> " + port;

            return template.formatted("#16a34a", "✅", "success-text", "Kết nối DB Thành công!", dbInfo);
        } catch (Exception e) {
            return template.formatted("#dc2626", "❌", "error-text", "Kết nối DB Thất bại!", "<b>Lỗi chi tiết:</b><br>" + e.getMessage());
        }
    }
}
