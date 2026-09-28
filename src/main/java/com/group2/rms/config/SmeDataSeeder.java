package com.group2.rms.config;

import com.group2.rms.entity.*;
import com.group2.rms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SmeDataSeeder - Sinh dữ liệu mẫu quy mô công ty Vừa và Nhỏ (SME).
 *
 * Thứ tự insert đảm bảo không vi phạm Foreign Key:
 *   Role -> Department -> User -> JobRequisition -> JobPosting
 *   -> Candidate -> ApplicationReview (Resume) -> Application
 *   -> InterviewSchedule -> OfferProposal
 *
 * YÊU CẦU CẤU HÌNH:
 * - Cần dòng "hibernate.use_nationalized_character_data=true"
 *   trong application.properties để Hibernate dùng NVARCHAR thay VARCHAR.
 *   NVARCHAR hỗ trợ Unicode → tiếng Việt có dấu lưu đúng trong MS SQL Server.
 * - Đã điền đầy đủ data cho các trường như overallScore, hrReviewNotes,
 *   reviewedBy, avatarUrl,... để dữ liệu trông chuyên nghiệp.
 */
@Component
@Order(2)
public class SmeDataSeeder implements CommandLineRunner {

    // ====================================================================
    // HANG SO QUY MO DU LIEU SME
    // ====================================================================
    private static final int NUM_USERS_ADMIN        = 1;
    private static final int NUM_USERS_HR            = 2;
    private static final int NUM_USERS_HIRING_MGR    = 5;
    private static final int NUM_USERS_DIRECTOR      = 1;
    private static final int NUM_USERS_INTERVIEWER   = 6;
    private static final int NUM_JOB_REQUISITIONS    = 15;
    private static final int NUM_JOB_POSTINGS        = 15;
    private static final int NUM_CANDIDATES          = 50;
    private static final int NUM_APPLICATIONS        = 100;
    private static final int NUM_INTERVIEW_SCHEDULES = 30;
    private static final int NUM_OFFER_PROPOSALS     = 10;

    // ====================================================================
    // CONSTRUCTOR INJECTION
    // ====================================================================
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final JobRequisitionRepository jobRequisitionRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationReviewRepository applicationReviewRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final OfferProposalRepository offerProposalRepository;
    private final PasswordEncoder passwordEncoder;

