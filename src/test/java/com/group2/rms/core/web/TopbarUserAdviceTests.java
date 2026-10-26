package com.group2.rms.core.web;

import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.user.service.LocalAvatarStorage;
import com.group2.rms.user.service.UserProfileService;
import com.group2.rms.user.validator.AvatarFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Ảnh trên topbar: chỉ ảnh tải lên có file trên máy mới thành link; còn lại null (chữ cái đầu). */
class TopbarUserAdviceTests {

    @TempDir
    Path uploads;

    private final UserRepository users = mock(UserRepository.class);
    private final User hr = User.builder().userId(3).username("hr_lan").email("lan@x.test").fullName("Lê Thị Mai Lan")
            .role(Role.builder().roleName("HR").build()).build();
    private LocalAvatarStorage storage;
    private TopbarUserAdvice advice;

    @BeforeEach
    void setUp() {
        storage = new LocalAvatarStorage(uploads.toString());
        UserProfileService profiles = new UserProfileService(users, mock(PasswordEncoder.class), storage, new AvatarFileValidator());
        advice = new TopbarUserAdvice(users, provider(profiles));
        when(users.findByUsernameIgnoreCase("hr_lan")).thenReturn(Optional.of(hr));
    }

    @Test
    void seedUiAvatarsLinkIsIgnored() {
        hr.setAvatarUrl("https://ui-avatars.com/api/?name=Lan&background=random");
        assertNull(advice.topbarUser(signedIn()).avatarUrl());
    }

    @Test
    void uploadedAvatarWithFileBecomesViewLink() {
        String key = storage.store(new MockMultipartFile("avatarFile", "a.png", "image/png", new byte[]{1}), 3, "png");
        hr.setAvatarUrl(LocalAvatarStorage.URL_PREFIX + key);
        String uuid = key.substring(key.lastIndexOf('/') + 1, key.lastIndexOf('.'));

        TopbarUser topbar = advice.topbarUser(signedIn());
        assertEquals("/users/3/avatar?v=" + uuid, topbar.avatarUrl());
        assertEquals("HR", topbar.roleName());
    }

    @Test
    void uploadedAvatarMissingOnThisMachineFallsBackToInitials() {
        hr.setAvatarUrl("local:avatar/3/0b9c2f1e-0000-0000-0000-000000000000.png");
        assertNull(advice.topbarUser(signedIn()).avatarUrl());
    }

    @Test
    void guestHasNoTopbarUser() {
        var guest = new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        assertNull(advice.topbarUser(guest));
        assertNull(advice.topbarUser(null));
    }

    @Test
    void withoutProfileServiceBeanAvatarIsNull() {
        hr.setAvatarUrl("local:avatar/3/x.png");
        TopbarUserAdvice slice = new TopbarUserAdvice(users, new StaticListableBeanFactory().getBeanProvider(UserProfileService.class));
        assertNull(slice.topbarUser(signedIn()).avatarUrl());
    }

    private static UsernamePasswordAuthenticationToken signedIn() {
        return new UsernamePasswordAuthenticationToken("hr_lan", null, List.of());
    }

    private static ObjectProvider<UserProfileService> provider(UserProfileService service) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("userProfileService", service);
        return factory.getBeanProvider(UserProfileService.class);
    }
}
