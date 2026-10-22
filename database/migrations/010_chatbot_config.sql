-- =============================================================================
-- 010: Cấu hình chatbot ứng viên (GĐ 1) trong bảng SystemConfig có sẵn — không đổi schema.
-- CHATBOT_ENABLED: "false" để tắt chatbot. CHATBOT_FAQ: JSON [{keywords, question, answer}],
-- từ khóa gõ có dấu hay không dấu đều được (chatbot tự bỏ dấu khi so khớp).
-- HR_CONTACT_EMAIL / HR_CONTACT_PHONE / HR_CONTACT_HOURS: hiện khi ứng viên cần liên hệ HR (để trống = không hiện).
-- Chạy lại nhiều lần vẫn an toàn (idempotent): khóa đã có thì giữ nguyên giá trị HR đã sửa.
-- =============================================================================
USE RitirementManagement2;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfig WHERE ConfigKey = N'CHATBOT_ENABLED')
    INSERT INTO dbo.SystemConfig (ConfigKey, ConfigValue, [Description], UpdatedAt)
    VALUES (N'CHATBOT_ENABLED', N'true', N'Bật/tắt trợ lý tuyển dụng (chatbot) trên trang công khai: true / false', SYSDATETIME());
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfig WHERE ConfigKey = N'HR_CONTACT_EMAIL')
    INSERT INTO dbo.SystemConfig (ConfigKey, ConfigValue, [Description], UpdatedAt)
    VALUES (N'HR_CONTACT_EMAIL', N'careers@moc.com', N'Email bộ phận Nhân sự chatbot hiển thị cho ứng viên', SYSDATETIME());
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SystemConfig WHERE ConfigKey = N'CHATBOT_FAQ')
    INSERT INTO dbo.SystemConfig (ConfigKey, ConfigValue, [Description], UpdatedAt)
    VALUES (N'CHATBOT_FAQ', N'[
  {"keywords": ["nộp hồ sơ", "cách nộp", "cách ứng tuyển", "ứng tuyển như thế nào", "apply"],
   "question": "Cách nộp hồ sơ",
   "answer": "Bạn đăng nhập tài khoản ứng viên, mở tin tuyển dụng muốn ứng tuyển, bấm \"Ứng tuyển ngay\" rồi tải CV lên. Mỗi tin bạn chỉ nộp một hồ sơ."},
  {"keywords": ["cv", "định dạng", "dung lượng", "pdf", "file"],
   "question": "CV cần định dạng gì?",
   "answer": "CV cần là file PDF, dung lượng tối đa 5 MB."},
  {"keywords": ["quy trình", "các bước", "mấy vòng", "vòng tuyển"],
   "question": "Quy trình tuyển dụng",
   "answer": "Quy trình gồm 4 bước: Nộp CV → Xét CV → Phỏng vấn → Thư mời nhận việc. Bạn theo dõi từng bước của hồ sơ ở trang \"Hồ sơ của tôi\"."},
  {"keywords": ["đăng ký", "tạo tài khoản", "otp"],
   "question": "Đăng ký tài khoản ứng viên",
   "answer": "Bấm \"Đăng ký ứng viên\", nhập thông tin rồi xác thực bằng mã OTP gửi về email của bạn."},
  {"keywords": ["quên mật khẩu", "mật khẩu"],
   "question": "Quên mật khẩu",
   "answer": "Ở trang đăng nhập, bấm \"Quên mật khẩu\" và nhập mã OTP gửi về email để đặt mật khẩu mới."},
  {"keywords": ["rút hồ sơ", "rút đơn", "hủy hồ sơ"],
   "question": "Rút hồ sơ đã nộp",
   "answer": "Bạn rút được hồ sơ ở trang \"Hồ sơ của tôi\" khi hồ sơ còn ở vòng đầu (HR chưa duyệt). Hồ sơ đã qua vòng sơ tuyển thì cần liên hệ bộ phận tuyển dụng."},
  {"keywords": ["lỗi", "không nộp được", "không ứng tuyển được", "hết hạn"],
   "question": "Không nộp được hồ sơ",
   "answer": "Bạn kiểm tra giúp: CV là file PDF tối đa 5 MB, tin vẫn còn hạn nộp hồ sơ và bạn chưa nộp tin này trước đó. Vẫn không được thì bạn liên hệ bộ phận Nhân sự nhé."}
]', N'Câu hỏi thường gặp của chatbot (JSON: keywords, question, answer)', SYSDATETIME());
GO
