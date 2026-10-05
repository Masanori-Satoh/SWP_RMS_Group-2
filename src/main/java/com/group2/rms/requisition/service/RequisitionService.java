package com.group2.rms.requisition.service;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;

import org.springframework.data.domain.Page;
public interface RequisitionService {
 Page<RequisitionResponse> search(int page,int size,String query,Integer department,String type,String status);
 Page<RequisitionResponse> search(int page,int size,String query,Integer department,String type,String status,String sort);
 Page<RequisitionResponse> search(int page,int size,String query,Integer department,String position,Integer round,String type,String status,String sort);
 java.util.List<com.group2.rms.user.entity.Department> getAvailableDepartments(com.group2.rms.user.entity.User actor);
 java.util.List<String> getAvailablePositions(com.group2.rms.user.entity.User actor);
 java.util.List<Integer> getAvailableRounds();
 long countVisible();
 RequisitionResponse getById(Integer id);
 RequisitionRequest getRequestDtoById(Integer id);
 RequisitionRequest copy(Integer id);
 Integer createRequisition(RequisitionRequest dto);
 void updateRequisition(Integer id,RequisitionRequest dto);
 void deleteRequisition(Integer id);
 void decide(Integer id,boolean approved,String comment);
 void withdraw(Integer id);
}
