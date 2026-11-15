package com.group2.rms.interview.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Dữ liệu dropdown cho form lịch phỏng vấn và tra cứu người dùng, để controller không gọi thẳng repository. */
@Service
@RequiredArgsConstructor
public class InterviewFormOptions {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    /** Hồ sơ đủ điều kiện xếp lịch; khi sửa lịch thì giữ cả hồ sơ hiện tại dù không còn đủ điều kiện. */
    public List<Application> eligibleApplications(Integer currentApplicationId) {
        List<Application> applications = new ArrayList<>(applicationRepository.findEligibleForInterview());
        if (currentApplicationId != null
                && applications.stream().noneMatch(a -> a.getApplicationId().equals(currentApplicationId))) {
            applicationRepository.findDetailById(currentApplicationId).ifPresent(applications::add);
        }
        return applications;
    }

    /** Mọi tài khoản nội bộ đang Active (kể cả HR khác, Director), trừ hiddenUserId nếu có. */
    public List<User> availableInterviewers(Integer hiddenUserId) {
        return userRepository.findAllWithRoleAndDepartment().stream()
                .filter(u -> u.getRole() != null
                        && RoleAuthorities.INTERNAL_ROLE_NAMES.contains(u.getRole().getRoleName()))
                .filter(u -> "Active".equalsIgnoreCase(u.getAccountStatus()))
                .filter(u -> hiddenUserId == null || !hiddenUserId.equals(u.getUserId()))
                .toList();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    /** Fallback môi trường demo/dev khi chưa có session: HR đầu tiên, không có thì user đầu tiên. */
    public User fallbackUser() {
        List<User> all = userRepository.findAll();
        return all.stream()
                .filter(u -> u.getRole() != null && "HR".equalsIgnoreCase(u.getRole().getRoleName()))
                .findFirst()
                .orElseGet(() -> all.stream().findFirst().orElse(null));
    }
}
