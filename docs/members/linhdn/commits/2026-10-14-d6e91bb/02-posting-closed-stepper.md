# Commit 02 — Xử lý hồ sơ bị dừng do đóng đợt trong stepper

Ngày: 14/10/2026. Tách từ `d6e91bb`.

## File và thay đổi

- `src/main/java/com/group2/rms/candidate/service/ApplicationStepper.java`: thêm case POSTING_CLOSED → APPLIED trong lastStagePassed.
- `src/test/java/com/group2/rms/candidate/service/ApplicationStepperTests.java`: thêm postingClosedKeepsTheReasonWithoutInferringPassedRounds.

## Lý do

Đợt tuyển có thể đóng ở bất kỳ vòng nào. Stepper phải giữ lý do đóng và hiển thị APPLIED/CLOSED, không ném IllegalStateException hoặc suy ra các vòng chưa có bằng chứng đã đạt.

## Kiểm tra

1. Chạy ApplicationStepperTests.
2. Mở hồ sơ bị đóng đợt: thanh tiến độ có lý do Đóng đợt tuyển dụng.
3. Xác nhận không tự đánh dấu Interview/Offer đã qua.

Bằng chứng trên snapshot gốc: 9 test stepper PASS. Đợt chia commit chỉ phân bố lại thay đổi, không sửa nội dung source.