    public SmeDataSeeder(RoleRepository roleRepository,
                         DepartmentRepository departmentRepository,
                         UserRepository userRepository,
                         JobRequisitionRepository jobRequisitionRepository,
                         JobPostingRepository jobPostingRepository,
                         CandidateRepository candidateRepository,
                         ApplicationReviewRepository applicationReviewRepository,
                         ApplicationRepository applicationRepository,
                         InterviewScheduleRepository interviewScheduleRepository,
                         OfferProposalRepository offerProposalRepository,
                         PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.jobRequisitionRepository = jobRequisitionRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.candidateRepository = candidateRepository;
        this.applicationReviewRepository = applicationReviewRepository;
        this.applicationRepository = applicationRepository;
        this.interviewScheduleRepository = interviewScheduleRepository;
        this.offerProposalRepository = offerProposalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ====================================================================
    // NGUON DU LIEU GIA (Fake Data) - Ten Viet, Job Title IT, v.v.
    // ====================================================================
    private static final String[] FIRST_NAMES = {
        "John", "Jane", "Michael", "Emily", "David", "Sarah", "James", "Laura",
        "Robert", "Emma", "William", "Olivia", "Richard", "Sophia", "Thomas", "Mia"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson",
        "Moore", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin"
    };

    

    private static final String[] JOB_TITLES = {
        "Senior Java Developer", "Frontend React Developer", "DevOps Engineer",
        "Data Analyst", "UI/UX Designer", "QA Automation Engineer",
        "Backend .NET Developer", "Mobile Flutter Developer",
        "Cloud Solutions Architect", "AI/ML Engineer",
        "Fullstack JavaScript Developer", "Cybersecurity Specialist",
        "Scrum Master", "Business Analyst", "Technical Project Manager"
    };

    private static final String[] EMPLOYMENT_TYPES = {
        "Full-time", "Full-time", "Full-time", "Part-time", "Internship", "Contract"
    };

    private static final String[] WORK_LOCATIONS = {
        "District 1, HCMC", "District 7, HCMC", "Cau Giay, Hanoi",
        "Dong Da, Hanoi", "Hai Chau, Da Nang", "Remote"
    };

    private static final String[] CANDIDATE_SOURCES = {
        "Website", "TopCV", "Referral", "LinkedIn", "Website", "TopCV"
    };

    private static final String[] GENDERS = {"Male", "Female", "Other"};

    private static final String[] ADDRESSES = {
        "123 Nguyen Hue, Dist 1, HCMC", "456 Le Loi, Dist 1, HCMC",
        "789 Tran Hung Dao, Dist 5, HCMC", "12 Pham Ngoc Thach, Dist 3, HCMC",
        "34 Hoang Dieu, Dist 4, HCMC", "56 Nguyen Trai, Dist 5, HCMC",
        "78 Lac Long Quan, Tay Ho, HN", "90 Giai Phong, Dong Da, HN",
        "11 Tran Phu, Hai Chau, DN", "22 Nguyen Van Linh, Hai Chau, DN"
    };

    private static final String[] BENEFITS = {
        "Premium Health Insurance, 13th-month salary, New Macbook",
        "Quarterly team building, Free parking, Gym & Yoga at office",
        "Salary review twice a year, $500/year training budget, WFH 2 days/week",
        "Performance bonus, ESOP, Free snack bar & coffee"
    };

    private static final String[] INTERVIEW_ROUNDS = {
        "Round 1 - HR", "Round 2 - Technical", "Final"
    };

    private static final String[] INTERVIEW_FORMATS = {
        "Online_GoogleMeet", "Offline_Office"
    };

    /** HR review notes mẫu */
    private static final String[] HR_REVIEW_NOTES = {
        "Complete CV, experience matches JD. Recommend for interview.",
        "Basic skills are fine, needs technical assessment.",
        "Good profile, has AWS cert. Priority for early interview.",
        "Decent profile, 3 years equivalent experience. Needs coding test.",
        "Impressive CV, has open-source projects. Recommend immediate interview."
    };

    /** HM review notes mẫu */
    private static final String[] HM_REVIEW_NOTES = {
        "Candidate answered technical questions well. Strong problem-solving.",
        "Has System Design knowledge, but not deep. Needs training.",
        "Good practical experience, worked with similar tech stack.",
        "Good communication, high teamwork. Fits company culture.",
        "Excellent candidate, led a team of 5. Recommend hire."
    };

    private final Random random = new Random(42); // Seed cố định để data lặp lại được

    // ====================================================================
    // ENTRY POINT - CommandLineRunner
    // ====================================================================
    @Override
    public void run(String... args) {

        // Guard: Neu da co du lieu -> bo qua
        if (roleRepository.count() > 0) {
            System.out.println("========================================================");
            System.out.println("  [SKIP] SME Seed data da ton tai -> Bo qua SmeDataSeeder.");
            System.out.println("========================================================");
            return;
        }

        System.out.println("========================================================");
        System.out.println("  [START] BAT DAU SEEDING DU LIEU SME (~300 records)...");
        System.out.println("========================================================");

        // ================================================================
        // BUOC 1: TAO 5 ROLE (bang cha, khong FK ra ngoai)
        // ================================================================
        Role rAdmin       = Role.builder().roleName("System Admin").description("System administrator, full access").build();
        Role rHR          = Role.builder().roleName("HR").description("HR, manages recruitment process").build();
        Role rHiringMgr   = Role.builder().roleName("Hiring Manager").description("Hiring Manager at department level").build();
        Role rDirector    = Role.builder().roleName("Director").description("Director, approves requisitions and offers").build();
        Role rInterviewer = Role.builder().roleName("Interviewer").description("Interviewer for candidates").build();
        roleRepository.saveAll(List.of(rAdmin, rHR, rHiringMgr, rDirector, rInterviewer));
        System.out.println("  [OK] 5 Roles da tao.");

        // ================================================================
        // BUOC 2: TAO 4 DEPARTMENT (FK manager = null tam, update sau)
        // ================================================================
        Department dIT   = Department.builder().departmentName("IT Department").status("Active").build();
        Department dHR   = Department.builder().departmentName("Human Resources").status("Active").build();
        Department dMkt  = Department.builder().departmentName("Marketing").status("Active").build();
        Department dBOD  = Department.builder().departmentName("Board of Directors").status("Active").build();
        departmentRepository.saveAll(List.of(dIT, dHR, dMkt, dBOD));
        System.out.println("  [OK] 4 Departments da tao.");

        // ================================================================
        // BUOC 3: MA HOA MAT KHAU - CHI GOI encode() DUNG 1 LAN
        // ================================================================
        String defaultPasswordHash = passwordEncoder.encode("123456");

        // ================================================================
        // BUOC 4: TAO 15 USERS (1 Admin + 2 HR + 5 HM + 1 Dir + 6 IV)
        // ================================================================
        List<User> allUsers = new ArrayList<>();

        // 4a. 1 System Admin -> IT Department
        allUsers.addAll(buildUsers(NUM_USERS_ADMIN, rAdmin, dIT, "admin", defaultPasswordHash));

        // 4b. 2 HR -> Human Resources
        allUsers.addAll(buildUsers(NUM_USERS_HR, rHR, dHR, "hr", defaultPasswordHash));

        // 4c. 5 Hiring Managers -> phan deu vao IT(3), Marketing(2)
        List<Department> hmDepts = List.of(dIT, dIT, dIT, dMkt, dMkt);
        for (int i = 0; i < NUM_USERS_HIRING_MGR; i++) {
            allUsers.add(buildSingleUser("hm" + (i + 1), rHiringMgr, hmDepts.get(i), defaultPasswordHash));
        }

        // 4d. 1 Director -> Board of Directors
        allUsers.addAll(buildUsers(NUM_USERS_DIRECTOR, rDirector, dBOD, "director", defaultPasswordHash));

        // 4e. 6 Interviewers -> phan deu IT(4), Marketing(2)
        List<Department> ivDepts = List.of(dIT, dIT, dIT, dIT, dMkt, dMkt);
        for (int i = 0; i < NUM_USERS_INTERVIEWER; i++) {
            allUsers.add(buildSingleUser("interviewer" + (i + 1), rInterviewer, ivDepts.get(i), defaultPasswordHash));
        }

        userRepository.saveAll(allUsers);
        System.out.println("  [OK] " + allUsers.size() + " Users da tao (mat khau chung: 123456).");

        // Phan loai user theo role de dung tiep
        List<User> hrUsers = allUsers.stream().filter(u -> u.getRole().equals(rHR)).toList();
        List<User> hiringManagers = allUsers.stream().filter(u -> u.getRole().equals(rHiringMgr)).toList();

        // ================================================================
        // BUOC 5: TAO 15 JOB REQUISITIONS (FK -> Department, User/HM)
        // ================================================================
        List<JobRequisition> requisitions = new ArrayList<>();

        for (int i = 0; i < NUM_JOB_REQUISITIONS; i++) {
            User hm = hiringManagers.get(i % hiringManagers.size());
            Department dept = hm.getDepartment();

            BigDecimal minSalary = BigDecimal.valueOf(10_000_000L + random.nextInt(20_000_000));
            BigDecimal maxSalary = minSalary.add(BigDecimal.valueOf(5_000_000L + random.nextInt(15_000_000)));

            requisitions.add(JobRequisition.builder()
                    .title(JOB_TITLES[i % JOB_TITLES.length])
                    .department(dept)
                    .hiringManager(hm)
                    .numberOfPositions(1 + random.nextInt(3))
                    .employmentType(pickRandom(EMPLOYMENT_TYPES))
                    .minSalary(minSalary)
                    .maxSalary(maxSalary)
                    .reasonForHiring(i % 2 == 0 ? "New headcount for expansion" : "Replacement for resigned employee")
                    .jobDescription(generateJobDescription(JOB_TITLES[i % JOB_TITLES.length]))
                    .requirementDetails(generateRequirements(JOB_TITLES[i % JOB_TITLES.length]))
                    .approvalStatus("Approved")
                    .build());
        }
        jobRequisitionRepository.saveAll(requisitions);
        System.out.println("  [OK] " + NUM_JOB_REQUISITIONS + " Job Requisitions da tao.");

        // ================================================================
        // BUOC 6: TAO 15 JOB POSTINGS (FK -> JobRequisition, User/HR)
        // ================================================================
        List<JobPosting> postings = new ArrayList<>();
        for (int i = 0; i < NUM_JOB_POSTINGS; i++) {
            JobRequisition req = requisitions.get(i);
            User hr = hrUsers.get(i % hrUsers.size());
            LocalDate postDate = LocalDate.now().minusDays(random.nextInt(60));
            LocalDate deadline = postDate.plusDays(30 + random.nextInt(30));

            postings.add(JobPosting.builder()
                    .requisition(req)
                    .postingTitle(req.getTitle())
                    .jobDescription(req.getJobDescription())
                    .jobRequirements(req.getRequirementDetails())
                    .benefits(pickRandom(BENEFITS))
                    .salaryDisplay(formatSalaryDisplay(req.getMinSalary(), req.getMaxSalary()))
                    .workLocation(pickRandom(WORK_LOCATIONS))
                    .postingDate(postDate)
                    .applicationDeadline(deadline)
                    .postingStatus("Published")
                    .createdBy(hr)
                    .build());
        }
        jobPostingRepository.saveAll(postings);
        System.out.println("  [OK] " + NUM_JOB_POSTINGS + " Job Postings da tao.");

        // ================================================================
        // BUOC 7: TAO 50 CANDIDATES (bang cha, khong FK ra ngoai)
        // Dien DAY DU tat ca truong, KHONG de NULL
        // ================================================================
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < NUM_CANDIDATES; i++) {
            String fullName = generateEnglishName();
            String emailSlug = toEmailSlug(fullName) + (i + 1);

            candidates.add(Candidate.builder()
                    .fullName(fullName)
                    .email(emailSlug + "@gmail.com")
                    .phoneNumber(generatePhoneNumber())
                    .dateOfBirth(LocalDate.of(
                            1990 + random.nextInt(10),
                            1 + random.nextInt(12),
                            1 + random.nextInt(28)))
                    .gender(pickRandom(GENDERS))
                    .address(pickRandom(ADDRESSES))
                    .linkedInUrl("https://linkedin.com/in/" + emailSlug)
                    .portfolioUrl("https://github.com/" + emailSlug)
                    .candidateSource(pickRandom(CANDIDATE_SOURCES))
                    .build());
        }
        candidateRepository.saveAll(candidates);
        System.out.println("  [OK] " + NUM_CANDIDATES + " Candidates da tao.");

        // ================================================================
        // BUOC 8: TAO 50 RESUMES (ApplicationReview, FK -> Candidate)
        //         Moi Candidate co 1 file CV. Dien DAY DU, KHONG NULL.
        // ================================================================
        List<ApplicationReview> resumes = new ArrayList<>();
        for (int i = 0; i < NUM_CANDIDATES; i++) {
            Candidate c = candidates.get(i);
            String slugName = toEmailSlug(c.getFullName());
            String fileType = random.nextBoolean() ? "PDF" : "DOCX";

            resumes.add(ApplicationReview.builder()
                    .candidate(c)
                    .fileName("CV_" + slugName + "_" + (i + 1) + "." + fileType.toLowerCase())
                    .filePath("/uploads/cv/" + slugName + "_" + (i + 1) + "." + fileType.toLowerCase())
                    .fileType(fileType)
                    .fileSize((long) (80_000 + random.nextInt(450_000))) // 80KB - 530KB
                    .build());
        }
        applicationReviewRepository.saveAll(resumes);
        System.out.println("  [OK] " + NUM_CANDIDATES + " Resumes (ApplicationReview) da tao.");

        // ================================================================
        // BUOC 9: TAO 100 APPLICATIONS (FK -> Candidate, JobPosting)
        //         Dien DAY DU: overallScore, hrReviewNotes, hmReviewNotes,
        //         reviewedBy, reviewedAt cho cac Application da qua Screening.
        // ================================================================
        Set<String> usedPairs = new HashSet<>();
        List<Application> applications = new ArrayList<>();

        // Pool status phan bo da dang
        String[] statusPool = buildStatusPool();

        int attempts = 0;
        while (applications.size() < NUM_APPLICATIONS && attempts < NUM_APPLICATIONS * 5) {
            attempts++;
            Candidate c = candidates.get(random.nextInt(candidates.size()));
            JobPosting jp = postings.get(random.nextInt(postings.size()));
            String pairKey = c.getCandidateId() + "-" + jp.getJobPostingId();

            // Tranh 1 ung vien nop 2 lan vao cung 1 vi tri
            if (usedPairs.contains(pairKey)) continue;
            usedPairs.add(pairKey);

            int idx = applications.size();
            String status = statusPool[idx % statusPool.length];
            ApplicationReview resume = resumes.get(candidates.indexOf(c));
            User reviewerHR = hrUsers.get(idx % hrUsers.size());
            User reviewerHM = hiringManagers.get(idx % hiringManagers.size());

            // Xay dung Application builder - DIEN DU data theo tung trang thai
            Application.ApplicationBuilder appBuilder = Application.builder()
                    .candidate(c)
                    .jobPosting(jp)
                    .appliedCvUrl(resume.getFilePath())
                    .submissionDate(LocalDateTime.now().minusDays(5 + random.nextInt(40)))
                    .applicationStatus(status);

            // Cac Application da qua giai doan "Applied" -> co HR review
            if (!status.equals("Applied")) {
                appBuilder.hrReviewNotes(pickRandom(HR_REVIEW_NOTES));
                appBuilder.reviewedBy(reviewerHR);
                appBuilder.reviewedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
            }

            // Cac Application da qua Interviewing -> co HM review + diem
            if (status.equals("Interviewing") || status.equals("Offered")
                    || status.equals("Hired") || status.equals("Rejected")) {
                appBuilder.hmReviewNotes(pickRandom(HM_REVIEW_NOTES));
                appBuilder.reviewedBy(reviewerHM); // cap nhat reviewer moi nhat la HM
                // Diem tu 40.00 den 95.00
                BigDecimal score = BigDecimal.valueOf(40 + random.nextInt(5500) / 100.0)
                        .setScale(2, RoundingMode.HALF_UP);
                appBuilder.overallScore(score);
            }

            // Offered/Hired co diem cao hon (70-98)
            if (status.equals("Offered") || status.equals("Hired")) {
                BigDecimal highScore = BigDecimal.valueOf(70 + random.nextInt(2800) / 100.0)
                        .setScale(2, RoundingMode.HALF_UP);
                appBuilder.overallScore(highScore);
            }

            applications.add(appBuilder.build());
        }
        applicationRepository.saveAll(applications);
        System.out.println("  [OK] " + applications.size() + " Applications da tao (day du review notes & scores).");

        // ================================================================
        // BUOC 10: TAO 30 INTERVIEW SCHEDULES
        //          Lay tu Application co status "Interviewing" / "Interview_Pending"
        // ================================================================
        List<Application> interviewableApps = applications.stream()
                .filter(a -> "Interviewing".equals(a.getApplicationStatus())
                          || "Interview_Pending".equals(a.getApplicationStatus()))
                .collect(Collectors.toList());

        // Neu khong du, bo sung tu Screening
        if (interviewableApps.size() < NUM_INTERVIEW_SCHEDULES) {
            applications.stream()
                    .filter(a -> "Screening".equals(a.getApplicationStatus()))
                    .limit((long) NUM_INTERVIEW_SCHEDULES - interviewableApps.size())
                    .forEach(interviewableApps::add);
        }

        List<InterviewSchedule> schedules = new ArrayList<>();
        int scheduleCount = Math.min(NUM_INTERVIEW_SCHEDULES, interviewableApps.size());

        for (int i = 0; i < scheduleCount; i++) {
            Application app = interviewableApps.get(i);
            User hr = hrUsers.get(i % hrUsers.size());
            LocalDateTime startTime = LocalDateTime.now().plusDays(1 + random.nextInt(14))
                    .withHour(9 + random.nextInt(6)).withMinute(0).withSecond(0).withNano(0);

            String round = INTERVIEW_ROUNDS[i % INTERVIEW_ROUNDS.length];
            String format = pickRandom(INTERVIEW_FORMATS);

            schedules.add(InterviewSchedule.builder()
                    .application(app)
                    .interviewRound(round)
                    .interviewFormat(format)
                    .startTime(startTime)
                    .endTime(startTime.plusMinutes(60))
                    .locationOrLink(format.equals("Online_GoogleMeet")
                            ? "https://meet.google.com/rms-interview-" + (1000 + i)
                            : "Meeting Room A" + (1 + i % 5) + ", Floor " + (3 + i % 3))
                    .interviewStatus("Scheduled")
                    .createdBy(hr)
                    .build());
        }
        interviewScheduleRepository.saveAll(schedules);
        System.out.println("  [OK] " + schedules.size() + " Interview Schedules da tao.");

        // ================================================================
        // BUOC 11: TAO 10 OFFER PROPOSALS
        //          Lay tu Application co status "Offered" hoac "Hired".
        //          @OneToOne voi Application (UNIQUE).
        // ================================================================
        List<Application> offeredApps = applications.stream()
                .filter(a -> "Offered".equals(a.getApplicationStatus()))
                .collect(Collectors.toList());

        // Neu khong du 10, bo sung tu Hired
        if (offeredApps.size() < NUM_OFFER_PROPOSALS) {
            applications.stream()
                    .filter(a -> "Hired".equals(a.getApplicationStatus()))
                    .limit((long) NUM_OFFER_PROPOSALS - offeredApps.size())
                    .forEach(offeredApps::add);
        }

        List<OfferProposal> offers = new ArrayList<>();
        int offerCount = Math.min(NUM_OFFER_PROPOSALS, offeredApps.size());

        for (int i = 0; i < offerCount; i++) {
            Application app = offeredApps.get(i);
            User hm = hiringManagers.get(i % hiringManagers.size());
            JobPosting jp = app.getJobPosting();

            BigDecimal proposedSalary = BigDecimal.valueOf(15_000_000L + random.nextInt(25_000_000));
            // ProbationSalary >= 85% ProposedSalary (theo constraint model)
            BigDecimal probationSalary = proposedSalary.multiply(BigDecimal.valueOf(0.85))
                    .setScale(2, RoundingMode.HALF_UP);

            offers.add(OfferProposal.builder()
                    .application(app)
                    .proposedSalary(proposedSalary)
                    .probationSalary(probationSalary)
                    .probationDays(60)
                    .proposedPosition(jp.getPostingTitle())
                    .workLocation(jp.getWorkLocation())
                    .proposedBy(hm)
                    .offerStatus("Sent_To_Candidate")
                    .build());
        }
        offerProposalRepository.saveAll(offers);
        System.out.println("  [OK] " + offers.size() + " Offer Proposals da tao.");

        // ================================================================
        // THONG BAO HOAN TAT
        // ================================================================
        int totalRecords = 5 + 4 + allUsers.size() + requisitions.size() + postings.size()
                + candidates.size() + resumes.size() + applications.size()
                + schedules.size() + offers.size();
        System.out.println("========================================================");
        System.out.println("  [DONE] SME SEEDING HOAN TAT! Tong: " + totalRecords + " records.");
        System.out.println("  Kiem tra tren SSMS: SELECT * FROM [User];");
        System.out.println("========================================================");
    }

