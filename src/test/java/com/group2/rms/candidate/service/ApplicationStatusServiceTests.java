package com.group2.rms.candidate.service;

import com.group2.rms.candidate.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Hook đổi trạng thái đơn theo phỏng vấn. Câu {@code transition} thật (đổi có điều kiện) được kiểm ở
 * {@code ApplicationPipelineQueryTests}; ở đây chỉ kiểm tham số: từ trạng thái nào sang trạng thái nào.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationStatusServiceTests {

    private static final int ID = 298;

    @Mock ApplicationRepository applications;

    private ApplicationStatusService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationStatusService(applications);
    }

    @Test
    void schedulingMovesOnlyHiringManagerPassedToInterviewing() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onInterviewScheduled(ID);

        verify(applications).transition(ID, Set.of("HM_Passed"), "Interviewing");
    }

    @Test
    void passedDecisionMovesOnlyHiringManagerPassedToInterviewing() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onInterviewDecided(ID, true);

        verify(applications).transition(ID, Set.of("HM_Passed"), "Interviewing");
    }

    @Test
    void failedDecisionRejectsApplicationsStillInInterview() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onInterviewDecided(ID, false);

        verify(applications).transition(ID, Set.of("HM_Passed", "Interviewing"), "Rejected");
    }

    @Test
    void acceptedOfferMovesOnlyOfferedToHired() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onOfferAnswered(ID, true);

        verify(applications).transition(ID, Set.of("Offered"), "Hired");
    }

    @Test
    void declinedOfferMovesOnlyOfferedToRejected() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(1);

        service.onOfferAnswered(ID, false);

        verify(applications).transition(ID, Set.of("Offered"), "Rejected");
    }

    @Test
    void offerAnsweredOnApplicationNotOfferedIsLeftUntouchedWithoutError() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(0);

        assertDoesNotThrow(() -> service.onOfferAnswered(ID, true));
        assertDoesNotThrow(() -> service.onOfferAnswered(ID, false));
    }

    /** Đơn đã Offered / Hired / Rejected: repository trả 0, hook không được chặn thao tác của module interview. */
    @Test
    void applicationAlreadyMovedOnIsLeftUntouchedWithoutError() {
        when(applications.transition(anyInt(), anyCollection(), anyString())).thenReturn(0);

        assertDoesNotThrow(() -> service.onInterviewScheduled(ID));
        assertDoesNotThrow(() -> service.onInterviewDecided(ID, true));
        assertDoesNotThrow(() -> service.onInterviewDecided(ID, false));
        verify(applications, times(3)).transition(eq(ID), anyCollection(), anyString());
    }
}
