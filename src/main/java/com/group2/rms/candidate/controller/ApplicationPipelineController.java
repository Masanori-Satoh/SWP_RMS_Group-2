package com.group2.rms.candidate.controller;

import com.group2.rms.candidate.dto.ApplicationListResponse;
import com.group2.rms.candidate.dto.ApplicationSearch;
import com.group2.rms.candidate.service.ApplicationPipelineService;
import com.group2.rms.candidate.service.ApplicationStatusLabels;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Hồ sơ ứng tuyển cho người tuyển dụng: danh sách (5.1.22). Quyền theo vai trò ở {@code SecurityConfig},
 * phạm vi từng người (HM chỉ thấy phòng ban mình) ở {@code ApplicationAccess}.
 */
@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationPipelineController {

    static final int PAGE_SIZE = 20;

    private final ApplicationPipelineService pipeline;

    @GetMapping
    public String list(@ModelAttribute ApplicationSearch search,
                       @PageableDefault(size = PAGE_SIZE) Pageable pageable, Model model) {
        ApplicationListResponse result = pipeline.list(search, pageable);
        model.addAttribute("result", result);
        model.addAttribute("statusOptions", ApplicationStatusLabels.options());
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("viewerRole", result.viewerRole());
        return "candidate/applications";
    }
}
