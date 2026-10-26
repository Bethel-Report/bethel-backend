package com.bethelreport.backend.users.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bethelreport.backend.config.SecurityConfig;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.bethel.entities.Bethel;
import com.bethelreport.backend.permissions.entities.Permission;
import com.bethelreport.backend.permissions.entities.Role;
import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.users.services.AdmissionService;
import com.bethelreport.backend.users.services.UserService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AuthorizationService.class})
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private AdmissionService admissionService;

  @MockitoBean
  private PermissionRepository permissionRepository;

  @BeforeEach
  void setUp() {
    when(admissionService.admittedUser(any())).thenReturn(true);
    Permission councilPermission = new Permission();
    councilPermission.setRole(Role.CONSELHO_DE_BETHEL);
    Bethel bethel = new Bethel();
    bethel.setId(10L);
    councilPermission.setBethel(bethel);
    when(permissionRepository.findByUserEmail("council@example.com"))
        .thenReturn(java.util.Optional.of(councilPermission));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor councilJwt() {
    return jwt().jwt(token -> token.claim("email", "council@example.com"));
  }

  @Test
  void createUserReturnsCreated() throws Exception {
    CreateUserRequest request = new CreateUserRequest("user@example.com", "Test User",
        LocalDate.of(1990, 1, 1), "City", "Address", false, false, false, false);
    UserResponse response = userResponse(1L, "user@example.com", true);

    when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

    mockMvc
        .perform(post("/api/users").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void createUserWithInvalidDataReturnsBadRequest() throws Exception {
    CreateUserRequest invalid = new CreateUserRequest("", "", null, "", "", false, false, false,
        false);

    mockMvc
        .perform(post("/api/users").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void createUserWithDuplicateEmailReturnsConflict() throws Exception {
    CreateUserRequest request = new CreateUserRequest("dup@example.com", "User",
        LocalDate.of(1990, 1, 1), "City", "Address", false, false, false, false);

    when(userService.createUser(any(CreateUserRequest.class)))
        .thenThrow(new DuplicateEmailException("dup@example.com"));

    mockMvc
        .perform(post("/api/users").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
  }

  @Test
  void listUsersReturnsOk() throws Exception {
    when(userService.listUsers()).thenReturn(List.of(userResponse(1L, "a@example.com", true)));

    mockMvc.perform(get("/api/users").with(councilJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("a@example.com"));
  }

  @Test
  void getUserByIdReturnsOk() throws Exception {
    when(userService.getUserById(eq(1L), any(Authentication.class)))
        .thenReturn(userResponse(1L, "a@example.com", true));

    mockMvc.perform(get("/api/users/1").with(councilJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void getUserByIdNotFoundReturnsNotFound() throws Exception {
    when(userService.getUserById(eq(99L), any(Authentication.class)))
        .thenThrow(new UserNotFoundException(99L));

    mockMvc.perform(get("/api/users/99").with(councilJwt())).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void updateUserReturnsOk() throws Exception {
    UpdateUserRequest request = new UpdateUserRequest("updated@example.com", "Updated",
        LocalDate.of(1995, 5, 5), "New City", "New Addr", true, false, true, false);
    UserResponse response = userResponse(1L, "updated@example.com", true);

    when(userService.updateUser(eq(1L), any(UpdateUserRequest.class), any(Authentication.class)))
        .thenReturn(response);

    mockMvc
        .perform(put("/api/users/1").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("updated@example.com"));
  }

  @Test
  void updateUserWithInvalidDataReturnsBadRequest() throws Exception {
    UpdateUserRequest invalid = new UpdateUserRequest("", "", null, "", "", false, false, false,
        false);

    mockMvc
        .perform(put("/api/users/1").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void updateUserWithDuplicateEmailReturnsConflict() throws Exception {
    UpdateUserRequest request = new UpdateUserRequest("taken@example.com", "Name",
        LocalDate.of(1990, 1, 1), "City", "Addr", false, false, false, false);

    when(userService.updateUser(eq(1L), any(UpdateUserRequest.class), any(Authentication.class)))
        .thenThrow(new DuplicateEmailException("taken@example.com"));

    mockMvc.perform(put("/api/users/1").with(councilJwt()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
  }

  @Test
  void activateUserReturnsOk() throws Exception {
    when(userService.activateUser(eq(1L), any(Authentication.class)))
        .thenReturn(userResponse(1L, "a@example.com", true));

    mockMvc.perform(patch("/api/users/1/activate").with(councilJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void deactivateUserReturnsOk() throws Exception {
    when(userService.deactivateUser(eq(1L), any(Authentication.class)))
        .thenReturn(userResponse(1L, "a@example.com", false));

    mockMvc.perform(patch("/api/users/1/deactivate").with(councilJwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void inactiveUserIsDeniedAccess() throws Exception {
    reset(admissionService);
    when(admissionService.admittedUser(any())).thenReturn(false);

    mockMvc.perform(get("/api/users").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void unauthenticatedRequestReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void memberCannotCreateUser() throws Exception {
    Permission memberPermission = new Permission();
    memberPermission.setRole(Role.MEMBRO_ATIVO);
    when(permissionRepository.findByUserEmail("member@example.com"))
        .thenReturn(java.util.Optional.of(memberPermission));
    mockMvc
        .perform(
            post("/api/users").with(jwt().jwt(token -> token.claim("email", "member@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper
                    .writeValueAsString(new CreateUserRequest("new@example.com", "New Member",
                        LocalDate.of(2000, 1, 1), "City", "Address", false, false, false, false))))
        .andExpect(status().isForbidden());
  }

  private UserResponse userResponse(Long id, String email, boolean active) {
    return new UserResponse(id, email, "Test User", LocalDate.of(1990, 1, 1), active, "City",
        "Address", false, false, false, false);
  }
}
