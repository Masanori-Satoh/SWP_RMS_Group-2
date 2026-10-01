package com.group2.rms.service;

import java.util.List;

import com.group2.rms.dto.request.RequisitionRequest;
import com.group2.rms.dto.response.RequisitionResponse;

import org.springframework.data.domain.Page;

public interface RequisitionService {
    // Lay danh sach co phan trang
    Page<RequisitionResponse> getAllRequisitions(int page, int size);
    List<RequisitionResponse> getByStatus(String status);
    List<RequisitionResponse> getByHiringManager(Integer userId);

    //Lay 1 req theo id de xem chi tiet
    RequisitionResponse getById(Integer id);

    //Lay 1 req theo id de sua (form update)
    RequisitionRequest getRequestDtoById(Integer id);
    
    //Create
    void createRequisition(RequisitionRequest dto, Integer hiringManagerId);

    //update
    void updateRequisition(Integer id, RequisitionRequest dto);

    //delete
    void deleteRequisition(Integer id);

    //Change Status
    void submitForApproval(Integer id);
    void approveRequisition(Integer id, Integer directorId, String comment);
    void rejectRequisition(Integer id, Integer directorId, String comment);
}
