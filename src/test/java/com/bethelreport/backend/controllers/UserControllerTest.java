package com.bethelreport.backend.controllers;

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
import com.bethelreport.backend.users.controllers.UserController;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private AdmissionService admissionService;

  @BeforeEach
  void setUp() {
    when(admissionService.admittedUser(any())).thenReturn(true);
  }

  @Test
  void createUserReturnsCreated() throws Exception {
    CreateUserRequest request = new CreateUserRequest("user@example.com", "Test User",
        LocalDate.of(1990, 1, 1), "City", "Address", false, false, false, false);
    UserResponse response = userResponse(1L, "user@example.com", true);

    when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

    mockMvc
        .perform(post("/api/users").with(jwt()).contentType(MediaType.APPLICATION_JSON)
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
        .perform(post("/api/users").with(jwt()).contentType(MediaType.APPLICATION_JSON)
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
        .perform(post("/api/users").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
  }

  @Test
  void listUsersReturnsOk() throws Exception {
    when(userService.listUsers()).thenReturn(List.of(userResponse(1L, "a@example.com", true)));

    mockMvc.perform(get("/api/users").with(jwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].email").value("a@example.com"));
  }

  @Test
  void getUserByIdReturnsOk() throws Exception {
    when(userService.getUserById(1L)).thenReturn(userResponse(1L, "a@example.com", true));

    mockMvc.perform(get("/api/users/1").with(jwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void getUserByIdNotFoundReturnsNotFound() throws Exception {
    when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

    mockMvc.perform(get("/api/users/99").with(jwt())).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void updateUserReturnsOk() throws Exception {
    UpdateUserRequest request = new UpdateUserRequest("updated@example.com", "Updated",
        LocalDate.of(1995, 5, 5), "New City", "New Addr", true, false, true, false);
    UserResponse response = userResponse(1L, "updated@example.com", true);

    when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(response);

    mockMvc
        .perform(put("/api/users/1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("updated@example.com"));
  }

  @Test
  void updateUserWithInvalidDataReturnsBadRequest() throws Exception {
    UpdateUserRequest invalid = new UpdateUserRequest("", "", null, "", "", false, false, false,
        false);

    mockMvc
        .perform(put("/api/users/1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").exists());
  }

  @Test
  void updateUserWithDuplicateEmailReturnsConflict() throws Exception {
    UpdateUserRequest request = new UpdateUserRequest("taken@example.com", "Name",
        LocalDate.of(1990, 1, 1), "City", "Addr", false, false, false, false);

    when(userService.updateUser(eq(1L), any(UpdateUserRequest.class)))
        .thenThrow(new DuplicateEmailException("taken@example.com"));

    mockMvc.perform(put("/api/users/1").with(jwt()).contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());
  }

  @Test
  void activateUserReturnsOk() throws Exception {
    when(userService.activateUser(1L)).thenReturn(userResponse(1L, "a@example.com", true));

    mockMvc.perform(patch("/api/users/1/activate").with(jwt())).andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void deactivateUserReturnsOk() throws Exception {
    when(userService.deactivateUser(1L)).thenReturn(userResponse(1L, "a@example.com", false));

    mockMvc.perform(patch("/api/users/1/deactivate").with(jwt())).andExpect(status().isOk())
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

  private UserResponse userResponse(Long id, String email, boolean active) {
    return new UserResponse(id, email, "Test User", LocalDate.of(1990, 1, 1), active, "City",
        "Address", false, false, false, false);
  }
}
