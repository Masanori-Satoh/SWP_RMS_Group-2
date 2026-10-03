# Kiểm tra Impeccable init — 01/10/2026

## Phạm vi

Khởi tạo ngữ cảnh sản phẩm cho toàn bộ RMS của một doanh nghiệp. Người dùng xác nhận phạm vi này và chọn viết code trực tiếp làm mặc định. Đây là thay đổi tài liệu/cấu hình công cụ; không phải triển khai thêm flow ứng dụng.

## Các bước kiểm tra lại

| ID | Các bước | Kết quả mong đợi | Kết quả lần này |
| --- | --- | --- | --- |
| INIT-01 | Mở `PRODUCT.md` ở root repo. Đọc Platform, Users, Purpose và Positioning. | Có comment `impeccable:product-schema 1`, platform `web`, một doanh nghiệp, Candidate và năm role nội bộ; Guest bị giới hạn. | PASS: phần bắt buộc, platform và mọi liên kết nội bộ tồn tại; review nội dung theo câu trả lời người dùng. |
| INIT-02 | Đối chiếu phần capabilities với `SecurityConfig`, các controller, `docs/flows/README.md` và `database/schema/db.sql`. | Ghi session/form login/CSRF và không JWT; schema là chuẩn; không tuyên bố các luồng chỉ có model/query đã hoàn thiện. | PASS ở mức review source/document; chưa có runtime test trong init. |
| INIT-03 | Mở `.impeccable/config.json`, parse JSON; đối chiếu câu trả lời người dùng. | `buildPath` là `code`. File local sẵn có vẫn giữ hook consent. | PASS: JSON parse được, `buildPath=code`, local hook consent vẫn `accepted`. |
| INIT-04 | Parse `.impeccable/live/config.json`; kiểm tra đường dẫn ở `files`, anchor và CSP. | Chỉ target `prototype/index.html`, file tồn tại và có `</body>`; `cspChecked=true` dựa trên kết quả detector. | PASS: `detect-csp` trả `shape:null`, `signals:[]`; JSON, file và anchor hợp lệ. |
| INIT-05 | Xem Git diff và file thay đổi trong đợt này. | Thay đổi chỉ là PRODUCT/config và tài liệu ghi log/test; không sửa UI/Java/schema hay inject live vào HTML. | PASS: patch chỉ ghi các file kể trên; không inject live; `git diff --check` PASS. Các thay đổi có từ trước lượt init vẫn được giữ. |
| INIT-06 | Khi cần sửa trực quan sau này, chạy Impeccable live theo hướng dẫn skill và mở đúng file prototype. | Picker live chạy khi chủ động khởi động; setup hiện tại chưa chứng minh helper/browser hoạt động. | NOT RUN; init chỉ tạo config, không khởi động live session. |

Có thể parse JSON bằng PowerShell từ root repo:

```powershell
Get-Content .impeccable/config.json -Raw | ConvertFrom-Json
Get-Content .impeccable/live/config.json -Raw | ConvertFrom-Json
Test-Path prototype/index.html
```

Không chạy lại `impeccable context` trong cùng session init; launcher đã được gọi một lần và resolve đúng root/target. Ở session sau, bước Setup của skill sẽ đọc `PRODUCT.md` mới.

Bằng chứng terminal: `PRODUCT_RECORD_PASS: platform, sections, local links` và `CONFIG_PASS: code default, local consent preserved, single prototype target, anchor`; các command kiểm tra trả exit code 0. `git diff --check` trả exit code 0.

## Giới hạn

Không chạy Maven, Spring startup, DB hoặc browser E2E trong đợt tài liệu/cấu hình này. Các kết quả runtime cũ nằm trong test doc riêng; không ghi lại thành PASS của init. Tên Mộc vẫn là placeholder; thông tin công ty, nội dung legal, catalogue AI và các tích hợp ngoài còn chưa được xác nhận.
