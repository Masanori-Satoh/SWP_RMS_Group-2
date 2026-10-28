package com.group2.rms.offer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.Mockito;

@SpringBootTest
@AutoConfigureMockMvc
public class OfferControllerBugTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.group2.rms.user.repository.UserRepository userRepository;

    @Test
    @WithMockUser(username = "director_1", authorities = {"ROLE_DIRECTOR"})
    public void testDirectorOffersPage() throws Exception {
        com.group2.rms.user.entity.Role role = com.group2.rms.user.entity.Role.builder().roleName("Director").build();
        com.group2.rms.user.entity.User fakeUser = new com.group2.rms.user.entity.User();
        fakeUser.setUserId(10);
        fakeUser.setUsername("director_1");
        fakeUser.setFullName("Director 1");
        fakeUser.setAccountStatus("Active");
        fakeUser.setRole(role);
        
        org.mockito.Mockito.when(userRepository.findByUsernameIgnoreCase("director_1"))
                .thenReturn(java.util.Optional.of(fakeUser));

        mockMvc.perform(get("/offers"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("offers/director-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeExists("statusCounts"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("currentStatus", "Pending_Director"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("currentTab", "PENDING"));
    }

    @Test
    @WithMockUser(username = "director_1", authorities = {"ROLE_DIRECTOR"})
    public void testDirectorOffersPage_withStatusFilter() throws Exception {
        com.group2.rms.user.entity.Role role = com.group2.rms.user.entity.Role.builder().roleName("Director").build();
        com.group2.rms.user.entity.User fakeUser = new com.group2.rms.user.entity.User();
        fakeUser.setUserId(10);
        fakeUser.setUsername("director_1");
        fakeUser.setFullName("Director 1");
        fakeUser.setAccountStatus("Active");
        fakeUser.setRole(role);
        
        org.mockito.Mockito.when(userRepository.findByUsernameIgnoreCase("director_1"))
                .thenReturn(java.util.Optional.of(fakeUser));

        mockMvc.perform(get("/offers").param("status", "Accepted"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("offers/director-list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeExists("statusCounts"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("currentStatus", "Accepted"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("currentTab", "HISTORY"));
    }

    @Test
    @WithMockUser(username = "hr_1", authorities = {"ROLE_HR"})
    public void testCreateOfferForm_withoutApplicationId_shouldNotLockCandidate() throws Exception {
        com.group2.rms.user.entity.Role role = com.group2.rms.user.entity.Role.builder().roleName("HR").build();
        com.group2.rms.user.entity.User fakeUser = new com.group2.rms.user.entity.User();
        fakeUser.setUserId(20);
        fakeUser.setUsername("hr_1");
        fakeUser.setFullName("HR Specialist");
        fakeUser.setAccountStatus("Active");
        fakeUser.setRole(role);

        org.mockito.Mockito.when(userRepository.findByUsernameIgnoreCase("hr_1"))
                .thenReturn(java.util.Optional.of(fakeUser));

        mockMvc.perform(get("/offers/create"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("offers/form"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("isLockedCandidate", false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attribute("isEdit", false));
    }
}
