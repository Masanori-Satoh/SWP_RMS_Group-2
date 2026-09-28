package com.group2.rms.service;

import java.util.List;

import com.group2.rms.dto.request.RequisitionRequestDto;
import com.group2.rms.dto.response.RequisitionResponseDto;

import org.springframework.data.domain.Page;

public interface RequisitionService {
    // Lay danh sach co phan trang
    Page<RequisitionResponseDto> getAllRequisitions(int page, int size);
    List<RequisitionResponseDto> getByStatus(String status);
    List<RequisitionResponseDto> getByHiringManager(Integer userId);

    //Lay 1 req theo id
    RequisitionResponseDto getById(Integer id);
    
    //Create
    void createRequisition(RequisitionRequestDto dto, Integer hiringManagerId);

    //update
    void updateRequisition(Integer id, RequisitionRequestDto dto);

    //Change Status
    void submitForApproval(Integer id);
    void approveRequisition(Integer id, Integer directorId, String comment);
    void rejectRequisition(Integer id, Integer directorId, String comment);
}