    // ====================================================================
    // HELPER: Tao danh sach User theo role & department
    // ====================================================================
    private List<User> buildUsers(int count, Role role, Department dept,
                                  String usernamePrefix, String passwordHash) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> buildSingleUser(
                        count == 1 ? usernamePrefix : usernamePrefix + i,
                        role, dept, passwordHash))
                .collect(Collectors.toList());
    }

    private User buildSingleUser(String username, Role role, Department dept, String passwordHash) {
        String fullName = generateEnglishName();
        return User.builder()
                .username(username)
                .passwordHash(passwordHash)
                .email(username + "@rms-group2.com")
                .fullName(fullName)
                .phoneNumber(generatePhoneNumber())
                .avatarUrl("https://ui-avatars.com/api/?name=" + fullName.replace(" ", "+") + "&background=random")
                .role(role)
                .department(dept)
                .accountStatus("Active")
                .build();
    }

    // ====================================================================
    // HELPER: Sinh tên người Việt ngẫu nhiên (có dấu, NVARCHAR hỗ trợ Unicode)
    // ====================================================================
    private String generateEnglishName() {
        return pickRandom(FIRST_NAMES) + " " + pickRandom(LAST_NAMES);
    }

    // ====================================================================
    // HELPER: Sinh so dien thoai Viet Nam (09x, 03x, 07x, 08x)
    // ====================================================================
    private String generatePhoneNumber() {
        String[] prefixes = {"09", "03", "07", "08"};
        String prefix = pickRandom(prefixes);
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    // ====================================================================
    // HELPER: Chuyen ten thanh email slug (lowercase, dot-separated)
    // ====================================================================
    private String toEmailSlug(String name) {
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", ".")
                .toLowerCase();
        return slug;
    }

    // ====================================================================
    // HELPER: Sinh Job Description
    // ====================================================================
    private String generateJobDescription(String title) {
        return "We are looking for a talented " + title + " to join our team.\n\n"
                + "Key Responsibilities:\n"
                + "- Design, develop and maintain high-quality software solutions.\n"
                + "- Collaborate with cross-functional teams to ensure product delivery.\n"
                + "- Review code and mentor junior developers.\n"
                + "- Participate in daily Agile/Scrum meetings.\n\n"
                + "We offer:\n"
                + "- Dynamic and youthful working environment.\n"
                + "- Salary review twice a year. Attractive performance bonus.";
    }

    // ====================================================================
    // HELPER: Sinh Requirements
    // ====================================================================
    private String generateRequirements(String title) {
        return "Requirements for " + title + ":\n\n"
                + "- Bachelor degree in Computer Science, IT or related fields.\n"
                + "- Minimum 2 years of experience in an equivalent position.\n"
                + "- Proficient in at least one programming language: Java, C#, JavaScript, Python.\n"
                + "- Experience with Git, CI/CD, Docker is a plus.\n"
                + "- Good communication and teamwork skills.\n"
                + "- Ability to read and understand English technical documents.";
    }

    // ====================================================================
    // HELPER: Format hien thi muc luong
    // ====================================================================
    private String formatSalaryDisplay(BigDecimal min, BigDecimal max) {
        long minM = min.longValue() / 1_000_000;
        long maxM = max.longValue() / 1_000_000;
        return minM + " - " + maxM + " Million VND";
    }

    // ====================================================================
    // HELPER: Tao pool status cho 100 Application
    // ================================================================
    private String[] buildStatusPool() {
        List<String> pool = new ArrayList<>();
        addNTimes(pool, "Applied", 25);
        addNTimes(pool, "Screening", 15);
        addNTimes(pool, "Interview_Pending", 10);
        addNTimes(pool, "Interviewing", 20);
        addNTimes(pool, "Offered", 10);
        addNTimes(pool, "Hired", 15);
        addNTimes(pool, "Rejected", 5);
        Collections.shuffle(pool, random);
        return pool.toArray(new String[0]);
    }

    private void addNTimes(List<String> list, String value, int n) {
        for (int i = 0; i < n; i++) {
            list.add(value);
        }
    }

    // ====================================================================
    // HELPER: Chon phan tu ngau nhien tu mang
    // ====================================================================
    private String pickRandom(String[] array) {
        return array[random.nextInt(array.length)];
    }
}
