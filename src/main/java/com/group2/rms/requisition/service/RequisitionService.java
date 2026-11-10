package com.group2.rms.requisition.service;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;

public interface RequisitionService {
    // Returns requisition details by ID.
    RequisitionResponse getById(Integer id);

    // Prepares the requisition edit form.
    RequisitionRequest prepareEditForm(Integer id);

    // Prepares a copy of the source requisition.
    RequisitionRequest copy(Integer id);

    // Creates a requisition.
    Integer createRequisition(RequisitionRequest request);

    // Updates an existing requisition.
    void updateRequisition(Integer id, RequisitionRequest request);

    // Deletes an eligible requisition.
    void deleteRequisition(Integer id);

    // Processes the approval decision.
    void decide(Integer id, boolean approved, String comment);

}
