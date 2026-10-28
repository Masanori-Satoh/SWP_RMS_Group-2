package com.group2.rms.dashboard.internal.service;

import com.group2.rms.dashboard.internal.dto.DashboardResponse;
import com.group2.rms.dashboard.shared.DashboardAccess;
import com.group2.rms.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * SERVICE: điều phối dashboard nội bộ, kiểm tra người xem rồi giao cho service của đúng role.
 */
@Service
public class InternalDashboardService {

    private final DashboardAccess access;
    private final HrDashboardService hrDashboard;
    private final HiringManagerDashboardService hiringManagerDashboard;
    private final DirectorDashboardService directorDashboard;
    private final InterviewerDashboardService interviewerDashboard;

    public InternalDashboardService(DashboardAccess access, HrDashboardService hrDashboard,
            HiringManagerDashboardService hiringManagerDashboard, DirectorDashboardService directorDashboard,
            InterviewerDashboardService interviewerDashboard) {
        this.access = access;
        this.hrDashboard = hrDashboard;
        this.hiringManagerDashboard = hiringManagerDashboard;
        this.directorDashboard = directorDashboard;
        this.interviewerDashboard = interviewerDashboard;
    }

    /** requestedWeek: tham số ?week= của lịch Interviewer; role khác bỏ qua. */
    @Transactional(readOnly = true)
    public DashboardResponse forUsername(String username, String requestedWeek) {
        User user = access.actor(username);
        access.requireInternal(user);

        String role = access.role(user);
        if (DashboardAccess.ROLE_HR.equals(role)) {
            return hrDashboard.build(user);
        }
        if (DashboardAccess.ROLE_HIRING_MANAGER.equals(role)) {
            return hiringManagerDashboard.build(user);
        }
        if (DashboardAccess.ROLE_DIRECTOR.equals(role)) {
            return directorDashboard.build(user);
        }
        return interviewerDashboard.build(user, requestedWeek);
    }
}
