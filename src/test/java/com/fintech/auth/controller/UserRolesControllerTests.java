package com.fintech.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.auth.config.ServiceConfig;
import com.fintech.auth.dto.SaveRoleToUserDTO;
import com.fintech.auth.dto.UserWithRolesDTO;
import com.fintech.auth.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({ServiceConfig.class})
class UserRolesControllerTests {

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    public static void setTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetUserByUsernameReturnsUserWithItsRolesByUsername() throws Exception {
        SaveRoleToUserDTO saveRoleToUserDTO = new SaveRoleToUserDTO();
        saveRoleToUserDTO.setUsername("test");
        saveRoleToUserDTO.setRoleIds(Set.of("TEST ROLE"));

        UserWithRolesDTO userWithRolesDTO = new UserWithRolesDTO();
        userWithRolesDTO.setUsername("test");
        userWithRolesDTO.setRoleIds(Set.of("TEST ROLE"));

        when(roleService.getUserWithRolesByUsername(saveRoleToUserDTO.getUsername())).thenReturn(userWithRolesDTO);

        mockMvc.perform(get("/user-roles/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveRoleToUserDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test"))
                .andExpect(jsonPath("$.roles[0]").value("TEST ROLE"));
    }

    @Test
    void testGetUserReturnsUserWithItsRoles() throws Exception {
        setupSecurityContext();
        UserWithRolesDTO userWithRolesDTO = new UserWithRolesDTO();
        userWithRolesDTO.setUsername("testUser");
        userWithRolesDTO.setRoleIds(Set.of("USER"));

        when(roleService.getUserWithRolesByUsername("testUser")).thenReturn(userWithRolesDTO);

        mockMvc.perform(get("/user-roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    private void setupSecurityContext() {
        UserDetails user = User.withUsername("testUser")
                .password("password")
                .authorities(new SimpleGrantedAuthority("USER"))
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

}