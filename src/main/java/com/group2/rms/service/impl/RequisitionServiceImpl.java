package com.group2.rms.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group2.rms.dto.request.RequisitionRequestDto;
import com.group2.rms.dto.response.RequisitionResponseDto;
import com.group2.rms.entity.Department;
import com.group2.rms.entity.JobRequisition;
import com.group2.rms.entity.User;
import com.group2.rms.repository.DepartmentRepository;
import com.group2.rms.repository.JobRequisitionRepository;
import com.group2.rms.repository.RequisitionApprovalRepository;
import com.group2.rms.repository.ScreeningCriteriaRepository;
import com.group2.rms.repository.UserRepository;
import com.group2.rms.service.RequisitionService;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class RequisitionServiceImpl implements RequisitionService {

    private final JobRequisitionRepository requisitionRepo;

    private final UserRepository userRepo;

    private final DepartmentRepository departmentRepo;

    private final RequisitionApprovalRepository approvalRepo;

    private final ScreeningCriteriaRepository criteriaRepo;

    // constructor injection
    public RequisitionServiceImpl(
            JobRequisitionRepository requisitionRepo,
            UserRepository userRepo,
            DepartmentRepository departmentRepo,
            RequisitionApprovalRepository approvalRepo,
            ScreeningCriteriaRepository criteriaRepo) {
        this.requisitionRepo = requisitionRepo;
        this.userRepo = userRepo;
        this.departmentRepo = departmentRepo;
        this.approvalRepo = approvalRepo;
        this.criteriaRepo = criteriaRepo;
    }

    @Override
    public RequisitionResponseDto getById(Integer id) {
        JobRequisition req = requisitionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Not find Requisition with ID: " + id));
        return convertToDto(req);
    }

    @Override
    public Page<RequisitionResponseDto> getAllRequisitions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<JobRequisition> pagedResult = requisitionRepo.findAllByOrderByCreatedAtDesc(pageable);
        return pagedResult.map(this::convertToDto);
    }

    @Override
    public List<RequisitionResponseDto> getByStatus(String status) {
        // Loc theo 1 trang thai
        List<JobRequisition> list = requisitionRepo.findByApprovalStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::convertToDto).toList();
    }

    @Override
    public List<RequisitionResponseDto> getByHiringManager(Integer userId) {
        // Hiring Manager xem req cua chinh minh
        List<JobRequisition> list = requisitionRepo.findByHiringManager_UserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::convertToDto).toList();
    }

    @Override
    public void createRequisition(RequisitionRequestDto dto, Integer hiringManagerId) {
        // Khoi tao entity moi
        JobRequisition req = new JobRequisition();

        // set thong tin tu dto -> entity
        req.setTitle(dto.getTitle());
        req.setNumberOfPositions(dto.getNumberOfPositions());
        req.setEmploymentType(dto.getEmploymentType());
        req.setMinSalary(dto.getMinSalary());
        req.setMaxSalary(dto.getMaxSalary());
        req.setReasonForHiring(dto.getReasonForHiring());
        req.setJobDescription(dto.getJobDescription());
        req.setRequirementDetails(dto.getRequirementDetails());

        // Khi khoi tao -> Draft
        req.setApprovalStatus("Draft");

        // Tim va set Department(Khoa ngoai)
        Department dept = departmentRepo.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Not find any department"));
        req.setDepartment(dept);

        // Tim va set Hiring Manager
        User hm = userRepo.findById(hiringManagerId)
                .orElseThrow(() -> new RuntimeException("Not found Hiring Manager"));
        req.setHiringManager(hm);

        // Luu vao db
        requisitionRepo.save(req);

        // Luu danh sach tieu chi (Screening Criteria)
        if (dto.getScreeningCriteria() != null && !dto.getScreeningCriteria().isEmpty()) {
            List<com.group2.rms.entity.ScreeningCriteria> criteriaList = dto.getScreeningCriteria().stream().map(c -> {
                com.group2.rms.entity.ScreeningCriteria entity = new com.group2.rms.entity.ScreeningCriteria();
                entity.setRequisition(req);
                entity.setCriteriaName(c.getCriteriaName());
                entity.setCriteriaType(c.getCriteriaType());
                entity.setRequiredValue(c.getRequiredValue());
                entity.setWeight(c.getWeight() != null ? c.getWeight() : java.math.BigDecimal.ONE);
                entity.setIsMandatory(c.getIsMandatory() != null ? c.getIsMandatory() : false);
                return entity;
            }).toList();
            criteriaRepo.saveAll(criteriaList);
        }
    }

    @Override
    public void updateRequisition(Integer id, RequisitionRequestDto dto) {
        // Tí code sau
    }

    @Override
    public void submitForApproval(Integer id) {
        // Tí code sau
    }

    @Override
    public void approveRequisition(Integer id, Integer directorId, String comment) {
        // Tí code sau
    }

    @Override
    public void rejectRequisition(Integer id, Integer directorId, String comment) {
        // Tí code sau
    }

    private RequisitionResponseDto convertToDto(JobRequisition req) {
        java.util.List<com.group2.rms.dto.response.ScreeningCriteriaDto> criteriaDtoList = null;
        if (req.getScreeningCriteria() != null) {
            criteriaDtoList = req.getScreeningCriteria().stream().map(c -> {
                com.group2.rms.dto.response.ScreeningCriteriaDto cDto = new com.group2.rms.dto.response.ScreeningCriteriaDto();
                cDto.setCriteriaId(c.getCriteriaId());
                cDto.setCriteriaName(c.getCriteriaName());
                cDto.setCriteriaType(c.getCriteriaType());
                cDto.setRequiredValue(c.getRequiredValue());
                cDto.setWeight(c.getWeight());
                cDto.setIsMandatory(c.getIsMandatory());
                return cDto;
            }).toList();
        }

        return RequisitionResponseDto.builder()
                .requisitionId(req.getRequisitionId())
                .title(req.getTitle())
                .departmentName(req.getDepartment().getDepartmentName())
                .hiringManagerName(req.getHiringManager().getFullName())
                .numberOfPositions(req.getNumberOfPositions())
                .employmentType(req.getEmploymentType())
                .approvalStatus(req.getApprovalStatus())
                .createdAt(req.getCreatedAt())
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .reasonForHiring(req.getReasonForHiring())
                .jobDescription(req.getJobDescription())
                .requirementDetails(req.getRequirementDetails())
                .screeningCriteria(criteriaDtoList)
                .build();
    }
}
