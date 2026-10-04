package com.group2.rms.service;

import com.group2.rms.dashboard.*;
import com.group2.rms.user.repository.*;
import com.group2.rms.user.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** Real schema validation and SELECTs only. No CRUD writes, seed or schema initialization. */
@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=validate","spring.sql.init.mode=never","spring.jpa.show-sql=false"})
@Transactional(readOnly=true)
class DepartmentReadOnlyTests {
    @Autowired private DepartmentService service;
    @Autowired private DepartmentRepository departments;
    @Autowired private DashboardMetricsRepository metrics;
    @Autowired private UserRepository users;
    @Test void mappingsDerivedQueriesSearchAndCountsMatchCurrentDatabase() {
        assertEquals(departments.count(),metrics.departmentStatuses().stream().mapToLong(row->row.count()).sum());
        assertEquals(departments.count(),service.findDepartments("","",0).getTotalElements());
        assertEquals(0,service.findDepartments("__missing_department_98df12","",0).getTotalElements());
        assertTrue(service.managerChoices(-1).isEmpty());
        departments.findAll().forEach(d->{
            assertTrue(java.util.Set.of("Active","Inactive").contains(d.getDepartmentStatus()));
            assertEquals(d.getDepartmentName(),service.findForEdit(d.getDepartmentId()).name());
            service.managerChoices(d.getDepartmentId()).forEach(m->{
                var u=users.findById(m.id()).orElseThrow(); assertEquals("Active",u.getAccountStatus());
                assertEquals(d.getDepartmentId(),u.getDepartment().getDepartmentId()); assertNotEquals("Candidate",u.getRole().getRoleName());
            });
        });
    }
}
