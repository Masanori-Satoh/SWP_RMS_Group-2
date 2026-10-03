package com.group2.rms.service;

import com.group2.rms.dto.request.CreateOfferRequestDto;
import com.group2.rms.dto.response.OfferResponseDto;
import com.group2.rms.entity.Application;
import com.group2.rms.entity.OfferProposal;
import com.group2.rms.entity.User;
import com.group2.rms.repository.ApplicationRepository;
import com.group2.rms.repository.OfferProposalRepository;
import com.group2.rms.repository.UserRepository;
import com.group2.rms.service.impl.OfferServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTests {

    @Mock
    private OfferProposalRepository offerProposalRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OfferServiceImpl offerService;

    @Test
    @DisplayName("BR-OFF-01: Lương thử việc < 85% lương chính thức phải ném IllegalArgumentException")
    void testProbationSalaryLessThan85Percent_throwsException() {
        CreateOfferRequestDto request = CreateOfferRequestDto.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("16900000")) // 16.9M < 17M (85% của 20M)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            offerService.createOfferProposal(request);
        });

        assertTrue(ex.getMessage().contains("BR-OFF-01"));
    }

    @Test
    @DisplayName("BR-OFF-01: Lương thử việc đúng bằng 85% lương chính thức phải hợp lệ")
    void testProbationSalaryExactly85Percent_passes() {
        Application application = Application.builder().applicationId(1).build();
        User proposedBy = User.builder().userId(10).fullName("Hiring Manager").build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(userRepository.findById(10)).thenReturn(Optional.of(proposedBy));
        when(offerProposalRepository.findByApplication_ApplicationIdAndOfferStatusNotIn(eq(1), any()))
                .thenReturn(List.of());
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> {
            OfferProposal op = i.getArgument(0);
            op.setOfferId(101);
            return op;
        });

        CreateOfferRequestDto request = CreateOfferRequestDto.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000")) // Đúng 85% của 20M
                .expectedStartDate(LocalDate.now().plusWeeks(2))
                .proposedById(10)
                .build();

        OfferResponseDto response = offerService.createOfferProposal(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("20000000"), response.getProposedSalary());
        assertEquals(new BigDecimal("17000000"), response.getProbationSalary());
        assertEquals("Pending_Director", response.getOfferStatus());
    }

    @Test
    @DisplayName("GBR-14 / GBR-07: Khi tạo Offer mới, các Offer cũ đang active phải chuyển sang trạng thái Voided")
    void testSingleActiveOfferRule_deactivatesOldOffers() {
        Application application = Application.builder().applicationId(1).build();

        OfferProposal oldOffer1 = OfferProposal.builder()
                .offerId(1)
                .application(application)
                .offerStatus("Draft")
                .build();

        OfferProposal oldOffer2 = OfferProposal.builder()
                .offerId(2)
                .application(application)
                .offerStatus("Pending_Director")
                .build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(offerProposalRepository.findByApplication_ApplicationIdAndOfferStatusNotIn(eq(1), any()))
                .thenReturn(List.of(oldOffer1, oldOffer2));
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> {
            OfferProposal op = i.getArgument(0);
            op.setOfferId(999);
            return op;
        });

        CreateOfferRequestDto request = CreateOfferRequestDto.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("25500000")) // 85%
                .build();

        OfferResponseDto response = offerService.createOfferProposal(request);

        assertNotNull(response);
        // Kiểm tra các offer cũ đã bị Voided
        assertEquals("Voided", oldOffer1.getOfferStatus());
        assertEquals("Voided", oldOffer2.getOfferStatus());

        // Kiểm tra đã gọi saveAll và flush để lưu trạng thái Voided
        verify(offerProposalRepository).saveAll(List.of(oldOffer1, oldOffer2));
        verify(offerProposalRepository).flush();
    }
}
