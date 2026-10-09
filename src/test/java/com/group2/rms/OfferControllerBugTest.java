package com.group2.rms;

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
public class OfferControllerBugTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.group2.rms.user.repository.UserRepository userRepository;

    @Test
    @WithMockUser(username = "director_1", authorities = {"ROLE_DIRECTOR"})
    public void testDirectorOffersPage() throws Exception {
        com.group2.rms.user.entity.User fakeUser = new com.group2.rms.user.entity.User();
        fakeUser.setUserId(10);
        fakeUser.setUsername("director_1");
        fakeUser.setFullName("Director 1");
        
        org.mockito.Mockito.when(userRepository.findByUsernameIgnoreCase("director_1"))
                .thenReturn(java.util.Optional.of(fakeUser));

        MvcResult result = mockMvc.perform(get("/offers"))
                .andReturn();
        
        System.out.println("HTTP_STATUS_CODE: " + result.getResponse().getStatus());
        System.out.println("LOCATION: " + result.getResponse().getHeader("Location"));
        if (result.getResolvedException() != null) {
            result.getResolvedException().printStackTrace();
        } else {
            System.out.println("RESPONSE_BODY: " + result.getResponse().getContentAsString());
        }
    }
}
