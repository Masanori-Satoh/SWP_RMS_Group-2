Method: dual-agent (A: /root/homepage_design_review · B: /root/homepage_detector_review)

# Impeccable critique — homepage Mộc sau implementation

## Design specificity và tổng quan

Assessment A đọc source, PRODUCT.md, bốn ảnh viewport và hai ảnh dialog; probe riêng dialog mobile. Assessment B chạy một CLI scan cùng browser evidence, không đọc A/Gemini. Parent chỉ đọc B sau khi A kết thúc. Đây là nhận định thiết kế cho frontend prototype, không chứng nhận WCAG/production.

Trang giữ editorial off-white/forest-green, Lora/Source Sans 3, hairline và type scale gọn. Hero nói rõ phần mềm workflow; positions/benefits/process thuộc một công ty. Không cần redesign. Culture còn ít kết nối con người vì chỉ có copy và hình học; chưa có media thật được duyệt để bổ sung.

## Design health

Điểm chất lượng 0 = không hỗ trợ, 4 = excellent. A chấm 34/36; synthesis dưới đây bổ sung lỗi focus được kiểm chứng bằng Chrome và giữ mức thận trọng cho các điều kiện chưa test.

| # | Nielsen heuristic | Điểm | Nhận xét |
|---|---|---:|---|
| 1 | Visibility of system status | 4 | Live count, pressed state, clipboard feedback. |
| 2 | Match system / real world | 4 | Chức danh, lương, location và quy trình dễ hiểu. |
| 3 | User control / freedom | 2 | Close ngoài viewport khi cuộn và focus về body sau Escape/hash Back. |
| 4 | Consistency / standards | 4 | Native link/button/details/dialog; metadata nhất quán. |
| 5 | Error prevention | n/a | Không form nộp hồ sơ hoặc thao tác có hậu quả trong prototype này. |
| 6 | Recognition / recall | 3 | Card dễ so sánh; facts mobile chiếm nhiều chiều cao. |
| 7 | Flexibility / efficiency | 3 | Search không dấu/hash/share; close mobile còn bất tiện. |
| 8 | Aesthetic / minimalism | 4 | Palette và hierarchy hợp brief, không decoration dư. |
| 9 | Error recovery | 3 | Empty/invalid slug/clipboard fallback rõ; cần xác nhận lịch sử/focus sau sửa. |
| 10 | Help / documentation | 3 | Process và bước email rõ; contact chỉ là fixture, chưa test mail client. |
| **Tổng** | | **30/36** | **Tốt trong phạm vi prototype; còn lỗi usability cục bộ.** |

## What works

1. Salary/city/type có ngay trên card; không phải mở JD để lấy thông tin quyết định.
2. Bộ lọc và kết quả cùng section; feedback tức thời, clear theo context.
3. Benefits theo hàng, culture split và process tuần tự tạo nhịp khác nhau, giữ hướng thiết kế ban đầu.

## Cognitive load và emotional journey

Search có ba chiều quen thuộc. Bảy department choices vượt ngưỡng bốn của checklist nhưng là bộ phận thật của sáu fixture, nhóm có nhãn và count; không giảm giả số lựa chọn. Mobile chips xuống hàng vẫn đọc được. Trang mobile dài nhưng heading chia nội dung rõ, không coi độ dài tự nó là lỗi.

Hero bình tĩnh → so sánh việc có lương → benefits/process giảm bất định → JD đủ nội dung. Điểm khó chịu ở cuối là phải tìm lại nút đóng sau cuộn. Culture chưa tạo cảm xúc như ảnh/đời sống thực; không bịa testimonial để bù.

## Priority Issues

### P2 — Thoát detail còn mất ngữ cảnh

**What:** Close header nằm trong nội dung cuộn; ở 375×812, dialog clientHeight 731/scrollHeight 1600, scrollTop 500 đưa close lên y≈−435. Root test còn xác nhận Escape đóng dialog/hash nhưng activeElement về body.

**Why:** Người touch phải cuộn ngược lên; người dùng bàn phím mất điểm tiếp tục trong danh sách.

**Fix:** Giữ title/close sticky trên surface; giữ 44px target. Sau history/hash navigation, khôi phục focus ở animation frame tiếp theo; link dán trực tiếp đóng về positions/keyword nhìn thấy được.

**Suggested command:** `impeccable polish` / `impeccable harden`.

### P3 — Facts mobile quá cao

**What:** Năm facts một cột làm Tổng quan chỉ bắt đầu khoảng y=526 trong ảnh 375px.

**Why:** Mỗi lần so sánh JD phải cuộn sớm hơn cần thiết.

**Fix:** Giữ hai cột metadata khi có đủ chiều rộng; kiểm tra city/salary/department dài, không giảm cỡ chữ.

**Suggested command:** `impeccable layout` / `impeccable adapt`.

## Deterministic scan

Một lượt CLI `detect --json prototype/index.html`: 13 warnings gồm `undersized-ui-text` ×1 và `cramped-padding` ×12, location line=0 (DOM findings, không phải dòng source).

- **Thật:** CAREERS 10px tại mobile; tăng 11px.
- **False positives:** các section có container inset, benefit rows/life/process có padding-block/top 16px. Browser computed geometry cho children cách border top 17px; process heading còn có counter phía trước. Không thêm padding máy móc làm sai alignment.
- Favicon 404 là phát hiện root browser QA riêng, không phải detector warning; thêm favicon inline.
- Không suy ra “scan sạch” hoặc AI authorship từ các kết quả này. Sau fix không rerun detector theo bounded-pass rule; giữ raw evidence và disposition.

## Personas và minor observations

- Ứng viên touch: close sau JD cần ở trong vùng nhìn.
- Ứng viên bàn phím/so sánh nhiều jobs: Escape/Back phải trả focus đúng nguồn; facts cần gọn.
- Người tìm hiểu môi trường: cần company media thật được duyệt ở bước vận hành; giữ hình học trung tính hiện tại.
- Caption culture desktop xuống dòng giữa “trách nhiệm”: chỉnh measure nếu giúp cụm chữ đọc liền, không đổi nội dung.
- Contact `.example` và mẫu benefits/JD là scope được user cho phép, không coi chúng là lỗi cần fake backend để khắc phục.

Questions skipped: fewer than 3 Priority Issues; các thay đổi cục bộ nằm trong phạm vi người dùng đã yêu cầu.

## Disposition sau polish

Hai nhóm Priority Issues đã xử lý: header/close sticky, focus phục hồi sau hash action, link trực tiếp đóng về keyword nhìn thấy được; facts giữ hai cột mobile. Round2 75 checks PASS, một driver FAIL được xác định là same-document hash navigation và xác nhận lại bằng tab mới ở 1440/375 PASS. CAREERS nâng 11px, favicon inline, caption culture rộng hơn. Không có round ảnh thứ ba và không rerun detector.

Điểm 30/36 là snapshot trước batch đó; không tự tăng điểm từ tests hoặc sửa vài dòng. Xem [biên bản test](../tests/2026-10-01-homepage-gemini-audit.md). Snapshot Impeccable được close sau khi các priority issues có bằng chứng xử lý; giữ lịch sử critique.
