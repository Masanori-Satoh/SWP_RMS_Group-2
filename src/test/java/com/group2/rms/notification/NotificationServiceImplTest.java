package com.group2.rms.notification;

import com.group2.rms.notification.constants.NotificationEventType;
import com.group2.rms.notification.constants.NotificationMessages;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.candidate.entity.Application;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock NotificationRepository repository;
    @Mock UserRepository users;
    @InjectMocks NotificationServiceImpl service;

    private static User user(int id, String role) {
        return User.builder().userId(id).role(Role.builder().roleName(role).build())
                .accountStatus("Active").build();
    }

    @Test
    void everyEventHasVietnameseMessageWithinDatabaseLimits() {
        for (NotificationEventType event : NotificationEventType.values()) {
            var message = NotificationMessages.forEvent(event, false);
            assertFalse(message.title().isBlank(), event.name());
            assertFalse(message.content().isBlank(), event.name());
            assertTrue(message.title().length() <= 255, event.name());
            assertTrue(message.content().length() <= 1000, event.name());
            assertTrue(event.name().length() <= 50, event.name());
        }
    }

    @Test
    void sameEventIsDeliveredToEachRecipientAndDeduplicatedPerRecipient() {
        User manager = user(10, "Hiring Manager");
        User hr = user(20, "HR");
        service.publish(manager, NotificationEventType.REQUISITION_APPROVED, "JobRequisition", 5,
                "/requisitions/5", "REQ_APPROVE_5", false);
        service.publish(hr, NotificationEventType.REQUISITION_APPROVED, "JobRequisition", 5,
                "/requisitions/5", "REQ_APPROVE_5", true);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(saved.capture());
        assertEquals(manager, saved.getAllValues().get(0).getRecipient());
        assertEquals(hr, saved.getAllValues().get(1).getRecipient());
        assertNotEquals(saved.getAllValues().get(0).getTitle(), saved.getAllValues().get(1).getTitle());
        assertEquals("REQUISITION_APPROVED", saved.getAllValues().get(0).getEventType());
        assertEquals("JobRequisition", saved.getAllValues().get(0).getReferenceType());
    }

    @Test
    void duplicateOrInactiveRecipientIsSkipped() {
        User manager = user(10, "Hiring Manager");
        when(repository.existsByRecipient_UserIdAndEventId(10, "event-1")).thenReturn(true);
        service.publish(manager, NotificationEventType.JOB_POSTING_CLOSED, "JobPosting", 5,
                "/jobs/5", "event-1", false);
        service.publish(User.builder().userId(11).accountStatus("Locked").build(),
                NotificationEventType.JOB_POSTING_CLOSED, "JobPosting", 5, "/jobs/5", "event-2", false);
        verify(repository, never()).save(any());
    }

    @Test
    void applicationReceivedFallsBackToActiveHrWhenPostingCreatorIsAdmin() {
        User hr = user(2, "HR");
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of(hr, user(3, "Director")));
        JobPosting posting = JobPosting.builder().createdBy(user(1, "System Admin")).build();
        service.publishToResponsibleHr(posting, NotificationEventType.APPLICATION_RECEIVED,
                "Application", 41, "/applications/41", "APP_SUBMIT_41");
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertEquals(hr, saved.getValue().getRecipient());
    }

    @Test
    void submittedRequisitionGoesToActiveDirectorsOnly() {
        User director = user(1, "Director");
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of(director, user(2, "HR"),
                User.builder().userId(3).accountStatus("Locked")
                        .role(Role.builder().roleName("Director").build()).build()));
        JobRequisition requisition = JobRequisition.builder().requisitionId(12)
                .submittedAt(LocalDateTime.of(2026, 10, 14, 10, 0)).build();
        service.notifyRequisitionSubmitted(requisition);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertEquals(director, saved.getValue().getRecipient());
        assertEquals("REQUISITION_SUBMITTED", saved.getValue().getEventType());
        assertEquals("/requisitions/12", saved.getValue().getLinkUrl());
    }

    @Test
    void approvedRequisitionGoesToManagerAndActiveHrWithDifferentVietnameseTitles() {
        User manager = user(1, "Hiring Manager");
        User hr = user(2, "HR");
        when(users.findAllWithRoleAndDepartment()).thenReturn(List.of(hr, user(3, "Director")));
        JobRequisition requisition = JobRequisition.builder().requisitionId(12).hiringManager(manager)
                .decidedAt(LocalDateTime.of(2026, 10, 14, 11, 0)).build();
        service.notifyRequisitionApproved(requisition);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(saved.capture());
        assertEquals("Yêu cầu tuyển dụng đã được phê duyệt", saved.getAllValues().get(0).getTitle());
        assertEquals("Có yêu cầu tuyển dụng mới được phê duyệt", saved.getAllValues().get(1).getTitle());
        assertEquals(saved.getAllValues().get(0).getEventId(), saved.getAllValues().get(1).getEventId());
    }

    @Test
    void acceptedOfferGoesToHrAndManagerWithAuthorizedDestinations() {
        User manager = user(1, "Hiring Manager");
        User hr = user(2, "HR");
        JobRequisition requisition = JobRequisition.builder().hiringManager(manager).build();
        JobPosting posting = JobPosting.builder().requisition(requisition).build();
        Application application = Application.builder().applicationId(41).jobPosting(posting).build();
        OfferProposal offer = OfferProposal.builder().offerId(9).application(application).proposedBy(hr).build();
        service.notifyOfferResponseFromCandidate(offer, true, null);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(saved.capture());
        assertEquals(hr, saved.getAllValues().get(0).getRecipient());
        assertEquals("/offers/9", saved.getAllValues().get(0).getLinkUrl());
        assertEquals(manager, saved.getAllValues().get(1).getRecipient());
        assertEquals("/applications/41", saved.getAllValues().get(1).getLinkUrl());
    }

    @Test
    void offerReadUsesCandidateAndOfferScope() {
        service.markOfferNotificationRead(8, 25);
        verify(repository).markOfferNotificationRead(8, "25");
    }
}
