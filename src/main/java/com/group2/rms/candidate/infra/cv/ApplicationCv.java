package com.group2.rms.candidate.infra.cv;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * CV của một đơn: hoặc file đọc từ {@link CvStorage} (đơn nộp qua web), hoặc đường dẫn để chuyển hướng
 * (CV mẫu {@code /samples/cv/...} của dữ liệu seed, dịch vụ lưu file ngoài sau này). Đúng một trong hai khác null.
 * Trang HR ({@code /applications/{id}/cv}) và trang ứng viên ({@code /portal/applications/{id}/cv}) cùng đi qua
 * {@link #of} và {@link #toResponse}; mỗi nơi tự kiểm quyền trước.
 */
public record ApplicationCv(Resource file, String redirectUrl) {

    public static ApplicationCv file(Resource file) {
        return new ApplicationCv(file, null);
    }

    public static ApplicationCv redirect(String url) {
        return new ApplicationCv(null, url);
    }

    /**
     * CV theo {@code AppliedCvUrl}, có 3 dạng: {@code local:{key}} (đơn nộp qua web), {@code /samples/cv/...} (seed),
     * {@code http(s)://...} (dịch vụ lưu file ngoài sau này). Trống hoặc dạng khác (kể cả {@code //host}) thì rỗng.
     */
    public static Optional<ApplicationCv> of(String appliedCvUrl, CvStorage storage) {
        if (appliedCvUrl == null) {
            return Optional.empty();
        }
        if (appliedCvUrl.startsWith(LocalCvStorage.URL_PREFIX)) {
            return Optional.of(file(storage.load(appliedCvUrl.substring(LocalCvStorage.URL_PREFIX.length()))));
        }
        if (appliedCvUrl.startsWith("/") && !appliedCvUrl.startsWith("//")
                || appliedCvUrl.startsWith("https://") || appliedCvUrl.startsWith("http://")) {
            return Optional.of(redirect(appliedCvUrl));
        }
        return Optional.empty();
    }

    /**
     * Phản hồi cho khung xem CV (hoặc tab mới): file lưu nội bộ trả thẳng PDF {@code inline}; CV mẫu / link ngoài thì
     * chuyển hướng. Header cho phép nhúng khung cùng nguồn đặt ở {@code SecurityConfig} ({@code CV_PATHS}).
     */
    public ResponseEntity<Resource> toResponse(String fileName) {
        if (redirectUrl != null) {
            URI location = redirectUrl.startsWith("/")
                    ? ServletUriComponentsBuilder.fromCurrentContextPath().path(redirectUrl).build().toUri()
                    : URI.create(redirectUrl);
            return ResponseEntity.status(302).location(location).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fileName).build().toString())
                .body(file);
    }
}
