package com.group2.rms.requisition.controller;

import com.group2.rms.requisition.dto.InternalJobPostingDetailResponse;
import com.group2.rms.requisition.dto.InternalJobPostingResponse;
import com.group2.rms.requisition.dto.JobPostingCreateRequest;
import com.group2.rms.requisition.service.JobPostingService;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/internal/job-postings")
@RequiredArgsConstructor
public class InternalJobPostingController {

    public record DeptStat(String deptName, long count, int percentage) {}

    private final JobPostingService jobPostingService;
    private final RequisitionAccess requisitionAccess;
    private final DepartmentRepository departmentRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobRequisitionRepository jobRequisitionRepository;

    @ModelAttribute
    void populateViewerAttributes(Model model) {
        User currentUser = requisitionAccess.actor();
        model.addAttribute("viewerRole", requisitionAccess.role(currentUser));
        model.addAttribute("viewerName", currentUser.getFullName());
    }

    @GetMapping
    public String listJobPostings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        User currentUser = requisitionAccess.actor();
        Page<InternalJobPostingResponse> postingPage = jobPostingService.searchInternalJobPostings(
                page, size, q, departmentId, status, sort, currentUser
        );

        model.addAttribute("postings", postingPage.getContent());
        model.addAttribute("currentPage", postingPage.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, postingPage.getTotalPages()));
        model.addAttribute("pageSize", postingPage.getSize());
        model.addAttribute("startPage", Math.max(1, postingPage.getNumber() - 1));
        model.addAttribute("endPage", Math.min(Math.max(1, postingPage.getTotalPages()), postingPage.getNumber() + 3));
        model.addAttribute("totalElements", postingPage.getTotalElements());

        model.addAttribute("search", q);
        model.addAttribute("selectedDepartment", departmentId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sort);

        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("statuses", List.of("Draft", "Published", "Closed", "Paused"));

        return "job-postings/list";
    }

    @GetMapping("/dashboard")
    public String hrDashboard(Model model) {
        List<JobPosting> allPostings = jobPostingRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long activeCount = allPostings.stream()
                .filter(p -> "Published".equalsIgnoreCase(p.getPostingStatus()) &&
                        (p.getApplicationDeadline() == null || p.getApplicationDeadline().isAfter(now)))
                .count();
        long draftCount = allPostings.stream()
                .filter(p -> "Draft".equalsIgnoreCase(p.getPostingStatus()))
                .count();
        long closedCount = allPostings.stream()
                .filter(p -> "Closed".equalsIgnoreCase(p.getPostingStatus()) ||
                        ("Published".equalsIgnoreCase(p.getPostingStatus()) && p.getApplicationDeadline() != null && p.getApplicationDeadline().isBefore(now)))
                .count();

        List<JobRequisition> approvedReqs = jobRequisitionRepository.findByApprovalStatusOrderByCreatedAtDesc("Approved");
        Set<Integer> postedReqIds = allPostings.stream()
                .filter(p -> p.getRequisition() != null)
                .map(p -> p.getRequisition().getRequisitionId())
                .collect(Collectors.toSet());

        List<JobRequisition> reqsAwaitingPosting = approvedReqs.stream()
                .filter(r -> !postedReqIds.contains(r.getRequisitionId()))
                .toList();

        // Thống kê phân bố theo phòng ban
        long totalPostings = allPostings.size();
        Map<String, Long> deptCounts = allPostings.stream()
                .filter(p -> p.getRequisition() != null && p.getRequisition().getDepartment() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getRequisition().getDepartment().getDepartmentName(),
                        Collectors.counting()
                ));

        List<DeptStat> deptStats = deptCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    int pct = totalPostings > 0 ? (int) Math.round((e.getValue() * 100.0) / totalPostings) : 0;
                    return new DeptStat(e.getKey(), e.getValue(), pct);
                })
                .toList();

        // 5 tin tuyển dụng mới nhất
        List<JobPosting> recentPostings = allPostings.stream()
                .sorted((a, b) -> {
                    LocalDateTime t1 = a.getPostingDate() != null ? a.getPostingDate() : a.getCreatedAt();
                    LocalDateTime t2 = b.getPostingDate() != null ? b.getPostingDate() : b.getCreatedAt();
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t2.compareTo(t1);
                })
                .limit(5)
                .toList();

        model.addAttribute("activePostingsCount", activeCount);
        model.addAttribute("draftPostingsCount", draftCount);
        model.addAttribute("pendingReqsCount", reqsAwaitingPosting.size());
        model.addAttribute("closedPostingsCount", closedCount);
        model.addAttribute("deptStats", deptStats);
        model.addAttribute("reqsAwaitingPosting", reqsAwaitingPosting);
        model.addAttribute("recentPostings", recentPostings);

        return "job-postings/dashboard";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam("requisitionId") Integer requisitionId, Model model) {
        User currentUser = requisitionAccess.actor();
        JobPostingCreateRequest formDto = jobPostingService.prepareCreateForm(requisitionId, currentUser);
        model.addAttribute("postingDto", formDto);
        return "job-postings/create";
    }

    @PostMapping("/create")
    public String handleCreatePost(
            @Valid @ModelAttribute("postingDto") JobPostingCreateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = requisitionAccess.actor();

        if (bindingResult.hasErrors()) {
            return "job-postings/create";
        }

        try {
            boolean isUpdate = request.getJobPostingId() != null;
            Integer postingId = jobPostingService.createJobPosting(request, currentUser);
            boolean isPublished = "publish".equalsIgnoreCase(request.getAction());
            if (isPublished) {
                redirectAttributes.addFlashAttribute("message", isUpdate
                        ? "Đã cập nhật và phát hành tin tuyển dụng thành công."
                        : "Đã đăng tin tuyển dụng thành công lên cổng việc làm công khai.");
            } else {
                redirectAttributes.addFlashAttribute("message", isUpdate
                        ? "Đã cập nhật bản nháp tin tuyển dụng thành công."
                        : "Đã lưu bản nháp tin tuyển dụng thành công.");
            }
            return "redirect:/internal/job-postings";
        } catch (ResponseStatusException ex) {
            model.addAttribute("errorMessage", ex.getReason());
            return "job-postings/create";
        }
    }

    @GetMapping("/{id}")
    public String showJobPostingDetail(@PathVariable("id") Integer id, Model model) {
        User currentUser = requisitionAccess.actor();
        InternalJobPostingDetailResponse detail = jobPostingService.getInternalJobPostingDetail(id, currentUser);
        model.addAttribute("posting", detail);
        return "job-postings/detail";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        User currentUser = requisitionAccess.actor();
        JobPostingCreateRequest formDto = jobPostingService.prepareEditForm(id, currentUser);
        model.addAttribute("postingDto", formDto);
        return "job-postings/create";
    }

    @PostMapping("/delete/{id}")
    public String deleteJobPosting(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        User currentUser = requisitionAccess.actor();
        jobPostingService.deleteJobPosting(id, currentUser);
        redirectAttributes.addFlashAttribute("message", "Đã xóa tin tuyển dụng thành công.");
        return "redirect:/internal/job-postings";
    }
}
