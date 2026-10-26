package com.bethelreport.backend.users.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import com.bethelreport.backend.permissions.repositories.PermissionRepository;
import com.bethelreport.backend.exceptions.UserNotFoundException;
import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.users.mappers.UserMapper;
import com.bethelreport.backend.users.mappers.UserMapperImpl;
import com.bethelreport.backend.users.repositories.UserRepository;
import com.bethelreport.backend.permissions.services.AuthorizationService;
import com.bethelreport.backend.permissions.services.PermissionService;
import org.mockito.Mockito;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;

class UserServiceTest {
  private final UserRepository userRepository = mock(UserRepository.class);
  private final UserMapper userMapper = new UserMapperImpl();
  private final PermissionService permissionService = mock(PermissionService.class);
  private final AuthorizationService authorizationService = mock(AuthorizationService.class);
  private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
  private final UserService userService = new UserService(userRepository, userMapper,
      permissionService, authorizationService);
  private final Authentication authentication = authentication("council@example.com");

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(authorizationService.canManageUsers(eq(authentication), any(Long.class))).thenReturn(true);
    when(authorizationService.isGrandeConselho(authentication)).thenReturn(false);
    when(authorizationService.hasRole(authentication,
        com.bethelreport.backend.permissions.entities.Role.CONSELHO_DE_BETHEL)).thenReturn(true);
    when(authorizationService.getAuthorizedBethelId(authentication)).thenReturn(10L);
  }

  @Test
  void createsUserSuccessfully() {
    CreateUserRequest request = new CreateUserRequest("user@example.com", "Test User",
        LocalDate.of(1990, 1, 1), "City", "Address", true, false, true, false);

    when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      saved.setId(1L);
      return saved;
    });

    UserResponse response = userService.createUser(request);

    assertEquals(1L, response.id());
    assertEquals("user@example.com", response.email());
    assertEquals("Test User", response.name());
    assertTrue(response.active());
    assertTrue(response.volunteer());
    assertTrue(response.member());
    assertFalse(response.masonicRelationship());
    assertFalse(response.sponsorship());
  }

  @Test
  void createUserThrowsOnDuplicateEmail() {
    CreateUserRequest request = new CreateUserRequest("dup@example.com", "User",
        LocalDate.of(1990, 1, 1), "City", "Address", false, false, false, false);

    when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

    assertThrows(DuplicateEmailException.class, () -> userService.createUser(request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void listsAllUsers() {
    User user = buildUser(1L, "a@example.com", true);
    when(userRepository.findAllByBethelId(10L)).thenReturn(List.of(user));

    List<UserResponse> result = userService.listUsers();

    assertEquals(1, result.size());
    assertEquals("a@example.com", result.getFirst().email());
  }

  @Test
  void getsUserById() {
    User user = buildUser(1L, "a@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    UserResponse response = userService.getUserById(1L, authentication);

    assertEquals(1L, response.id());
    assertEquals("a@example.com", response.email());
  }

  @Test
  void getUserByIdThrowsWhenNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L, authentication));
    verify(authorizationService, never()).canManageUsers(authentication, 99L);
  }

  @Test
  void deniesManagingUserFromAnotherBethel() {
    when(userRepository.findById(20L))
        .thenReturn(Optional.of(buildUser(20L, "another@example.com", true)));
    when(authorizationService.canManageUsers(authentication, 20L)).thenReturn(false);

    assertThrows(AccessDeniedException.class, () -> userService.getUserById(20L, authentication));
    verify(userRepository).findById(20L);
    verify(authorizationService).canManageUsers(authentication, 20L);
  }

  @Test
  void updatesUserSuccessfully() {
    User existing = buildUser(1L, "old@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest("new@example.com", "New Name",
        LocalDate.of(1995, 5, 5), "New City", "New Address", true, true, true, true);

    UserResponse response = userService.updateUser(1L, request, authentication);

    assertEquals("new@example.com", response.email());
    assertEquals("New Name", response.name());
    assertTrue(response.volunteer());
  }

  @Test
  void updateUserAllowsSameEmail() {
    User existing = buildUser(1L, "same@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest("same@example.com", "Updated",
        LocalDate.of(1990, 1, 1), "City", "Addr", false, false, false, false);

    UserResponse response = userService.updateUser(1L, request, authentication);

    assertEquals("same@example.com", response.email());
    verify(userRepository, never()).existsByEmailIgnoreCase(any());
  }

  @Test
  void updateUserThrowsOnDuplicateEmail() {
    User existing = buildUser(1L, "old@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

    UpdateUserRequest request = new UpdateUserRequest("taken@example.com", "Name",
        LocalDate.of(1990, 1, 1), "City", "Addr", false, false, false, false);

    assertThrows(DuplicateEmailException.class,
        () -> userService.updateUser(1L, request, authentication));
    verify(userRepository, never()).save(any());
  }

  @Test
  void activatesUser() {
    User user = buildUser(1L, "a@example.com", false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.activateUser(1L, authentication);

    assertTrue(response.active());
  }

  @Test
  void deactivatesUser() {
    User user = buildUser(1L, "a@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.deactivateUser(1L, authentication);

    assertFalse(response.active());
    Mockito.verify(permissionService).deactivateByUserId(1L);
  }

  @Test
  void activateThrowsWhenUserNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.activateUser(99L, authentication));
  }

  @Test
  void deactivateThrowsWhenUserNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class,
        () -> userService.deactivateUser(99L, authentication));
  }

  private User buildUser(Long id, String email, boolean active) {
    User user = new User();
    user.setId(id);
    user.setEmail(email);
    user.setName("Test User");
    user.setBirthday(LocalDate.of(1990, 1, 1));
    user.setActive(active);
    user.setCity("City");
    user.setAddress("Address");
    return user;
  }

  private Authentication authentication(String email) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("email", email).build();
    return new JwtAuthenticationToken(jwt);
  }
}
