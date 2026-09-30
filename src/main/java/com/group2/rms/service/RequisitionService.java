package com.group2.rms.service;
import com.group2.rms.dto.request.RequisitionRequestDto;
import com.group2.rms.dto.response.RequisitionResponseDto;
import org.springframework.data.domain.Page;
public interface RequisitionService {
 Page<RequisitionResponseDto> search(int page,int size,String query,Integer department,String type,String status);
 long countVisible();
 RequisitionResponseDto getById(Integer id);
 RequisitionRequestDto getRequestDtoById(Integer id);
 RequisitionRequestDto copy(Integer id);
 Integer createRequisition(RequisitionRequestDto dto);
 void updateRequisition(Integer id,RequisitionRequestDto dto);
 void deleteRequisition(Integer id,Long version);
 void decide(Integer id,Long version,boolean approved,String comment);
 void withdraw(Integer id,Long version);
}
