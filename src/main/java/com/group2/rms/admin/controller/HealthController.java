package com.group2.rms.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group2.rms.admin.dto.HealthResponse;
import com.group2.rms.admin.service.HealthService;


/** Fixed admin-only endpoint used by the internal HTTP probe. */
@RestController
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping(value = "/admin/api-monitoring/internal/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthResponse> health() {
        healthService.checkDatabase();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new HealthResponse("UP"));
    }
}
