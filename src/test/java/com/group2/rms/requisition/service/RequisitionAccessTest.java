package com.group2.rms.requisition.service;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RequisitionAccessTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private RequisitionAccess access;

    private User hrUser;
    private User hmUser;
    private User directorUser;

    @BeforeEach
    void setUp() {
        hrUser = User.builder().userId(1).username("hr_user").fullName("HR Lady")
                .role(Role.builder().roleName("HR").build()).build();
        hmUser = User.builder().userId(2).username("hm_user").fullName("HM Boss")
                .role(Role.builder().roleName("Hiring Manager").build()).build();
        directorUser = User.builder().userId(3).username("director_user").fullName("Big Director")
                .role(Role.builder().roleName("Director").build()).build();
    }

    @Test
    @DisplayName("HR xem Requisition Approved -> Được phép")
    void hr_canViewApproved() {
        JobRequisition req = JobRequisition.builder().requisitionId(1)
                .hiringManager(hmUser).approvalStatus("Approved").build();
        assertDoesNotThrow(() -> access.requireView(hrUser, req));
    }

    @Test
    @DisplayName("HR xem Requisition Draft -> Bị chặn 403 AccessDeniedException")
    void hr_cannotViewDraft() {
        JobRequisition req = JobRequisition.builder().requisitionId(1)
                .hiringManager(hmUser).approvalStatus("Draft").build();
        assertThrows(AccessDeniedException.class, () -> access.requireView(hrUser, req));
    }

    @Test
    @DisplayName("HR xem Requisition Pending_Director -> Bị chặn 403 AccessDeniedException")
    void hr_cannotViewPending() {
        JobRequisition req = JobRequisition.builder().requisitionId(1)
                .hiringManager(hmUser).approvalStatus("Pending_Director").build();
        assertThrows(AccessDeniedException.class, () -> access.requireView(hrUser, req));
    }

    @Test
    @DisplayName("HR xem Requisition Rejected -> Bị chặn 403 AccessDeniedException")
    void hr_cannotViewRejected() {
        JobRequisition req = JobRequisition.builder().requisitionId(1)
                .hiringManager(hmUser).approvalStatus("Rejected").build();
        assertThrows(AccessDeniedException.class, () -> access.requireView(hrUser, req));
    }

    @Test
    @DisplayName("HR dù là owner (hiringManagerId trùng) nhưng status chưa Approved -> Vẫn bị chặn")
    void hr_evenIfOwner_cannotViewNonApproved() {
        JobRequisition req = JobRequisition.builder().requisitionId(1)
                .hiringManager(hrUser).approvalStatus("Draft").build();
        assertThrows(AccessDeniedException.class, () -> access.requireView(hrUser, req));
    }

    @Test
    @DisplayName("Director xem Pending_Director và Approved -> Được phép; xem Draft -> Bị chặn")
    void director_viewRules() {
        JobRequisition pending = JobRequisition.builder().requisitionId(1).hiringManager(hmUser).approvalStatus("Pending_Director").build();
        JobRequisition approved = JobRequisition.builder().requisitionId(2).hiringManager(hmUser).approvalStatus("Approved").build();
        JobRequisition draft = JobRequisition.builder().requisitionId(3).hiringManager(hmUser).approvalStatus("Draft").build();

        assertDoesNotThrow(() -> access.requireView(directorUser, pending));
        assertDoesNotThrow(() -> access.requireView(directorUser, approved));
        assertThrows(AccessDeniedException.class, () -> access.requireView(directorUser, draft));
    }

    @Test
    @DisplayName("Hiring Manager xem request của mình -> Được phép; xem của người khác -> Bị chặn")
    void hm_ownerRules() {
        User otherHm = User.builder().userId(99).username("other").fullName("Other HM").role(Role.builder().roleName("Hiring Manager").build()).build();
        JobRequisition myReq = JobRequisition.builder().requisitionId(1).hiringManager(hmUser).approvalStatus("Draft").build();
        JobRequisition otherReq = JobRequisition.builder().requisitionId(2).hiringManager(otherHm).approvalStatus("Draft").build();

        assertDoesNotThrow(() -> access.requireView(hmUser, myReq));
        assertThrows(AccessDeniedException.class, () -> access.requireView(hmUser, otherReq));
    }
}
