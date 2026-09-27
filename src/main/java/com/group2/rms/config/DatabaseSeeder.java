package com.group2.rms.config;

import com.group2.rms.entity.Department;
import com.group2.rms.entity.Role;
import com.group2.rms.entity.User;
import com.group2.rms.repository.DepartmentRepository;
import com.group2.rms.repository.RoleRepository;
import com.group2.rms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DatabaseSeeder - Tự động insert dữ liệu khởi tạo (seed data) khi ứng dụng vừa khởi động.
 *
 * Implements CommandLineRunner → phương thức run() sẽ được Spring Boot gọi
 * ngay sau khi Application Context sẵn sàng (tương tự @PostConstruct nhưng chạy muộn hơn).
 *
 * Logic an toàn: Chỉ insert khi bảng Role CHƯA có dữ liệu (count == 0),
 * tránh lỗi duplicate data mỗi lần restart ứng dụng.
 *
 * ⚠️ ĐÃ TẮT: SmeDataSeeder thay thế với dữ liệu SME phong phú hơn.
 * Nếu muốn dùng lại bản đơn giản này, bỏ comment @Component bên dưới
 * và comment @Component ở SmeDataSeeder.
 */
// @Component  // ← TẮT: Dùng SmeDataSeeder thay thế
public class DatabaseSeeder implements CommandLineRunner {

    // ====================================================================
    // 1. INJECT CÁC DEPENDENCY QUA CONSTRUCTOR (Best Practice Spring Boot 3)
    //    - Không dùng @Autowired trên field, dùng Constructor Injection thay thế.
    //    - Khi class chỉ có 1 constructor, Spring tự hiểu và inject, không cần @Autowired.
    // ====================================================================
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(RoleRepository roleRepository,
                          DepartmentRepository departmentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ====================================================================
        // 2. KIỂM TRA: Nếu bảng Role đã có dữ liệu → bỏ qua, không insert lại.
        //    Đây là cách đơn giản nhất để tránh lỗi UniqueConstraintViolation
        //    khi restart ứng dụng nhiều lần.
        // ====================================================================
        if (roleRepository.count() > 0) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  ⏭️  Seed data đã tồn tại → Bỏ qua DatabaseSeeder.     ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            return;
        }

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  🌱  BẮT ĐẦU SEEDING DỮ LIỆU KHỞI TẠO...              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        // ====================================================================
        // 3. TẠO 5 ROLE
        //    Mỗi Role tương ứng với một vai trò cụ thể trong quy trình tuyển dụng.
        // ====================================================================
        Role roleAdmin       = Role.builder().roleName("System Admin").description("Quản trị viên hệ thống, toàn quyền quản lý").build();
        Role roleHR          = Role.builder().roleName("HR").description("Nhân sự, quản lý quy trình tuyển dụng").build();
        Role roleHiringMgr   = Role.builder().roleName("Hiring Manager").description("Quản lý tuyển dụng tại phòng ban").build();
        Role roleDirector    = Role.builder().roleName("Director").description("Giám đốc, phê duyệt yêu cầu tuyển dụng và offer").build();
        Role roleInterviewer = Role.builder().roleName("Interviewer").description("Người phỏng vấn ứng viên").build();

        // saveAll() lưu tất cả trong 1 batch, hiệu quả hơn gọi save() 5 lần
        roleRepository.saveAll(List.of(roleAdmin, roleHR, roleHiringMgr, roleDirector, roleInterviewer));
        System.out.println("  ✅ Đã tạo 5 Role: System Admin, HR, Hiring Manager, Director, Interviewer");

        // ====================================================================
        // 4. TẠO 3 DEPARTMENT MẪU
        //    Lưu ý: trường manager (trưởng phòng) để null vì User chưa tạo.
        //    Sau khi tạo User, có thể cập nhật lại nếu cần.
        // ====================================================================
        Department deptBOD = Department.builder().departmentName("Board of Directors").status("Active").build();
        Department deptHR  = Department.builder().departmentName("Human Resources").status("Active").build();
        Department deptIT  = Department.builder().departmentName("IT Department").status("Active").build();

        departmentRepository.saveAll(List.of(deptBOD, deptHR, deptIT));
        System.out.println("  ✅ Đã tạo 3 Department: Board of Directors, Human Resources, IT Department");

        // ====================================================================
        // 5. MÃ HOÁ MẬT KHẨU CHUNG
        //    Tất cả tài khoản mẫu dùng mật khẩu "123456" đã được mã hoá BCrypt.
        //    BCrypt tạo hash khác nhau mỗi lần encode, nhưng vẫn verify đúng.
        // ====================================================================
        String encodedPassword = passwordEncoder.encode("123456");

        // ====================================================================
        // 6. TẠO 5 USER MẪU
        //    Mỗi User gắn với 1 Role và 1 Department phù hợp:
        //    - System Admin   → IT Department  (quản trị hệ thống thuộc phòng IT)
        //    - HR             → Human Resources
        //    - Hiring Manager → IT Department  (trưởng nhóm tuyển dụng bên IT)
        //    - Director       → Board of Directors
        //    - Interviewer    → IT Department  (nhân viên kỹ thuật đi phỏng vấn)
        // ====================================================================
        User userAdmin = User.builder()
                .username("admin")
                .passwordHash(encodedPassword)
                .email("admin@rms.com")
                .fullName("Nguyễn Văn Admin")
                .role(roleAdmin)
                .department(deptIT)
                .accountStatus("Active")
                .build();

        User userHR = User.builder()
                .username("hr_user")
                .passwordHash(encodedPassword)
                .email("hr@rms.com")
                .fullName("Trần Thị HR")
                .role(roleHR)
                .department(deptHR)
                .accountStatus("Active")
                .build();

        User userHiringMgr = User.builder()
                .username("hiring_manager")
                .passwordHash(encodedPassword)
                .email("hiring.manager@rms.com")
                .fullName("Lê Văn Hiring Manager")
                .role(roleHiringMgr)
                .department(deptIT)
                .accountStatus("Active")
                .build();

        User userDirector = User.builder()
                .username("director")
                .passwordHash(encodedPassword)
                .email("director@rms.com")
                .fullName("Phạm Thị Director")
                .role(roleDirector)
                .department(deptBOD)
                .accountStatus("Active")
                .build();

        User userInterviewer = User.builder()
                .username("interviewer")
                .passwordHash(encodedPassword)
                .email("interviewer@rms.com")
                .fullName("Hoàng Văn Interviewer")
                .role(roleInterviewer)
                .department(deptIT)
                .accountStatus("Active")
                .build();

        userRepository.saveAll(List.of(userAdmin, userHR, userHiringMgr, userDirector, userInterviewer));
        System.out.println("  ✅ Đã tạo 5 User mẫu (mật khẩu: 123456, đã mã hoá BCrypt)");

        // ====================================================================
        // 7. THÔNG BÁO HOÀN TẤT
        // ====================================================================
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  🎉  SEEDING HOÀN TẤT! Kiểm tra trên SSMS để xác nhận. ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}
