package com.group2.rms.requisition.service;

import com.group2.rms.requisition.dto.RequisitionListResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;

public interface RequisitionService {
 /** Dữ liệu cho màn danh sách: kết quả trang hiện tại, bộ lọc đã làm sạch, ô lọc theo role và các chip. */
 RequisitionListResponse getListPage(RequisitionSearchRequest filter);
 RequisitionResponse getById(Integer id);
 RequisitionRequest getRequestDtoById(Integer id);
 RequisitionRequest copy(Integer id);
 Integer createRequisition(RequisitionRequest dto);
 void updateRequisition(Integer id,RequisitionRequest dto);
 void deleteRequisition(Integer id);
 void decide(Integer id,boolean approved,String comment);
 void withdraw(Integer id);
}
