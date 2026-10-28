package com.group2.rms.dashboard.dto;

import java.util.List;

/** Read-only chart data; each point remains visible as an ordinary HTML link or value. */
public record RecruitmentChartResponse(String title, String description, String type, List<Point> points) {
    public record Point(String label, long count, String href) {
    }
}
