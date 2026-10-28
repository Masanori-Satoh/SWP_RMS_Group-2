package com.group2.rms.dashboard.internal.dto;

import java.util.List;

/*
 * DTO: toàn bộ dữ liệu dashboard Giám đốc (dashboard/sections/director.html).
 */
public record DirectorDashboardResponse(
        String roleName,
        String fullName,
        List<DashboardMetricResponse> kpis,
        List<PendingRequisitionItem> pendingRequisitions,
        int morePendingRequisitions,
        List<PendingOfferItem> pendingOffers,
        int morePendingOffers,
        List<DashboardBarResponse> openByDepartment,
        List<DecisionItem> recentDecisions) implements DashboardResponse {

    @Override
    public String section() {
        return "director";
    }
}
