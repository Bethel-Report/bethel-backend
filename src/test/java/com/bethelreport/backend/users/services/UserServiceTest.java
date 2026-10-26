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

import com.bethelreport.backend.users.dtos.CreateUserRequest;
import com.bethelreport.backend.users.dtos.UpdateUserRequest;
import com.bethelreport.backend.users.dtos.UserResponse;
import com.bethelreport.backend.users.entities.User;
import com.bethelreport.backend.exceptions.DuplicateEmailException;
import com.bethelreport.backend.exceptions.UserNotFoundException;
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

class UserServiceTest {
  private final UserRepository userRepository = mock(UserRepository.class);
  private final UserMapper userMapper = new UserMapperImpl();
  private final PermissionService permissionService = mock(PermissionService.class);
  private final AuthorizationService authorizationService = mock(AuthorizationService.class);
  private final UserService userService = new UserService(userRepository, userMapper,
      permissionService, authorizationService);

  {
    when(authorizationService.canAssignRole(any(), any(), any())).thenReturn(true);
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
    when(userRepository.findAll()).thenReturn(List.of(user));

    List<UserResponse> result = userService.listUsers();

    assertEquals(1, result.size());
    assertEquals("a@example.com", result.getFirst().email());
  }

  @Test
  void getsUserById() {
    User user = buildUser(1L, "a@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    UserResponse response = userService.getUserById(1L);

    assertEquals(1L, response.id());
    assertEquals("a@example.com", response.email());
  }

  @Test
  void getUserByIdThrowsWhenNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
  }

  @Test
  void updatesUserSuccessfully() {
    User existing = buildUser(1L, "old@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UpdateUserRequest request = new UpdateUserRequest("new@example.com", "New Name",
        LocalDate.of(1995, 5, 5), "New City", "New Address", true, true, true, true);

    UserResponse response = userService.updateUser(1L, request);

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

    UserResponse response = userService.updateUser(1L, request);

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

    assertThrows(DuplicateEmailException.class, () -> userService.updateUser(1L, request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void activatesUser() {
    User user = buildUser(1L, "a@example.com", false);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.activateUser(1L);

    assertTrue(response.active());
  }

  @Test
  void deactivatesUser() {
    User user = buildUser(1L, "a@example.com", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    UserResponse response = userService.deactivateUser(1L);

    assertFalse(response.active());
    Mockito.verify(permissionService).deactivateByUserId(1L);
  }

  @Test
  void activateThrowsWhenUserNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.activateUser(99L));
  }

  @Test
  void deactivateThrowsWhenUserNotFound() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class, () -> userService.deactivateUser(99L));
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
}
