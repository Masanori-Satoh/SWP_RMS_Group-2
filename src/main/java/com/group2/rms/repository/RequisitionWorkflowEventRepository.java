package com.group2.rms.repository;
import com.group2.rms.entity.RequisitionWorkflowEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RequisitionWorkflowEventRepository extends JpaRepository<RequisitionWorkflowEvent,Long> {
 java.util.List<RequisitionWorkflowEvent> findByRequisition_RequisitionIdOrderByOccurredAtDescEventIdDesc(Integer id);
 void deleteByRequisition_RequisitionId(Integer id);
}
