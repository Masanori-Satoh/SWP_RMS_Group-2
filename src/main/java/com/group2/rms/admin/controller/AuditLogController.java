package com.group2.rms.admin.controller;

import com.group2.rms.admin.service.AuditLogService;
import com.group2.rms.admin.dto.AuditLogLabels;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService audit;

    @GetMapping("/admin/audit-logs")
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer actorId, @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String entity, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page, Model model) {
        boolean invalidDates = from != null && to != null && from.isAfter(to);
        model.addAttribute("logs", invalidDates ? Page.empty() : audit.search(from, to, actorId, action, entity, search, page));
        if (invalidDates) model.addAttribute("filterError", "Từ ngày phải trước hoặc bằng đến ngày.");
        model.addAttribute("actors", audit.actors());
        model.addAttribute("entities", audit.entities().stream().map(value -> new AuditLogLabels.Option(value, AuditLogLabels.entity(value))).toList());
        model.addAttribute("actions", AuditLogLabels.actions());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("actorId", actorId);
        model.addAttribute("action", action);
        model.addAttribute("entity", entity);
        model.addAttribute("search", search);
        model.addAttribute("activeMenu", "audit-logs");
        model.addAttribute("pageLang", "vi");
        return "admin/audit-log";
    }
}
