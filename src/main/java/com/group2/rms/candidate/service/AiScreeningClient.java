package com.group2.rms.candidate.service;

/**
 * Dịch vụ chấm mức độ phù hợp của CV với tin tuyển dụng. Bản hiện tại: {@link StubAiScreeningClient} (giả lập);
 * dịch vụ AI thật thêm bản mới sau. Kết quả chỉ để gợi ý, không dùng để tự loại ứng viên.
 */
public interface AiScreeningClient {

    AiScreeningOutcome score(AiScreeningInput input);
}
