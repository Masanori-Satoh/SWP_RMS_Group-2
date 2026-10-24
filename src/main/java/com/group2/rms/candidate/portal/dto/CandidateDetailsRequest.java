package com.group2.rms.candidate.portal.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Phần riêng ứng viên (bảng {@code Candidate}) trên trang {@code /portal/profile}. Mọi trường không bắt buộc.
 * Không có {@code CandidateSource}, {@code IsPotential}: là thông tin nội bộ, ứng viên không thấy, không sửa.
 * Giới tính lưu đúng chữ như seed ({@code Nam}, {@code Nữ}) cộng thêm {@code Khác}; giá trị khác đang có sẵn trong DB
 * vẫn giữ được ({@link #genderOptions(String)}), kiểm ở {@code CandidateProfileService.update}.
 * Câu báo của {@code @Past}, {@code @Size} lấy từ {@code messages.properties} của nhóm (khóa {@code Past}, {@code Size}).
 */
public record CandidateDetailsRequest(
        @Past
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOfBirth,

        @Size(max = 20)
        String gender,

        @Size(max = 255)
        String address,

        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://[^\\s<>\"]+$", message = "Link LinkedIn phải bắt đầu bằng http:// hoặc https://.")
        String linkedInUrl,

        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://[^\\s<>\"]+$", message = "Link Portfolio phải bắt đầu bằng http:// hoặc https://.")
        String portfolioUrl) {

    /** Các lựa chọn giới tính trên form, đúng thứ tự hiển thị. */
    public static final List<String> GENDERS = List.of("Nam", "Nữ", "Khác");

    /**
     * Lựa chọn giới tính cho một hồ sơ: 3 giá trị chuẩn, cộng giá trị đang lưu nếu nằm ngoài danh sách
     * (dữ liệu cũ), để mở form rồi lưu lại không làm mất giá trị đó.
     */
    public static List<String> genderOptions(String current) {
        if (current == null || current.isBlank() || GENDERS.contains(current)) {
            return GENDERS;
        }
        List<String> options = new ArrayList<>(GENDERS);
        options.add(current);
        return List.copyOf(options);
    }
}
